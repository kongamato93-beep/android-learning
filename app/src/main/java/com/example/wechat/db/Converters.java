package com.example.wechat.db;

import androidx.room.TypeConverter;

import com.example.wechat.model.ContactType;
import com.example.wechat.model.Message;

/**
 * 枚举在 SQLite 里没有对应类型，存成字符串读回来。
 *
 * 用 name() 而不是 ordinal()：将来枚举顺序调整不会把历史数据读错。
 */
public class Converters {

    @TypeConverter
    public static String fromContactType(ContactType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static ContactType toContactType(String value) {
        if (value == null) {
            return ContactType.NORMAL;
        }
        try {
            return ContactType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return ContactType.NORMAL;
        }
    }

    @TypeConverter
    public static String fromMessageType(Message.Type type) {
        return type == null ? Message.Type.TEXT.name() : type.name();
    }

    @TypeConverter
    public static Message.Type toMessageType(String value) {
        if (value == null) {
            return Message.Type.TEXT;
        }
        try {
            return Message.Type.valueOf(value);
        } catch (IllegalArgumentException e) {
            return Message.Type.TEXT;
        }
    }
}
