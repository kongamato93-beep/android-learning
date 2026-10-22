package com.example.wechat.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

/**
 * 通用的一行入口（发现页、我页、设置页、联系人详情页都用它）。
 *
 * 做成数据而不是写死在 XML 里，是为了让三个页面共用同一套行布局和分组间距，
 * 加一行只要在数据列表里加一个元素。
 */
public class EntryItem {

    private final int iconRes;
    private final int tintRes;
    private final String title;
    private String extra;
    /** 是否在这一行上方留出分组间隔（微信用灰色块分隔功能区） */
    private boolean startGroup;
    /** 是否显示行底部的分隔线 */
    private boolean showDivider = true;
    /** 标题颜色，0 表示用默认色（退出登录那种红字行会用到） */
    private int titleColorRes;

    public EntryItem(@DrawableRes int iconRes, @ColorRes int tintRes, String title) {
        this.iconRes = iconRes;
        this.tintRes = tintRes;
        this.title = title;
    }

    public int getIconRes() {
        return iconRes;
    }

    public int getTintRes() {
        return tintRes;
    }

    public String getTitle() {
        return title;
    }

    public String getExtra() {
        return extra;
    }

    public EntryItem setExtra(String extra) {
        this.extra = extra;
        return this;
    }

    public boolean isStartGroup() {
        return startGroup;
    }

    public EntryItem setStartGroup(boolean startGroup) {
        this.startGroup = startGroup;
        return this;
    }

    public boolean isShowDivider() {
        return showDivider;
    }

    public EntryItem setShowDivider(boolean showDivider) {
        this.showDivider = showDivider;
        return this;
    }

    public int getTitleColorRes() {
        return titleColorRes;
    }

    public EntryItem setTitleColorRes(int titleColorRes) {
        this.titleColorRes = titleColorRes;
        return this;
    }
}
