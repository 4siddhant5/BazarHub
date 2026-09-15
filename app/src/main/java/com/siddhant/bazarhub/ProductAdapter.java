package com.siddhant.bazarhub;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface AdminActionListener {
        void onApprove(String productId);
        void onReject(String productId);
    }

    private final Context ctx;
    private final List<Product> data = new ArrayList<>();
    private boolean adminMode = false;
    private AdminActionListener adminListener;

    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public ProductAdapter(List<Product> productList, Context ctx) {
        this.ctx = ctx;
        if (productList != null) {
            this.data.addAll(productList);
        }
    }

    public void setAdminMode(boolean adminMode, AdminActionListener listener) {
        this.adminMode = adminMode;
        this.adminListener = listener;
        notifyDataSetChanged();
    }

    public void setDocs(List<DocumentSnapshot> docs) {
        data.clear();
        for (DocumentSnapshot d : docs) {
            Product p = d.toObject(Product.class);
            if (p != null) data.add(p);
        }
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        Product p = data.get(i);

        h.tvTitle.setText(p.title != null ? p.title : "");

        // Price display (discount aware)
        double price = p.price;
        double discounted = (p.discountPercent > 0) ? p.getDiscountedPrice() : price;
        if (p.discountPercent > 0) {
            String mrp = currency.format(price);
            SpannableString strike = new SpannableString(mrp);
            strike.setSpan(new StrikethroughSpan(), 0, mrp.length(), 0);
            h.tvPrice.setText(strike);

            h.tvPriceDiscounted.setVisibility(View.VISIBLE);
            h.tvPriceDiscounted.setText(currency.format(discounted));
        } else {
            h.tvPrice.setText(currency.format(price));
            h.tvPriceDiscounted.setVisibility(View.GONE);
        }

        // Location string
        h.tvLocation.setText(p.location != null ? p.location : "");

        // Distance label
        if (p._distanceKm != null) {
            String dTxt = String.format(Locale.getDefault(), "%.1f km away", p._distanceKm);
            h.tvDistance.setText(dTxt);
            h.tvDistance.setVisibility(View.VISIBLE);
        } else {
            h.tvDistance.setVisibility(View.GONE);
        }

        // Thumbnail
        if (p.images != null && !p.images.isEmpty()) {
            Glide.with(ctx).load(p.images.get(0)).into(h.iv);
        } else {
            h.iv.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // Open details
        h.itemView.setOnClickListener(v -> {
            Intent it = new Intent(ctx, ProductDetailsActivity.class);
            it.putExtra("productId", p.id);
            ctx.startActivity(it);
        });

        // Admin buttons
        if (adminMode) {
            h.btnApprove.setVisibility(View.VISIBLE);
            h.btnReject.setVisibility(View.VISIBLE);
            h.btnApprove.setOnClickListener(v -> {
                if (adminListener != null) adminListener.onApprove(p.id);
            });
            h.btnReject.setOnClickListener(v -> {
                if (adminListener != null) adminListener.onReject(p.id);
            });
        } else {
            h.btnApprove.setVisibility(View.GONE);
            h.btnReject.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    public Product getAt(int pos) {
        return pos >= 0 && pos < data.size() ? data.get(pos) : null;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView iv;
        TextView tvTitle, tvPrice, tvPriceDiscounted, tvLocation, tvDistance;
        Button btnApprove, btnReject;
        VH(@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.ivThumb);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvPriceDiscounted = itemView.findViewById(R.id.tvPriceDiscounted);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
