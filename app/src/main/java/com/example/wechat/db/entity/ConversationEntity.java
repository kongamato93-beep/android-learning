package com.example.wechat.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 会话的持久化形态，对应 {@code conversations} 表。
 *
 * unread_count / pinned / muted 都落库，重启后保持退出前的状态。
 * 这里没有外键：目前没有删除会话的功能，也就不会有孤儿消息；
 * 关联一致性靠「同一事务写入 + 单一写线程」保证，见 AppDatabase。
 */
@Entity(tableName = "conversations", indices = {@Index(value = "peer_id")})
public class ConversationEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;
    /** 对方联系人 id；单聊即联系人 id。加索引方便按联系人反查会话 */
    @ColumnInfo(name = "peer_id")
    private String peerId;
    @ColumnInfo(name = "last_message")
    private String lastMessage;
    @ColumnInfo(name = "last_time_millis")
    private long lastTimeMillis;
    @ColumnInfo(name = "unread_count")
    private int unreadCount;
    @ColumnInfo(name = "muted")
    private boolean muted;
    @ColumnInfo(name = "pinned")
    private boolean pinned;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastTimeMillis() {
        return lastTimeMillis;
    }

    public void setLastTimeMillis(long lastTimeMillis) {
        this.lastTimeMillis = lastTimeMillis;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}
