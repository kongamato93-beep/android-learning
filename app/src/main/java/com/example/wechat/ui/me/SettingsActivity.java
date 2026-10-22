package com.example.wechat.ui.me;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wechat.R;
import com.example.wechat.adapter.EntryAdapter;
import com.example.wechat.model.EntryItem;
import com.example.wechat.util.InsetsHelper;

import java.util.ArrayList;
import java.util.List;

/** 设置页 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        InsetsHelper.applySystemBars(findViewById(R.id.settings_root), null);

        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.rv_entries);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        EntryAdapter adapter = new EntryAdapter();
        adapter.setData(buildEntries());
        adapter.setOnItemClickListener(new EntryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(EntryItem item, int position) {
                if (getString(R.string.settings_logout).equals(item.getTitle())) {
                    confirmLogout();
                    return;
                }
                Toast.makeText(SettingsActivity.this,
                        item.getTitle() + "：" + getString(R.string.msg_not_implemented),
                        Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private List<EntryItem> buildEntries() {
        List<EntryItem> items = new ArrayList<>();
        items.add(new EntryItem(R.drawable.ic_new_friend, R.color.entry_green,
                getString(R.string.settings_account)));
        items.add(new EntryItem(R.drawable.ic_live, R.color.entry_red,
                getString(R.string.settings_notification)));
        items.add(new EntryItem(R.drawable.ic_shake, R.color.entry_blue,
                getString(R.string.settings_privacy)));
        items.add(new EntryItem(R.drawable.ic_settings_gear, R.color.entry_blue,
                getString(R.string.settings_general)).setShowDivider(false));

        items.add(new EntryItem(R.drawable.ic_moments, R.color.entry_blue,
                getString(R.string.settings_about)).setStartGroup(true)
                .setExtra(getString(R.string.settings_about_version)));
        items.add(new EntryItem(R.drawable.ic_official, R.color.entry_green,
                getString(R.string.settings_help)).setShowDivider(false));

        items.add(new EntryItem(R.drawable.ic_group_chat, R.color.entry_orange,
                getString(R.string.settings_switch_account)).setStartGroup(true).setShowDivider(false));

        items.add(new EntryItem(0, 0, getString(R.string.settings_logout))
                .setStartGroup(true)
                .setShowDivider(false)
                .setTitleColorRes(R.color.badge_red));
        return items;
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.settings_logout_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(SettingsActivity.this, R.string.settings_logout_done,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}
