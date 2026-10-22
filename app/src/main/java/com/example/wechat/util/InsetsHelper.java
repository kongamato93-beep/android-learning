package com.example.wechat.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * 统一处理系统栏 / 软键盘的遮挡，所有页面共用一套逻辑。
 *
 * 背景：targetSdk >= 35（本项目是 36）以后，Android 15+ 强制 edge-to-edge，
 * 窗口会一直画到状态栏和导航栏下面。我们从来没把内容顶开过，
 * 所以标题被状态栏的时间/电量压住，底部 Tab 也会被导航条压住。
 *
 * 做法：把 systemBars（并上挖孔区域）的实际高度补成 padding，
 * 顶部垫多少完全由系统给的 inset 决定，不写死高度、也不猜机型。
 *
 * 两个判断保证偏移只算一次，不会和系统补的重叠：
 *  - {@link #enterEdgeToEdge}：不等系统判断，主动设定 layout 会画到系统栏之下，
 *    这样 Android 15 以下和以上走同一套逻辑 —— 我们自己放开了自己 Edge-to-edge，
 *    就必然由我们自己把内容顶回来，不存在「两边各补一次」的情况。
 *  - 底部取 max(导航栏, 键盘)：键盘升起时窗口已经缩到键盘上方，
 *    两者不会同时有值，取大值保证剩下的那一份就是正确的。
 */
public final class InsetsHelper {

    /** 同时关注系统栏和挖孔：有刘海/挖孔的机型上，状态栏高度藏在 displayCutout 里 */
    private static final int TYPE_BARS = WindowInsetsCompat.Type.systemBars()
            | WindowInsetsCompat.Type.displayCutout();

    private InsetsHelper() {
    }

    public interface Callback {
        /** 因适配 inset 改动了 padding 之后回调，参数是本次用到的上下 inset */
        void onInsetsApplied(int top, int bottom);
    }

    /**
     * @param root         根布局，顶部被顶下来的是它
     * @param navBarFiller 底部留白要自带底色时传它（主界面的 Tab 栏下面那条）；
     *                     传 null 表示底部留白直接用根布局自己的背景
     */
    public static void applySystemBars(View root, View navBarFiller) {
        apply(root, navBarFiller, false, null);
    }

    /** 聊天页：顶部让开状态栏，底部在「键盘」和「导航栏」里取更高的那个 */
    public static void applyWithKeyboard(View root, Callback callback) {
        apply(root, null, true, callback);
    }

    private static void apply(final View topTarget, final View navBarFiller,
                              final boolean withKeyboard, final Callback callback) {
        enterEdgeToEdge(topTarget.getContext());
        ViewCompat.setOnApplyWindowInsetsListener(topTarget, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                return dispatch(topTarget, navBarFiller, withKeyboard, callback, insets);
            }
        });
        ViewCompat.requestApplyInsets(topTarget);
        // 兜底：个别 ROM 首次 attach 时 insets 还没算好，布局完成后再要一次
        topTarget.post(new Runnable() {
            @Override
            public void run() {
                ViewCompat.requestApplyInsets(topTarget);
            }
        });
    }

    private static WindowInsetsCompat dispatch(View topTarget, View navBarFiller,
                                               boolean withKeyboard, Callback callback,
                                               WindowInsetsCompat insets) {
        int top = insets.getInsets(TYPE_BARS).top;
        int navBottom = insets.getInsets(TYPE_BARS).bottom;

        int keyboardBottom = withKeyboard
                ? insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                : 0;

        // 底部交给谁：有填充条时它负责导航栏这一段（顺带把自己的底色铺过去），
        // 根布局只管键盘；没有填充条时才能在根布局上取两者的最大值。
        int rootBottom;
        boolean navChanged;
        if (navBarFiller != null) {
            navChanged = setHeight(navBarFiller, navBottom);
            rootBottom = keyboardBottom;
        } else {
            navChanged = false;
            rootBottom = Math.max(navBottom, keyboardBottom);
        }

        boolean topChanged = setPaddingTop(topTarget, top);
        boolean bottomChanged = setPaddingBottom(topTarget, rootBottom);
        if ((topChanged || bottomChanged || navChanged) && callback != null) {
            callback.onInsetsApplied(top, navBarFiller != null ? navBottom : rootBottom);
        }
        return insets;
    }

    private static boolean setPaddingTop(View v, int top) {
        if (v.getPaddingTop() == top) {
            return false;
        }
        v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
        return true;
    }

    private static boolean setPaddingBottom(View v, int bottom) {
        if (v.getPaddingBottom() == bottom) {
            return false;
        }
        v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
        return true;
    }

    /**
     * 改高度而不是加 padding：填充条自身就是一条纯色 View，
     * 垫出来的那一条要能染成 Tab 栏的底色，用 padding 是染不上去的。
     */
    private static boolean setHeight(View v, int height) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp == null || lp.height == height) {
            return false;
        }
        lp.height = height;
        v.setLayoutParams(lp);
        return true;
    }

    /**
     * 主动进入 edge-to-edge：content 允许画到系统栏下面，偏移全部交给本类补。
     *
     * 为什么不等系统自己判断：Android 15 以上强制这样做，15 以下默认是避开系统栏的。
     * 二者混在一起时，同一份代码在不同机器上会得到不同的 inset，
     * 还得额外猜「系统有没有已经补过」。统一成一种行为，判据就简单了。
     */
    private static void enterEdgeToEdge(Context context) {
        Activity activity = findActivity(context);
        if (activity != null) {
            WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        }
    }

    /**
     * AppCompat 给 View 的 context 套了一层 ContextThemeWrapper，
     * 所以不能直接 instanceof Activity，要一路剥到真正的 Activity。
     */
    private static Activity findActivity(Context context) {
        Context ctx = context;
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity) {
                return (Activity) ctx;
            }
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }
}
