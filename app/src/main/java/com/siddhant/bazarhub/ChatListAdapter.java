package com.siddhant.bazarhub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.VH> {

    public interface Listener {
        void onChatClick(ChatsFragment.ChatListItem item);
    }

    private final List<ChatsFragment.ChatListItem> items;
    private final Listener listener;

    public ChatListAdapter(List<ChatsFragment.ChatListItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<ChatsFragment.ChatListItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void updateUnread(String chatId, int unreadCount) {
        for (int i = 0; i < items.size(); i++) {
            ChatsFragment.ChatListItem it = items.get(i);
            if (it.chatId.equals(chatId)) {
                it.unreadCount = unreadCount;
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void updateTitle(String chatId, String title) {
        for (int i = 0; i < items.size(); i++) {
            ChatsFragment.ChatListItem it = items.get(i);
            if (it.chatId.equals(chatId)) {
                it.otherName = title;
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_thread, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatsFragment.ChatListItem item = items.get(position);

        h.tvName.setText(item.otherName == null ? "User" : item.otherName);
        h.tvLastMsg.setText(item.lastMessage == null ? "" : item.lastMessage);
        h.tvTime.setText(item.lastUpdatedText == null ? "" : item.lastUpdatedText);

        if (item.unreadCount > 0) {
            h.tvBadge.setVisibility(View.VISIBLE);
            h.tvBadge.setText(String.valueOf(item.unreadCount));
        } else {
            h.tvBadge.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onChatClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMsg, tvTime, tvBadge;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMsg = itemView.findViewById(R.id.tvLastMsg);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvBadge = itemView.findViewById(R.id.tvBadge);
        }
    }
}
