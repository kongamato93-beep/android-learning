package com.example.wechat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wechat.R;
import com.example.wechat.data.DataProvider;
import com.example.wechat.data.DataRepository;
import com.example.wechat.model.Contact;
import com.example.wechat.model.Conversation;
import com.example.wechat.util.AvatarLoader;
import com.example.wechat.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;

/** 会话列表适配器 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private final List<Conversation> data = new ArrayList<>();
    private final DataRepository repository = DataProvider.get();
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(Conversation conversation);
    }

    /** 长按会话：微信里在这里弹「置顶 / 标为未读 / 删除」之类的菜单 */
    public interface OnItemLongClickListener {
        void onItemLongClick(View anchor, Conversation conversation);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    /** 用关键字过滤会话（匹配昵称或最后一条消息），传空字符串恢复全部 */
    public void setData(List<Conversation> conversations, String keyword) {
        data.clear();
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Conversation conversation = data.get(position);
        // 联系人信息按 id 现查，不在这里缓存对象，联系人改了列表能跟着变
        Contact peer = repository.getContact(conversation.getPeerId());

        // 置顶会话用浅灰底区分；根布局保留 ?attr/selectableItemBackground 的点击效果，
        // 所以背景设置在内容区这一层，波纹不会被覆盖
        holder.llContent.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(),
                conversation.isPinned() ? R.color.conversation_pinned_bg : R.color.card_bg));

        if (peer != null) {
            holder.tvName.setText(peer.getName());
            AvatarLoader.load(holder.itemView.getContext(), holder.ivAvatar, peer.getAvatar());
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

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(conversation);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(v, conversation);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View llContent;
        ImageView ivAvatar;
        TextView tvName;
        TextView tvLastMessage;
        TextView tvTime;
        TextView tvUnread;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            llContent = itemView.findViewById(R.id.ll_conversation_content);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvUnread = itemView.findViewById(R.id.tv_unread);
        }
    }
}
