package com.example.wechat.ui.main;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.wechat.R;
import com.example.wechat.data.DataProvider;
import com.example.wechat.data.WriteFailureListener;
import com.example.wechat.util.InsetsHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 主界面：顶部标题栏 + 内容区 + 底部四个 Tab。
 *
 * 四个 Fragment 常驻内存，用 show/hide 切换（不用 ViewPager）：
 * 微信的 Tab 本来就不能左右滑，而且这样切回来列表位置、滚动状态都还在。
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, WriteFailureListener {

    private static final int[] TAB_IDS = {
            R.id.tab_chat, R.id.tab_contacts, R.id.tab_discover, R.id.tab_me
    };

    private final List<View> tabViews = new ArrayList<>();
    private final List<Fragment> fragments = new ArrayList<>();
    private final boolean[] fragmentsAdded = new boolean[TAB_IDS.length];

    private ChatListFragment chatListFragment;

    private TextView tvTitle;
    private TextView tvTabBadge;
    private ImageView ivAction;
    private int currentIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // —— 1. 加载布局 ——
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // —— 2. 状态栏 / 导航栏适配 ——
        // targetSdk 36 在 Android 15+ 上会被强制画到状态栏/导航栏下面，
        // 顶部把标题栏顶下来，底部留一条和 Tab 栏同色的导航区域
        InsetsHelper.applySystemBars(findViewById(R.id.main_root), findViewById(R.id.nav_bar_space));

        // —— 3. 绑定标题栏控件 ——
        tvTitle = findViewById(R.id.tv_title);
        tvTabBadge = findViewById(R.id.tv_tab_badge);
        ivAction = findViewById(R.id.iv_action);
        ivAction.setOnClickListener(this);

        // —— 4. 四个 Tab 统一用 Activity 自己当监听器 ——
        for (int id : TAB_IDS) {
            View tab = findViewById(id);
            tab.setOnClickListener(this);
            tabViews.add(tab);
        }

        // —— 5. 建四个 Fragment，顺序必须和 TAB_IDS 一致（按下标对应）——
        chatListFragment = new ChatListFragment();
        fragments.add(chatListFragment);
        fragments.add(new ContactsFragment());
        fragments.add(new DiscoverFragment());
        fragments.add(new MeFragment());

        // —— 6. 默认停在「微信」页 ——
        selectTab(0);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        // 右上角加号
        if (id == R.id.iv_action) {
            showActionMenu();
            return;
        }
        // 底部四个 Tab：看 id 落在 TAB_IDS 的哪个位置，就是第几个 Tab
        for (int i = 0; i < TAB_IDS.length; i++) {
            if (TAB_IDS[i] == id) {
                selectTab(i);
                return;
            }
        }
    }

    private void selectTab(int index) {
        if (index == currentIndex) {
            return;
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        for (int i = 0; i < fragments.size(); i++) {
            Fragment f = fragments.get(i);
            if (i == index) {
                if (fragmentsAdded[i]) {
                    ft.show(f);
                } else {
                    ft.add(R.id.fl_container, f);
                    fragmentsAdded[i] = true;
                }
            } else if (fragmentsAdded[i]) {
                ft.hide(f);
            }
        }
        ft.commit();

        for (int i = 0; i < tabViews.size(); i++) {
            // setSelected 会向下传给子 View，图标和文字的颜色/填充态靠它切换
            tabViews.get(i).setSelected(i == index);
        }

        // 必须先记住当前 Tab 再刷新标题：updateUnread() 是拿 currentIndex 去查标题文字的，
        // 赋值放在后面会让「我」页顶着「微信(N)」的标题
        currentIndex = index;
        updateUnread();
        // 发现页和「我」页右上角没有操作入口
        ivAction.setVisibility(index <= 1 ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从聊天页返回后，标题和 Tab 角标上的未读总数要跟着变
        updateUnread();
        // 回到前台就接管写失败通道：置顶、长按菜单这些写操作都发生在主界面里
        DataProvider.get().setWriteFailureListener(this);
    }

    /**
     * 写库失败：数据层已经把内存回滚了，这里只需要把界面重新读一遍。
     * 否则会出现「列表显示已置顶 / 未读已清零，其实磁盘上还是旧的」这种假成功。
     */
    @Override
    public void onWriteFailed(String conversationId, String messageId) {
        updateUnread();
        if (chatListFragment != null) {
            chatListFragment.refreshList();
        }
    }

    /**
     * 刷新未读相关的显示：标题上的「微信(N)」和 Tab 图标右上角的角标。
     * public 是因为会话列表里长按菜单改了未读后（还在同一个 Activity 里）要手动同步一次。
     */
    public void updateUnread() {
        int total = DataProvider.get().getTotalUnreadCount();
        String title = getTabTitle(currentIndex < 0 ? 0 : currentIndex);
        if (currentIndex <= 0 && total > 0) {
            title = title + "(" + total + ")";
        }
        tvTitle.setText(title);

        if (total > 0) {
            tvTabBadge.setVisibility(View.VISIBLE);
            tvTabBadge.setText(total > 99 ? "99+" : String.valueOf(total));
        } else {
            tvTabBadge.setVisibility(View.GONE);
        }
    }

    private String getTabTitle(int index) {
        switch (index) {
            case 1:
                return getString(R.string.tab_contacts);
            case 2:
                return getString(R.string.tab_discover);
            case 3:
                return getString(R.string.tab_me);
            default:
                return getString(R.string.tab_chat);
        }
    }

    /**
     * 右上角加号的下拉菜单：本期只做弹出和反馈，页面本身后续再做。
     *
     * 用 ListPopupWindow 而不是 PopupMenu：它可以直接传 ArrayAdapter，
     * 后面想换成带图标的自定义行时不用换控件。
     */
    private void showActionMenu() {
        // 菜单项文字都放在 strings.xml 里，方便以后统一改文案
        final String[] items = {
                getString(R.string.menu_group_chat),
                getString(R.string.menu_add_friend),
                getString(R.string.menu_scan),
                getString(R.string.menu_pay)
        };
        final ListPopupWindow popup = new ListPopupWindow(this);
        // 锚点＝加号图标，弹窗会贴着它下方展开
        popup.setAnchorView(ivAction);
        // setWidth 的单位是 px，这里把 150dp 按屏幕密度换算过去
        popup.setWidth((int) (getResources().getDisplayMetrics().density * 150));
        popup.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, Arrays.asList(items)));
        popup.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, items[position] + "：" + getString(R.string.msg_not_implemented),
                        Toast.LENGTH_SHORT).show();
                popup.dismiss();
            }
        });
        popup.show();
    }
}
