package com.example.wechat.ui.launch;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.wechat.R;
import com.example.wechat.db.InitResult;
import com.example.wechat.db.RoomDataRepository;
import com.example.wechat.ui.main.MainActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 启动页：把「数据库就绪」和「主界面创建」这两件事严格排成先后关系。
 *
 * 为什么需要它：DataRepository 是同步接口（比如 getConversations() 直接返回 List），
 * 而数据库读写必须在后台线程。同步接口 + 异步加载三者只能选——
 *   1. 主线程等待锁（会卡 UI，已否决）；
 *   2. 接口改成异步回调（要重写所有 Activity / Fragment / Adapter）；
 *   3. 保证数据就绪之后才创建主界面（本方案，改动最小，界面代码一行不改）。
 *
 * 失败处理：不假装成功、也不卡在空白页 —— 把错误摆在用户面前，让他选重试还是退出。
 * 没有 setContentView：窗口直接用主题里的 windowBackground，通常几百毫秒就跳走了。
 */
public class LauncherActivity extends AppCompatActivity {

    private static final String TAG = "LauncherActivity";

    private ExecutorService initExecutor;
    private boolean enteredMain = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final RoomDataRepository repository = RoomDataRepository.getInstance(this);
        if (repository.isReady()) {
            enterMain();
            return;
        }
        initExecutor = Executors.newSingleThreadExecutor();
        runInitialize(repository);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (initExecutor != null) {
            initExecutor.shutdownNow();
        }
    }

    private void runInitialize(final RoomDataRepository repository) {
        initExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final InitResult result = repository.initialize();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (enteredMain || isFinishing()) {
                            return;
                        }
                        if (result.isSuccess()) {
                            enterMain();
                        } else {
                            showFailure(result);
                        }
                    }
                });
            }
        });
    }

    private void enterMain() {
        enteredMain = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showFailure(final InitResult result) {
        Log.w(TAG, "初始化失败：" + result.describe());
        new AlertDialog.Builder(this)
                .setTitle(R.string.init_failed_title)
                .setMessage(getString(R.string.init_failed_message, result.describe()))
                .setCancelable(false)
                .setPositiveButton(R.string.init_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (initExecutor == null || initExecutor.isShutdown()) {
                            initExecutor = Executors.newSingleThreadExecutor();
                        }
                        runInitialize(RoomDataRepository.getInstance(LauncherActivity.this));
                    }
                })
                .setNegativeButton(R.string.init_exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }
}
