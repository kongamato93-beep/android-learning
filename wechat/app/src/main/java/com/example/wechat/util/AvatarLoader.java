package com.example.wechat.util;

import android.content.Context;
import android.widget.ImageView;

import com.example.wechat.R;

/**
 * 头像加载的统一入口（第二条接缝）。
 *
 * 现在的联系人头像字段存的是 drawable 名（avatar_1 这种占位图）；
 * 等 mato 导入真实图片后，只要把图片放进 res/drawable 并把名字写进 Contact.avatar 就行，
 * 或者以后改成 URL 时，在这个类里换掉解析方式，所有界面自动生效。
 */
public final class AvatarLoader {

    private AvatarLoader() {
    }

    public static void load(Context context, ImageView imageView, String avatar) {
        // vector drawable 在 minSdk 24 上原生支持，不需要 AppCompat 特殊处理
        imageView.setImageResource(resolve(context, avatar));
    }

    /** 把头像字段解析成 drawable 资源 id，解析不到就用默认灰头像 */
    private static int resolve(Context context, String avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return R.drawable.avatar_default;
        }
        int resId = context.getResources().getIdentifier(avatar, "drawable", context.getPackageName());
        if (resId == 0) {
            return R.drawable.avatar_default;
        }
        return resId;
    }
}
