package com.example.wechat.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.fragment.app.Fragment;

import com.example.wechat.R;
import com.example.wechat.adapter.ConversationListAdapter;
import com.example.wechat.data.DataProvider;
import com.example.wechat.data.DataRepository;
import com.example.wechat.model.Contact;
import com.example.wechat.model.Conversation;
import com.example.wechat.ui.chat.ChatActivity;

import java.util.ArrayList;
import java.util.List;

/** 微信 Tab：搜索栏 + 会话列表 */
public class ChatListFragment extends Fragment {

    private final DataRepository repository = DataProvider.get();
    private ConversationListAdapter adapter;
    private EditText etSearch;
    private TextView tvEmpty;
    private ListView listView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // —— 1. 绑定控件：搜索框、空状态提示、会话 ListView ——
        etSearch = view.findViewById(R.id.et_search);
        tvEmpty = view.findViewById(R.id.tv_empty);
        listView = view.findViewById(R.id.lv_conversations);

        // —— 2. 给 ListView 装上自定义适配器（BaseAdapter + ViewHolder）——
        adapter = new ConversationListAdapter(requireContext());
        listView.setAdapter(adapter);

        // 点击某一行：先弹 Toast 提示点到了谁，再进聊天页
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Conversation conversation = adapter.getItem(position);
                if (conversation == null) {
                    return;
                }
                Toast.makeText(requireContext(),
                        getString(R.string.msg_click_conversation, displayName(conversation)),
                        Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(requireContext(), ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getId());
                startActivity(intent);
            }
        });

        // 长按：置顶 / 测试收消息，和之前一致
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Conversation conversation = adapter.getItem(position);
                if (conversation != null) {
                    showConversationMenu(view, conversation);
                }
                return true;
            }
        });

        // —— 3. 搜索框：输入变化时重新过滤会话列表 ——
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refresh(s == null ? "" : s.toString());
            }
        });

        // —— 4. 首次进入：关键字为空，显示全部会话 ——
        refresh("");
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从聊天页返回后，会话的最后一条消息和时间要跟着变
        if (adapter != null) {
            String keyword = etSearch == null ? "" : etSearch.getText().toString();
            refresh(keyword);
        }
    }

    private void refresh(String keyword) {
        List<Conversation> conversations = repository.getConversations();
        adapter.setData(conversations, keyword);
        tvEmpty.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
    }

    /** Toast 里显示的名字：取对方的昵称，取不到就退回会话 id，避免显示空白 */
    private String displayName(Conversation conversation) {
        Contact peer = repository.getContact(conversation.getPeerId());
        return peer == null ? conversation.getId() : peer.getName();
    }

    /**
     * 供 MainActivity 在「写库失败回滚」后调用：重新读一次数据刷新列表。
     * 和 onResume 里的刷新是同一套逻辑，只是入口不同。
     */
    public void refreshList() {
        if (adapter == null) {
            return;
        }
        refresh(etSearch == null ? "" : etSearch.getText().toString());
    }

    /**
     * 长按会话的菜单。
     *
     * 置顶入口按微信的习惯放在长按里；「[测试] 收到一条消息」是开发验证用的，
     * 单机没有真实来信，靠它触发 receiveTextMessage。
     */
    private void showConversationMenu(View anchor, final Conversation conversation) {
        final List<String> items = new ArrayList<>();
        items.add(conversation.isPinned()
                ? getString(R.string.menu_unpin) : getString(R.string.menu_pin));
        items.add(getString(R.string.debug_receive_conversation));
        final List<String> itemLabels = new ArrayList<>(items);

        final ListPopupWindow popup = new ListPopupWindow(requireContext());
        popup.setAnchorView(anchor);
        popup.setWidth((int) (getResources().getDisplayMetrics().density * 180));
        popup.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, itemLabels));
        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    boolean pinned = repository.togglePinned(conversation.getId());
                    Toast.makeText(requireContext(),
                            pinned ? R.string.msg_pinned : R.string.msg_unpinned,
                            Toast.LENGTH_SHORT).show();
                } else {
                    repository.receiveTextMessage(conversation.getId(),
                            System.currentTimeMillis() % 100000 + "（测试消息）");
                    Toast.makeText(requireContext(), R.string.debug_received,
                            Toast.LENGTH_SHORT).show();
                }
                refresh(etSearch == null ? "" : etSearch.getText().toString());
                // 长按菜单没离开 Activity，手动把标题/Tab 上的未读总数同步一次
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateUnread();
                }
                popup.dismiss();
            }
        });
        popup.show();
    }
}
