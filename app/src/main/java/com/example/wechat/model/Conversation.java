package com.example.wechat.model;

/**
 * 一个会话（聊天列表里的一行）。业务对象，纯 Java，不含数据库注解。
 *
 * 会话不保存联系人对象，只保存 peerId，渲染时再通过 DataRepository 查 Contact。
 * 这样联系人信息改了（换头像、改名、将来换成 Agent）列表能自动跟着变。
 *
 * unreadCount / pinned 都由数据层统一落库，界面不去各自维护一份计数。
 */
public class Conversation {

    private String id;
    /** 对方联系人 id；单聊即联系人 id */
    private String peerId;
    private String lastMessage;
    private long lastTimeMillis;
    private int unreadCount;
    private boolean muted;
    /** 是否置顶：置顶会话排在列表最前面 */
    private boolean pinned;

    public Conversation() {
    }

    public Conversation(String id, String peerId) {
        this.id = id;
        this.peerId = peerId;
    }

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
