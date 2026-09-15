package com.siddhant.bazarhub;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private final List<Message> list;
    private final String myUid;
    private Map<String, Timestamp> lastSeen; // keep lastSeen info

    public ChatAdapter(List<Message> list, String myUid) {
        this.list = list;
        this.myUid = myUid;
    }

    public void setList(List<Message> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }

    public void setLastSeen(Map<String, Timestamp> lastSeen) {
        this.lastSeen = lastSeen;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Message m = list.get(position);
        boolean mine = m.from != null && m.from.equals(myUid);

        LinearLayout root = h.itemView.findViewById(R.id.root);
        root.setGravity(mine ? Gravity.END : Gravity.START);

        h.tv.setVisibility(View.GONE);
        h.iv.setVisibility(View.GONE);
        h.tick.setVisibility(View.GONE);

        if (m.imageUrl != null && !m.imageUrl.isEmpty()) {
            h.iv.setVisibility(View.VISIBLE);
            Glide.with(h.iv.getContext()).load(m.imageUrl).into(h.iv);
            h.iv.setBackgroundResource(mine ? R.drawable.bg_bubble_sender : R.drawable.bg_bubble_receiver);
        } else {
            h.tv.setVisibility(View.VISIBLE);
            h.tv.setText(m.text != null ? m.text : "");
            h.tv.setBackgroundResource(mine ? R.drawable.bg_bubble_sender : R.drawable.bg_bubble_receiver);
            h.tv.setTextColor(mine ? 0xFFFFFFFF : 0xFF000000);
        }

        if (mine) {
            h.tick.setVisibility(View.VISIBLE);
            h.tick.setImageResource(R.drawable.ic_tick); // default single tick

            if (lastSeen != null) {
                for (Map.Entry<String, Timestamp> e : lastSeen.entrySet()) {
                    if (!e.getKey().equals(myUid)) {
                        // if peer has seen message after it was sent → double tick
                        if (m.sentAt != null && e.getValue() != null && e.getValue().compareTo(m.sentAt) >= 0) {
                            h.tick.setImageResource(R.drawable.ic_double_tick);
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        ImageView iv, tick;

        VH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvMessage);
            iv = itemView.findViewById(R.id.ivMessageImage);
            tick = itemView.findViewById(R.id.ivSeenTick);
        }
    }
}
