package com.siddhant.bazarhub;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> orderList;
    private final boolean isSeller; // true = SellerOrdersActivity, false = MyOrdersActivity
    private final FirebaseFirestore db;

    public OrdersAdapter(Context context, List<Map<String, Object>> orderList, boolean isSeller) {
        this.context = context;
        this.orderList = orderList;
        this.isSeller = isSeller;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Map<String, Object> order = orderList.get(position);

        String orderId = (String) order.get("orderId");
        String status = (String) order.get("status");
        String date = (String) order.get("date");
        String buyerName = (String) order.get("buyerName");
        String sellerName = (String) order.get("sellerName");
        String sellerId = (String) order.get("sellerId");
        String buyerId = (String) order.get("buyerId");

        holder.textOrderId.setText("Order: " + orderId);
        holder.textStatus.setText("Status: " + status);
        holder.textDate.setText(date != null ? date : "");

        // ✅ Navigation → open OrderDetailsActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            intent.putExtra("orderId", orderId);
            intent.putExtra("userId", isSeller ? sellerId : buyerId); // pick correct Firestore path
            context.startActivity(intent);
        });

        if (isSeller) {
            // Seller view → show buyer info
            holder.textUser.setVisibility(View.VISIBLE);
            holder.textUser.setText("Buyer: " + (buyerName != null ? buyerName : buyerId));

            holder.layoutSellerControls.setVisibility(View.VISIBLE);

            // Reset button visibility
            holder.btnMarkShipped.setVisibility(View.GONE);
            holder.btnMarkDelivered.setVisibility(View.GONE);
            holder.btnCancel.setVisibility(View.GONE);

            // Show correct buttons depending on status
            if ("pending".equalsIgnoreCase(status)) {
                holder.btnMarkShipped.setVisibility(View.VISIBLE);
                holder.btnMarkShipped.setOnClickListener(v -> updateOrderStatus(orderId, sellerId, buyerId, "shipped"));
            } else if ("shipped".equalsIgnoreCase(status)) {
                holder.btnMarkDelivered.setVisibility(View.VISIBLE);
                holder.btnMarkDelivered.setOnClickListener(v -> updateOrderStatus(orderId, sellerId, buyerId, "delivered"));
            }

        } else {
            // Buyer view → show seller info
            holder.textUser.setVisibility(View.VISIBLE);
            holder.textUser.setText("Seller: " + (sellerName != null ? sellerName : sellerId));

            holder.layoutSellerControls.setVisibility(View.GONE);

            // Reset cancel button
            holder.btnCancel.setVisibility(View.GONE);

            // Buyer cancel allowed only when pending
            if ("pending".equalsIgnoreCase(status)) {
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnCancel.setOnClickListener(v -> cancelOrder(orderId, buyerId, sellerId));
            }
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView textOrderId, textStatus, textDate, textUser;
        LinearLayout layoutSellerControls;
        Button btnMarkShipped, btnMarkDelivered, btnCancel;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            textOrderId = itemView.findViewById(R.id.textOrderId);
            textStatus = itemView.findViewById(R.id.textStatus);
            textDate = itemView.findViewById(R.id.textDate);
            textUser = itemView.findViewById(R.id.textUser);
            layoutSellerControls = itemView.findViewById(R.id.layoutSellerControls);
            btnMarkShipped = itemView.findViewById(R.id.btnMarkShipped);
            btnMarkDelivered = itemView.findViewById(R.id.btnMarkDelivered);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }

    private void updateOrderStatus(String orderId, String sellerId, String buyerId, String newStatus) {
        // Seller path update
        db.collection("users")
                .document(sellerId)
                .collection("orders")
                .document(orderId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    // Mirror update in Buyer path
                    db.collection("users")
                            .document(buyerId)
                            .collection("orders")
                            .document(orderId)
                            .update("status", newStatus)
                            .addOnSuccessListener(aVoid2 ->
                                    Toast.makeText(context, "Order marked as " + newStatus, Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Failed to update buyer order", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to update order", Toast.LENGTH_SHORT).show()
                );
    }

    private void cancelOrder(String orderId, String buyerId, String sellerId) {
        // ✅ Cancel → update status instead of delete
        db.collection("users")
                .document(buyerId)
                .collection("orders")
                .document(orderId)
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
                    db.collection("users")
                            .document(sellerId)
                            .collection("orders")
                            .document(orderId)
                            .update("status", "cancelled")
                            .addOnSuccessListener(aVoid2 ->
                                    Toast.makeText(context, "Order cancelled", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Failed to update seller order", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to cancel order", Toast.LENGTH_SHORT).show()
                );
    }
}
