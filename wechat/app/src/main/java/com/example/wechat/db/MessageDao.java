package com.example.wechat.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.wechat.db.entity.MessageEntity;

import java.util.List;

@Dao
public interface MessageDao {

    /** 某个会话的消息，按时间升序：聊天页直接按这个顺序渲染 */
    @Query("SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY timestamp ASC, id ASC")
    List<MessageEntity> getByConversation(String conversationId);

    /**
     * 一次性把所有消息读出来做内存分组。
     * 数据量是几十到几百条，一次读回比按会话分次查更省；将来量大再改成分页加载。
     */
    @Query("SELECT * FROM messages ORDER BY timestamp ASC, id ASC")
    List<MessageEntity> getAll();

    /**
     * 消息用 ABORT：主键是 UUID，重复插入一定是 bug，
     * 必须让调用方知道，而不是静默把历史记录覆盖掉。
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertAll(List<MessageEntity> messages);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(MessageEntity message);
}
