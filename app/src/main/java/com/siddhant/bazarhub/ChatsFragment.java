package com.siddhant.bazarhub;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

/**
 * Lists all conversations for the current user with preview + unread count.
 *
 * Firestore (chats/{chatId}):
 * - participants: [uid1, uid2]
 * - productId: string (optional)
 * - lastMessage: string
 * - lastMessageBy: uid
 * - lastUpdated: Timestamp
 * - lastSeen: { uid1: Timestamp, uid2: Timestamp }
 */
public class ChatsFragment extends Fragment {

    private RecyclerView rv;
    private View emptyState;
    private ProgressBar progress;

    private ChatListAdapter adapter;
    private ListenerRegistration chatsReg;

    private FirebaseUser me;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chats, container, false);
        rv = v.findViewById(R.id.rvChats);
        emptyState = v.findViewById(R.id.tvEmpty);
        progress = v.findViewById(R.id.progress);

        me = Fire.auth().getCurrentUser();
        if (me == null) {
            // Not logged in; show empty
            rv.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            return v;
        }

        adapter = new ChatListAdapter(new ArrayList<>(), new ChatListAdapter.Listener() {
            @Override
            public void onChatClick(ChatListItem item) {
                Intent it = new Intent(requireContext(), ChatActivity.class);
                it.putExtra("chatId", item.chatId);
                it.putExtra("otherId", item.otherId);
                startActivity(it);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        listenChats();

        return v;
    }

    @Override
    public void onDestroyView() {
        if (chatsReg != null) {
            chatsReg.remove();
            chatsReg = null;
        }
        super.onDestroyView();
    }

    private void listenChats() {
        progress.setVisibility(View.VISIBLE);

        // NOTE: whereArrayContains + orderBy(lastUpdated) usually needs a Firestore index.
        // If Firestore prompts for an index URL, create it once.
        chatsReg = Fire.db().collection("chats")
                .whereArrayContains("participants", me.getUid())
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    progress.setVisibility(View.GONE);
                    if (e != null) return;

                    List<ChatListItem> items = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            String chatId = d.getId();

                            List<String> participants = (List<String>) d.get("participants");
                            if (participants == null || !participants.contains(me.getUid())) continue;

                            String otherId = null;
                            for (String uid : participants) {
                                if (!uid.equals(me.getUid())) { otherId = uid; break; }
                            }
                            if (otherId == null) continue;

                            String lastMessage = d.getString("lastMessage");
                            Timestamp lastUpdated = d.getTimestamp("lastUpdated");
                            Map<String, Object> lastSeenMap = (Map<String, Object>) d.get("lastSeen");

                            Timestamp myLastSeen = null;
                            if (lastSeenMap != null) {
                                Object ts = lastSeenMap.get(me.getUid());
                                if (ts instanceof Timestamp) myLastSeen = (Timestamp) ts;
                            }

                            ChatListItem item = new ChatListItem();
                            item.chatId = chatId;
                            item.otherId = otherId;
                            item.lastMessage = (lastMessage == null ? "" : lastMessage);
                            item.lastUpdated = lastUpdated;
                            item.lastUpdatedText = lastUpdated != null
                                    ? DateUtils.getRelativeTimeSpanString(lastUpdated.toDate().getTime(),
                                    System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
                                    : "";

                            items.add(item);

                            // Compute unread count (light query; counts messages sent after my lastSeen)
                            computeUnreadCount(item, myLastSeen);
                            // Fetch other user's name (for title)
                            fetchOtherUserName(item);
                        }
                    }

                    adapter.setItems(items);
                    emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void computeUnreadCount(ChatListItem item, @Nullable Timestamp myLastSeen) {
        // If never seen, set to epoch to count from the beginning
        Timestamp since = (myLastSeen != null) ? myLastSeen : new Timestamp(new Date(0));

        Fire.db().collection("chats").document(item.chatId)
                .collection("messages")
                .whereGreaterThan("sentAt", since)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .limit(50) // cheap guard; most recent 50 after lastSeen is plenty for a badge
                .get()
                .addOnSuccessListener(snap -> {
                    int count = 0;
                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            Message m = d.toObject(Message.class);
                            if (m == null) continue;
                            // count only messages from the other user
                            if (m.from != null && !m.from.equals(me.getUid())) count++;
                        }
                    }
                    item.unreadCount = count;
                    adapter.updateUnread(item.chatId, count);
                });
    }

    private void fetchOtherUserName(ChatListItem item) {
        Fire.db().collection("users").document(item.otherId)
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc != null ? doc.getString("name") : null;
                    item.otherName = (name == null ? "User" : name);
                    adapter.updateTitle(item.chatId, item.otherName);
                });
    }

    // --- POJO used by the list/adapter ---
    public static class ChatListItem {
        public String chatId;
        public String otherId;
        public String otherName;
        public String lastMessage;
        public Timestamp lastUpdated;
        public String lastUpdatedText;
        public int unreadCount = 0;
    }
}
