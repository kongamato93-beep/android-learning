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
import com.example.wechat.model.Contact;
import com.example.wechat.util.AvatarLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 通讯录列表。
 *
 * 三种行共用一个 RecyclerView：
 * - 固定入口（新的朋友 / 群聊 / 标签 / 公众号）
 * - 字母分组头（A、B、C…）
 * - 联系人
 * 右侧字母索引条靠 letterPositions 找到对应分组的位置直接跳过去。
 */
public class ContactsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int TYPE_ENTRY = 0;
    static final int TYPE_HEADER = 1;
    static final int TYPE_CONTACT = 2;

    private final List<Row> rows = new ArrayList<>();
    private final Map<String, Integer> letterPositions = new LinkedHashMap<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onContactClick(Contact contact);

        void onEntryClick(String title);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /** 取某个字母分组在列表中的位置，用于右侧索引跳转 */
    public int getPositionForLetter(String letter) {
        Integer position = letterPositions.get(letter);
        return position == null ? -1 : position;
    }

    public void build(List<Contact> contacts, String keyword, List<EntryRow> fixedEntries) {
        rows.clear();
        letterPositions.clear();

        boolean searching = keyword != null && !keyword.trim().isEmpty();

        if (!searching && fixedEntries != null) {
            for (EntryRow entry : fixedEntries) {
                Row row = new Row(TYPE_ENTRY);
                row.title = entry.title;
                row.iconRes = entry.iconRes;
                row.tintRes = entry.tintRes;
                rows.add(row);
            }
        }

        List<Contact> filtered = new ArrayList<>();
        String kw = searching ? keyword.trim().toLowerCase(Locale.CHINA) : null;
        for (Contact c : contacts) {
            if (kw == null || (c.getName() != null && c.getName().toLowerCase(Locale.CHINA).contains(kw))) {
                filtered.add(c);
            }
        }

        Collections.sort(filtered, new Comparator<Contact>() {
            @Override
            public int compare(Contact a, Contact b) {
                return a.getIndexLetter().compareTo(b.getIndexLetter());
            }
        });

        String lastLetter = null;
        for (Contact c : filtered) {
            String letter = c.getIndexLetter();
            if (!searching && !letter.equals(lastLetter)) {
                letterPositions.put(letter, rows.size());
                Row header = new Row(TYPE_HEADER);
                header.title = letter;
                rows.add(header);
                lastLetter = letter;
            }
            Row row = new Row(TYPE_CONTACT);
            row.contact = c;
            row.title = c.getName();
            rows.add(row);
        }

        // 每个分组的最后一行不画分隔线（微信在分组之间是用整条灰块隔开的）
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            Row next = i + 1 < rows.size() ? rows.get(i + 1) : null;
            row.showDivider = next != null && next.type == TYPE_CONTACT
                    && (rows.get(i).type == TYPE_CONTACT);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_contact_header, parent, false));
        }
        if (viewType == TYPE_ENTRY) {
            return new EntryViewHolder(inflater.inflate(R.layout.item_entry, parent, false));
        }
        View view = inflater.inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final Row row = rows.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvLetter.setText(row.title);
            return;
        }
        if (holder instanceof EntryViewHolder) {
            EntryViewHolder vh = (EntryViewHolder) holder;
            vh.ivIcon.setImageResource(row.iconRes);
            ImageViewCompat.setImageTintList(vh.ivIcon,
                    ColorStateList.valueOf(ContextCompat.getColor(vh.itemView.getContext(), row.tintRes)));
            vh.tvTitle.setText(row.title);
            vh.divider.setVisibility(row.showDivider ? View.VISIBLE : View.GONE);
            vh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onEntryClick(row.title);
                    }
                }
            });
            return;
        }
        ContactViewHolder vh = (ContactViewHolder) holder;
        vh.tvName.setText(row.title);
        AvatarLoader.load(vh.itemView.getContext(), vh.ivAvatar,
                row.contact == null ? null : row.contact.getAvatar());
        vh.divider.setVisibility(row.showDivider ? View.VISIBLE : View.GONE);
        vh.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null && row.contact != null) {
                    listener.onContactClick(row.contact);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    /** 固定入口行的数据（图标 + 颜色 + 标题） */
    public static class EntryRow {
        final int iconRes;
        final int tintRes;
        final String title;

        public EntryRow(int iconRes, int tintRes, String title) {
            this.iconRes = iconRes;
            this.tintRes = tintRes;
            this.title = title;
        }
    }

    private static class Row {
        final int type;
        String title;
        int iconRes;
        int tintRes;
        Contact contact;
        boolean showDivider = true;

        Row(int type) {
            this.type = type;
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvLetter;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLetter = itemView.findViewById(R.id.tv_letter);
        }
    }

    private static class EntryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        View divider;

        EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvTitle = itemView.findViewById(R.id.tv_title);
            divider = itemView.findViewById(R.id.view_divider);
        }
    }

    private static class ContactViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName;
        View divider;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            divider = itemView.findViewById(R.id.view_divider);
        }
    }
}
