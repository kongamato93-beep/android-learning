package com.example.wechat.ai;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.wechat.BuildConfig;

/**
 * AI 配置的唯一入口。
 *
 * 现在有两条调用通道，用一个开关切换：
 *
 * ┌ 中转模式（{@link #USE_PROXY} = true，默认）──────────────────────┐
 * │  App → 自己的 server/proxy.mjs → 模型厂商                        │
 * │  只需要：服务地址 AI_PROXY_URL + 自己的令牌 AI_PROXY_TOKEN        │
 * │  厂商密钥永远不上手机，只存在于服务器进程里                       │
 * └─────────────────────────────────────────────────────────┘
 * ┌ 直连模式（USE_PROXY = false，仅供本地调试）───────────────────────┐
 * │  App → 模型厂商                                                   │
 * │  ZHIPU_API_KEY 会被 buildConfigField 编进 dex，反编译可读         │
 * └─────────────────────────────────────────────────────────┘
 *
 * 两者的值都来自 local.properties（已被 .gitignore 忽略），源码里没有真实凭据。
 *
 * 还有一点要说清楚：**中转也不是绝对安全**。
 * 令牌同样会被编进 APK，别人拿到后可以拿它蹭你的额度 —— 只是换一个令牌的成本，
 * 远低于换一个已经烧出账单的厂商密钥。
 * 真正的公网方案是把「App 自己的账户体系」引入：用户登录后服务端下发短期令牌。
 * 做到那一步只需要改 {@link #proxyConfig()} 里令牌的取值来源，其余代码不动。
 */
public final class AiConfig {

    private static final String TAG = "AiConfig";

    /**
     * 走哪条通道：true = 自己的服务器中转（推荐），false = 直连厂商（只用来本地调试）。
     *
     * 做成编译期常量而不是运行时开关，是因为这不该让最终用户选 ——
     * 选了直连就等于把密钥风险一起带上，属于只有开发者才需要碰的分支。
     */
    private static final boolean USE_PROXY = true;

    /** 免费模型，响应快，适合聊天场景；中转模式下这只是给日志看的，实际由服务端决定 */
    public static final String DEFAULT_MODEL_ID = "glm-4-flash";

    /** 直连模式用的 Chat Completions 兼容端点，末尾带斜杠，拼路径时不再重复加 */
    public static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/";

    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
    public static final int DEFAULT_READ_TIMEOUT_MS = 60_000;

    /** 最近 N 条历史；10 条约等于 5 轮对话，够用又不会把 token 撑爆 */
    public static final int DEFAULT_HISTORY_LIMIT = 10;

    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是 aichat 里的一个 AI 助手。回答用简体中文，简洁自然，像真人聊天，"
                    + "不要使用 Markdown 标题和列表，不要自称人工智能模型。";

    private static volatile AiModelConfig sDefault;

    private AiConfig() {
    }

    @NonNull
    public static AiModelConfig getDefault() {
        if (sDefault == null) {
            synchronized (AiConfig.class) {
                if (sDefault == null) {
                    sDefault = pick();
                }
            }
        }
        return sDefault;
    }

    /**
     * 决定这次用哪条通道，并处理「中转没配好但还想先用着」的情况。
     *
     * 降级规则只有一条：**中转缺配置、而本地正好有直连密钥时，退回直连并打 WARN 日志**。
     * 为什么要兜底 —— 不想让「忘了起服务」变成一次完全没法对话的调试事故。
     * 为什么又要打日志 —— 静默切走会让「到底有没有在走中转」变成猜谜，
     * Logcat 里一定会看到那行降级提示，排查时不用猜。
     */
    private static AiModelConfig pick() {
        if (!USE_PROXY) {
            return directConfig();
        }
        AiModelConfig proxy = proxyConfig();
        if (proxy.hasProxy()) {
            return proxy;
        }
        AiModelConfig direct = directConfig();
        if (direct.isConfigured()) {
            Log.w(TAG, "中转服务未配置，本次降级为直连模式。"
                    + "注意：直连会把密钥编进 APK，只适合本机调试，不要用于要发出去的包。");
            return direct;
        }
        // 两条路都没配：原样返回中转配置，调用方会拿到一个明确的 NOT_CONFIGURED，
        // 比返回 null 或者随便挑一个要好排查。
        return proxy;
    }

    /** 中转模式的配置：不需要厂商密钥，只要服务地址和自己的令牌 */
    private static AiModelConfig proxyConfig() {
        return new AiModelConfig(
                AiModelConfig.PROVIDER_PROXY,
                DEFAULT_MODEL_ID,
                BuildConfig.AI_PROXY_URL,
                "",                              // 厂商密钥：中转模式用不到
                BuildConfig.AI_PROXY_TOKEN,
                DEFAULT_SYSTEM_PROMPT,
                DEFAULT_CONNECT_TIMEOUT_MS,
                DEFAULT_READ_TIMEOUT_MS,
                DEFAULT_HISTORY_LIMIT);
    }

    /** 直连模式的配置：需要厂商密钥，不要用于正式发布 */
    private static AiModelConfig directConfig() {
        return new AiModelConfig(
                AiModelConfig.PROVIDER_ZHIPU,
                DEFAULT_MODEL_ID,
                DEFAULT_BASE_URL,
                BuildConfig.ZHIPU_API_KEY,
                DEFAULT_SYSTEM_PROMPT,
                DEFAULT_CONNECT_TIMEOUT_MS,
                DEFAULT_READ_TIMEOUT_MS,
                DEFAULT_HISTORY_LIMIT);
    }

    /**
     * 按会话取配置 —— 这是「不同会话用不同服务商 / 模型」的扩展点。
     *
     * 第一版所有会话共用一份配置，所以这里直接返回默认值。
     * 将来会话表带上 provider_id / model_id 之后，只需要改这一个方法，
     * AiReplyManager 和 ChatActivity 都不用动。
     */
    @NonNull
    public static AiModelConfig resolve(String conversationId) {
        return getDefault();
    }

    /** 当前这份配置的凭证是否齐全（按它自己那条通道判断，中转看令牌、直连看密钥） */
    public static boolean isConfigured() {
        AiModelConfig config = getDefault();
        return AiModelConfig.PROVIDER_PROXY.equals(config.getProviderId())
                ? config.hasProxy()
                : config.isConfigured();
    }
}
