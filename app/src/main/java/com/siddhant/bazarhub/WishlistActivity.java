package com.siddhant.bazarhub;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class WishlistActivity extends AppCompatActivity {

    private RecyclerView recyclerWishlist;
    private TextView tvEmptyWishlist;
    private WishlistAdapter adapter;
    private List<Product> wishlistItems;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        recyclerWishlist = findViewById(R.id.recyclerWishlist);
        tvEmptyWishlist = findViewById(R.id.tvEmptyWishlist);

        wishlistItems = new ArrayList<>();
        adapter = new WishlistAdapter(this, wishlistItems);

        recyclerWishlist.setLayoutManager(new LinearLayoutManager(this));
        recyclerWishlist.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadWishlist();
    }

    private void loadWishlist() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("wishlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    wishlistItems.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Product product = doc.toObject(Product.class);
                        if (product != null) {
                            wishlistItems.add(product);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    if (wishlistItems.isEmpty()) {
                        tvEmptyWishlist.setVisibility(View.VISIBLE);
                        recyclerWishlist.setVisibility(View.GONE);
                    } else {
                        tvEmptyWishlist.setVisibility(View.GONE);
                        recyclerWishlist.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    tvEmptyWishlist.setText("Failed to load wishlist ❌");
                    tvEmptyWishlist.setVisibility(View.VISIBLE);
                });
    }
}
