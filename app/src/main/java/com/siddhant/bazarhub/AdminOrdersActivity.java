package com.siddhant.bazarhub;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.List;

public class AdminOrdersActivity extends AppCompatActivity {

    private RecyclerView rv;
    private OrderAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_orders);

        rv = findViewById(R.id.rvOrders);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OrderAdapter(this, true, new OrderAdapter.AdminOrderActionListener() {
            @Override
            public void onStatusChange(String orderId, String status) {
                setStatus(orderId, status);
            }
        });
        rv.setAdapter(adapter);

        Fire.db().collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap != null) {
                        List<DocumentSnapshot> docs = snap.getDocuments();
                        adapter.setDocs(docs);
                    }
                });
    }

    private void setStatus(String orderId, String status) {
        Fire.db().collection("orders").document(orderId)
                .update("status", status)
                .addOnSuccessListener(v -> Toast.makeText(this, "Marked " + status, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
