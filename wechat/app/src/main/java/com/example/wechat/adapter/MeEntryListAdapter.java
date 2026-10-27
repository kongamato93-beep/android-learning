package com.example.wechat.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.example.wechat.R;
import com.example.wechat.model.EntryItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 「我」分页功能列表的自定义适配器（ListView + BaseAdapter + ViewHolder）。
 *
 * 和 {@link ConversationListAdapter} 一样走 BaseAdapter：实验要求用自定义 ListView，
 * 适配器就自己继承 BaseAdapter 并在 getView 里用 ViewHolder 复用行。
 *
 * 行布局用 {@code item_me_entry.xml}（图标 + 标题 + 右侧说明 + 分隔线 + 分组间距），
 * 没有复用发现页那套 RecyclerView 的行，两边的显示规则不一样，分开更好改。
 */
public class MeEntryListAdapter extends BaseAdapter {

    private final List<EntryItem> data = new ArrayList<>();
    private final LayoutInflater inflater;

    public MeEntryListAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setData(List<EntryItem> items) {
        data.clear();
        if (items != null) {
            data.addAll(items);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public EntryItem getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_me_entry, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // —— 第 2 步：取这一行的数据 ——
        EntryItem item = getItem(position);
        Context ctx = convertView.getContext();

        // —— 第 3 步：图标（含配色）+ 标题 + 右侧说明 ——
        if (item.getIconRes() == 0) {
            holder.ivIcon.setVisibility(View.GONE);
        } else {
            holder.ivIcon.setVisibility(View.VISIBLE);
            holder.ivIcon.setImageResource(item.getIconRes());
            ImageViewCompat.setImageTintList(holder.ivIcon,
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, item.getTintRes())));
        }

        holder.tvTitle.setText(item.getTitle());
        holder.tvTitle.setTextColor(ContextCompat.getColor(ctx,
                item.getTitleColorRes() != 0 ? item.getTitleColorRes() : R.color.text_primary));

        if (item.getExtra() == null) {
            holder.tvExtra.setVisibility(View.GONE);
        } else {
            holder.tvExtra.setVisibility(View.VISIBLE);
            holder.tvExtra.setText(item.getExtra());
        }

        // —— 第 4 步：分隔线与分组间隔 ——
        // divider：本组内部的行之间才有线，每组最后一项关掉
        // groupSpace：一段 8dp 的灰条，用来把「朋友圈组」「设置组」分开
        holder.divider.setVisibility(item.isShowDivider() ? View.VISIBLE : View.GONE);
        holder.groupSpace.setVisibility(item.isStartGroup() ? View.VISIBLE : View.GONE);

        return convertView;
    }

    private static class ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvExtra;
        View divider;
        View groupSpace;

        ViewHolder(View itemView) {
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvExtra = itemView.findViewById(R.id.tv_extra);
            divider = itemView.findViewById(R.id.view_divider);
            groupSpace = itemView.findViewById(R.id.view_group_space);
        }
    }
}
