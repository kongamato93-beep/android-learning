package com.example.wechat.db;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import com.example.wechat.db.entity.ContactEntity;

import java.util.List;

@Dao
public interface ContactDao {

    /** 按通讯录原始次序返回，保证每次启动顺序一致 */
    @Query("SELECT * FROM contacts ORDER BY sort_order ASC")
    List<ContactEntity> getAll();

    @Query("SELECT * FROM contacts WHERE id = :id")
    ContactEntity getById(String id);

    /**
     * 用 @Upsert 而不是 INSERT OR REPLACE：
     * REPLACE 的语义是「先删冲突行再插入」，一旦将来给表加了带级联的外键，
     * 一次替换就会连带删掉关联数据。@Upsert 生成的是 ON CONFLICT DO UPDATE，不动删除。
     */
    @Upsert
    void upsertAll(List<ContactEntity> contacts);

    @Upsert
    void upsert(ContactEntity contact);
}
