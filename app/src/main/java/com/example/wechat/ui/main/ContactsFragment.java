package com.example.wechat.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wechat.R;
import com.example.wechat.adapter.ContactsAdapter;
import com.example.wechat.data.DataProvider;
import com.example.wechat.data.DataRepository;
import com.example.wechat.model.Contact;
import com.example.wechat.ui.contact.ContactDetailActivity;
import com.example.wechat.widget.SideIndexView;

import java.util.ArrayList;
import java.util.List;

/**
 * 通讯录 Tab：顶部搜索框 + 四个固定入口 + A~Z 分组的联系人列表 + 右侧字母索引条。
 *
 * 这一页仍然用 RecyclerView（评分点只要求「微信」页和「我」页用 ListView，
 * 这里没动），列表的分组、字母跳转逻辑都放在 {@link ContactsAdapter} 里。
 *
 * 生命周期上要注意：Fragment 的 View 会在切 Tab 时被保留（MainActivity 用 show/hide），
 * 所以控件的初始化只做一次（在 onViewCreated），数据的刷新走 refresh()。
 */
public class ContactsFragment extends Fragment {

    /** 数据仓库：所有联系人/会话的读写入口，UI 不直接碰数据库 */
    private final DataRepository repository = DataProvider.get();
    /** 列表顶部的四个固定入口（新的朋友 / 群聊 / 标签 / 公众号） */
    private final List<ContactsAdapter.EntryRow> fixedEntries = new ArrayList<>();
    private ContactsAdapter adapter;
    private EditText etSearch;
    private RecyclerView recyclerView;

    /**
     * 创建这一页的 View。
     * 只负责 inflate 布局，findViewById 放到 {@link #onViewCreated} 里做——
     * 那是官方推荐的分工（View 建好之后再找控件）。
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    /**
     * View 建好之后：绑控件 → 装配列表 → 接上搜索和字母索引
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // —— 1. 绑定布局里的控件 ——
        etSearch = view.findViewById(R.id.et_search);
        recyclerView = view.findViewById(R.id.rv_contacts);
        SideIndexView sideIndex = view.findViewById(R.id.side_index);

        // —— 2. 准备列表顶部那四个固定入口 ——
        // 它们不属于联系人数据，也不参与字母分组，所以单独建一份列表传给适配器
        buildFixedEntries();

        // —— 3. 装配 RecyclerView ——
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContactsAdapter();
        adapter.setOnItemClickListener(new ContactsAdapter.OnItemClickListener() {
            /** 点联系人：带 id 跳转到联系人详情页 */
            @Override
            public void onContactClick(Contact contact) {
                Intent intent = new Intent(requireContext(), ContactDetailActivity.class);
                intent.putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, contact.getId());
                startActivity(intent);
            }

            /** 点固定入口：本期只做反馈，页面以后再补 */
            @Override
            public void onEntryClick(String title) {
                Toast.makeText(requireContext(), title + "：" + getString(R.string.msg_not_implemented),
                        Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        // —— 4. 右侧字母索引：手指滑到某个字母，列表滚到对应分组 ——
        sideIndex.setOnLetterChangeListener(new SideIndexView.OnLetterChangeListener() {
            @Override
            public void onLetterChanged(String letter) {
                int position = adapter.getPositionForLetter(letter);
                if (position >= 0) {
                    // scrollToPositionWithOffset(x, 0)：把该分组顶到列表最上面
                    ((LinearLayoutManager) recyclerView.getLayoutManager())
                            .scrollToPositionWithOffset(position, 0);
                }
            }
        });

        // —— 5. 搜索框：内容一变就重新过滤列表 ——
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            /** 只在文本变化结束后刷新，避免每敲一个字符都重排列表 */
            @Override
            public void afterTextChanged(Editable s) {
                refresh(s == null ? "" : s.toString());
            }
        });

        // —— 6. 首次进入：不过滤，显示全部 ——
        refresh("");
    }

    /**
     * 四个固定入口的数据。
     * 每项 = 图标 + 图标配色 + 标题，用 {@link ContactsAdapter.EntryRow} 装，
     * 适配器会把它当成联系人列表前面的几行显示。
     */
    private void buildFixedEntries() {
        fixedEntries.clear();
        fixedEntries.add(new ContactsAdapter.EntryRow(
                R.drawable.ic_new_friend, R.color.entry_blue, getString(R.string.contacts_new_friend)));
        fixedEntries.add(new ContactsAdapter.EntryRow(
                R.drawable.ic_group_chat, R.color.wechat_green, getString(R.string.contacts_group_chat)));
        fixedEntries.add(new ContactsAdapter.EntryRow(
                R.drawable.ic_tag, R.color.entry_orange, getString(R.string.contacts_tag)));
        fixedEntries.add(new ContactsAdapter.EntryRow(
                R.drawable.ic_official, R.color.entry_purple, getString(R.string.contacts_official)));
    }

    /**
     * 重新取数据并按关键字过滤。
     *
     * @param keyword 搜索关键字；传空串表示不过滤，显示全部联系人
     */
    private void refresh(String keyword) {
        // 分组、字母索引、过滤都在 build() 里一次算完，避免多次遍历
        adapter.build(repository.getContacts(), keyword, fixedEntries);
    }
}
