package com.example.wechat.db.mapper;

import com.example.wechat.db.entity.ContactEntity;
import com.example.wechat.db.entity.ConversationEntity;
import com.example.wechat.db.entity.MessageEntity;
import com.example.wechat.model.Contact;
import com.example.wechat.model.Conversation;
import com.example.wechat.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity（数据库行）与 Model（业务对象）之间的转换。
 *
 * 全手写静态方法：不引第三方映射库，不做反射。
 * 这层存在的意义是让两边可以各自演进 —— 改业务字段不必碰表结构，改表结构不必碰界面。
 */
public final class EntityMapper {

    private EntityMapper() {
    }

    public static Contact toContact(ContactEntity entity) {
        if (entity == null) {
            return null;
        }
        Contact c = new Contact(entity.getId(), entity.getName(), entity.getAvatar());
        c.setBio(entity.getBio());
        c.setType(entity.getType());
        c.setWechatId(entity.getWechatId());
        c.setRegion(entity.getRegion());
        c.setSortOrder(entity.getSortOrder());
        return c;
    }

    public static ContactEntity toContactEntity(Contact c) {
        if (c == null) {
            return null;
        }
        ContactEntity entity = new ContactEntity();
        entity.setId(c.getId());
        entity.setName(c.getName());
        entity.setAvatar(c.getAvatar());
        entity.setBio(c.getBio());
        entity.setType(c.getType());
        entity.setWechatId(c.getWechatId());
        entity.setRegion(c.getRegion());
        entity.setSortOrder(c.getSortOrder());
        return entity;
    }

    public static List<Contact> toContacts(List<ContactEntity> entities) {
        List<Contact> list = new ArrayList<>();
        if (entities == null) {
            return list;
        }
        for (ContactEntity e : entities) {
            list.add(toContact(e));
        }
        return list;
    }

    public static List<ContactEntity> toContactEntities(List<Contact> contacts) {
        List<ContactEntity> list = new ArrayList<>();
        if (contacts == null) {
            return list;
        }
        for (Contact c : contacts) {
            list.add(toContactEntity(c));
        }
        return list;
    }

    public static Conversation toConversation(ConversationEntity entity) {
        if (entity == null) {
            return null;
        }
        Conversation c = new Conversation(entity.getId(), entity.getPeerId());
        c.setLastMessage(entity.getLastMessage());
        c.setLastTimeMillis(entity.getLastTimeMillis());
        c.setUnreadCount(entity.getUnreadCount());
        c.setMuted(entity.isMuted());
        c.setPinned(entity.isPinned());
        return c;
    }

    public static ConversationEntity toConversationEntity(Conversation c) {
        if (c == null) {
            return null;
        }
        ConversationEntity entity = new ConversationEntity();
        entity.setId(c.getId());
        entity.setPeerId(c.getPeerId());
        entity.setLastMessage(c.getLastMessage());
        entity.setLastTimeMillis(c.getLastTimeMillis());
        entity.setUnreadCount(c.getUnreadCount());
        entity.setMuted(c.isMuted());
        entity.setPinned(c.isPinned());
        return entity;
    }

    public static List<Conversation> toConversations(List<ConversationEntity> entities) {
        List<Conversation> list = new ArrayList<>();
        if (entities == null) {
            return list;
        }
        for (ConversationEntity e : entities) {
            list.add(toConversation(e));
        }
        return list;
    }

    public static List<ConversationEntity> toConversationEntities(List<Conversation> conversations) {
        List<ConversationEntity> list = new ArrayList<>();
        if (conversations == null) {
            return list;
        }
        for (Conversation c : conversations) {
            list.add(toConversationEntity(c));
        }
        return list;
    }

    public static Message toMessage(MessageEntity entity) {
        if (entity == null) {
            return null;
        }
        Message m = new Message(entity.getId(), entity.getConversationId(), entity.getSenderId(),
                entity.getContent(), entity.getTimestamp());
        m.setType(entity.getType() == null ? Message.Type.TEXT : entity.getType());
        return m;
    }

    public static MessageEntity toMessageEntity(Message m) {
        if (m == null) {
            return null;
        }
        MessageEntity entity = new MessageEntity();
        entity.setId(m.getId());
        entity.setConversationId(m.getConversationId());
        entity.setSenderId(m.getSenderId());
        entity.setType(m.getType() == null ? Message.Type.TEXT : m.getType());
        entity.setContent(m.getContent());
        entity.setTimestamp(m.getTimestamp());
        return entity;
    }

    public static List<Message> toMessages(List<MessageEntity> entities) {
        List<Message> list = new ArrayList<>();
        if (entities == null) {
            return list;
        }
        for (MessageEntity e : entities) {
            list.add(toMessage(e));
        }
        return list;
    }

    public static List<MessageEntity> toMessageEntities(List<Message> messages) {
        List<MessageEntity> list = new ArrayList<>();
        if (messages == null) {
            return list;
        }
        for (Message m : messages) {
            list.add(toMessageEntity(m));
        }
        return list;
    }
}
