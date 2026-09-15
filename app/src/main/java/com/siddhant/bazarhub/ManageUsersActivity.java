package com.siddhant.bazarhub;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UsersAdapter usersAdapter;
    private List<UserModel> userList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        recyclerView = findViewById(R.id.recyclerUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        // ✅ Pass the interface implementation here
        usersAdapter = new UsersAdapter(userList, new UsersAdapter.OnUserActionListener() {
            @Override
            public void onBlockToggle(UserModel user) {
                toggleBlockStatus(user);
            }

            @Override
            public void onDelete(UserModel user) {
                deleteUser(user);
            }
        });

        recyclerView.setAdapter(usersAdapter);

        loadUsers();
    }

    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        UserModel user = doc.toObject(UserModel.class);
                        if (user != null) {
                            user.setId(doc.getId()); // keep Firestore ID reference
                            userList.add(user);
                        }
                    }
                    usersAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show());
    }

    private void toggleBlockStatus(UserModel user) {
        boolean newStatus = !user.isBlocked();

        db.collection("users").document(user.getId())
                .update("isBlocked", newStatus)
                .addOnSuccessListener(unused -> {
                    user.setBlocked(newStatus);
                    usersAdapter.notifyDataSetChanged();
                    Toast.makeText(this,
                            newStatus ? "✅ User blocked" : "✅ User unblocked",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Update failed", Toast.LENGTH_SHORT).show());
    }

    private void deleteUser(UserModel user) {
        db.collection("users").document(user.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    userList.remove(user);
                    usersAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "🗑️ User deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Delete failed", Toast.LENGTH_SHORT).show());
    }
}
