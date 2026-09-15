package com.siddhant.bazarhub;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ReportViewHolder> {

    private Context context;
    private List<Report> reportList;

    public ReportsAdapter(Context context, List<Report> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);

        holder.reasonText.setText("Reason: " + report.getReason());
        holder.reporterText.setText("Reporter: " + report.getReporterId());
        holder.productText.setText("Product: " + report.getProductId());

        ReportsActivity activity = (ReportsActivity) context;

        holder.viewProductBtn.setOnClickListener(v ->
                activity.viewProduct(report.getProductId()));

        holder.dismissBtn.setOnClickListener(v ->
                activity.dismissReport(report.getId()));

        holder.blockSellerBtn.setOnClickListener(v -> {
            // Show confirmation dialog before blocking seller
            new AlertDialog.Builder(context)
                    .setTitle("Confirm Block")
                    .setMessage("Are you sure you want to block this seller?\n\nSeller ID: " + report.getSellerId())
                    .setPositiveButton("Block", (dialog, which) ->
                            activity.blockSeller(report.getSellerId()))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView reasonText, reporterText, productText;
        Button viewProductBtn, dismissBtn, blockSellerBtn;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            reasonText = itemView.findViewById(R.id.reasonText);
            reporterText = itemView.findViewById(R.id.reporterText);
            productText = itemView.findViewById(R.id.productText);
            viewProductBtn = itemView.findViewById(R.id.viewProductBtn);
            dismissBtn = itemView.findViewById(R.id.dismissBtn);
            blockSellerBtn = itemView.findViewById(R.id.blockSellerBtn);
        }
    }
}
