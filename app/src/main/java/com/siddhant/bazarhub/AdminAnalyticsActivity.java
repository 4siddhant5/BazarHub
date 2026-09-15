package com.siddhant.bazarhub;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminAnalyticsActivity extends AppCompatActivity {

    private TextView totalUsersText, pendingProductsText, todayOrdersText;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_analytics);

        totalUsersText = findViewById(R.id.totalUsersText);
        pendingProductsText = findViewById(R.id.pendingProductsText);
        todayOrdersText = findViewById(R.id.todayOrdersText);

        db = FirebaseFirestore.getInstance();

        loadAnalytics();
    }

    private void loadAnalytics() {
        fetchTotalUsers();
        fetchPendingProducts();
        fetchTodayOrders();
    }

    private void fetchTotalUsers() {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                int count = task.getResult().size();
                totalUsersText.setText("Total Users: " + count);
            } else {
                totalUsersText.setText("Total Users: Error");
            }
        });
    }

    private void fetchPendingProducts() {
        db.collection("products")
                .whereEqualTo("approved", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int count = task.getResult().size();
                        pendingProductsText.setText("Pending Products: " + count);
                    } else {
                        pendingProductsText.setText("Pending Products: Error");
                    }
                });
    }

    private void fetchTodayOrders() {
        // Get today’s date (yyyy-MM-dd) to compare
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("orders")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int todayCount = 0;
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Timestamp timestamp = doc.getTimestamp("timestamp");
                            if (timestamp != null) {
                                String orderDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        .format(timestamp.toDate());
                                if (orderDate.equals(today)) {
                                    todayCount++;
                                }
                            }
                        }
                        todayOrdersText.setText("Today’s Orders: " + todayCount);
                    } else {
                        todayOrdersText.setText("Today’s Orders: Error");
                    }
                });
    }
}
