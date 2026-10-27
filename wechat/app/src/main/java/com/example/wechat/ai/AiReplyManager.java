package com.example.wechat.ai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.wechat.data.DataProvider;
import com.example.wechat.data.DataRepository;
import com.example.wechat.model.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 回复的编排层：把「取历史 → 组上下文 → 调模型 → 回复落库 → 通知界面」串起来。
 *
 * 为什么要有这一层，而不是让 ChatActivity 直接调 ChatProvider：
 *  - 历史怎么取、怎么裁剪、怎么映射成 role，是业务规则，不属于界面；
 *  - 落库必须走 receiveTextMessage（未读判定、会话最后一句、写失败回滚都在里面），
 *    这个决定放在这里，界面就不必知道「AI 回复也是一条收到的消息」；
 *  - 服务商路由只在这里出现一次，将来按会话换模型时界面零改动。
 */
public final class AiReplyManager {

    private static final String TAG = "AiReplyManager";
    private static volatile AiReplyManager sInstance;

    public interface Callback {
        /** 回复已经写进本地消息列表（内存已更新，落库在后台排队） */
        void onReply(@NonNull Message message);

        void onError(@NonNull AiError error);
    }

    private final DataRepository repository;
    private final Handler mainHandler;
    private final Map<String, ChatProvider> providers = new HashMap<>();

    private AiReplyManager() {
        this.repository = DataProvider.get();
        this.mainHandler = new Handler(Looper.getMainLooper());
        register(new ZhipuProvider());
    }

    public static AiReplyManager getInstance() {
        if (sInstance == null) {
            synchronized (AiReplyManager.class) {
                if (sInstance == null) {
                    sInstance = new AiReplyManager();
                }
            }
        }
        return sInstance;
    }

    /** 加服务商只改这一处，其余代码不认识具体实现类 */
    private void register(ChatProvider provider) {
        providers.put(provider.getProviderId(), provider);
    }

    /**
     * 请求一条 AI 回复。
     *
     * 必须在主线程调用：读内存消息列表、以及成功后调 receiveTextMessage 写库，
     * 都要求在主线程（见 RoomDataRepository 里「内存集合由主线程改」的约定）。
     * 网络本身由 provider 在后台线程执行，这里不会卡住调用方。
     *
     * @return 请求句柄，界面在 onDestroy 里 cancel 掉即可
     */
    @NonNull
    public AiRequest requestReply(final String conversationId, @NonNull final Callback callback) {
        final AiModelConfig config = AiConfig.resolve(conversationId);
        Log.d(TAG, "request: conversation=" + conversationId + ", " + config.describeSafe());

        final ChatProvider provider = providers.get(config.getProviderId());
        if (provider == null) {
            AiError error = new AiError(AiError.Code.UNKNOWN,
                    "没有注册的服务商实现: " + config.getProviderId());
            Log.w(TAG, error.toString());
            callback.onError(error);
            return new NoopRequest();
        }

        final List<ChatMessage> messages = buildMessages(conversationId, config);
        Log.d(TAG, "sending " + messages.size() + " messages (含 system 与当前输入)");

        return provider.chat(config, messages, new ChatProvider.Callback() {
            @Override
            public void onSuccess(final String reply, final String modelId) {
                // provider 的回调在 OkHttp 的线程上，落库和界面更新都必须切回主线程
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        deliver(conversationId, reply, callback);
                    }
                });
            }

            @Override
            public void onFailure(final AiError error) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(error);
                    }
                });
            }
        });
    }

    /**
     * 把回复交给数据层。
     *
     * 走 receiveTextMessage 而不是自己拼 Message：未读判定、会话最后一句、
     * 写库失败时的内存回滚，全部自动继承，DataRepository 接口不用动一个字。
     */
    private void deliver(String conversationId, String reply, Callback callback) {
        Message saved = repository.receiveTextMessage(conversationId, reply);
        if (saved == null) {
            // 会话不存在等异常情况。这里刻意报 SAVE_FAILED 而不是 UNKNOWN：
            // 请求是成功的，失败发生在本地保存这一步，界面不能说成「AI 没回复」
            callback.onError(new AiError(AiError.Code.SAVE_FAILED, "回复没能写入本地消息列表"));
            return;
        }
        callback.onReply(saved);
    }

    /**
     * 组装发给模型的上下文。
     *
     * 三条硬规则：
     *  - 只取本会话的消息，别的会话一条都不混进来；
     *  - 只取文本消息且内容非空（图片/语音/系统提示对模型没有意义，还会浪费 token）；
     *  - 按时间升序，取最后 N 条；当前这条刚发出的消息时间戳最大，必然排在最后且只出现一次。
     *
     * 为什么取尾部 N 条而不是从头截断：历史会越来越长，
     * 从头截会丢掉最近的上下文，而模型最需要的恰恰是最近的几轮。
     */
    @NonNull
    private List<ChatMessage> buildMessages(String conversationId, AiModelConfig config) {
        List<Message> usable = new ArrayList<>();
        List<Message> all = repository.getMessages(conversationId);
        if (all != null) {
            for (Message m : all) {
                if (m == null || m.getType() != Message.Type.TEXT) {
                    continue;
                }
                String content = m.getContent();
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }
                usable.add(m);
            }
        }

        // 不依赖 getMessages 的内部顺序：显式按时间排一次，保证「对话顺序 = 发送顺序」。
        // Collections.sort 是稳定的，同一毫秒内的多条消息保持原有先后。
        Collections.sort(usable, new Comparator<Message>() {
            @Override
            public int compare(Message a, Message b) {
                return Long.compare(a.getTimestamp(), b.getTimestamp());
            }
        });

        int limit = Math.max(1, config.getHistoryLimit());
        int from = Math.max(0, usable.size() - limit);

        List<ChatMessage> result = new ArrayList<>();
        String systemPrompt = config.getSystemPrompt().trim();
        if (!systemPrompt.isEmpty()) {
            result.add(ChatMessage.system(systemPrompt));
        }
        for (int i = from; i < usable.size(); i++) {
            Message m = usable.get(i);
            String content = m.getContent().trim();
            if (DataRepository.SELF_ID.equals(m.getSenderId())) {
                result.add(ChatMessage.user(content));
            } else {
                result.add(ChatMessage.assistant(content));
            }
        }
        return result;
    }

    /** 请求没发出去时返回的空句柄，让调用方可以无脑 cancel() */
    private static final class NoopRequest implements AiRequest {
        @Override
        public void cancel() {
            // 没有在途请求
        }
    }
}
