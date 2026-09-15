package com.siddhant.bazarhub;

import android.content.Intent;
import android.os.Bundle;
import androidx.cardview.widget.CardView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView cardApprovals, cardUsers, cardOrders, cardAnalytics, cardReports;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        cardApprovals = findViewById(R.id.cardApprovals);
        cardUsers = findViewById(R.id.cardUsers);
        cardOrders = findViewById(R.id.cardOrders);
        cardAnalytics = findViewById(R.id.cardAnalytics);
        cardReports = findViewById(R.id.cardReports);

        cardApprovals.setOnClickListener(v ->
                startActivity(new Intent(this, PendingApprovalsActivity.class)));

        cardUsers.setOnClickListener(v ->
                startActivity(new Intent(this, ManageUsersActivity.class)));

        cardOrders.setOnClickListener(v ->
                startActivity(new Intent(this, AdminOrdersActivity.class)));

        cardAnalytics.setOnClickListener(v ->
                startActivity(new Intent(this, AdminAnalyticsActivity.class)));

        cardReports.setOnClickListener(v ->
                startActivity(new Intent(this, ReportsActivity.class)));
    }
}
