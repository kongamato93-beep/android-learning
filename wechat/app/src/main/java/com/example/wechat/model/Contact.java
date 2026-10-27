package com.example.wechat.model;

import com.example.wechat.util.PinyinIndex;

/**
 * 联系人（业务对象，纯 Java，不含任何数据库注解）。
 *
 * 持久化版本是 {@code db/entity/ContactEntity}，两者通过 {@code db/mapper/EntityMapper} 转换。
 * 分开的意义：这里的字段可以按界面需要自由改，将来接 AI Service 的 DTO 也不会把
 * 数据库约束（主键、列名、索引）带进业务层。
 *
 * 与微信的对应关系：
 * - id       ：唯一标识。会话、消息都通过它关联，不依赖昵称（昵称可以改，id 不变）
 * - avatar   ：头像资源名。现在是 drawable 名（如 avatar_1），
 *              将来换成真实图片或网络地址时，只要改 AvatarLoader 的解析方式即可
 * - bio      ：简介 / 个性签名
 * - type     ：普通联系人还是 Agent（本期不区分行为）
 * - sortOrder：通讯录的原始次序。SQLite 不保证返回插入顺序，
 *              而通讯录要求每次启动顺序一致，所以由数据层显式存一列。
 */
public class Contact {

    private String id;
    private String name;
    private String avatar;
    private String bio;
    private ContactType type;
    private String wechatId;
    private String region;
    private int sortOrder;

    public Contact() {
        this.type = ContactType.NORMAL;
    }

    public Contact(String id, String name, String avatar) {
        this();
        this.id = id;
        this.name = name;
        this.avatar = avatar;
    }

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

    /** 通讯录字母索引用的首字母：中文取拼音首字母的简易映射，其余取首字符 */
    public String getIndexLetter() {
        if (name == null || name.isEmpty()) {
            return "#";
        }
        char first = name.charAt(0);
        if (first >= 'A' && first <= 'Z') {
            return String.valueOf(first);
        }
        if (first >= 'a' && first <= 'z') {
            return String.valueOf((char) (first - 'a' + 'A'));
        }
        if (first >= 0x4e00 && first <= 0x9fa5) {
            return PinyinIndex.firstLetter(first);
        }
        return "#";
    }
}
