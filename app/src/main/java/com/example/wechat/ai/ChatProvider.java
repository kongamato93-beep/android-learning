package com.example.wechat.ai;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * 模型调用的唯一抽象。
 *
 * 界面层和业务层只认这个接口，不认智谱、不认 OkHttp。
 * 这样做有两个具体收益：
 *  - 换服务商 / 换成自己的后端转发，只是新增一个实现类，ChatActivity 不动；
 *  - 将来「不同会话用不同模型」时，config 从 AiConfig.resolve(conversationId) 拿到，
 *    调用方不用知道当前用的是哪一家。
 *
 * 合约（实现方必须遵守）：
 *  - chat() 立即返回，网络工作在后台线程，不得阻塞调用线程；
 *  - 回调可以由任意线程触发，界面层负责切主线程；
 *  - 失败一律走 onFailure，不抛异常给调用方。
 */
public interface ChatProvider {

    /** 与 AiModelConfig.PROVIDER_* 对应，用于日志和将来的路由 */
    @NonNull
    String getProviderId();

    /**
     * @param config   本次调用用什么服务商 / 模型 / 密钥 / 超时
     * @param messages 完整上下文，第一条通常是 system，最后一条是用户当前输入
     * @param callback 结果回调，最多被调用一次
     * @return 请求句柄，用于取消；不关心取消时可以忽略返回值
     */
    @NonNull
    AiRequest chat(@NonNull AiModelConfig config,
                   @NonNull List<ChatMessage> messages,
                   @NonNull Callback callback);

    interface Callback {
        /**
         * @param reply   模型回复的正文，已去首尾空白，非空
         * @param modelId 服务端实际使用的模型名，用于日志核对
         */
        void onSuccess(@NonNull String reply, @NonNull String modelId);

        void onFailure(@NonNull AiError error);
    }
}
