package com.example.wechat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.wechat.R;
import com.example.wechat.data.DataProvider;
import com.example.wechat.data.DataRepository;
import com.example.wechat.model.Contact;
import com.example.wechat.model.Conversation;
import com.example.wechat.util.AvatarLoader;
import com.example.wechat.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 微信分页会话列表的自定义适配器（ListView + BaseAdapter + ViewHolder）。
 *
 * 为什么是 BaseAdapter 而不是 RecyclerView.Adapter：
 * 实验要求「使用自定义 ListView 控件」，ListView 的自定义适配器就是从 BaseAdapter 继承，
 * 自己管 getView 的复用。这里用 ViewHolder 把 findViewById 的结果缓存在 convertView 的 tag 里，
 * 滑动时不再重复找控件——这是自定义 ListView 的标准写法。
 *
 * 行布局复用 {@code item_conversation.xml}（头像 + 姓名 + 最近消息 + 时间 + 未读角标），
 * 和原来完全一样，只是换了个适配器基类。
 */
public class ConversationListAdapter extends BaseAdapter {

    private final List<Conversation> data = new ArrayList<>();
    private final DataRepository repository = DataProvider.get();
    private final LayoutInflater inflater;

    public ConversationListAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    /** 用关键字过滤会话（匹配昵称或最后一条消息），传空字符串恢复全部 */
    public void setData(List<Conversation> conversations, String keyword) {
        data.clear();
        if (conversations == null) {
            notifyDataSetChanged();
            return;
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            data.addAll(conversations);
        } else {
            String kw = keyword.trim().toLowerCase();
            for (Conversation c : conversations) {
                Contact peer = repository.getContact(c.getPeerId());
                String name = peer == null ? "" : peer.getName();
                String last = c.getLastMessage() == null ? "" : c.getLastMessage();
                if (name.toLowerCase().contains(kw) || last.toLowerCase().contains(kw)) {
                    data.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Conversation getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // —— 第 1 步：行的复用 ——
        // convertView 为 null 说明屏幕上还没有可复用的行（第一次显示 / 缓存不够），
        // 这时才 inflate 布局，并把所有控件装进 ViewHolder 存到 tag 里；
        // 之后滑出去的行会被回收成 convertView，直接从 tag 取 ViewHolder 就能用。
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_conversation, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // —— 第 2 步：取这一行要显示的数据 ——
        Conversation conversation = getItem(position);
        // 联系人信息按 id 现查，不在这里缓存对象，联系人改了列表能跟着变
        Contact peer = repository.getContact(conversation.getPeerId());

        // 置顶会话用浅灰底区分；根布局保留点击效果，背景设在内容区这一层
        holder.llContent.setBackgroundColor(ContextCompat.getColor(
                convertView.getContext(),
                conversation.isPinned() ? R.color.conversation_pinned_bg : R.color.card_bg));

        // —— 第 3 步：把数据填进控件（头像 / 昵称 / 最近消息 / 时间 / 未读角标）——
        if (peer != null) {
            holder.tvName.setText(peer.getName());
            AvatarLoader.load(convertView.getContext(), holder.ivAvatar, peer.getAvatar());
        } else {
            holder.tvName.setText("");
            holder.ivAvatar.setImageResource(R.drawable.avatar_default);
        }

        holder.tvLastMessage.setText(conversation.getLastMessage());
        holder.tvTime.setText(TimeUtil.formatListTime(conversation.getLastTimeMillis()));

        int unread = conversation.getUnreadCount();
        if (unread > 0) {
            holder.tvUnread.setVisibility(View.VISIBLE);
            holder.tvUnread.setText(unread > 99 ? "99+" : String.valueOf(unread));
        } else {
            holder.tvUnread.setVisibility(View.GONE);
        }

        return convertView;
    }

    private static class ViewHolder {
        View llContent;
        ImageView ivAvatar;
        TextView tvName;
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvUnread;

        ViewHolder(View itemView) {
            llContent = itemView.findViewById(R.id.ll_conversation_content);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvUnread = itemView.findViewById(R.id.tv_unread);
        }
    }
}
