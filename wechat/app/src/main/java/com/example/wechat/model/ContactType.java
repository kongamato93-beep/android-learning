package com.example.wechat.model;

/**
 * 联系人类型。
 *
 * 现在只有两种取值，界面上不做任何区分展示 —— 这一层存在的意义是：
 * 将来把某个联系人替换成 AI Agent 时，只需要按 id 关联一份 Agent 配置，
 * 现有的列表 / 详情 / 聊天界面一行都不用改。
 *
 * 注意：本期不实现任何 Agent 相关能力，这里只保留类型位。
 */
public enum ContactType {

    /** 普通联系人（微信好友） */
    NORMAL,

    /** 将来可替换为 AI Agent 的联系人，本期行为与普通联系人完全一致 */
    AGENT
}
