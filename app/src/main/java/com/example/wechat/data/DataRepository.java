package com.example.wechat.data;

import com.example.wechat.model.Contact;
import com.example.wechat.model.Conversation;
import com.example.wechat.model.Message;

import java.util.List;

/**
 * 数据层接口。
 *
 * 界面只依赖这个接口，不依赖具体实现。当前用 SQLite（Room）实现，见
 * {@link com.example.wechat.db.RoomDataRepository}；以后要换成网络，只要再写一个实现，界面不用动。
 *
 * 这也是为 Agent 预留的第三条接缝：发送只有 {@link #sendTextMessage} 这一个入口。
 */
public interface DataRepository {

    /** “我”的 id：所有由自己发出的消息都用这个 senderId */
    String SELF_ID = "me_self";

    /**
     * 按 id 取联系人。
     *
     * 将来 Agent 的额外信息（模型、系统提示词）可以用同一个 id 关联，
     * 取人的方式不变。
     */
    Contact getContact(String id);

    List<Contact> getContacts();

    List<Conversation> getConversations();

    /** 某个会话的全部消息，按时间升序 */
    List<Message> getMessages(String conversationId);

    /** 取会话对应的联系人 */
    Contact getPeer(String conversationId);

    /** 按联系人 id 找会话；从联系人详情页「发消息」时用 */
    Conversation getConversationByPeerId(String peerId);

    /** 没有会话就新建一个（从联系人详情页发起聊天时） */
    Conversation getOrCreateConversation(String peerId);

    /**
     * 发出一条文本消息。
     *
     * 目前的实现只是把消息追加到内存列表并同步会话最后一条；
     * 将来接 Agent，就在这个实现内部先交给消息处理器再落库，调用方无感。
     *
     * 注意：发送与未读彻底解耦，这里不会去动未读计数。
     */
    Message sendTextMessage(String conversationId, String text);

    /**
     * 收到一条对方发来的文本消息（唯一的“收消息”入口）。
     *
     * 未读只在「这条消息所属的会话不在前台」时才 +1：
     * 用户正看着这个会话，消息直接展示，不该算未读。
     * 每条消息只在这里计一次，消息 id 唯一，不会重复累计。
     *
     * 将来接 Agent 时，这个方法就是 Agent 回复落库的地方，调用方不用改。
     */
    Message receiveTextMessage(String conversationId, String text);

    /**
     * 把某个会话标记为已读（未读归零）。幂等：重复调用不会出错。
     * 进入聊天页时调用，与发送逻辑互不影响。
     */
    void markConversationRead(String conversationId);

    /**
     * 告诉数据层“哪个会话正被聊天页打开”，传 null 表示没有聊天页在前台。
     * 用来决定收到的消息要不要计入未读。
     */
    void setForegroundConversation(String conversationId);

    /** 所有会话未读数之和，主界面标题和 Tab 角标上的数字用它 */
    int getTotalUnreadCount();

    /**
     * 切换某个会话的置顶状态，返回切换后是否置顶。
     *
     * 只改会话上的 pinned 标记，不动未读数、最后一条消息和消息记录；
     * 排序变化交给 {@link #getConversations()} 内部的统一排序。
     */
    boolean togglePinned(String conversationId);

    /**
     * 注册「异步写库失败」的回调，传 null 表示取消。
     *
     * 写操作在后台线程执行，失败时数据层会回滚内存并回调这里，
     * 界面据此重新读取数据，避免出现「看起来存了其实没存」。
     */
    void setWriteFailureListener(WriteFailureListener listener);
}
