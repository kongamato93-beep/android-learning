package com.example.wechat.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.wechat.model.Message;

/**
 * 消息的持久化形态，对应 {@code messages} 表。
 *
 * conversation_id 上建了索引：聊天页按会话查消息是最频繁的查询。
 * 主键 id 是 UUID，写入用 ABORT 冲突策略 —— 撞号说明有 bug，
 * 宁可报错，也不能静默覆盖历史聊天记录。
 */
@Entity(tableName = "messages", indices = {@Index(value = "conversation_id")})
public class MessageEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;
    @ColumnInfo(name = "conversation_id")
    private String conversationId;
    @ColumnInfo(name = "sender_id")
    private String senderId;
    @ColumnInfo(name = "type")
    private Message.Type type;
    @ColumnInfo(name = "content")
    private String content;
    @ColumnInfo(name = "timestamp")
    private long timestamp;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Message.Type getType() {
        return type;
    }

    public void setType(Message.Type type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
