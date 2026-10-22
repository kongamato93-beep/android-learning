package com.example.wechat.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;
import androidx.room.TypeConverters;

import com.example.wechat.db.entity.ContactEntity;
import com.example.wechat.db.entity.ConversationEntity;
import com.example.wechat.db.entity.MessageEntity;
import com.example.wechat.db.entity.MetaEntity;

/**
 * 本地数据库（Room / SQLite）。
 *
 * version = 1，并且**不开** fallbackToDestructiveMigration：
 * 那个开关会在版本变更时静默清空整库，跟“数据不能丢”正好相反。
 * 将来改表结构必须写正式的 Migration(startVersion, endVersion)。
 *
 * exportSchema = true：Room 会把每个版本的建表语句导出成 JSON，
 * 落在 app/schemas/ 下，写迁移时可以直接对着生成 Migration 的 SQL。
 */
@Database(
        entities = {ContactEntity.class, ConversationEntity.class, MessageEntity.class, MetaEntity.class},
        version = 1,
        exportSchema = true
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public static final String DB_NAME = "wechat.db";

    public abstract ContactDao contactDao();

    public abstract ConversationDao conversationDao();

    public abstract MessageDao messageDao();

    public abstract MetaDao metaDao();

    /**
     * 追加一条消息，同时把会话的最后一条 / 时间 / 未读一起落库。
     *
     * 必须在一个事务里：否则进程中途被杀会出现「消息存了但会话没更新」的中间态。
     * 由 {@link RoomDataRepository} 在写线程上调用，不占主线程。
     */
    @Transaction
    public void appendMessage(MessageEntity message, ConversationEntity conversation) {
        messageDao().insert(message);
        conversationDao().upsert(conversation);
    }

    /**
     * 新建会话。先查再插，check-then-insert 必须在同一事务里才不会有竞态。
     * 返回 true 表示这次真的插入了。
     */
    @Transaction
    public boolean createConversationIfAbsent(ConversationEntity conversation) {
        if (conversationDao().getById(conversation.getId()) != null) {
            return false;
        }
        conversationDao().upsert(conversation);
        return true;
    }
}
