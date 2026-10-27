package com.example.wechat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wechat.R;
import com.example.wechat.data.DataProvider;
import com.example.wechat.data.DataRepository;
import com.example.wechat.model.Contact;
import com.example.wechat.model.Message;
import com.example.wechat.util.AvatarLoader;
import com.example.wechat.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天窗口的消息列表。
 *
 * 两种 item：对方（左）和自己（右），靠 senderId 是否等于 DataRepository.SELF_ID 判断。
 * 时间只在与上一条间隔超过 5 分钟时才显示。
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private static final int TYPE_OTHER = 0;
    private static final int TYPE_SELF = 1;

    private final List<Message> data = new ArrayList<>();
    private final DataRepository repository = DataProvider.get();
    private final String peerAvatar;

    public MessageAdapter(String peerAvatar) {
        this.peerAvatar = peerAvatar;
    }

    public void setData(List<Message> messages) {
        data.clear();
        if (messages != null) {
            data.addAll(messages);
        }
        notifyDataSetChanged();
    }

    public void append(Message message) {
        data.add(message);
        notifyItemInserted(data.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        Message m = data.get(position);
        return DataRepository.SELF_ID.equals(m.getSenderId()) ? TYPE_SELF : TYPE_OTHER;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_SELF ? R.layout.item_message_self : R.layout.item_message_other;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = data.get(position);
        holder.tvContent.setText(message.getContent());

        long previous = position > 0 ? data.get(position - 1).getTimestamp() : 0;
        if (TimeUtil.needShowTime(previous, message.getTimestamp())) {
            holder.tvTime.setVisibility(View.VISIBLE);
            holder.tvTime.setText(TimeUtil.formatChatTime(message.getTimestamp()));
        } else {
            holder.tvTime.setVisibility(View.GONE);
        }

        if (getItemViewType(position) == TYPE_SELF) {
            Contact self = repository.getContact(DataRepository.SELF_ID);
            AvatarLoader.load(holder.itemView.getContext(), holder.ivAvatar,
                    self == null ? null : self.getAvatar());
        } else {
            AvatarLoader.load(holder.itemView.getContext(), holder.ivAvatar, peerAvatar);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvContent;
        ImageView ivAvatar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvContent = itemView.findViewById(R.id.tv_content);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
        }
    }
}
