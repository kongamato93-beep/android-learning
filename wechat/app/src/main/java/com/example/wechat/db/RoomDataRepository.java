package com.example.wechat.db;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.room.Room;

import com.example.wechat.R;
import com.example.wechat.data.DataRepository;
import com.example.wechat.data.WriteFailureListener;
import com.example.wechat.db.entity.ConversationEntity;
import com.example.wechat.db.mapper.EntityMapper;
import com.example.wechat.model.Contact;
import com.example.wechat.model.Conversation;
import com.example.wechat.model.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SQLite（Room）版数据仓库 —— 替换掉原来的内存版 MockDataRepository，接口签名一个没改。
 *
 * 一、读写策略
 * - 读：全部走内存 Map/List，界面线程不碰数据库，滑动不受影响；
 * - 写：先更新内存（界面立刻生效），再把这一次动作丢给单线程 executor 落库，
 *   串行执行保证写库顺序与业务发生顺序一致；
 * - 「插消息 + 更新会话」在同一个事务里，见 {@link AppDatabase#appendMessage}。
 *
 * 二、初始化
 * 不用 CountDownLatch 阻塞主线程：由 LauncherActivity 在后台线程调 {@link #initialize()}，
 * 成功之后才创建 MainActivity。读取前的 {@link #ensureReady()} 只是哨兵 ——
 * 正常运行中它永远不会触发，一旦触发说明有人绕过了启动流程，直接抛异常而不是假装有数据。
 *
 * 三、写失败
 * 后台写库失败时回滚本次内存改动 + Toast 提示 + 回调 {@link WriteFailureListener}，
 * 界面重新读数据，保证「显示的结果」和「磁盘上的结果」一致。
 */
public final class RoomDataRepository implements DataRepository {

    private static final String TAG = "RoomDataRepository";

    /** 初始化状态机的三个状态 */
    private enum InitState {
        /** 还没跑过 initialize()，或者正在跑 */
        INITIALIZING,
        READY,
        FAILED
    }

    private static RoomDataRepository instance;

    private final Context appContext;
    private final AppDatabase db;
    /** 所有落库操作都在这一个线程上串行执行 */
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, Contact> contacts = new LinkedHashMap<>();
    private final Map<String, Conversation> conversations = new LinkedHashMap<>();
    private final Map<String, List<Message>> messages = new LinkedHashMap<>();

    /** 当前被聊天页打开的会话 id；null 表示没有聊天页在前台 */
    private volatile String foregroundConversationId;
    private volatile InitState state = InitState.INITIALIZING;
    private volatile WriteFailureListener writeFailureListener;

    public static synchronized RoomDataRepository getInstance(Context context) {
        if (instance == null) {
            instance = new RoomDataRepository(context.getApplicationContext());
        }
        return instance;
    }

    private RoomDataRepository(Context context) {
        this.appContext = context;
        // build() 不开库；真正的建表 / 首次 read 发生在 initialize() 里（后台线程）
        this.db = Room.databaseBuilder(context, AppDatabase.class, AppDatabase.DB_NAME).build();
    }

    /**
     * 后台初始化：确保默认数据只灌一次 → 把三张表读进内存 → 置为 READY。
     *
     * 必须在后台线程调用。失败时不会修改主界面可见的任何状态：
     * 启动页直接把失败原因摆给用户，由用户决定重试还是退出。
     */
    public InitResult initialize() {
        try {
            DatabaseInitializer.ensureDefaultData(db);
            loadIntoMemory();
            state = InitState.READY;
            Log.i(TAG, "初始化完成：联系人 " + contacts.size() + " 条，会话 " + conversations.size() + " 条");
            return InitResult.success();
        } catch (Throwable t) {
            state = InitState.FAILED;
            Log.e(TAG, "初始化失败", t);
            return InitResult.failure(t);
        }
    }

    public boolean isReady() {
        return state == InitState.READY;
    }

    /**
     * 读取前的哨兵。
     *
     * 刻意不使用「返回空集合」这种降级：那样会把「数据还没准备好」
     * 和「数据库里真的没数据」混成同一种表现，问题会被掩盖到很久以后才暴露。
     */
    private void ensureReady() {
        if (state != InitState.READY) {
            throw new IllegalStateException(
                    "数据层尚未就绪（state=" + state + "）：MainActivity 只能在 initialize() 成功之后创建");
        }
    }

    private void loadIntoMemory() {
        contacts.clear();
        for (Contact c : EntityMapper.toContacts(db.contactDao().getAll())) {
            contacts.put(c.getId(), c);
        }

        conversations.clear();
        for (Conversation c : EntityMapper.toConversations(db.conversationDao().getAll())) {
            conversations.put(c.getId(), c);
        }

        messages.clear();
        for (Conversation c : conversations.values()) {
            messages.put(c.getId(), new ArrayList<Message>());
        }
        // 数据库按时间升序返回，内存里的顺序和聊天页渲染顺序保持一致
        for (Message m : EntityMapper.toMessages(db.messageDao().getAll())) {
            List<Message> list = messages.get(m.getConversationId());
            if (list == null) {
                list = new ArrayList<>();
                messages.put(m.getConversationId(), list);
            }
            list.add(m);
        }
    }

    // ------------------------------------------------------------------
    // 写：内存先改 → 后台串行落库 → 失败则回滚 + 通知界面
    // ------------------------------------------------------------------

    /**
     * 提交一次写库。
     *
     * @param dbTask        真正执行 SQL 的任务，在写线程上运行
     * @param conversationId 受影响的会话，失败时告诉界面
     * @param messageId     被写入的消息 id（非消息写入传 null）
     * @param rollback      写失败时用来恢复内存的补偿动作，在**主线程**执行
     *                      （内存集合平时都由主线程改，保持同一个线程避免可见性问题）
     */
    private void postWrite(final Runnable dbTask, final String conversationId,
                           final String messageId, final Runnable rollback) {
        writer.execute(new Runnable() {
            @Override
            public void run() {
                Throwable failure = null;
                try {
                    dbTask.run();
                } catch (Throwable t) {
                    failure = t;
                }
                if (failure == null) {
                    return;
                }
                Log.e(TAG, "写库失败，回滚本次改动", failure);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (rollback != null) {
                            rollback.run();
                        }
                        Toast.makeText(appContext, R.string.msg_write_failed, Toast.LENGTH_LONG).show();
                        WriteFailureListener listener = writeFailureListener;
                        if (listener != null) {
                            listener.onWriteFailed(conversationId, messageId);
                        }
                    }
                });
            }
        });
    }

    /**
     * 排序规则：置顶会话在前，其余按最后一条消息时间倒序。
     *
     * Collections.sort 是稳定排序，同一组内时间相同的会话不会因为重排乱跳，
     * 不需要额外的置顶序号字段。
     */
    private void sortConversations(List<Conversation> list) {
        Collections.sort(list, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation a, Conversation b) {
                if (a.isPinned() != b.isPinned()) {
                    return a.isPinned() ? -1 : 1;
                }
                return Long.compare(b.getLastTimeMillis(), a.getLastTimeMillis());
            }
        });
    }

    // ------------------------------------------------------------------
    // 读
    // ------------------------------------------------------------------

    @Override
    public Contact getContact(String id) {
        ensureReady();
        return contacts.get(id);
    }

    @Override
    public List<Contact> getContacts() {
        ensureReady();
        return new ArrayList<>(contacts.values());
    }

    @Override
    public List<Conversation> getConversations() {
        ensureReady();
        List<Conversation> list = new ArrayList<>(conversations.values());
        sortConversations(list);
        return list;
    }

    @Override
    public List<Message> getMessages(String conversationId) {
        ensureReady();
        List<Message> list = messages.get(conversationId);
        if (list == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list);
    }

    @Override
    public Contact getPeer(String conversationId) {
        ensureReady();
        Conversation c = findConversation(conversationId);
        return c == null ? null : contacts.get(c.getPeerId());
    }

    @Override
    public Conversation getConversationByPeerId(String peerId) {
        ensureReady();
        for (Conversation c : conversations.values()) {
            if (c.getPeerId().equals(peerId)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Conversation getOrCreateConversation(String peerId) {
        ensureReady();
        Conversation existing = getConversationByPeerId(peerId);
        if (existing != null) {
            return existing;
        }
        final Conversation created = new Conversation("conv_" + peerId, peerId);
        created.setLastMessage("");
        created.setLastTimeMillis(System.currentTimeMillis());

        conversations.put(created.getId(), created);
        messages.put(created.getId(), new ArrayList<Message>());

        postWrite(new Runnable() {
            @Override
            public void run() {
                db.createConversationIfAbsent(EntityMapper.toConversationEntity(created));
            }
        }, created.getId(), null, new Runnable() {
            @Override
            public void run() {
                // 会话没真正建成，内存里也不能留着，否则后面发的消息会挂在一条不存在的会话上
                conversations.remove(created.getId());
                messages.remove(created.getId());
            }
        });
        return created;
    }

    @Override
    public Message sendTextMessage(String conversationId, final String text) {
        ensureReady();
        final Conversation c = findConversation(conversationId);
        if (c == null) {
            return null;
        }

        final Message message = new Message(DefaultDataSeed.newMessageId(), conversationId, SELF_ID,
                text, System.currentTimeMillis());
        List<Message> list = messages.get(conversationId);
        if (list == null) {
            list = new ArrayList<>();
            messages.put(conversationId, list);
        }
        list.add(message);

        // 写之前先拍快照，写失败才能精确还原
        final String oldLastMessage = c.getLastMessage();
        final long oldTime = c.getLastTimeMillis();
        final int oldUnread = c.getUnreadCount();

        // 只同步“最后一条/时间”，不碰未读计数：发送和清零是两件事
        c.setLastMessage(text);
        c.setLastTimeMillis(message.getTimestamp());
        // 提交前把这一刻的状态定下来，写线程不必再去读可能被后续操作改过的同一个对象
        final ConversationEntity snapshot = EntityMapper.toConversationEntity(c);

        postWrite(new Runnable() {
            @Override
            public void run() {
                db.appendMessage(EntityMapper.toMessageEntity(message), snapshot);
            }
        }, conversationId, message.getId(), new Runnable() {
            @Override
            public void run() {
                undoMessage(message);
                restoreConversation(c, oldLastMessage, oldTime, oldUnread);
            }
        });
        return message;
    }

    @Override
    public Message receiveTextMessage(String conversationId, final String text) {
        ensureReady();
        final Conversation c = findConversation(conversationId);
        if (c == null) {
            return null;
        }

        final Message message = new Message(DefaultDataSeed.newMessageId(), conversationId,
                c.getPeerId(), text, System.currentTimeMillis());
        List<Message> list = messages.get(conversationId);
        if (list == null) {
            list = new ArrayList<>();
            messages.put(conversationId, list);
        }
        list.add(message);

        final String oldLastMessage = c.getLastMessage();
        final long oldTime = c.getLastTimeMillis();
        final int oldUnread = c.getUnreadCount();

        c.setLastMessage(text);
        c.setLastTimeMillis(message.getTimestamp());

        // 只有这个会话不在前台时才计未读；计一次，消息 id 唯一不会重复
        if (!conversationId.equals(foregroundConversationId)) {
            c.setUnreadCount(oldUnread + 1);
        }
        final ConversationEntity snapshot = EntityMapper.toConversationEntity(c);

        postWrite(new Runnable() {
            @Override
            public void run() {
                db.appendMessage(EntityMapper.toMessageEntity(message), snapshot);
            }
        }, conversationId, message.getId(), new Runnable() {
            @Override
            public void run() {
                undoMessage(message);
                restoreConversation(c, oldLastMessage, oldTime, oldUnread);
            }
        });
        return message;
    }

    @Override
    public void markConversationRead(String conversationId) {
        ensureReady();
        final Conversation c = findConversation(conversationId);
        if (c == null || c.getUnreadCount() == 0) {
            return;
        }
        final int oldUnread = c.getUnreadCount();
        c.setUnreadCount(0);
        final ConversationEntity snapshot = EntityMapper.toConversationEntity(c);
        postWrite(new Runnable() {
            @Override
            public void run() {
                db.conversationDao().upsert(snapshot);
            }
        }, conversationId, null, new Runnable() {
            @Override
            public void run() {
                c.setUnreadCount(oldUnread);
            }
        });
    }

    @Override
    public void setForegroundConversation(String conversationId) {
        this.foregroundConversationId = conversationId;
    }

    @Override
    public int getTotalUnreadCount() {
        ensureReady();
        int total = 0;
        for (Conversation c : conversations.values()) {
            total += c.getUnreadCount();
        }
        return total;
    }

    @Override
    public boolean togglePinned(String conversationId) {
        ensureReady();
        final Conversation c = findConversation(conversationId);
        if (c == null) {
            return false;
        }
        // 只翻转置顶标记：未读数、最后一条消息、消息记录一律不动
        final boolean oldPinned = c.isPinned();
        c.setPinned(!oldPinned);
        final ConversationEntity snapshot = EntityMapper.toConversationEntity(c);
        postWrite(new Runnable() {
            @Override
            public void run() {
                db.conversationDao().upsert(snapshot);
            }
        }, conversationId, null, new Runnable() {
            @Override
            public void run() {
                c.setPinned(oldPinned);
            }
        });
        return c.isPinned();
    }

    @Override
    public void setWriteFailureListener(WriteFailureListener listener) {
        this.writeFailureListener = listener;
    }

    // ------------------------------------------------------------------
    // 回滚辅助
    // ------------------------------------------------------------------

    /** 把一条刚加进内存的消息撤掉。Message 没有重写 equals，remove 按引用比对 */
    private void undoMessage(Message message) {
        List<Message> list = messages.get(message.getConversationId());
        if (list != null) {
            list.remove(message);
        }
    }

    private void restoreConversation(Conversation c, String lastMessage, long timeMillis, int unread) {
        c.setLastMessage(lastMessage);
        c.setLastTimeMillis(timeMillis);
        c.setUnreadCount(unread);
    }

    private Conversation findConversation(String conversationId) {
        if (conversationId == null) {
            return null;
        }
        return conversations.get(conversationId);
    }
}
