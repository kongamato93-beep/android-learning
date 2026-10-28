## 头像从哪里来？
本地图片文件 → 数据库里的图片名 → 加载器翻译成图片 
1. 图片在`res/drawable/` 里：`avatar_1.jpg` ~`avatar_6.jpg` （联系人）、`login_avatar_1~3.jpg` （登录用户）、`avatar_default.xml` （兜底）。
2. 数据库里不存图片，只存名字。首次灌数据时写进`avatar` 字段， DefaultDataSeed.java 第 33~43 行 ：

`list.add(contact("c_zhangwei", "张伟", "avatar_1", ...));`

3. 显示时由 AvatarLoader 第 26~34 行 把字符串翻译成资源 id：
`int resId = context.getResources().getIdentifier(avatar, "drawable", context.getPackageName());
return resId == 0 ? R.drawable.avatar_default : resId;`

所以头像全程不联网、不上传，就是 APK 里自带的图片。

## 如何在我的界面显示头像
分“布局占位”和“Java 填图”两步。

布局 ： fragment_me.xml 第 21~31 行 用一个 ImageView：

`<ImageView
    android:id="@+id/iv_avatar"
    android:layout_width="@dimen/avatar_large"
    android:layout_height="@dimen/avatar_large"
    android:background="@drawable/bg_avatar_rounded"
    android:clipToOutline="true"
    android:scaleType="centerCrop" />`

- `avatar_large` = 64dp；
- `bg_avatar_rounded + clipToOutline` 把图片裁成微信式圆角方形；
- `centerCrop` 保证图片填满不变形。
Java 填图 ： MeFragment 第 41~46 行 ：


`final LoginUserStore loginUser = new LoginUserStore(requireContext());
ImageView ivAvatar = view.findViewById(R.id.iv_avatar);
TextView tvName = view.findViewById(R.id.tv_name);
...
ivAvatar.setImageResource(loginUser.getAvatarRes());
tvName.setText(loginUser.getUsername());`

头像资源由 LoginUserStore 第 51~57 行 返回：读 SharedPreferences 里的`avatar_index` ，在`login_avatar_1~3` 中选一张，默认索引 0。

数据流：`SharedPreferences(avatar_index=0)` →`login_avatar_1` →`ivAvatar.setImageResource(...)` → 屏幕显示圆角头像。

## 底部导航是如何实现4个
不是装一个系统“底部导航组件”就自动出来 4 个，而是 手写 4 个长得一样的按钮 ：

1. 一个横向的`LinearLayout` （id`ll_tabs` ）当容器， activity_main.xml 第 74~202 行 。
2. 里面放 4 个子`LinearLayout` ，每个`layout_width="0dp" + layout_weight="1"` ，所以 屏幕宽度四等分 ，各占 1/4。
3. 每个子容器里再上下放一个`ImageView` （图标）和一个`TextView` （文字）。
4. 4 个按钮的 id 分别是`tab_chat` 、`tab_contacts` 、`tab_discover` 、`tab_me` 。
以“我”为例， 第 179~200 行 ：


`<LinearLayout
    android:id="@+id/tab_me"
    android:layout_width="0dp"
    android:layout_height="match_parent"
    android:layout_weight="1"
    android:gravity="center"
    android:orientation="vertical">

    <ImageView
        android:layout_width="@dimen/tab_icon_size"
        android:layout_height="@dimen/tab_icon_size"
        app:srcCompat="@drawable/tab_icon_me"
        app:tint="@color/tab_icon_tint" />

    <TextView
        style="@style/WeChatTabText"
        android:text="@string/tab_me" />
</LinearLayout>`

选中变色靠`setSelected()` + 两个 selector：`tab_icon_me.xml` （实心/空心切换）和`tab_icon_tint.xml` （绿/灰切换）。

## 底部的四个是用那个组件实现的


## 点击如何实现切换页面
四步，全在 MainActivity.java 。

① 四个 id 排成一张表 ， 第 33~35 行 ：


`private static final int[] TAB_IDS = {
        R.id.tab_chat, R.id.tab_contacts, R.id.tab_discover, R.id.tab_me
};`

② 四个按钮统一交给 Activity 监听 ， 第 68~73 行 ：


`for (int id : TAB_IDS) {
    View tab = findViewById(id);
    tab.setOnClickListener(this);
    tabViews.add(tab);
}`

③ 点击时判断是第几个 ， 第 99~105 行 ：


`for (int i = 0; i < TAB_IDS.length; i++) {
    if (TAB_IDS[i] == id) {
        selectTab(i);
        return;
    }
}`

④ selectTab 真正换页 ， 第 108~131 行 ：


`FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
for (int i = 0; i < fragments.size(); i++) {
    Fragment f = fragments.get(i);
    if (i == index) {
        if (fragmentsAdded[i]) {
            ft.show(f);                 // 以前加过，直接显示
        } else {
            ft.add(R.id.fl_container, f); // 第一次，装进页面容器
            fragmentsAdded[i] = true;
        }
    } else if (fragmentsAdded[i]) {
        ft.hide(f);                     // 其他页面藏起来（不销毁）
    }
}
ft.commit();

for (int i = 0; i < tabViews.size(); i++) {
    tabViews.get(i).setSelected(i == index); // 触发图标/文字变色
}`

四个 Fragment 在 第 75~80 行 按同样顺序创建：


`fragments.add(chatListFragment);
fragments.add(new ContactsFragment());
fragments.add(new DiscoverFragment());
fragments.add(new MeFragment());`

`TAB_IDS` 的下标和`fragments` 的下标必须一一对应，第 0 个对应微信，第 3 个对应“我”。

### 一次点击的完整顺序


`手指点“发现”（tab_discover）
  → onClick(id = R.id.tab_discover)
  → 在 TAB_IDS 里找到下标 2
  → selectTab(2)
      → DiscoverFragment add/show 到 fl_container
      → 另外三个 Fragment hide
      → tab_discover.setSelected(true)，其他 false
          → 图标变实心绿，文字变绿
  → currentIndex = 2 → 顶部标题变“发现”`

一句话总结： 4 个 Tab 是 4 个自定义 LinearLayout 按钮，点击由 MainActivity 统一监听，按`TAB_IDS` 的下标用`FragmentTransaction` 的`add/show/hide` 在`FrameLayout` 里切换 4 个常驻 Fragment。
