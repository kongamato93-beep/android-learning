# 基本要求

## 1.使用AI工具创建一个三分页的界面程序。（20分）

### 实现思路

AI 工具生成了基础的「主界面 + 底部导航 + Fragment 切换」骨架：

- 一个 `MainActivity` 作为容器
- 底部用若干 `LinearLayout` 当 Tab 按钮
- 内容区用 `FrameLayout`，通过 `FragmentTransaction` 切换 3 个 `Fragment`

### 关键文件

1. [MainActivity.java](file:///D:/andriod/WeChat/wechat/app/src/main/java/com/example/wechat/ui/main/MainActivity.java)
2. [activity_main.xml](file:///D:/andriod/WeChat/wechat/app/src/main/res/layout/activity_main.xml)

### 关键代码（MainActivity.java）

```java
// 第 31-33 行：Tab 的 id 数组，用来循环绑定点击事件
private static final int[] TAB_IDS = {
        R.id.tab_chat, R.id.tab_contacts, R.id.tab_discover
};

// 第 70-75 行：创建 3 个 Fragment 对象（题目 1 阶段只有 3 个）
fragments.add(new ChatListFragment());
fragments.add(new ContactsFragment());
fragments.add(new DiscoverFragment());

// 第 98-129 行：selectTab() —— Tab 切换的核心
private void selectTab(int index) {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    for (int i = 0; i < fragments.size(); i++) {
        Fragment f = fragments.get(i);
        if (i == index) {
            if (fragmentsAdded[i]) {
                ft.show(f);      // 已添加过就显示
            } else {
                ft.add(R.id.fl_container, f);  // 没添加过就加到容器
                fragmentsAdded[i] = true;
            }
        } else if (fragmentsAdded[i]) {
            ft.hide(f);      // 其它 Tab 隐藏（不销毁，保留状态）
        }
    }
    ft.commit();
}
```

### 通俗解释

- `TAB_IDS` 是一个数组，里面存了 3 个底部按钮的 id，后面用 `for` 循环给每个按钮都绑定同一个点击监听器。
- 切换 Tab 时，不是用 `replace`（会销毁重建），而是 `show` / `hide`。这样切回来时列表的滚动位置还在。
- `FragmentTransaction` 可以理解为「对 Fragment 操作的记事本」，`commit()` 就是「提交这次操作」。

---

## 2.手动添加第四分页，并参照微信界面命名四个分页。（10分）

### 实现思路

在 AI 生成的 3 个 Tab 基础上，**手动**：

1. 新增第 4 个 Fragment：`MeFragment`（"我"）
2. 在 `TAB_IDS` 数组和 `fragments` 列表里都加上第 4 项
3. 在 `activity_main.xml` 底部导航栏里加第 4 个 Tab 的布局
4. 在 `strings.xml` 里定义 4 个 Tab 的文字：微信、通讯录、发现、我

### 关键文件

1. [MainActivity.java](file:///D:/andriod/WeChat/wechat/app/src/main/java/com/example/wechat/ui/main/MainActivity.java) — `TAB_IDS`、`fragments`、`getTabTitle()`
2. [activity_main.xml](file:///D:/andriod/WeChat/wechat/app/src/main/res/layout/activity_main.xml) — 底部第 4 个 Tab 布局
3. [MeFragment.java](file:///D:/andriod/WeChat/wechat/app/src/main/java/com/example/wechat/ui/main/MeFragment.java) — 新增的第 4 页
4. `res/values/strings.xml` — Tab 文字

### 关键代码（MainActivity.java）

```java
// 第 31-33 行：现在是 4 个 Tab，加了 R.id.tab_me
private static final int[] TAB_IDS = {
        R.id.tab_chat, R.id.tab_contacts, R.id.tab_discover, R.id.tab_me
};

// 第 70-75 行：fragments 列表加了 new MeFragment()
chatListFragment = new ChatListFragment();
fragments.add(chatListFragment);
fragments.add(new ContactsFragment());
fragments.add(new DiscoverFragment());
fragments.add(new MeFragment());   // ← 手动新增的第 4 个分页

// 第 172-183 行：标题栏文字随 Tab 变化
private String getTabTitle(int index) {
    switch (index) {
        case 1: return getString(R.string.tab_contacts);  // 通讯录
        case 2: return getString(R.string.tab_discover);  // 发现
        case 3: return getString(R.string.tab_me);        // 我
        default: return getString(R.string.tab_chat);     // 微信
    }
}
```

### 通俗解释

- 加第 4 个 Tab = 在数组里加一项 + 在列表里加一个 Fragment 对象。因为 `selectTab()` 是用循环遍历的，所以加完自动就能切换。
- 4 个 Tab 的名字来自 `strings.xml`：`tab_chat`="微信"、`tab_contacts`="通讯录"、`tab_discover`="发现"、`tab_me`="我"。


3.在第一个微信分页里插入一个ListView控件，并显示10个好友姓名。（20分）
4.使用自定义ListView控件，让“微信”分页每行显示好友的头像、姓名、最近消息以及发消息时间。（10分）
5.实现点击“微信”分页每行好友后弹出一个Tosast提示框消息。（10分）
6.参考微信里“我”分页，顶部使用ImageView、TextView控件显示实验三的登录用户头像、姓名。（10分）
7.使用自定义ListView控件完善“我”分页内列表内容的显示，如服务、收藏、朋友圈、作品、小店与卡包、表情、设置（10分）
8.参考微信界面修改导航栏底部图标（需要使用矢量图类型图标，位图类型无法正常显示）。（10分）
