package com.example.wechat.ai;

import androidx.annotation.NonNull;

/**
 * 发给模型的单条消息（Chat Completions 的 {role, content} 结构）。
 *
 * 刻意不复用 model.Message：
 *  - Message 是数据库里的业务模型，带 id / conversationId / senderId / 时间戳；
 *  - 这里只关心「谁说的、说了什么」，而且 role 只有三个取值。
 * 两者混在一起的话，将来加摘要、加 token 统计时数据库模型会被请求协议污染。
 * 转换在 AiReplyManager 里做一次，方向单一。
 */
public final class ChatMessage {

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private final String role;
    private final String content;

    private ChatMessage(String role, String content) {
        this.role = role;
        this.content = content == null ? "" : content;
    }

    @NonNull
    public static ChatMessage system(String content) {
        return new ChatMessage(ROLE_SYSTEM, content);
    }

    @NonNull
    public static ChatMessage user(String content) {
        return new ChatMessage(ROLE_USER, content);
    }

    @NonNull
    public static ChatMessage assistant(String content) {
        return new ChatMessage(ROLE_ASSISTANT, content);
    }

    @NonNull
    public String getRole() {
        return role;
    }

    @NonNull
    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return role + ": " + content;
    }
}
