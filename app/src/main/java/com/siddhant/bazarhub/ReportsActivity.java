package com.siddhant.bazarhub;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ReportsActivity extends AppCompatActivity {

    private RecyclerView reportsRecyclerView;
    private ReportsAdapter reportsAdapter;
    private List<Report> reportList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        db = FirebaseFirestore.getInstance();
        reportList = new ArrayList<>();
        reportsAdapter = new ReportsAdapter(this, reportList);

        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportsRecyclerView.setAdapter(reportsAdapter);

        loadReports();
    }

    private void loadReports() {
        db.collection("reports")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    reportList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Report report = doc.toObject(Report.class);
                        if (report != null) {
                            report.setId(doc.getId());
                            reportList.add(report);
                        }
                    }
                    reportsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load reports", Toast.LENGTH_SHORT).show();
                });
    }

    // Called from adapter to dismiss report
    public void dismissReport(String reportId) {
        db.collection("reports").document(reportId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Report dismissed", Toast.LENGTH_SHORT).show();
                    loadReports();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to dismiss report", Toast.LENGTH_SHORT).show()
                );
    }

    // Called from adapter to block seller (with confirmation dialog)
    public void blockSeller(String sellerId) {
        new AlertDialog.Builder(this)
                .setTitle("Block Seller")
                .setMessage("Are you sure you want to block this seller? This action cannot be undone.")
                .setPositiveButton("Block", (dialog, which) -> {
                    db.collection("users").document(sellerId)
                            .update("blocked", true)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Seller blocked", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to block seller", Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Called from adapter to open product
    public void viewProduct(String productId) {
        Intent intent = new Intent(this, ProductDetailsActivity.class);
        intent.putExtra("productId", productId);
        startActivity(intent);
    }
}
