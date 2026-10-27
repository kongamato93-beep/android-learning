package com.example.wechat.db;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import com.example.wechat.db.entity.MetaEntity;

@Dao
public interface MetaDao {

    /** 没有这条 key 时返回 null，调用方据此判断“还没初始化过” */
    @Query("SELECT meta_value FROM db_meta WHERE meta_key = :key LIMIT 1")
    String getValue(String key);

    @Upsert
    void upsert(MetaEntity meta);
}
