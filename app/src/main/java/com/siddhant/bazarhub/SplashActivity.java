package com.siddhant.bazarhub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FirebaseUser current = Fire.auth().getCurrentUser();
        if (current == null) {
            goLogin();
            return;
        }

        String uid = current.getUid();
        final DocumentReference userRef = Fire.db().collection("users").document(uid);

        userRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String role = doc.getString("role");
                route(role);
            } else {
                // create user doc on the fly
                String email = current.getEmail();
                String displayName = current.getDisplayName();
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = "Bazar User";
                }
                String role = AdminList.isAdminEmail(email) ? "admin" : "user";
                AppUser appUser = new AppUser(uid, displayName, email, role);
                userRef.set(appUser.toMap())
                        .addOnSuccessListener(aVoid -> route(role))
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to init user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            goLogin();
                        });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            goLogin();
        });
    }

    private void goLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void route(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
        } else {
            startActivity(new Intent(this, UserDashboardActivity.class));
        }
        finish();
    }
}
