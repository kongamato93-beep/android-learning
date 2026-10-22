package com.example.wechat.ai;

/**
 * 一次 AI 请求的句柄，只用来取消。
 *
 * 存在的理由：用户发完消息就退出页面时，请求还在飞，
 * 回调回来会去更新一个已经销毁的 Activity。
 * 界面层在 onDestroy 里调一次 cancel()，provider 会在收到结果时自己丢弃回调。
 */
public interface AiRequest {

    void cancel();
}
