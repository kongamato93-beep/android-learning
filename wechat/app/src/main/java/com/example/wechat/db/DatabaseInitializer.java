package com.example.wechat.db;

import com.example.wechat.db.entity.MetaEntity;

/**
 * 默认数据的一次性初始化。
 *
 * 判断依据是 db_meta 里的独立标记，而不是「会话表是否为空」：
 * 空表判断在用户删光会话后会失效 —— 下次启动会当成首次安装，重新灌入默认数据。
 *
 * 原子性：三张表的插入和标记的写入放在同一个事务里，
 * 只有全部成功，标记才存在。中途失败 → 整事务回滚 → 下次启动重新尝试，
 * 不会出现「数据灌了一半但被当成已完成」的状态。
 */
public final class DatabaseInitializer {

    /** 标记：默认数据已经灌过 */
    public static final String KEY_DEFAULT_DATA = "default_data_seeded";
    /** 默认数据的版本，将来要补默认数据（比如加 Agent 联系人）时按版本增量处理 */
    public static final String KEY_DEFAULT_VERSION = "default_data_version";
    public static final String DEFAULT_VERSION = "1";

    private DatabaseInitializer() {
    }

    /** @return true 表示这次真的插入了默认数据 */
    public static boolean ensureDefaultData(final AppDatabase db) {
        if (db.metaDao().getValue(KEY_DEFAULT_DATA) != null) {
            return false;
        }
        db.runInTransaction(new Runnable() {
            @Override
            public void run() {
                // 事务里再确认一次：防止外层判断和这里之间被别的线程抢先插过
                if (db.metaDao().getValue(KEY_DEFAULT_DATA) != null) {
                    return;
                }
                long now = System.currentTimeMillis();
                db.contactDao().upsertAll(DefaultDataSeed.contacts());
                db.conversationDao().upsertAll(DefaultDataSeed.conversations(now));
                db.messageDao().insertAll(DefaultDataSeed.messages(now));
                db.metaDao().upsert(new MetaEntity(KEY_DEFAULT_DATA, "1"));
                db.metaDao().upsert(new MetaEntity(KEY_DEFAULT_VERSION, DEFAULT_VERSION));
            }
        });
        return true;
    }
}
