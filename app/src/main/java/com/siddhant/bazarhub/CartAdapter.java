package com.siddhant.bazarhub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<CartItem> cartItemList;
    private OnCartChangedListener listener;

    public interface OnCartChangedListener {
        void onQuantityChanged();
    }

    public CartAdapter(Context context, List<CartItem> cartItemList, OnCartChangedListener listener) {
        this.context = context;
        this.cartItemList = cartItemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItemList.get(position);

        holder.textName.setText(item.getProductName());
        holder.textPrice.setText("₹" + item.getPrice());
        holder.textQuantity.setText(String.valueOf(item.getQuantity()));

        Glide.with(context).load(item.getProductImage()).into(holder.imageProduct);

        // Increase quantity
        holder.btnIncrease.setOnClickListener(v -> {
            item.setQuantity(item.getQuantity() + 1);
            holder.textQuantity.setText(String.valueOf(item.getQuantity()));
            if (listener != null) listener.onQuantityChanged();
            notifyItemChanged(position);
        });

        // Decrease quantity
        holder.btnDecrease.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                holder.textQuantity.setText(String.valueOf(item.getQuantity()));
                if (listener != null) listener.onQuantityChanged();
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    public List<CartItem> getCartItems() {
        return cartItemList;
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imageProduct;
        TextView textName, textPrice, textQuantity;
        ImageButton btnIncrease, btnDecrease;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProduct = itemView.findViewById(R.id.imageProduct);
            textName = itemView.findViewById(R.id.textName);
            textPrice = itemView.findViewById(R.id.textPrice);
            textQuantity = itemView.findViewById(R.id.textQuantity);
            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
        }
    }
}
