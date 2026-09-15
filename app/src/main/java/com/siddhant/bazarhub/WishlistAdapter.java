package com.siddhant.bazarhub;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder> {

    private Context context;
    private List<Product> wishlist;

    public WishlistAdapter(Context context, List<Product> wishlist) {
        this.context = context;
        this.wishlist = wishlist;
    }

    @NonNull
    @Override
    public WishlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wishlist, parent, false);
        return new WishlistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WishlistViewHolder holder, int position) {
        Product product = wishlist.get(position);

        holder.tvName.setText(product.getName());
        holder.tvPrice.setText("₹" + product.getPrice());

        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.drawable.placeholder)
                .into(holder.ivProduct);

        // 👉 View product details
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("productId", product.getProductId());
            context.startActivity(intent);
        });

        // 👉 Add to cart
        holder.btnAddToCart.setOnClickListener(v -> {
            Intent intent = new Intent(context, CheckoutActivity.class);
            intent.putExtra("productId", product.getProductId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return wishlist.size();
    }

    public static class WishlistViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvPrice;
        Button btnAddToCart;

        public WishlistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivWishlistProduct);
            tvName = itemView.findViewById(R.id.tvWishlistName);
            tvPrice = itemView.findViewById(R.id.tvWishlistPrice);
            btnAddToCart = itemView.findViewById(R.id.btnWishlistAddToCart);
        }
    }
}
