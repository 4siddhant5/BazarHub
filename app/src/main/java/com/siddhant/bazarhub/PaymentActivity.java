package com.siddhant.bazarhub;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {

    private TextView tvProduct, tvPrice;
    private Button btnCOD, btnOnline;
    private ProgressBar progress;
    private String productId, sellerId;
    private double price;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        tvProduct = findViewById(R.id.tvProduct);
        tvPrice = findViewById(R.id.tvPrice);
        btnCOD = findViewById(R.id.btnCOD);
        btnOnline = findViewById(R.id.btnOnline);
        progress = findViewById(R.id.progress);

        productId = getIntent().getStringExtra("productId");
        sellerId = getIntent().getStringExtra("sellerId");
        price = getIntent().getDoubleExtra("price", 0);

        tvProduct.setText("Product: " + (productId == null ? "-" : productId));
        tvPrice.setText("Amount: ₹ " + ((long)price));

        btnCOD.setOnClickListener(v -> placeOrder("COD", "pending"));
        btnOnline.setOnClickListener(v -> startMockOnline());
    }

    private void startMockOnline() {
        // show simple dialog to accept fake UPI id
        View v = getLayoutInflater().inflate(R.layout.dialog_upi, null);
        EditText et = v.findViewById(R.id.etUpi);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Mock UPI Payment")
                .setView(v)
                .setPositiveButton("Pay", (d,w) -> {
                    String upi = et.getText().toString().trim();
                    if (upi.isEmpty()) { Toast.makeText(this,"Provide UPI",Toast.LENGTH_SHORT).show(); return; }
                    // simulate processing
                    progress.setVisibility(View.VISIBLE);
                    btnCOD.setEnabled(false); btnOnline.setEnabled(false);
                    // wait 1.5s then create order as paid
                    btnOnline.postDelayed(() -> {
                        progress.setVisibility(View.GONE);
                        placeOrder("Online", "paid");
                    }, 1500);
                }).setNegativeButton("Cancel", null).show();
    }

    private void placeOrder(String paymentType, String paymentStatus) {
        String buyer = Fire.auth().getCurrentUser() != null ? Fire.auth().getCurrentUser().getUid() : "anon";
        String orderId = Fire.db().collection("orders").document().getId();
        Map<String,Object> m = new HashMap<>();
        m.put("orderId", orderId);
        m.put("productId", productId);
        m.put("sellerId", sellerId);
        m.put("buyerId", buyer);
        m.put("amount", price);
        m.put("paymentType", paymentType);
        m.put("paymentStatus", paymentStatus);
        m.put("status", "placed");
        m.put("createdAt", Timestamp.now());
        Fire.db().collection("orders").document(orderId).set(m)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Order placed (" + paymentType + ")", Toast.LENGTH_LONG).show();
                    finish();
                }).addOnFailureListener(e -> Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
    }
}
