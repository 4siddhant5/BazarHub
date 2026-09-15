package com.siddhant.bazarhub;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderDetailsActivity extends AppCompatActivity {

    private TextView textOrderId, textStatus, textAmount, textAddress, textTimestamp;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    // Buttons
    private Button btnCancelOrder;     // buyer
    private Button btnMarkShipped;     // seller
    private Button btnMarkDelivered;   // seller

    private FirebaseFirestore db;
    private String orderId, userId; // userId = list owner passed from intent
    private OrderItemsAdapter itemsAdapter;
    private List<CartItem> itemList;

    private DocumentReference orderRef;

    // From order doc
    private String buyerId, sellerId;

    // Current logged-in user id (to decide role)
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        textOrderId = findViewById(R.id.textOrderId);
        textStatus = findViewById(R.id.textStatus);
        textAmount = findViewById(R.id.textAmount);
        textAddress = findViewById(R.id.textAddress);
        textTimestamp = findViewById(R.id.textTimestamp);
        recyclerView = findViewById(R.id.recyclerOrderItems);
        progressBar = findViewById(R.id.progressBar);

        // Match XML button IDs
        btnCancelOrder   = findViewById(R.id.btnCancelOrder);
        btnMarkShipped   = findViewById(R.id.btnMarkShipped);
        btnMarkDelivered = findViewById(R.id.btnMarkDelivered);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        orderId = getIntent().getStringExtra("orderId");
        userId  = getIntent().getStringExtra("userId");

        itemList = new ArrayList<>();
        itemsAdapter = new OrderItemsAdapter(itemList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(itemsAdapter);

        if (orderId == null || userId == null) {
            Toast.makeText(this, "Invalid order", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderRef = db.collection("users")
                .document(userId)
                .collection("orders")
                .document(orderId);

        listenToOrderUpdates();

        // Button clicks → handled based on visibility
        btnCancelOrder.setOnClickListener(v -> updateOrderStatusBoth("cancelled"));
        btnMarkShipped.setOnClickListener(v -> updateOrderStatusBoth("shipped"));
        btnMarkDelivered.setOnClickListener(v -> updateOrderStatusBoth("delivered"));
    }

    private void listenToOrderUpdates() {
        progressBar.setVisibility(View.VISIBLE);

        orderRef.addSnapshotListener((snapshot, error) -> {
            progressBar.setVisibility(View.GONE);

            if (error != null) {
                Toast.makeText(this, "Error loading order", Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                updateUI(snapshot);
            } else {
                Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void updateUI(DocumentSnapshot snapshot) {
        textOrderId.setText("Order ID: " + snapshot.getId());
        String status = snapshot.getString("status");
        textStatus.setText("Status: " + status);

        Double amount = snapshot.getDouble("amount");
        textAmount.setText("Amount: ₹" + (amount != null ? amount : 0));

        textAddress.setText("Address: " + snapshot.getString("address"));

        if (snapshot.getTimestamp("timestamp") != null) {
            textTimestamp.setText("Placed on: " + snapshot.getTimestamp("timestamp").toDate().toString());
        }

        // Extract buyer & seller IDs
        buyerId = snapshot.getString("buyerId");
        sellerId = snapshot.getString("sellerId");

        // Role-based button visibility (same as OrdersAdapter)
        applyRoleButtons(status);

        // Load items
        List<Map<String, Object>> items = (List<Map<String, Object>>) snapshot.get("items");
        itemList.clear();
        if (items != null) {
            for (Map<String, Object> map : items) {
                String productId    = (String) map.get("productId");
                String productName  = (String) map.get("productName");
                String productImage = (String) map.get("productImage");
                Double price        = map.get("price") instanceof Double ? (Double) map.get("price") : 0.0;
                int quantity        = map.get("quantity") != null ? ((Long) map.get("quantity")).intValue() : 1;
                String sellerIdItem = (String) map.get("sellerId");

                CartItem item = new CartItem(
                        productId,
                        productName,
                        productImage,
                        price,
                        quantity,
                        sellerIdItem
                );
                itemList.add(item);
            }
        }
        itemsAdapter.notifyDataSetChanged();
    }

    private void applyRoleButtons(String status) {
        // Hide all by default
        btnCancelOrder.setVisibility(View.GONE);
        btnMarkShipped.setVisibility(View.GONE);
        btnMarkDelivered.setVisibility(View.GONE);

        if (currentUid == null) return;

        boolean isBuyer  = buyerId != null && currentUid.equals(buyerId);
        boolean isSeller = sellerId != null && currentUid.equals(sellerId);

        if (isBuyer) {
            // Buyer: can cancel only when pending
            if ("pending".equalsIgnoreCase(status)) {
                btnCancelOrder.setVisibility(View.VISIBLE);
            }
        } else if (isSeller) {
            // Seller: shipped if pending, delivered if shipped
            if ("pending".equalsIgnoreCase(status)) {
                btnMarkShipped.setVisibility(View.VISIBLE);
            } else if ("shipped".equalsIgnoreCase(status)) {
                btnMarkDelivered.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Update status on BOTH buyer + seller copies (never deletes).
     */
    private void updateOrderStatusBoth(String newStatus) {
        if (buyerId == null || sellerId == null) {
            Toast.makeText(this, "Order party IDs missing", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update seller’s copy
        db.collection("users")
                .document(sellerId)
                .collection("orders")
                .document(orderId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    // Mirror to buyer’s copy
                    db.collection("users")
                            .document(buyerId)
                            .collection("orders")
                            .document(orderId)
                            .update("status", newStatus)
                            .addOnSuccessListener(aVoid2 ->
                                    Toast.makeText(this, "Order updated: " + newStatus, Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to update buyer order", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update seller order", Toast.LENGTH_SHORT).show()
                );
    }
}
