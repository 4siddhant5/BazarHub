package com.siddhant.bazarhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

public class ProductDetailsActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabDots;
    private ImagePagerAdapter pagerAdapter;
    private TextView tvTitle, tvPrice, tvDesc, tvSeller, tvCategory, tvLocation, tvStatus, tvAvgRating;
    private Button btnWishlist, btnRate, btnReport, btnChat, btnBuy;
    private RatingBar ratingBar;
    private String productId;
    private Product product;
    private DocumentReference productRef;
    private FirebaseUser current;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        viewPager = findViewById(R.id.viewPagerImages);
        tabDots = findViewById(R.id.tabDots);
        tvTitle = findViewById(R.id.tvTitle);
        tvPrice = findViewById(R.id.tvPrice);
        tvDesc = findViewById(R.id.tvDesc);
        tvSeller = findViewById(R.id.tvSeller);
        tvCategory = findViewById(R.id.tvCategory);
        tvLocation = findViewById(R.id.tvLocation);
        tvStatus = findViewById(R.id.tvStatus);
        tvAvgRating = findViewById(R.id.tvAvgRating);

        btnWishlist = findViewById(R.id.btnWishlist);
        btnRate = findViewById(R.id.btnRate);
        btnReport = findViewById(R.id.btnReport);
        btnChat = findViewById(R.id.btnChat);
        btnBuy = findViewById(R.id.btnBuy);
        ratingBar = findViewById(R.id.ratingBar);

        current = Fire.auth().getCurrentUser();

        productId = getIntent().getStringExtra("productId");
        if (productId == null) {
            Toast.makeText(this, "No product", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        productRef = Fire.db().collection("products").document(productId);

        pagerAdapter = new ImagePagerAdapter(new ArrayList<>());
        viewPager.setAdapter(pagerAdapter);
        new TabLayoutMediator(tabDots, viewPager, (tab, pos) -> {}).attach();

        loadProduct();

        btnWishlist.setOnClickListener(v -> toggleWishlist());
        btnRate.setOnClickListener(v -> openRateDialog());
        btnReport.setOnClickListener(v -> reportProduct());
        btnChat.setOnClickListener(v -> openChat());
        btnBuy.setOnClickListener(v -> startBuyFlow());
    }

    private void loadProduct() {
        productRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Toast.makeText(this, "Not found", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            product = doc.toObject(Product.class);
            if (product == null) return;

            tvTitle.setText(product.title);
            tvPrice.setText("₹ " + ((long) product.price));
            tvDesc.setText(product.description);
            tvCategory.setText("Category: " + (product.category == null ? "Other" : product.category));
            tvLocation.setText("Location: " + (product.location == null ? "-" : product.location));
            tvStatus.setText("Status: " + product.status);

            tvAvgRating.setText(String.format(Locale.getDefault(),
                    "Avg: %.1f (%d)", product.avgRating, product.ratingCount));
            ratingBar.setRating((float) product.avgRating);

            if (product.images != null) pagerAdapter.setList(product.images);
            fetchSellerInfo(product.sellerId);
            updateWishlistButton();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void fetchSellerInfo(String sellerId) {
        Fire.db().collection("users").document(sellerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        tvSeller.setText("Seller: " + (name != null ? name : "Unknown"));
                    } else tvSeller.setText("Seller: Unknown");
                });
    }

    // --- Wishlist
    private void updateWishlistButton() {
        if (current == null) {
            btnWishlist.setEnabled(false);
            return;
        }
        DocumentReference docRef = Fire.db()
                .collection("users")
                .document(current.getUid())
                .collection("wishlist")
                .document(productId);

        docRef.get().addOnSuccessListener(snap -> {
            if (snap.exists()) {
                btnWishlist.setText("Remove from Wishlist");
            } else {
                btnWishlist.setText("Add to Wishlist");
            }
        });
    }

    private void toggleWishlist() {
        if (current == null) {
            Toast.makeText(this, "Login to wishlist", Toast.LENGTH_SHORT).show();
            return;
        }
        DocumentReference docRef = Fire.db()
                .collection("users")
                .document(current.getUid())
                .collection("wishlist")
                .document(productId);

        docRef.get().addOnSuccessListener(snap -> {
            if (snap.exists()) {
                docRef.delete()
                        .addOnSuccessListener(v -> {
                            Toast.makeText(this, "Removed from Wishlist", Toast.LENGTH_SHORT).show();
                            updateWishlistButton();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            } else {
                Map<String, Object> map = new HashMap<>();
                map.put("productId", productId);
                map.put("name", product.title);
                map.put("price", product.price);
                map.put("imageUrl", (product.images != null && !product.images.isEmpty()) ? product.images.get(0) : "");
                map.put("addedAt", com.google.firebase.Timestamp.now());

                docRef.set(map)
                        .addOnSuccessListener(v -> {
                            Toast.makeText(this, "Added to Wishlist", Toast.LENGTH_SHORT).show();
                            updateWishlistButton();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    // --- Rating dialog
    private void openRateDialog() {
        if (current == null) {
            Toast.makeText(this, "Login to rate", Toast.LENGTH_SHORT).show();
            return;
        }
        View v = getLayoutInflater().inflate(R.layout.dialog_rate, null);
        RatingBar rb = v.findViewById(R.id.dialogRatingBar);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Rate product")
                .setView(v)
                .setPositiveButton("Submit", (d, w) -> {
                    float r = rb.getRating();
                    if (r <= 0) {
                        Toast.makeText(this, "Pick rating", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DocumentReference rref = productRef.collection("ratings").document(current.getUid());
                    Map<String, Object> m = new HashMap<>();
                    m.put("rating", r);
                    m.put("givenAt", com.google.firebase.Timestamp.now());

                    rref.set(m).addOnSuccessListener(x -> {
                        Toast.makeText(this, "Thanks for rating", Toast.LENGTH_SHORT).show();
                        recomputeRating(); // 🔑 instantly update UI
                    }).addOnFailureListener(e ->
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null).show();
    }

    // 🔥 Recompute avgRating & count live
    private void recomputeRating() {
        productRef.collection("ratings").get().addOnSuccessListener(snaps -> {
            int count = snaps.size();
            double sum = 0;
            for (DocumentSnapshot d : snaps) {
                Number n = d.getDouble("rating");
                if (n != null) sum += n.doubleValue();
            }
            double avg = (count > 0) ? (sum / count) : 0;

            // update Firestore product doc
            Map<String, Object> upd = new HashMap<>();
            upd.put("avgRating", avg);
            upd.put("ratingCount", count);
            productRef.update(upd);

            // update UI instantly
            tvAvgRating.setText(String.format(Locale.getDefault(),
                    "Avg: %.1f (%d)", avg, count));
            ratingBar.setRating((float) avg);
        });
    }

    private void reportProduct() {
        View v = getLayoutInflater().inflate(R.layout.dialog_report, null);
        EditText et = v.findViewById(R.id.etReport);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Report product")
                .setView(v)
                .setPositiveButton("Send", (d, w) -> {
                    String reason = et.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Write reason", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (current == null) {
                        Toast.makeText(this, "Login required to report", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> m = new HashMap<>();
                    m.put("productId", productId);
                    m.put("reporterId", current.getUid());
                    m.put("sellerId", product != null ? product.sellerId : "unknown");
                    m.put("reason", reason);
                    m.put("timestamp", com.google.firebase.Timestamp.now());

                    Fire.db().collection("reports").add(m)
                            .addOnSuccessListener(xx -> Toast.makeText(this, "Reported. Admin will review.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- Chat
    private void openChat() {
        if (current == null) {
            Toast.makeText(this, "Login to chat", Toast.LENGTH_SHORT).show();
            return;
        }
        if (product == null) {
            Toast.makeText(this, "Product not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String sellerId = product.sellerId;
        String buyerId = current.getUid();

        if (buyerId.equals(sellerId)) {
            Toast.makeText(this, "You cannot chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        String chatId = buyerId + "_" + sellerId + "_" + productId;
        DocumentReference chatRef = Fire.db().collection("chats").document(chatId);

        chatRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Intent i = new Intent(this, ChatActivity.class);
                i.putExtra("chatId", chatId);
                i.putExtra("otherId", sellerId);
                i.putExtra("productId", productId);
                startActivity(i);
            } else {
                Map<String, Object> map = new HashMap<>();
                map.put("chatId", chatId);
                map.put("buyerId", buyerId);
                map.put("sellerId", sellerId);
                map.put("productId", productId);
                map.put("participants", Arrays.asList(buyerId, sellerId));
                map.put("productName", product.title);
                map.put("productImage", (product.images != null && !product.images.isEmpty()) ? product.images.get(0) : "");
                map.put("lastMessage", "");
                map.put("lastUpdated", com.google.firebase.Timestamp.now());

                chatRef.set(map).addOnSuccessListener(v -> {
                    Intent i = new Intent(this, ChatActivity.class);
                    i.putExtra("chatId", chatId);
                    i.putExtra("otherId", sellerId);
                    i.putExtra("productId", productId);
                    startActivity(i);
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    // --- Buy flow
    private void startBuyFlow() {
        if (current == null) {
            Toast.makeText(this, "Login to buy", Toast.LENGTH_SHORT).show();
            return;
        }
        if (product == null) {
            Toast.makeText(this, "Product not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, CheckoutActivity.class);
        i.putExtra("productId", productId);
        i.putExtra("sellerId", product.sellerId);
        i.putExtra("productName", product.title);
        i.putExtra("productImage", (product.images != null && !product.images.isEmpty()) ? product.images.get(0) : "");
        i.putExtra("productPrice", product.price);
        i.putExtra("quantity", 1);
        startActivity(i);
    }

    // --- Pager Adapter
    static class ImagePagerAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ImagePagerAdapter.IVH> {
        private final List<String> list;
        ImagePagerAdapter(List<String> list) { this.list = list; }
        void setList(List<String> l) { list.clear(); if (l != null) list.addAll(l); notifyDataSetChanged(); }
        @Override public IVH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.widget.ImageView iv = new android.widget.ImageView(parent.getContext());
            iv.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            return new IVH(iv);
        }
        @Override public void onBindViewHolder(IVH holder, int position) {
            com.bumptech.glide.Glide.with(holder.iv.getContext()).load(list.get(position)).into(holder.iv);
        }
        @Override public int getItemCount() { return list.size(); }
        static class IVH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.ImageView iv;
            IVH(android.view.View v){ super(v); iv=(android.widget.ImageView)v; }
        }
    }
}
