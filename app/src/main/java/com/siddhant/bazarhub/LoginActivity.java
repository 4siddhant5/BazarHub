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
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private ProgressBar progress;
    private Button btnLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        progress = findViewById(R.id.progress);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> doLogin());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void doLogin() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();

        if (TextUtils.isEmpty(email)) { etEmail.setError("Required"); return; }
        if (TextUtils.isEmpty(pass)) { etPassword.setError("Required"); return; }

        setLoading(true);
        Fire.auth().signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(this::onLoginSuccess)
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void onLoginSuccess(AuthResult authResult) {
        FirebaseUser user = authResult.getUser();
        if (user == null) {
            setLoading(false);
            Toast.makeText(this, "Login error: null user", Toast.LENGTH_LONG).show();
            return;
        }

        String uid = user.getUid();
        Fire.db().collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    setLoading(false);
                    if (!snapshot.exists()) {
                        Toast.makeText(this, "User profile missing", Toast.LENGTH_LONG).show();
                        Fire.auth().signOut();
                        return;
                    }

                    Boolean blocked = snapshot.getBoolean("blocked");
                    if (blocked != null && blocked) {
                        Toast.makeText(this, "Your account has been blocked. Contact support.", Toast.LENGTH_LONG).show();
                        Fire.auth().signOut();
                        return;
                    }

                    // Redirect based on role
                    String role = snapshot.getString("role");
                    Intent i;
                    if ("admin".equals(role)) {
                        i = new Intent(this, AdminDashboardActivity.class);
                    } else {
                        i = new Intent(this, UserDashboardActivity.class);
                    }
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Error loading user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Fire.auth().signOut();
                });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
