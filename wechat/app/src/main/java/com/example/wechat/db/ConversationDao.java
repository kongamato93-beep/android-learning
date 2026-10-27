package com.example.wechat.db;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import com.example.wechat.db.entity.ConversationEntity;

import java.util.List;

@Dao
public interface ConversationDao {

    /**
     * 查询时不排序：排序规则（置顶优先、其余按时间倒序）在 Repository 里统一做，
     * 避免 SQL 和 Java 两处各排一次。
     */
    @Query("SELECT * FROM conversations")
    List<ConversationEntity> getAll();

    @Query("SELECT * FROM conversations WHERE id = :id")
    ConversationEntity getById(String id);

    @Query("SELECT * FROM conversations WHERE peer_id = :peerId LIMIT 1")
    ConversationEntity getByPeerId(String peerId);

    @Upsert
    void upsertAll(List<ConversationEntity> conversations);

    /**
     * 会话的所有更新（未读归零、置顶切换、最后一条消息）都走 upsert：
     * 整行覆盖但不删行，配合单一写线程，读写顺序与业务发生顺序一致。
     */
    @Upsert
    void upsert(ConversationEntity conversation);
}
