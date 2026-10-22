package com.example.wechat;

import android.app.Application;

import com.example.wechat.data.DataProvider;
import com.example.wechat.db.RoomDataRepository;

/**
 * 唯一的初始化入口。
 *
 * 这里只把数据仓库挂到 DataProvider 上，**不在这里读数据库**：
 * 数据库的首轮初始化（建表 + 灌默认数据 + 载入内存）由 LauncherActivity
 * 在后台线程完成，见 {@link com.example.wechat.ui.launch.LauncherActivity}。
 */
public class WeChatApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // getInstance 只是构造对象（Room 的 build() 不开库），
        // 真正读盘在后台线程，绝不放主线程
        DataProvider.init(RoomDataRepository.getInstance(this));
    }
}
