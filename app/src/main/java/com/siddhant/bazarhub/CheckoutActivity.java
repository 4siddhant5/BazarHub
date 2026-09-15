package com.siddhant.bazarhub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class CheckoutActivity extends AppCompatActivity {

    private ImageView productImage;
    private TextView productTitle, productPrice, totalPrice, tvQuantity;
    private Button btnConfirmOrder, btnCancelCheckout, btnIncrease, btnDecrease;

    private String productId, sellerId, productName, productImageUrl;
    private double price;
    private int quantity = 1;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // Init views
        productImage = findViewById(R.id.productImage);
        productTitle = findViewById(R.id.productTitle);
        productPrice = findViewById(R.id.productPrice);
        totalPrice = findViewById(R.id.totalPrice);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);
        btnCancelCheckout = findViewById(R.id.btnCancelCheckout);
        btnIncrease = findViewById(R.id.btnIncrease);
        btnDecrease = findViewById(R.id.btnDecrease);
        tvQuantity = findViewById(R.id.tvQuantity);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get product info from intent
        productId = getIntent().getStringExtra("productId");
        sellerId = getIntent().getStringExtra("sellerId");
        productName = getIntent().getStringExtra("productName");
        productImageUrl = getIntent().getStringExtra("productImage");
        price = getIntent().getDoubleExtra("productPrice", 0);
        quantity = getIntent().getIntExtra("quantity", 1);

        // Bind to UI
        productTitle.setText(productName);
        productPrice.setText("₹" + price);
        tvQuantity.setText(String.valueOf(quantity));
        updateTotalPrice();
        Glide.with(this).load(productImageUrl).into(productImage);

        // Quantity increase
        btnIncrease.setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
            updateTotalPrice();
        });

        // Quantity decrease
        btnDecrease.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
                updateTotalPrice();
            }
        });

        // Confirm order
        btnConfirmOrder.setOnClickListener(v -> placeOrder());

        // Cancel checkout
        btnCancelCheckout.setOnClickListener(v -> finish());
    }

    private void updateTotalPrice() {
        double total = price * quantity;
        totalPrice.setText("Total: ₹" + total);
    }

    private void placeOrder() {
        String buyerId = auth.getCurrentUser().getUid();
        String orderId = db.collection("orders").document().getId();
        long timestamp = System.currentTimeMillis();

        // Create order object
        Order order = new Order(
                orderId,
                productId,
                buyerId,
                sellerId,
                "pending",
                timestamp
        );

        // Save under global orders
        DocumentReference globalOrderRef = db.collection("orders").document(orderId);

        globalOrderRef.set(order)
                .addOnSuccessListener(unused -> {
                    // Also save quantity & total price as extra fields
                    globalOrderRef.update("quantity", quantity, "totalPrice", price * quantity);

                    Toast.makeText(this, "Order placed!", Toast.LENGTH_SHORT).show();
                    // Redirect to orders screen
                    Intent intent = new Intent(CheckoutActivity.this, OrdersActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
