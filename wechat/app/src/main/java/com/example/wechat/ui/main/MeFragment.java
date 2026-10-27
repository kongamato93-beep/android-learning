package com.example.wechat.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.wechat.R;
import com.example.wechat.adapter.MeEntryListAdapter;
import com.example.wechat.data.LoginUserStore;
import com.example.wechat.model.EntryItem;
import com.example.wechat.ui.me.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

/** 我 Tab：个人资料区（实验三登录用户） + 自定义 ListView 功能列表 + 设置入口 */
public class MeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_me, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // —— 1. 顶部资料区：头像 + 姓名 + 微信号，数据来自实验三里登录的那个账号 ——
        final LoginUserStore loginUser = new LoginUserStore(requireContext());
        ImageView ivAvatar = view.findViewById(R.id.iv_avatar);
        TextView tvName = view.findViewById(R.id.tv_name);
        TextView tvWechatId = view.findViewById(R.id.tv_wechat_id);
        ivAvatar.setImageResource(loginUser.getAvatarRes());
        tvName.setText(loginUser.getUsername());
        tvWechatId.setText(getString(R.string.contact_wechat_id_prefix) + loginUser.getUsername());

        // —— 2. 点资料区：Toast 显示当前登录账号（个人信息页后续再做）——
        view.findViewById(R.id.ll_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(requireContext(),
                        getString(R.string.msg_current_login_user, loginUser.getUsername()),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // —— 3. 七项功能列表：自定义 ListView + MeEntryListAdapter ——
        ListView listView = view.findViewById(R.id.lv_entries);
        final MeEntryListAdapter adapter = new MeEntryListAdapter(requireContext());
        adapter.setData(buildEntries());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                EntryItem item = adapter.getItem(position);
                if (item == null) {
                    return;
                }
                if (getString(R.string.me_settings).equals(item.getTitle())) {
                    startActivity(new Intent(requireContext(), SettingsActivity.class));
                    return;
                }
                Toast.makeText(requireContext(),
                        item.getTitle() + "：" + getString(R.string.msg_not_implemented),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 「我」页的七项功能：服务、收藏、朋友圈、作品、小店与卡包、表情、设置。
     * 分组间隔和微信一致——朋友圈那组和设置那组各自另起一段。
     */
    private List<EntryItem> buildEntries() {
        List<EntryItem> items = new ArrayList<>();
        items.add(new EntryItem(R.drawable.ic_moments, R.color.entry_green,
                getString(R.string.me_services)));
        items.add(new EntryItem(R.drawable.ic_tag, R.color.entry_blue,
                getString(R.string.me_favorites)).setShowDivider(false));

        items.add(new EntryItem(R.drawable.ic_moments, R.color.entry_blue,
                getString(R.string.me_moments)).setStartGroup(true));
        items.add(new EntryItem(R.drawable.ic_mini_program, R.color.entry_orange,
                getString(R.string.me_album)));
        items.add(new EntryItem(R.drawable.ic_tag, R.color.entry_red,
                getString(R.string.me_cards)));
        items.add(new EntryItem(R.drawable.ic_top_stories, R.color.entry_yellow,
                getString(R.string.me_sticker)).setShowDivider(false));

        items.add(new EntryItem(R.drawable.ic_settings_gear, R.color.entry_blue,
                getString(R.string.me_settings)).setStartGroup(true).setShowDivider(false));
        return items;
    }
}
