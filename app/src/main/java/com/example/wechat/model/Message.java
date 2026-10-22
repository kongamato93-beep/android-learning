package com.example.wechat.model;

/**
 * 一条消息。业务对象，纯 Java，不含数据库注解。
 *
 * senderId 决定它显示在左边还是右边：等于 DataRepository 里的“自己”id 就靠右。
 * 本期只实现文本消息，其他类型先把枚举位留着。
 *
 * id 必须是全局唯一的（UUID）：历史上用过内存自增序号，一旦落库，
 * 重启后序号归零会生成重复 id，写库时会撞主键覆盖旧记录。
 */
public class Message {

    public enum Type {
        TEXT,
        IMAGE,
        VOICE,
        SYSTEM
    }

    private String id;
    private String conversationId;
    private String senderId;
    private Type type = Type.TEXT;
    private String content;
    private long timestamp;

    public Message() {
    }

    public Message(String id, String conversationId, String senderId, String content, long timestamp) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
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
