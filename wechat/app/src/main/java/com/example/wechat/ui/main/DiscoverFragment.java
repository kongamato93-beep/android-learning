package com.example.wechat.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wechat.R;
import com.example.wechat.adapter.EntryAdapter;
import com.example.wechat.model.EntryItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 发现 Tab：朋友圈 / 视频号 / 直播 / 扫一扫 / 摇一摇 / 看一看 / 搜一搜 / 小程序。
 *
 * 和「我」页的七项功能用的是同一套数据模型 {@link EntryItem}（图标 + 配色 + 标题），
 * 区别只在容器：发现页用 RecyclerView（{@link EntryAdapter}），
 * 「我」页按实验要求改成了 ListView（{@link com.example.wechat.adapter.MeEntryListAdapter}）。
 */
public class DiscoverFragment extends Fragment {

    /**
     * 加载布局。
     * 复用 {@code fragment_entry_list.xml}——发现页和「我」页都是「一组图标入口」，
     * 只是 id 不同（rv_entries），所以共用同一个布局文件。
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entry_list, container, false);
    }

    /**
     * View 建好之后：组装 RecyclerView 并接上点击回调
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.rv_entries);
        // 线性纵向排列＝普通列表；发现页没有横向滚动的需求
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        EntryAdapter adapter = new EntryAdapter();
        adapter.setData(buildEntries());
        adapter.setOnItemClickListener(new EntryAdapter.OnItemClickListener() {
            /**
             * 点任意一项：弹出「xxx：功能未实现」的 Toast。
             * 八项入口这期只要求做出界面和点击反馈，二级页面后续再补。
             */
            @Override
            public void onItemClick(EntryItem item, int position) {
                Toast.makeText(requireContext(),
                        item.getTitle() + "：" + getString(R.string.msg_not_implemented),
                        Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    /**
     * 发现页的八项入口，按微信的顺序分四组：
     * 朋友圈/视频号/直播 → 扫一扫/摇一摇 → 看一看/搜一搜 → 小程序。
     *
     * 分组靠两个开关控制（都在 {@link EntryItem} 上）：
     * <ul>
     *   <li>{@code setStartGroup(true)}：这一项前面加一段灰色间隔，相当于另起一组</li>
     *   <li>{@code setShowDivider(false)}：去掉这一项下面的分隔线，也就是本组最后一项</li>
     * </ul>
     */
    private List<EntryItem> buildEntries() {
        List<EntryItem> items = new ArrayList<>();
        // 第一组
        items.add(new EntryItem(R.drawable.ic_moments, R.color.entry_blue,
                getString(R.string.discover_moments)));
        items.add(new EntryItem(R.drawable.ic_channels, R.color.entry_orange,
                getString(R.string.discover_channels)));
        items.add(new EntryItem(R.drawable.ic_live, R.color.entry_red,
                getString(R.string.discover_live)).setShowDivider(false));

        // 第二组
        items.add(new EntryItem(R.drawable.ic_scan, R.color.entry_blue,
                getString(R.string.discover_scan)).setStartGroup(true));
        items.add(new EntryItem(R.drawable.ic_shake, R.color.entry_blue,
                getString(R.string.discover_shake)).setShowDivider(false));

        // 第三组
        items.add(new EntryItem(R.drawable.ic_top_stories, R.color.entry_yellow,
                getString(R.string.discover_top_stories)).setStartGroup(true));
        items.add(new EntryItem(R.drawable.ic_search, R.color.entry_green,
                getString(R.string.discover_search)).setShowDivider(false));

        // 第四组：只有小程序一项，所以既另起一组，也不画分隔线
        items.add(new EntryItem(R.drawable.ic_mini_program, R.color.entry_purple,
                getString(R.string.discover_mini_program)).setStartGroup(true).setShowDivider(false));
        return items;
    }
}
