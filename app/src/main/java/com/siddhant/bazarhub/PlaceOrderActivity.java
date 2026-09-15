package com.siddhant.bazarhub;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlaceOrderActivity extends AppCompatActivity {

    private TextView textTotalAmount;
    private EditText editAddress;
    private Button btnConfirmOrder;

    private double totalAmount;
    private ArrayList<CartItem> cartItems;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);

        textTotalAmount = findViewById(R.id.textTotalAmount);
        editAddress = findViewById(R.id.editAddress);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        totalAmount = getIntent().getDoubleExtra("totalAmount", 0.0);
        cartItems = (ArrayList<CartItem>) getIntent().getSerializableExtra("cartItems");

        textTotalAmount.setText("Total: ₹" + totalAmount);

        btnConfirmOrder.setOnClickListener(v -> {
            String address = editAddress.getText().toString().trim();
            if (address.isEmpty()) {
                Toast.makeText(this, "Please enter delivery address", Toast.LENGTH_SHORT).show();
                return;
            }

            placeOrder(address);
        });
    }

    private void placeOrder(String address) {
        String buyerId = auth.getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();

        // Each order ID unique
        String orderId = UUID.randomUUID().toString();

        // For now, assume single-seller per cart (you can extend to multiple)
        if (cartItems.isEmpty()) return;
        String sellerId = cartItems.get(0).getSellerId();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("buyerId", buyerId);
        orderData.put("sellerId", sellerId);
        orderData.put("status", "pending");
        orderData.put("timestamp", timestamp);
        orderData.put("amount", totalAmount);
        orderData.put("address", address);

        // Save items as sub-list
        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (CartItem item : cartItems) {
            Map<String, Object> map = new HashMap<>();
            map.put("productId", item.getProductId());
            map.put("productName", item.getProductName());
            map.put("productImage", item.getProductImage());
            map.put("price", item.getPrice());
            map.put("quantity", item.getQuantity());
            map.put("sellerId", item.getSellerId());
            itemsList.add(map);
        }
        orderData.put("items", itemsList);

        // Save under buyer
        db.collection("users").document(buyerId).collection("orders").document(orderId)
                .set(orderData)
                .addOnSuccessListener(aVoid -> {
                    // Mirror under seller
                    db.collection("users").document(sellerId).collection("orders").document(orderId)
                            .set(orderData)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show();

                                // Clear buyer cart
                                clearCart(buyerId);
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to save order for seller", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to place order", Toast.LENGTH_SHORT).show());
    }

    private void clearCart(String buyerId) {
        db.collection("users").document(buyerId).collection("cart")
                .get()
                .addOnSuccessListener(query -> {
                    for (var doc : query.getDocuments()) {
                        doc.getReference().delete();
                    }
                });
    }
}
