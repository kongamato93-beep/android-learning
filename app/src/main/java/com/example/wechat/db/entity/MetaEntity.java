package com.example.wechat.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 数据库自身的元信息表，目前只用来标记「默认数据是否已经灌过」。
 *
 * 为什么不用“会话表是否为空”来判断：用户把会话删光之后，
 * 表也是空的，下次启动就会重新灌入默认数据，等于覆盖了用户操作。
 * 一张独立的标记表不受业务数据增删影响。
 */
@Entity(tableName = "db_meta")
public class MetaEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "meta_key")
    private String key;
    @ColumnInfo(name = "meta_value")
    private String value;

    public MetaEntity() {
    }

    /** @Ignore 告诉 Room 忽略这个构造器，只用上面的无参构造器 + setter，不要自己猜 */
    @Ignore
    public MetaEntity(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
