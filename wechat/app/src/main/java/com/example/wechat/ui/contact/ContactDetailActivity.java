package com.example.wechat.ui.contact;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wechat.R;
import com.example.wechat.adapter.EntryAdapter;
import com.example.wechat.data.DataProvider;
import com.example.wechat.data.DataRepository;
import com.example.wechat.model.Contact;
import com.example.wechat.model.Conversation;
import com.example.wechat.model.EntryItem;
import com.example.wechat.ui.chat.ChatActivity;
import com.example.wechat.util.AvatarLoader;
import com.example.wechat.util.InsetsHelper;

import java.util.ArrayList;
import java.util.List;

/** 联系人详情页 */
public class ContactDetailActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String EXTRA_CONTACT_ID = "extra_contact_id";

    private final DataRepository repository = DataProvider.get();
    private Contact contact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_detail);
        InsetsHelper.applySystemBars(findViewById(R.id.detail_root), null);

        String contactId = getIntent().getStringExtra(EXTRA_CONTACT_ID);
        contact = repository.getContact(contactId);
        if (contact == null) {
            finish();
            return;
        }

        ImageView ivAvatar = findViewById(R.id.iv_avatar);
        AvatarLoader.load(this, ivAvatar, contact.getAvatar());

        TextView tvName = findViewById(R.id.tv_name);
        TextView tvWechatId = findViewById(R.id.tv_wechat_id);
        TextView tvRegion = findViewById(R.id.tv_region);
        tvName.setText(contact.getName());
        tvWechatId.setText(getString(R.string.contact_wechat_id_prefix)
                + (contact.getWechatId() == null ? "" : contact.getWechatId()));
        tvRegion.setText(getString(R.string.contact_region_prefix)
                + (contact.getRegion() == null ? "" : contact.getRegion()));

        RecyclerView recyclerView = findViewById(R.id.rv_entries);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        EntryAdapter adapter = new EntryAdapter();
        adapter.setData(buildEntries());
        adapter.setOnItemClickListener(new EntryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(EntryItem item, int position) {
                Toast.makeText(ContactDetailActivity.this,
                        item.getTitle() + "：" + getString(R.string.msg_not_implemented),
                        Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.iv_more).setOnClickListener(this);
        findViewById(R.id.tv_send_message).setOnClickListener(this);
        findViewById(R.id.tv_voice_call).setOnClickListener(this);
        findViewById(R.id.tv_video_call).setOnClickListener(this);
    }

    private List<EntryItem> buildEntries() {
        List<EntryItem> items = new ArrayList<>();
        // 没有图标的行：iconRes 传 0，适配器会把图标位隐藏
        items.add(new EntryItem(0, 0, getString(R.string.contact_remark))
                .setStartGroup(true)
                .setExtra(contact.getBio() == null ? "" : contact.getBio()));
        items.add(new EntryItem(0, 0, getString(R.string.contact_moments)));
        items.add(new EntryItem(0, 0, getString(R.string.contact_more_info)).setShowDivider(false));
        return items;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_back) {
            finish();
        } else if (id == R.id.tv_send_message) {
            // 没有现成会话就新建一个，再进聊天页
            Conversation conversation = repository.getOrCreateConversation(contact.getId());
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getId());
            startActivity(intent);
        } else if (id == R.id.iv_more || id == R.id.tv_voice_call || id == R.id.tv_video_call) {
            Toast.makeText(this, R.string.msg_not_implemented, Toast.LENGTH_SHORT).show();
        }
    }
}
