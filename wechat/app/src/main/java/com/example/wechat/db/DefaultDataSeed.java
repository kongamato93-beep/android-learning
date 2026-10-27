package com.example.wechat.db;

import com.example.wechat.data.DataRepository;
import com.example.wechat.db.entity.ContactEntity;
import com.example.wechat.db.entity.ConversationEntity;
import com.example.wechat.db.entity.MessageEntity;
import com.example.wechat.model.ContactType;
import com.example.wechat.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 首次安装时的默认数据（内容与原 MockDataRepository 完全一致，一个联系人一条消息都没改）。
 *
 * 产出的是 Entity 而不是 Model：这份数据是直接写进数据库的，
 * 不需要再过一遍业务对象的约束。
 *
 * 什么时候灌、怎么保证只灌一次，见 {@link DatabaseInitializer}。
 */
public final class DefaultDataSeed {

    private static final long MINUTE = 60 * 1000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    private DefaultDataSeed() {
    }

    public static List<ContactEntity> contacts() {
        List<ContactEntity> list = new ArrayList<>();
        list.add(contact("c_zhangwei", "张伟", "avatar_1", "一天一小步", ContactType.NORMAL, "zhangwei_1988", "四川 绵阳", 0));
        list.add(contact("c_lina", "李娜", "avatar_2", "好好学习，天天向上", ContactType.NORMAL, "lina_na", "北京 海淀", 1));
        list.add(contact("c_wangfang", "王芳", "avatar_3", "生活不止眼前的苟且", ContactType.NORMAL, "wf_0521", "上海 浦东", 2));
        list.add(contact("c_liuqiang", "刘强", "avatar_4", "代码改变世界", ContactType.NORMAL, "lq_coder", "广东 深圳", 3));
        list.add(contact("c_chenchen", "陈晨", "avatar_5", "早睡早起", ContactType.NORMAL, "morning_cc", "浙江 杭州", 4));
        list.add(contact("c_zhaomin", "赵敏", "avatar_6", "安静地做一件事", ContactType.NORMAL, "zhaomin_m", "江苏 南京", 5));
        list.add(contact("c_zhoujie", "周杰", "avatar_1", "在路上", ContactType.NORMAL, "zj_on_road", "四川 成都", 6));
        list.add(contact("c_mom", "妈妈", "avatar_2", "记得吃饭", ContactType.NORMAL, "mama_1965", "四川 绵阳", 7));
        list.add(contact("c_dad", "老爸", "avatar_3", "身体健康", ContactType.NORMAL, "baba_1963", "四川 绵阳", 8));
        list.add(contact("c_roommates", "大学室友群", "avatar_4", "[4人]群聊", ContactType.NORMAL, "", "群聊", 9));
        list.add(contact("c_filehelper", "文件传输助手", "avatar_5", "手机与电脑之间的桥梁", ContactType.NORMAL, "", "", 10));

        // 预留：将来把这个联系人接上 Agent 时，只需要按 id 关联一份配置
        list.add(contact("c_agent_helper", "小助手", "avatar_6", "有问必答（预留 Agent 位）", ContactType.AGENT, "helper_bot", "", 11));

        // “我”自己：聊天页右侧气泡要用自己的头像，通讯录里也会出现
        list.add(contact(DataRepository.SELF_ID, "mato", "avatar_2", "这个人很懒，什么都没留下",
                ContactType.NORMAL, "mato_2024", "四川 绵阳", 12));
        return list;
    }

    /**
     * 默认会话。
     *
     * @param now 安装时刻，历史消息按相对时间往前推
     */
    public static List<ConversationEntity> conversations(long now) {
        List<ConversationEntity> list = new ArrayList<>();
        list.add(conversation("conv_1", "c_zhangwei", "明天下午三点，老地方见", now - 8 * MINUTE, 2, false));
        list.add(conversation("conv_2", "c_lina", "作业我传你邮箱了，记得查收", now - 35 * MINUTE, 1, false));
        list.add(conversation("conv_3", "c_wangfang", "[图片]", now - 2 * HOUR, 0, false));
        list.add(conversation("conv_4", "c_roommates", "刘强:晚上撸串谁去", now - 5 * HOUR, 5, true));
        list.add(conversation("conv_5", "c_mom", "记得吃饭", now - DAY, 0, false));
        list.add(conversation("conv_6", "c_liuqiang", "代码我提交到 master 了", now - DAY - 3 * HOUR, 0, false));
        list.add(conversation("conv_7", "c_filehelper", "实验报告.docx", now - 2 * DAY, 0, false));
        list.add(conversation("conv_8", "c_agent_helper", "你好，我是小助手，有什么可以帮你的吗", now - 3 * DAY, 0, false));
        list.add(conversation("conv_9", "c_zhaomin", "好的，那就这么定", now - 4 * DAY, 0, false));
        list.add(conversation("conv_10", "c_zhoujie", "下周一考试，加油", now - 6 * DAY, 0, false));
        return list;
    }

    public static List<MessageEntity> messages(long now) {
        List<MessageEntity> list = new ArrayList<>();

        add(list, "conv_1", "c_zhangwei", "在吗", now - 2 * HOUR);
        add(list, "conv_1", DataRepository.SELF_ID, "在，怎么了", now - 100 * MINUTE);
        add(list, "conv_1", "c_zhangwei", "明天下午三点，老地方见", now - 8 * MINUTE);

        add(list, "conv_2", "c_lina", "作业我传你邮箱了，记得查收", now - 35 * MINUTE);

        add(list, "conv_3", "c_wangfang", "周末去爬山吗", now - 5 * HOUR);
        add(list, "conv_3", DataRepository.SELF_ID, "去啊，几点集合", now - 4 * HOUR);
        add(list, "conv_3", "c_wangfang", "[图片]", now - 2 * HOUR);

        add(list, "conv_4", "c_roommates", "刘强:晚上撸串谁去", now - 5 * HOUR);

        add(list, "conv_5", "c_mom", "儿子，吃饭了吗", now - DAY - 2 * HOUR);
        add(list, "conv_5", DataRepository.SELF_ID, "吃了吃了", now - DAY - HOUR);
        add(list, "conv_5", "c_mom", "记得吃饭", now - DAY);

        add(list, "conv_6", "c_liuqiang", "代码我提交到 master 了", now - DAY - 3 * HOUR);
        add(list, "conv_6", DataRepository.SELF_ID, "收到，我拉一下", now - DAY - 2 * HOUR);

        add(list, "conv_7", DataRepository.SELF_ID, "实验报告.docx", now - 2 * DAY);

        add(list, "conv_8", "c_agent_helper", "你好，我是小助手，有什么可以帮你的吗", now - 3 * DAY);

        add(list, "conv_9", DataRepository.SELF_ID, "方案我改好了，你看看", now - 4 * DAY - HOUR);
        add(list, "conv_9", "c_zhaomin", "好的，那就这么定", now - 4 * DAY);

        add(list, "conv_10", "c_zhoujie", "下周一考试，加油", now - 6 * DAY);
        return list;
    }

    /** 消息 id 用 UUID：自增序号落库后重启会归零，生成重复 id 撞主键 */
    public static String newMessageId() {
        return UUID.randomUUID().toString();
    }

    private static void add(List<MessageEntity> list, String conversationId, String senderId,
                            String content, long timestamp) {
        MessageEntity m = new MessageEntity();
        m.setId(newMessageId());
        m.setConversationId(conversationId);
        m.setSenderId(senderId);
        m.setType(Message.Type.TEXT);
        m.setContent(content);
        m.setTimestamp(timestamp);
        list.add(m);
    }

    private static ContactEntity contact(String id, String name, String avatar, String bio,
                                         ContactType type, String wechatId, String region, int sortOrder) {
        ContactEntity c = new ContactEntity();
        c.setId(id);
        c.setName(name);
        c.setAvatar(avatar);
        c.setBio(bio);
        c.setType(type);
        c.setWechatId(wechatId);
        c.setRegion(region);
        c.setSortOrder(sortOrder);
        return c;
    }

    private static ConversationEntity conversation(String convId, String peerId, String lastMessage,
                                                   long lastTime, int unread, boolean muted) {
        ConversationEntity c = new ConversationEntity();
        c.setId(convId);
        c.setPeerId(peerId);
        c.setLastMessage(lastMessage);
        c.setLastTimeMillis(lastTime);
        c.setUnreadCount(unread);
        c.setMuted(muted);
        return c;
    }
}
