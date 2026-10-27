package com.example.wechat.data;

/**
 * 异步写库失败时通知界面的通道。
 *
 * 数据层的写操作都在后台线程做；一旦失败，数据层会先把内存状态回滚到写入前，
 * 再通过这个回调告诉界面「刚才那步没落库，你重新读一次数据」。
 * 没有这个回调就会出现「界面显示成功、数据其实没保存」。
 *
 * 只留一个槽位：同一时刻只有一个 Activity 处于 resumed，
 * 谁回到前台谁在 onResume 里注册自己即可，不需要观察者列表。
 */
public interface WriteFailureListener {

    /**
     * @param conversationId 受影响的会话 id
     * @param messageId      被回滚的消息 id；不是消息写入（如置顶、已读）时为 null
     */
    void onWriteFailed(String conversationId, String messageId);
}
