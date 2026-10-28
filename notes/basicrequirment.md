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

总结：点“我” →`tab_me` 收到点击 →`onClick` 在`TAB_IDS` 查到下标 3 →`selectTab(3)` → MeFragment 进`fl_container` 、其他三个 hide →`setSelected` 让图标变实心变绿 →`currentIndex=3` → 顶部标题变“我” → MeFragment 渲染出“我”页内容。

### 1.界面上先得有个“我”按钮
流水的起点是这个按钮本身，它在 activity_main.xml 第 179~200 行：
```xml
<LinearLayout
    android:id="@+id/tab_me"
    android:layout_width="0dp"
    android:layout_height="match_parent"
    android:layout_weight="1"
    android:gravity="center"
    android:orientation="vertical">

    <ImageView
        android:layout_width="@dimen/tab_icon_size"
        android:layout_height="@dimen/tab_icon_size"
        android:contentDescription="@string/tab_me"
        app:srcCompat="@drawable/tab_icon_me"
        app:tint="@color/tab_icon_tint" />

    <TextView
        style="@style/WeChatTabText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/space_2"
        android:text="@string/tab_me" />
</LinearLayout>
```
它所在的整条 Tab 栏是 第 74~202 行 的`ll_tabs` ，里面从左到右四个按钮：`tab_chat` 、`tab_contacts` 、`tab_discover` 、`tab_me` 。

按钮上方还有页面内容的“舞台”， 第 62~66 行 ：

```xml
<FrameLayout
    android:id="@+id/fl_container"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1" />
```
哪个分页被选中，它的 Fragment 就放进`fl_container` 显示。

### 2.手指点下去，点击进入 MainActivity.onClick() 
按钮被点后，事件交给 MainActivity。它先靠 TAB_IDS 第 33~35 行 的id数组判断点的是第几个：
```java
private static final int[] TAB_IDS = {
        R.id.tab_chat, R.id.tab_contacts, R.id.tab_discover, R.id.tab_me
};
```
这四个按钮的监听是在 第 68~73 行 装的：
```java
for (int id : TAB_IDS) {
    View tab = findViewById(id);
    tab.setOnClickListener(this);
    tabViews.add(tab);
}
```
遍历`TAB_IDS` ，用 id 找到四个按钮，让 Activity 自己当监听器，并把按钮存进`tabViews` 备用。

点击真正落到的代码是 第 99~105 行 ：
```java
for (int i = 0; i < TAB_IDS.length; i++) {
    if (TAB_IDS[i] == id) {
        selectTab(i);
        return;
    }
}
```
拿被点控件的 id 去表里找下标。点到“我”时，`id` 是`R.id.tab_me` ，在下标 3 命中，于是执行`selectTab(3)` 。

###3.selectTab(3) 把「我」页装进内容区
第 108~126 行 ：
```java
for (int i = 0; i < fragments.size(); i++) {
    Fragment f = fragments.get(i);
    if (i == index) {
        if (fragmentsAdded[i]) {
            ft.show(f);          // 之前加过，直接显示
        } else {
            ft.add(R.id.fl_container, f);  // 第一次点，装进内容区
            fragmentsAdded[i] = true;
        }
    } else if (fragmentsAdded[i]) {
        ft.hide(f);              // 其他页藏起来
    }
}
ft.commit();
```
1. 被选中的 Fragment：第一次点 →`add` 进`fl_container` ；以后点 →`show` 。
2. 其他 Fragment →`hide` （不销毁，切回来滚动位置还在）。
3. `ft.commit()` 提交这次增删显示的操作。
这里的“第 3 个 Fragment”就是 MeFragment，它是在 第 75~80 行 提前按顺序准备好的：
```java
chatListFragment = new ChatListFragment();
fragments.add(chatListFragment);
fragments.add(new ContactsFragment());
fragments.add(new DiscoverFragment());
fragments.add(new MeFragment());
```

### 4.setSelected 触发图标和文字变色
内容切完，紧接着是 Tab 按钮的视觉状态， 第 128~131 行 ：
```java
for (int i = 0; i < tabViews.size(); i++) {
    tabViews.get(i).setSelected(i == index);
}
```

### 5.currentIndex + updateUnread 改顶部标题
第 133~136 行 ：
```java
currentIndex = index;
updateUnread();
```
先记住当前是第几个 Tab，再刷新顶部。`updateUnread()` 里用 getTabTitle 第 183~194 行 把下标翻译成标题：
这里要添加新的me标题
```java
private String getTabTitle(int index) {
    switch (index) {
        case 1: return getString(R.string.tab_contacts);
        case 2: return getString(R.string.tab_discover);
        case 3: return getString(R.string.tab_me);
        default: return getString(R.string.tab_chat);
    }
}
```
### 6.内容页由 MeFragment.java + fragment_me.xml 渲染


## 3.在第一个微信分页里插入一个ListView控件，并显示10个好友姓名。（20分）
### 1.在微信分页的布局里插入 ListView
fragment_chat_list.xml:
```xml
<ListView
            android:id="@+id/lv_conversations"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:background="@color/card_bg"
            android:divider="@null"
            android:dividerHeight="0dp"
            android:overScrollMode="never" />
```
### 2.每一行长什么样 —— item_conversation.xml
### 3.10 个好友的数据从哪来 —— DefaultDataSeed
### 4.微信分页拿到 ListView 并装上适配器
**适配器：翻译 + 装配**
在ChatListFragment.java；
绑定 ListView、装适配器， 第 48~54 行 ：
```java
tvEmpty = view.findViewById(R.id.tv_empty);
listView = view.findViewById(R.id.lv_conversations);

adapter = new ConversationListAdapter(requireContext());
listView.setAdapter(adapter);
```
### 5.取 10 条数据并刷新列表
第 86-87 行 首次进入就调`refresh("")` ，具体在 第 99~103 行 ：

```Java
private void refresh(String keyword) {
    List<Conversation> conversations = repository.getConversations();
    adapter.setData(conversations, keyword);
    tvEmpty.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
}
```

作用：

- `repository.getConversations()` ：从数据层取全部会话（首次安装就是那 10 条），接口定义在 DataRepository.java 第 32 行 ，真实实现按“置顶优先、时间倒序”排序。
- `adapter.setData(...)` ：把数据交给适配器，同时触发`notifyDataSetChanged()` 让 ListView 重画（ ConversationListAdapter 第 46~66 行 ）。
- `tvEmpty` ：列表为空才显示“暂时没有内容”，有 10 条时隐藏。

### 6.ListView 向适配器要“有几行、每行怎么画”
在ConversationListAdapter.java:
```java
@Override
public int getCount() { return data.size(); }          // 有几行 → 10

@Override
public Conversation getItem(int position) { return data.get(position); }  // 第 position 行的数据

@Override
public View getView(int position, View convertView, ViewGroup parent) { ... } // 这一行怎么画
```

### 7.getView 把“好友姓名”填进 TextView
这是整题最关键的一段， 第 90-96 行 负责“造行 / 复用行”：
真正把姓名写上去的在 第 99~115 行 ：


## 4.使用自定义ListView控件，让“微信”分页每行显示好友的头像、姓名、最近消息以及发消息时间。（10分）
## 5.实现点击“微信”分页每行好友后弹出一个Tosast提示框消息。（10分）
## 6.参考微信里“我”分页，顶部使用ImageView、TextView控件显示实验三的登录用户头像、姓名。（10分）
## 7.使用自定义ListView控件完善“我”分页内列表内容的显示，如服务、收藏、朋友圈、作品、小店与卡包、表情、设置（10分）
## 8.参考微信界面修改导航栏底部图标（需要使用矢量图类型图标，位图类型无法正常显示）。（10分）
