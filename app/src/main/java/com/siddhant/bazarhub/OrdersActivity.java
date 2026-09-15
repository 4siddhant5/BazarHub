package com.siddhant.bazarhub;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;
    private ProgressBar progressBar;
    private OrdersAdapter ordersAdapter;
    private List<Order> orderList = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        recyclerOrders = findViewById(R.id.recyclerOrders);
        progressBar = findViewById(R.id.progressBarOrders);

        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        ordersAdapter = new OrdersAdapter(orderList);
        recyclerOrders.setAdapter(ordersAdapter);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        loadOrders();
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("orders")
                .whereEqualTo("buyerId", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    orderList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Order order = doc.toObject(Order.class);
                        if (order != null) {
                            order.setId(doc.getId()); // if your model has an id field
                            orderList.add(order);
                        }
                    }
                    ordersAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);

                    if (orderList.isEmpty()) {
                        Toast.makeText(this, "No orders found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                });
    }
}
