package com.siddhant.bazarhub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private static final int RC_PICK_IMAGE = 101;

    private RecyclerView rv;
    private EditText etMessage;
    private Button btnSend;
    private ImageButton btnImage;
    private TextView tvToolbarTitle, tvTyping;

    private ChatAdapter adapter;
    private String chatId;
    private String otherId;
    private FirebaseUser me;

    private ListenerRegistration messagesReg;
    private ListenerRegistration typingReg;
    private DocumentReference chatRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvTyping = findViewById(R.id.tvTyping); // small "typing..." text under title

        rv = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnImage = findViewById(R.id.btnImage);

        chatId = getIntent().getStringExtra("chatId");
        otherId = getIntent().getStringExtra("otherId");
        me = Fire.auth().getCurrentUser();
        if (chatId == null || me == null) { finish(); return; }

        chatRef = Fire.db().collection("chats").document(chatId);

        // --- Load peer name ---
        Fire.db().collection("users").document(otherId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        tvToolbarTitle.setText(name != null ? name : "Seller");
                    }
                });

        adapter = new ChatAdapter(new ArrayList<>(), me.getUid());
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // --- Listen to messages ---
        messagesReg = chatRef.collection("messages")
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) return;
                    if (snap != null) {
                        List<Message> list = new ArrayList<>();
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            Message m = d.toObject(Message.class);
                            if (m != null) list.add(m);
                        }
                        adapter.setList(list);
                        if (!list.isEmpty()) rv.scrollToPosition(list.size() - 1);

                        markSeenNow();
                    }
                });

        // --- Typing indicator listener ---
        typingReg = chatRef.addSnapshotListener((doc, e) -> {
            if (doc != null && doc.exists()) {
                Boolean typing = doc.getBoolean("typing." + otherId);
                tvTyping.setText((typing != null && typing) ? "typing..." : "");
            }
        });

        btnSend.setOnClickListener(v -> sendTextMessage());
        btnImage.setOnClickListener(v -> pickImage());

        // --- Detect typing ---
        etMessage.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();
            private final long DELAY = 1000; // stop typing after 1s idle

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                setTyping(true);
                timer.cancel();
            }
            @Override public void afterTextChanged(Editable s) {
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override public void run() {
                        setTyping(false);
                    }
                }, DELAY);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        markSeenNow();
    }

    @Override
    protected void onDestroy() {
        if (messagesReg != null) messagesReg.remove();
        if (typingReg != null) typingReg.remove();
        setTyping(false);
        super.onDestroy();
    }

    private void sendTextMessage() {
        String txt = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(txt)) return;
        etMessage.setText("");

        String mid = chatRef.collection("messages").document().getId();
        Message m = new Message(mid, me.getUid(), txt, null, Timestamp.now());

        chatRef.collection("messages").document(mid).set(m)
                .addOnSuccessListener(a -> updateChatDoc(txt))
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void pickImage() {
        Intent it = new Intent(Intent.ACTION_GET_CONTENT);
        it.setType("image/*");
        startActivityForResult(Intent.createChooser(it, "Select image"), RC_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) uploadChatImage(uri);
        }
    }

    private void uploadChatImage(Uri imageUri) {
        String filename = System.currentTimeMillis() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("chat_images/" + chatId + "/" + filename);

        ref.putFile(imageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(url -> sendImageMessage(url.toString())))
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void sendImageMessage(String imageUrl) {
        String mid = chatRef.collection("messages").document().getId();
        Message m = new Message(mid, me.getUid(), null, imageUrl, Timestamp.now());

        chatRef.collection("messages").document(mid).set(m)
                .addOnSuccessListener(a -> updateChatDoc("📷 Photo"))
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void updateChatDoc(String lastMsg) {
        Map<String,Object> upd = new HashMap<>();
        upd.put("lastUpdated", Timestamp.now());
        upd.put("lastMessage", lastMsg);
        upd.put("lastMessageBy", me.getUid());
        chatRef.set(upd, SetOptions.merge());
    }

    private void markSeenNow() {
        if (chatRef == null || me == null) return;
        Map<String, Object> m = new HashMap<>();
        m.put("lastSeen." + me.getUid(), Timestamp.now());
        chatRef.set(m, SetOptions.merge());
    }

    private void setTyping(boolean typing) {
        if (chatRef == null || me == null) return;
        Map<String, Object> m = new HashMap<>();
        m.put("typing." + me.getUid(), typing);
        chatRef.set(m, SetOptions.merge());
    }
}
