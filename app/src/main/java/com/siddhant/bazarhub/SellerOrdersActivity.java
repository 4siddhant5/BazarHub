package com.siddhant.bazarhub;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SellerOrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;
    private OrdersAdapter ordersAdapter;
    private List<Order> orderList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_orders);

        recyclerOrders = findViewById(R.id.recyclerOrdersSeller);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));

        orderList = new ArrayList<>();
        ordersAdapter = new OrdersAdapter(orderList, true); // true = seller mode
        recyclerOrders.setAdapter(ordersAdapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadSellerOrders();
    }

    private void loadSellerOrders() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("orders")
                .whereEqualTo("sellerId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(SellerOrdersActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (value == null) return;

                        for (DocumentChange dc : value.getDocumentChanges()) {
                            Order order = dc.getDocument().toObject(Order.class);
                            switch (dc.getType()) {
                                case ADDED:
                                    orderList.add(0, order);
                                    break;
                                case MODIFIED:
                                    for (int i = 0; i < orderList.size(); i++) {
                                        if (orderList.get(i).getOrderId().equals(order.getOrderId())) {
                                            orderList.set(i, order);
                                            break;
                                        }
                                    }
                                    break;
                                case REMOVED:
                                    for (int i = 0; i < orderList.size(); i++) {
                                        if (orderList.get(i).getOrderId().equals(order.getOrderId())) {
                                            orderList.remove(i);
                                            break;
                                        }
                                    }
                                    break;
                            }
                        }
                        ordersAdapter.notifyDataSetChanged();
                    }
                });
    }
}
