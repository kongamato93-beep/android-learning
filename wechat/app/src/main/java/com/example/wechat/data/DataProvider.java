package com.example.wechat.data;

import com.example.wechat.db.RoomDataRepository;

/**
 * 数据层的唯一出入口。
 *
 * 界面只写 DataProvider.get()，不直接碰具体实现：
 * 第一版是内存假数据，现在是 SQLite（Room）版，界面代码一行没改。
 */
public final class DataProvider {

    private static volatile DataRepository repository;

    private DataProvider() {
    }

    /**
     * 由 {@code WeChatApplication} 在 Application.onCreate 里初始化，
     * 这是所有组件之前最早的执行点，界面来取时必然已就绪。
     */
    public static synchronized void init(RoomDataRepository repository) {
        DataProvider.repository = repository;
    }

    public static DataRepository get() {
        DataRepository repo = repository;
        if (repo == null) {
            // 理论上到不了这里（Application 先跑）；兜个底，好过 NullPointerException
            throw new IllegalStateException("DataProvider 未初始化：检查 manifest 是否注册了 WeChatApplication");
        }
        return repo;
    }
}
