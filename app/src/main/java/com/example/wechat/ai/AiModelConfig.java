package com.example.wechat.ai;

import androidx.annotation.NonNull;

/**
 * 一次 AI 调用需要的全部参数，做成不可变值对象。
 *
 * 为什么单独拆一个类而不是写成一堆常量：
 * 将来「不同会话用不同服务商 / 不同模型」时，变的只是这个对象里的字段，
 * 调用方（AiReplyManager → ChatProvider）拿到的形状不变，不用跟着改。
 * 现在字段里只有一套默认值，但形状已经是对的。
 */
public final class AiModelConfig {

    /**
     * 服务商标识。
     * - {@link #PROVIDER_ZHIPU}：App 自己拿密钥直连厂商，密钥会被编进 dex，只适合本地调试
     * - {@link #PROVIDER_PROXY}：App 只连自己的中转服务，密钥留在服务端，这是正式走法
     * 以后再加别的服务商在这里加常量即可。
     */
    public static final String PROVIDER_ZHIPU = "zhipu";
    public static final String PROVIDER_PROXY = "proxy";

    private final String providerId;
    private final String modelId;
    private final String baseUrl;
    private final String apiKey;
    /**
     * 中转服务的访问令牌（X-App-Token）。
     * 和 apiKey 的区别：这个值泄露不会连累厂商密钥 —— 自己换一个就行。
     * 直连模式下它是空字符串。
     */
    private final String accessToken;
    private final String systemPrompt;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int historyLimit;

    /** 直连模式用：不带令牌 */
    public AiModelConfig(String providerId,
                         String modelId,
                         String baseUrl,
                         String apiKey,
                         String systemPrompt,
                         int connectTimeoutMs,
                         int readTimeoutMs,
                         int historyLimit) {
        this(providerId, modelId, baseUrl, apiKey, "", systemPrompt,
                connectTimeoutMs, readTimeoutMs, historyLimit);
    }

    /** 中转模式用：带令牌、但不需要厂商密钥 */
    public AiModelConfig(String providerId,
                         String modelId,
                         String baseUrl,
                         String apiKey,
                         String accessToken,
                         String systemPrompt,
                         int connectTimeoutMs,
                         int readTimeoutMs,
                         int historyLimit) {
        this.providerId = providerId;
        this.modelId = modelId;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.accessToken = accessToken == null ? "" : accessToken;
        this.systemPrompt = systemPrompt == null ? "" : systemPrompt;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.historyLimit = historyLimit;
    }

    @NonNull
    public String getProviderId() {
        return providerId;
    }

    @NonNull
    public String getModelId() {
        return modelId;
    }

    @NonNull
    public String getBaseUrl() {
        return baseUrl;
    }

    /** 只在真正发请求时读取，任何日志 / 界面文案都不要直接输出它 */
    @NonNull
    public String getApiKey() {
        return apiKey;
    }

    /** 中转服务的令牌；同样不要出现在日志里 */
    @NonNull
    public String getAccessToken() {
        return accessToken;
    }

    @NonNull
    public String getSystemPrompt() {
        return systemPrompt;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    /** 发给模型时最多带几条本会话的消息，含当前这条（它永远排在最后） */
    public int getHistoryLimit() {
        return historyLimit;
    }

    /** 密钥为空 / 只有空白字符时视为未配置，调用方应走提示分支而不是发请求 */
    public boolean isConfigured() {
        return apiKey.trim().length() > 0;
    }

    /**
     * 中转模式的可用判据：地址和服务令牌都要有。
     *
     * 为什么和 {@link #isConfigured()} 分开：
     * 两条通道需要的凭证根本不是同一种东西 —— 一个是厂商密钥，一个是自己服务的令牌。
     * 合成一个布尔量会让「到底缺了哪个」变得含糊，报错也就说不明白了。
     */
    public boolean hasProxy() {
        return baseUrl.trim().length() > 0 && accessToken.trim().length() > 0;
    }

    /**
     * 打码后的描述，只用于 Logcat。
     * 刻意不输出密钥本身，避免调试时把 Key 复制到日志里。
     */
    @NonNull
    public String describeSafe() {
        String credential;
        if (PROVIDER_PROXY.equals(providerId)) {
            credential = "proxyConfigured=" + hasProxy();
        } else {
            credential = "keyConfigured=" + isConfigured();
        }
        return "provider=" + providerId
                + ", model=" + modelId
                + ", " + credential
                + ", connect=" + connectTimeoutMs + "ms"
                + ", read=" + readTimeoutMs + "ms"
                + ", history=" + historyLimit;
    }
}
