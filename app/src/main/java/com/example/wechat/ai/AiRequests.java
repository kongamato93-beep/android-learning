package com.example.wechat.ai;

import androidx.annotation.NonNull;

import okhttp3.Call;

/**
 * 两个 ChatProvider 实现都要用的几个小零件。
 *
 * 为什么单独拎出来：
 * {@link ZhipuProvider} 和 {@link ProxyProvider} 除了「请求发给谁、怎么解析返回」之外，
 * 剩下的样板完全一样 —— 包一层 OkHttp 的 Call、构造一个什么也不做的空句柄、
 * 用异常区分「内容是空的」和「结构坏了」。各写一份纯属重复。
 *
 * 注意这里只放**没有业务含义的通用件**；
 * 请求体怎么拼、私有字段怎么解析，仍然留在各自的 Provider 里，不往这边塞。
 */
final class AiRequests {

    private AiRequests() {
    }

    /**
     * 把 OkHttp 的 Call 包一层，让上层只认识 AiRequest 这个统一句柄。
     *
     * 有了这层之后，界面在 onDestroy 里调 cancel() 时不需要知道底下是
     * OkHttp 还是别的网络库 —— 换库的时候这一处跟着改就够了。
     */
    static final class CallRequest implements AiRequest {
        private final Call call;

        CallRequest(Call call) {
            this.call = call;
        }

        @Override
        public void cancel() {
            call.cancel();
        }
    }

    /** 请求根本没发出去时返回的空句柄，调用方 cancel() 是安全的 */
    static final class NoopRequest implements AiRequest {
        @Override
        public void cancel() {
            // 没有在途请求，什么都不用做
        }
    }

    /**
     * 模型返回了 200、结构也合法，但正文是空的。
     *
     * 单独定义成异常是为了让 catch 顺序能把它和 JSONException 分开：
     * 「内容空」值得重试或换个问法提示用户，
     * 「结构坏」说明接口变了，是代码要改。
     */
    static final class EmptyReplyException extends Exception {
        EmptyReplyException() {
            super("empty reply");
        }

        @NonNull
        @Override
        public String toString() {
            return getMessage() == null ? "EmptyReplyException" : getMessage();
        }
    }
}
