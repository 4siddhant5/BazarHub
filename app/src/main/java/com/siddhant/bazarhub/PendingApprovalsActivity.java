package com.siddhant.bazarhub;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class PendingApprovalsActivity extends AppCompatActivity {

    private RecyclerView rv;
    private TextView tvHead;
    private ProductAdapter adapter;

    // Provide a local list in case your adapter constructor needs it
    private final List<Product> productList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approvals);

        tvHead = findViewById(R.id.tvHead);
        rv = findViewById(R.id.rvPending);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(productList, this);

        // Admin mode: approve/reject callbacks
        adapter.setAdminMode(true, new ProductAdapter.AdminActionListener() {
            @Override
            public void onApprove(String productId) { setStatus(productId, "approved"); }
            @Override
            public void onReject(String productId) { setStatus(productId, "rejected"); }
        });

        rv.setAdapter(adapter);

        // Live query: pending products by createdAt ASC (same as your previous code)
        Fire.db().collection("products")
                .whereEqualTo("status", "pending")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap != null) {
                        List<DocumentSnapshot> docs = snap.getDocuments();
                        // Your adapter previously used setDocs(docs) – we keep it the same
                        adapter.setDocs(docs);
                        tvHead.setText("Pending Approvals (" + docs.size() + ")");
                    }
                });
    }

    private void setStatus(String productId, String status) {
        Fire.db().collection("products").document(productId)
                .update("status", status)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Marked " + status, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
