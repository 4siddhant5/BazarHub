package com.siddhant.bazarhub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textTotal;
    private Button btnPlaceOrder;

    private CartAdapter cartAdapter;
    private List<CartItem> cartItems = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private double totalPrice = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerView = findViewById(R.id.recyclerCart);
        textTotal = findViewById(R.id.textTotal);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(this, cartItems, this::updateTotalPrice);
        recyclerView.setAdapter(cartAdapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadCartItems();

        btnPlaceOrder.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show();
            } else {
                // Go to PlaceOrderActivity with total & items
                Intent intent = new Intent(this, PlaceOrderActivity.class);
                intent.putExtra("totalAmount", totalPrice);
                intent.putExtra("cartItems", new ArrayList<>(cartItems)); // CartItem must implement Serializable
                startActivity(intent);
            }
        });
    }

    private void loadCartItems() {
        String uid = auth.getCurrentUser().getUid();
        CollectionReference cartRef = db.collection("users").document(uid).collection("cart");

        cartRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                cartItems.clear();
                totalPrice = 0.0;

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    CartItem item = doc.toObject(CartItem.class);
                    cartItems.add(item);
                    totalPrice += item.getPrice() * item.getQuantity();
                }

                cartAdapter.notifyDataSetChanged();
                updateTotalPrice();
            }
        });
    }

    private void updateTotalPrice() {
        totalPrice = 0.0;
        for (CartItem item : cartItems) {
            totalPrice += item.getPrice() * item.getQuantity();
        }
        textTotal.setText("Total: ₹" + totalPrice);
    }
}
