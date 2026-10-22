package com.example.wechat.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 时间格式化，对齐微信的两套习惯：
 * - 会话列表：今天 HH:mm / 昨天 / 一周内星期X / 更早 M月d日
 * - 聊天窗口：HH:mm，跨天时带上 昨天 或 日期
 */
public final class TimeUtil {

    private static final long MINUTE = 60 * 1000L;
    private static final long DAY = 24 * 60 * MINUTE;

    private TimeUtil() {
    }

    /** 会话列表右上角的时间 */
    public static String formatListTime(long millis) {
        int dayOffset = dayOffset(millis);
        if (dayOffset == 0) {
            return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(millis));
        }
        if (dayOffset == 1) {
            return "昨天";
        }
        if (dayOffset < 7) {
            return weekday(millis);
        }
        return new SimpleDateFormat("M月d日", Locale.CHINA).format(new Date(millis));
    }

    /** 聊天窗口里气泡上方的时间 */
    public static String formatChatTime(long millis) {
        int dayOffset = dayOffset(millis);
        String hm = new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(millis));
        if (dayOffset == 0) {
            return hm;
        }
        if (dayOffset == 1) {
            return "昨天 " + hm;
        }
        if (dayOffset < 7) {
            return weekday(millis) + " " + hm;
        }
        return new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(new Date(millis));
    }

    /**
     * 两条消息间隔超过 5 分钟才显示时间，跟微信一致：
     * 连续对话时不重复刷时间，隔一会儿再聊才插一条时间。
     */
    public static boolean needShowTime(long previousMillis, long currentMillis) {
        return previousMillis <= 0 || currentMillis - previousMillis > 5 * MINUTE;
    }

    /** 返回 millis 距今多少天（按自然日计算，不是按 24 小时） */
    private static int dayOffset(long millis) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        int offset = today.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR);
        int yearDiff = today.get(Calendar.YEAR) - target.get(Calendar.YEAR);
        if (yearDiff != 0) {
            // 跨年时简单处理：按 365 天折算
            offset += yearDiff * 365;
        }
        return offset;
    }

    private static String weekday(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        String[] names = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        return names[cal.get(Calendar.DAY_OF_WEEK) - 1];
    }
}
