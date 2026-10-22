package com.example.wechat.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.wechat.model.ContactType;

/**
 * 联系人的持久化形态，字段与 {@code contacts} 表一一对应。
 *
 * 与业务对象 {@link com.example.wechat.model.Contact} 分开：
 * 这张表的列名、主键、索引只在这里定，业务对象那边可以自由演进。
 * 两者由 {@code db/mapper/EntityMapper} 转换。
 *
 * sort_order：SQLite 不保证按插入顺序返回，通讯录要求每次启动顺序一致，所以显式存一列。
 */
@Entity(tableName = "contacts")
public class ContactEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "avatar")
    private String avatar;
    @ColumnInfo(name = "bio")
    private String bio;
    @ColumnInfo(name = "type")
    private ContactType type;
    @ColumnInfo(name = "wechat_id")
    private String wechatId;
    @ColumnInfo(name = "region")
    private String region;
    @ColumnInfo(name = "sort_order")
    private int sortOrder;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public ContactType getType() {
        return type;
    }

    public void setType(ContactType type) {
        this.type = type;
    }

    public String getWechatId() {
        return wechatId;
    }

    public void setWechatId(String wechatId) {
        this.wechatId = wechatId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
