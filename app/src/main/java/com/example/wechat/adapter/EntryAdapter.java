package com.example.wechat.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wechat.R;
import com.example.wechat.model.EntryItem;

import java.util.ArrayList;
import java.util.List;

/** 通用入口列表适配器（发现页 / 我页 / 设置页 / 联系人详情共用） */
public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.ViewHolder> {

    private final List<EntryItem> data = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(EntryItem item, int position);
    }

    public void setData(List<EntryItem> items) {
        data.clear();
        if (items != null) {
            data.addAll(items);
        }
        notifyDataSetChanged();
    }

    public EntryItem getItem(int position) {
        return data.get(position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final EntryItem item = data.get(position);
        if (item.getIconRes() == 0) {
            // 有些行（备注、更多信息）在微信里是没有图标的
            holder.ivIcon.setVisibility(View.GONE);
        } else {
            holder.ivIcon.setVisibility(View.VISIBLE);
            holder.ivIcon.setImageResource(item.getIconRes());
            ImageViewCompat.setImageTintList(holder.ivIcon,
                    ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), item.getTintRes())));
        }
        holder.tvTitle.setText(item.getTitle());
        if (item.getTitleColorRes() != 0) {
            holder.tvTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                    item.getTitleColorRes()));
        } else {
            holder.tvTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                    R.color.text_primary));
        }

        if (item.getExtra() == null) {
            holder.tvExtra.setVisibility(View.GONE);
        } else {
            holder.tvExtra.setVisibility(View.VISIBLE);
            holder.tvExtra.setText(item.getExtra());
        }

        holder.divider.setVisibility(item.isShowDivider() ? View.VISIBLE : View.GONE);

        // 分组间隔：微信在两个功能区之间放一段灰底，这里用行上方的外边距实现
        int topMargin = item.isStartGroup()
                ? holder.itemView.getResources().getDimensionPixelSize(R.dimen.space_8)
                : 0;
        ViewGroup.MarginLayoutParams lp =
                (ViewGroup.MarginLayoutParams) holder.root.getLayoutParams();
        if (lp == null) {
            lp = new ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT,
                    ViewGroup.MarginLayoutParams.WRAP_CONTENT);
        }
        lp.topMargin = topMargin;
        holder.root.setLayoutParams(lp);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(item, holder.getBindingAdapterPosition());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View root;
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvExtra;
        View divider;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.root);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvExtra = itemView.findViewById(R.id.tv_extra);
            divider = itemView.findViewById(R.id.view_divider);
        }
    }
}
