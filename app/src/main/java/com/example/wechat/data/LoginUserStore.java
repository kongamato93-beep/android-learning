package com.example.wechat.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.wechat.R;

/**
 * 当前登录用户：「我」分页顶部要显示的头像和姓名。
 *
 * 数据来源是实验三（{@code com.example.myapplication}）的登录账号：
 * 那个 App 把注册用户存在自己的 SharedPreferences（文件名 "users"）里，
 * 演示账号是 5120244700，注册时选的头像索引是 0（对应 avatar_1）。
 * 两个 App 包名不同，Android 7 以后跨包读不到对方的私有 SharedPreferences，
 * 所以这里把实验三的账号信息作为默认值带过来，头像图片也复制成了 login_avatar_1~3。
 *
 * 如果本机 App 里另外保存过登录用户（比如以后自己加登录页），以保存的为准；
 * 没保存过就用实验三那个账号，保证「我」页顶部和实验三对得上。
 */
public final class LoginUserStore {

    private static final String PREF_NAME = "login_user";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_AVATAR_INDEX = "avatar_index";

    /** 实验三里登录的那个账号（演示账号，也是注册页可以自己注册的账号之一） */
    private static final String DEFAULT_USERNAME = "5120244700";
    /** 实验三里该账号的头像索引：0 → avatar_1 */
    private static final int DEFAULT_AVATAR_INDEX = 0;

    private static final int[] AVATAR_RES = {
            R.drawable.login_avatar_1,
            R.drawable.login_avatar_2,
            R.drawable.login_avatar_3
    };

    private final SharedPreferences sp;

    public LoginUserStore(@NonNull Context context) {
        sp = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getUsername() {
        return sp.getString(KEY_USERNAME, DEFAULT_USERNAME);
    }

    /** 头像资源 id；索引越界时退回第 0 个，避免 ArrayIndexOutOfBoundsException */
    public int getAvatarRes() {
        int index = sp.getInt(KEY_AVATAR_INDEX, DEFAULT_AVATAR_INDEX);
        if (index < 0 || index >= AVATAR_RES.length) {
            index = 0;
        }
        return AVATAR_RES[index];
    }

    /** 供以后本 App 自己加登录页时使用；现在没人调用，保留接口即可 */
    public void save(String username, int avatarIndex) {
        sp.edit()
                .putString(KEY_USERNAME, username)
                .putInt(KEY_AVATAR_INDEX, avatarIndex)
                .apply();
    }
}
