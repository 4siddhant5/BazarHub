package com.siddhant.bazarhub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

    private final Context ctx;
    private final List<DocumentSnapshot> docs = new ArrayList<>();
    private boolean adminMode = false;
    private AdminOrderActionListener listener;

    public OrderAdapter(Context ctx) {
        this.ctx = ctx;
    }

    public OrderAdapter(Context ctx, boolean adminMode, AdminOrderActionListener listener) {
        this.ctx = ctx;
        this.adminMode = adminMode;
        this.listener = listener;
    }

    public void setDocs(List<DocumentSnapshot> docs) {
        this.docs.clear();
        this.docs.addAll(docs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.row_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        DocumentSnapshot doc = docs.get(i);
        String id = doc.getId();
        String buyer = doc.getString("buyerName");
        String status = doc.getString("status");

        h.tvBuyer.setText("Buyer: " + buyer);
        h.tvId.setText("Order ID: " + id);

        if (adminMode) {
            h.spnStatus.setVisibility(View.VISIBLE);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    ctx,
                    android.R.layout.simple_spinner_item,
                    new String[]{"pending", "approved", "shipped", "delivered", "cancelled"}
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            h.spnStatus.setAdapter(adapter);

            if (status != null) {
                int pos = adapter.getPosition(status);
                if (pos >= 0) h.spnStatus.setSelection(pos);
            }

            h.spnStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id2) {
                    String newStatus = (String) parent.getItemAtPosition(pos);
                    if (!newStatus.equals(status)) {
                        listener.onStatusChange(id, newStatus);
                    }
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

        } else {
            h.spnStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return docs.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvBuyer, tvId;
        Spinner spnStatus;
        VH(View v) {
            super(v);
            tvBuyer = v.findViewById(R.id.tvBuyer);
            tvId = v.findViewById(R.id.tvId);
            spnStatus = v.findViewById(R.id.spnStatus);
        }
    }

    public interface AdminOrderActionListener {
        void onStatusChange(String orderId, String status);
    }
}
