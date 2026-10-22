package com.example.wechat.ai;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
 * 智谱 BigModel 的 Chat Completions 实现。
 *
 * 这个类只做两件事：把 ChatMessage 列表拼成 HTTP 请求、把响应解析成一段文本。
 * 不碰数据库、不碰 UI、不判断「这条消息该不该发」——那是 AiReplyManager 的事。
 *
 * 端点与鉴权按官方文档：
 *   POST https://open.bigmodel.cn/api/paas/v4/chat/completions
 *   Authorization: Bearer <API Key>
 * 消息格式是 Chat Completions 兼容结构，将来换服务商时组装逻辑可以复用。
 */
public final class ZhipuProvider implements ChatProvider {

    private static final String TAG = "ZhipuProvider";
    private static final String PATH_CHAT_COMPLETIONS = "chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int LOG_BODY_LIMIT = 200;

    /** 共享连接池和线程池；超时按每次调用的 config 覆盖 */
    private final OkHttpClient baseClient;

    public ZhipuProvider() {
        baseClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
    }

    @NonNull
    @Override
    public String getProviderId() {
        return AiModelConfig.PROVIDER_ZHIPU;
    }

    @NonNull
    @Override
    public AiRequest chat(@NonNull AiModelConfig config,
                          @NonNull List<ChatMessage> messages,
                          @NonNull final ChatProvider.Callback callback) {
        // 没配密钥就不要发请求，直接告诉调用方原因
        if (!config.isConfigured()) {
            callback.onFailure(new AiError(AiError.Code.NOT_CONFIGURED,
                    "API Key 未配置，请在 local.properties 里填写 ZHIPU_API_KEY"));
            return new AiRequests.NoopRequest();
        }

        final String requestBody;
        try {
            requestBody = buildRequestBody(config, messages);
        } catch (JSONException e) {
            callback.onFailure(new AiError(AiError.Code.UNKNOWN, "请求体组装失败: " + e.getMessage()));
            return new AiRequests.NoopRequest();
        }

        // 复用连接池，只按本次 config 覆盖超时
        OkHttpClient client = baseClient.newBuilder()
                .connectTimeout(config.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + PATH_CHAT_COMPLETIONS)
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON))
                .build();

        final Call call = client.newCall(request);
        final long startAt = System.currentTimeMillis();

        // 用全限定名：ChatProvider.Callback 已经占用了「Callback」这个短名
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
                        callback.onFailure(toHttpError(response.code(), raw, cost));
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

    private static String buildRequestBody(AiModelConfig config, List<ChatMessage> messages)
            throws JSONException {
        JSONObject root = new JSONObject();
        root.put("model", config.getModelId());

        JSONArray array = new JSONArray();
        for (ChatMessage m : messages) {
            JSONObject item = new JSONObject();
            item.put("role", m.getRole());
            item.put("content", m.getContent());
            array.put(item);
        }
        root.put("messages", array);

        // 第一版不做流式：一次性拿完整回复，靠 read 超时兜住总耗时
        root.put("stream", false);
        return root.toString();
    }

    private static String parseReply(String raw, long cost) throws JSONException, AiRequests.EmptyReplyException {
        JSONObject root = new JSONObject(raw);
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null) {
            throw new JSONException("missing choices");
        }
        if (choices.length() == 0) {
            throw new AiRequests.EmptyReplyException();
        }

        JSONObject first = choices.getJSONObject(0);
        String finishReason = first.optString("finish_reason", "");
        JSONObject message = first.optJSONObject("message");
        String content = message == null ? "" : message.optString("content", "");
        content = content == null ? "" : content.trim();

        int totalTokens = -1;
        JSONObject usage = root.optJSONObject("usage");
        if (usage != null) {
            totalTokens = usage.optInt("total_tokens", -1);
        }

        // 这几项是「回复确实来自模型 API」的证据：模拟数据给不出 usage 和耗时
        Log.d(TAG, "reply received: model=" + root.optString("model", "unknown")
                + ", finish_reason=" + finishReason
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
            JSONObject root = new JSONObject(raw);
            return root.optString("model", "");
        } catch (JSONException e) {
            return "";
        }
    }

    private static AiError toHttpError(int status, String raw, long cost) {
        String serverMessage = "";
        try {
            JSONObject root = new JSONObject(raw);
            JSONObject error = root.optJSONObject("error");
            serverMessage = error != null
                    ? error.optString("message", "")
                    : root.optString("message", "");
        } catch (JSONException ignored) {
            // 错误响应不一定是可解析的 JSON，用状态码兜底即可
        }
        if (serverMessage.length() > LOG_BODY_LIMIT) {
            serverMessage = serverMessage.substring(0, LOG_BODY_LIMIT);
        }

        String message;
        if (status == 401 || status == 403) {
            message = "密钥无效或没有该模型的调用权限（" + status + "）";
        } else if (status == 429) {
            message = "请求过于频繁，已被限流（429）";
        } else if (status >= 500) {
            message = "模型服务暂时不可用（" + status + "）";
        } else {
            message = "请求被拒绝（" + status + "）";
        }
        if (!serverMessage.isEmpty()) {
            message = message + "：" + serverMessage;
        }
        Log.w(TAG, "http error status=" + status + ", cost=" + cost + "ms, server=" + serverMessage);
        return new AiError(AiError.Code.HTTP, message, status);
    }

}
