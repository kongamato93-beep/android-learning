package com.example.wechat.ai;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 走自己服务器中转的模型调用实现 —— 这是**正式推荐**的那条路。
 *
 * 和 {@link ZhipuProvider}（直连）的区别只在于两件事：
 *   1. 请求发给自己的中转服务，不带厂商 key，改带一个自定义令牌；
 *   2. 返回只需要解 {@code reply / model / usage} 三个字段，比厂商原始响应简单得多。
 * 其余（异步、超时、取消、线程）完全一致。
 *
 * 为什么值得多这一层：
 *   直连模式下 key 要靠 buildConfigField 编进 APK，dex 里能直接搜到明文。
 *   中转之后 APK 里只有「服务地址 + 令牌」：
 *     - 令牌泄露只需自己换一个，不会连累厂商密钥；
 *     - 限额流的情况下可以在服务端限中间件做，不必改 App；
 *     - 换模型厂商时只改服务端，App 一行不动。
 *
 * 服务端契约见 {@code server/README.md}：
 *   POST  {baseUrl}/v1/chat   请求头 X-App-Token: <token>
 *   成功  { reply, model, usage:{ total_tokens } }
 *   失败  { error:{ code, message } }
 */
public final class ProxyProvider implements ChatProvider {

    private static final String TAG = "ProxyProvider";
    /** 服务端承暴露的路径；baseUrl 里不要重复写这一段 */
    private static final String PATH_CHAT = "v1/chat";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int LOG_BODY_LIMIT = 200;

    /** 共享连接池和线程池；超时按每次调用的 config 覆盖 */
    private final OkHttpClient baseClient;

    public ProxyProvider() {
        baseClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
    }

    @NonNull
    @Override
    public String getProviderId() {
        return AiModelConfig.PROVIDER_PROXY;
    }

    @NonNull
    @Override
    public AiRequest chat(@NonNull AiModelConfig config,
                          @NonNull List<ChatMessage> messages,
                          @NonNull final ChatProvider.Callback callback) {
        // 地址和令牌缺一样就没法发，先在客户端挡住，省一次无谓的网络往返
        if (!config.hasProxy()) {
            callback.onFailure(new AiError(AiError.Code.NOT_CONFIGURED,
                    "中转服务未配置，请在 local.properties 里填写 AI_PROXY_URL 和 AI_PROXY_TOKEN"));
            return new AiRequests.NoopRequest();
        }

        final String requestBody;
        try {
            requestBody = buildRequestBody(messages);
        } catch (JSONException e) {
            callback.onFailure(new AiError(AiError.Code.UNKNOWN, "请求体组装失败: " + e.getMessage()));
            return new AiRequests.NoopRequest();
        }

        OkHttpClient client = baseClient.newBuilder()
                .connectTimeout(config.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(joinUrl(config.getBaseUrl(), PATH_CHAT))
                // 令牌只用来证明「这是我自己的 App」，里面没有任何厂商密钥
                .addHeader("X-App-Token", config.getAccessToken())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON))
                .build();

        final Call call = client.newCall(request);
        final long startAt = System.currentTimeMillis();

        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call c, @NonNull IOException e) {
                long cost = System.currentTimeMillis() - startAt;
                if (c.isCanceled()) {
                    Log.d(TAG, "request cancelled, cost=" + cost + "ms");
                    callback.onFailure(new AiError(AiError.Code.CANCELLED, "已取消"));
                    return;
                }
                if (e instanceof SocketTimeoutException) {
                    Log.w(TAG, "timeout cost=" + cost + "ms: " + e.getMessage());
                    callback.onFailure(new AiError(AiError.Code.TIMEOUT, "请求超时"));
                    return;
                }
                if (e instanceof ConnectException) {
                    // 本机服务没起是最常见的情况，给一句能直接照做的提示
                    Log.w(TAG, "service unreachable cost=" + cost + "ms: " + e.getMessage());
                    callback.onFailure(new AiError(AiError.Code.NETWORK,
                            "连不上中转服务，先确认本机 server/proxy.mjs 已经在运行"));
                    return;
                }
                Log.w(TAG, "network error cost=" + cost + "ms: " + e.getMessage());
                callback.onFailure(new AiError(AiError.Code.NETWORK, "网络异常，请检查网络连接"));
            }

            @Override
            public void onResponse(@NonNull Call c, @NonNull Response response) {
                long cost = System.currentTimeMillis() - startAt;
                ResponseBody body = response.body();
                String raw = "";
                try {
                    if (body != null) {
                        raw = body.string();
                    }
                    if (!response.isSuccessful()) {
                        callback.onFailure(toServiceError(response.code(), raw, cost));
                        return;
                    }
                    callback.onSuccess(parseReply(raw, cost), parseModel(raw));
                } catch (JSONException e) {
                    Log.w(TAG, "parse failed cost=" + cost + "ms: " + e.getMessage());
                    callback.onFailure(new AiError(AiError.Code.PARSE, "返回内容解析失败"));
                } catch (IOException e) {
                    Log.w(TAG, "read body failed: " + e.getMessage());
                    callback.onFailure(new AiError(AiError.Code.NETWORK, "读取响应失败"));
                } catch (AiRequests.EmptyReplyException e) {
                    Log.w(TAG, "empty reply cost=" + cost + "ms");
                    callback.onFailure(new AiError(AiError.Code.EMPTY, "模型没有返回内容"));
                } finally {
                    response.close();
                }
            }
        });

        return new AiRequests.CallRequest(call);
    }

    /**
     * 拼接「服务基地址 + 路径」。
     *
     * 为什么要单独处理：baseUrl 写成 "http://10.0.2.2:8787" 还是 "...8787/" 都见过，
     * 手抖多写或少写一个斜杠很常见，这里统一兜掉，
     * 免得配错一次就收到 404 却看不出到底错在哪。
     */
    private static String joinUrl(String base, String path) {
        String trimmed = base == null ? "" : base.trim();
        if (trimmed.isEmpty()) {
            return path;
        }
        return trimmed.endsWith("/") ? trimmed + path : trimmed + "/" + path;
    }

    /**
     * 组装发给中转服务的请求体。
     *
     * 刻意**不传 model**：用哪个模型由服务端说了算。
     * 否则拿到令牌的人可以拿你的额度去调最贵的那个模型，账单会很意外。
     */
    private static String buildRequestBody(List<ChatMessage> messages) throws JSONException {
        JSONObject root = new JSONObject();
        JSONArray array = new JSONArray();
        for (ChatMessage m : messages) {
            JSONObject item = new JSONObject();
            item.put("role", m.getRole());
            item.put("content", m.getContent());
            array.put(item);
        }
        root.put("messages", array);
        return root.toString();
    }

    /** 解服务端裁剪过的最小响应：只有正文、模型名、用量 */
    private static String parseReply(String raw, long cost)
            throws JSONException, AiRequests.EmptyReplyException {
        JSONObject root = new JSONObject(raw);
        String content = root.optString("reply", "");
        content = content == null ? "" : content.trim();

        int totalTokens = -1;
        JSONObject usage = root.optJSONObject("usage");
        if (usage != null) {
            totalTokens = usage.optInt("total_tokens", -1);
        }

        Log.d(TAG, "reply received: model=" + root.optString("model", "unknown")
                + ", tokens=" + totalTokens
                + ", cost=" + cost + "ms"
                + ", length=" + content.length());

        if (content.isEmpty()) {
            throw new AiRequests.EmptyReplyException();
        }
        return content;
    }

    private static String parseModel(String raw) {
        try {
            return new JSONObject(raw).optString("model", "");
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * 把中转服务的错误翻译成用户能看懂的话。
     *
     * 服务那边已经在详情页帮你把上游错误归过一次类了，
     * 这里主要是补上「该去检查什么」 —— 同样是 401，
     * 「令牌无效」和「服务端没配令牌」要采取的行动完全不同。
     */
    private static AiError toServiceError(int status, String raw, long cost) {
        String serverMessage = "";
        String serverCode = "";
        try {
            JSONObject root = new JSONObject(raw);
            JSONObject error = root.optJSONObject("error");
            if (error != null) {
                serverCode = error.optString("code", "");
                serverMessage = error.optString("message", "");
            }
        } catch (JSONException ignored) {
            // 错误响应不一定是可解析的 JSON，用状态码兜底即可
        }
        if (serverMessage.length() > LOG_BODY_LIMIT) {
            serverMessage = serverMessage.substring(0, LOG_BODY_LIMIT);
        }

        String message;
        if (status == 401) {
            if ("server_not_configured".equals(serverCode)) {
                message = "服务端还没配置 APP_TOKEN";
            } else if ("unauthorized".equals(serverCode)) {
                message = "令牌不对，检查两边是否一致（服务端 APP_TOKEN / App 端 AI_PROXY_TOKEN）";
            } else {
                message = "中转服务拒绝了请求（401）";
            }
        } else if (status == 403) {
            message = "中转服务拒绝访问（403）";
        } else if (status == 429) {
            message = "请求过于频繁，服务端已限流（429）";
        } else if (status == 502 || status == 504) {
            message = "中转服务没能拿到模型回复";
        } else if (status >= 500) {
            message = "中转服务内部错误（" + status + "）";
        } else {
            message = "请求被中转服务拒绝（" + status + "）";
        }
        if (!serverMessage.isEmpty()) {
            message = message + "：" + serverMessage;
        }
        Log.w(TAG, "service error status=" + status + ", cost=" + cost + "ms, server=" + serverMessage);
        return new AiError(AiError.Code.HTTP, message, status);
    }
}
