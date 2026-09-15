package com.siddhant.bazarhub;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private ProgressBar progress;
    private Button btnRegister;
    private TextView tvLogin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        progress = findViewById(R.id.progress);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> doRegister());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void doRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();

        if (TextUtils.isEmpty(name)) { etName.setError("Required"); return; }
        if (TextUtils.isEmpty(email)) { etEmail.setError("Required"); return; }
        if (pass.length() < 6) { etPassword.setError("Minimum 6 chars"); return; }

        setLoading(true);
        Fire.auth().createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(this::onRegisterSuccess)
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void onRegisterSuccess(AuthResult result) {
        FirebaseUser u = result.getUser();
        if (u == null) {
            setLoading(false);
            Toast.makeText(this, "Registration error: null user", Toast.LENGTH_LONG).show();
            return;
        }
        String uid = u.getUid();
        String email = u.getEmail();
        String name = etName.getText().toString().trim();
        String role = AdminList.isAdminEmail(email) ? "admin" : "user";

        // Add "blocked" field default false
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("role", role);
        userMap.put("blocked", false);

        DocumentReference ref = Fire.db().collection("users").document(uid);
        ref.set(userMap).addOnSuccessListener(aVoid -> {
            setLoading(false);

            // Directly route user to correct dashboard
            Intent i;
            if ("admin".equals(role)) {
                i = new Intent(this, AdminDashboardActivity.class);
            } else {
                i = new Intent(this, UserDashboardActivity.class);
            }
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
