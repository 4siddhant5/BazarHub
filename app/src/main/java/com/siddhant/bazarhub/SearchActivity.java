package com.siddhant.bazarhub;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.GeoPoint;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {

    private EditText searchBox, etMinPrice, etMaxPrice;
    private Spinner spCategoryFilter;
    private Button btnApply, btnClear;
    private TextView tvEmpty, tvStats; // <- stats bar
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private final List<Product> productList = new ArrayList<>();
    private FirebaseFirestore db;

    private String currentKeyword = "";
    private String currentCategory = "All";
    private double minPrice = 0, maxPrice = Double.MAX_VALUE;

    // Location filter
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentUserLocation;
    private double filterRadiusKm = 10; // change if you want a different default

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchBox = findViewById(R.id.searchBox);
        recyclerView = findViewById(R.id.recyclerViewSearch);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(productList, this);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Filters
        spCategoryFilter = findViewById(R.id.spCategoryFilter);
        etMinPrice = findViewById(R.id.etMinPrice);
        etMaxPrice = findViewById(R.id.etMaxPrice);
        btnApply = findViewById(R.id.btnApplyFilter);
        btnClear = findViewById(R.id.btnClearFilter);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvStats = findViewById(R.id.tvStats);

        // Categories
        List<String> categories = Arrays.asList("All", "Electronics", "Clothes", "Home", "Books", "Others");
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategoryFilter.setAdapter(catAdapter);

        // Search typing
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentKeyword = s.toString().trim();
                searchProducts();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Apply
        btnApply.setOnClickListener(v -> {
            currentCategory = spCategoryFilter.getSelectedItem() != null
                    ? spCategoryFilter.getSelectedItem().toString()
                    : "All";

            try {
                minPrice = etMinPrice.getText().toString().isEmpty()
                        ? 0
                        : Double.parseDouble(etMinPrice.getText().toString());
            } catch (Exception e) { minPrice = 0; }

            try {
                maxPrice = etMaxPrice.getText().toString().isEmpty()
                        ? Double.MAX_VALUE
                        : Double.parseDouble(etMaxPrice.getText().toString());
            } catch (Exception e) { maxPrice = Double.MAX_VALUE; }

            searchProducts();
        });

        // Clear
        btnClear.setOnClickListener(v -> {
            currentCategory = "All";
            minPrice = 0;
            maxPrice = Double.MAX_VALUE;
            etMinPrice.setText("");
            etMaxPrice.setText("");
            if (spCategoryFilter.getAdapter() != null) spCategoryFilter.setSelection(0);
            searchProducts();
        });

        // Location once
        requestUserLocation();
    }

    private void requestUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentUserLocation = location;
                    }
                    // Run search regardless (with/without location)
                    searchProducts();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestUserLocation();
            } else {
                // No location -> still show results
                searchProducts();
            }
        }
    }

    private void searchProducts() {
        Query query = db.collection("products")
                .whereEqualTo("status", "approved");

        if (!currentCategory.equals("All")) {
            query = query.whereEqualTo("category", currentCategory);
        }

        query = query.whereGreaterThanOrEqualTo("price", minPrice)
                .whereLessThanOrEqualTo("price", maxPrice);

        query.limit(100).get().addOnSuccessListener(snap -> {
            productList.clear();

            for (DocumentSnapshot doc : snap) {
                Product p = doc.toObject(Product.class);
                if (p == null) continue;

                boolean matchesKeyword = currentKeyword.isEmpty()
                        || (p.title != null && p.title.toLowerCase().contains(currentKeyword.toLowerCase()));

                boolean matchesLocation = true;
                Double computedDistanceKm = null;

                if (currentUserLocation != null && doc.contains("locationGeo")) {
                    GeoPoint gp = doc.getGeoPoint("locationGeo");
                    if (gp != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(
                                currentUserLocation.getLatitude(),
                                currentUserLocation.getLongitude(),
                                gp.getLatitude(),
                                gp.getLongitude(),
                                results
                        );
                        computedDistanceKm = results[0] / 1000.0;
                        // Filter by radius
                        matchesLocation = computedDistanceKm <= filterRadiusKm;
                    }
                }

                if (matchesKeyword && matchesLocation) {
                    // attach distance for adapter/UI (not persisted)
                    p._distanceKm = computedDistanceKm;
                    productList.add(p);
                }
            }

            // Sort: if both items have distance -> nearest first; else keep natural order
            Collections.sort(productList, new Comparator<Product>() {
                @Override
                public int compare(Product a, Product b) {
                    if (a._distanceKm != null && b._distanceKm != null) {
                        return Double.compare(a._distanceKm, b._distanceKm);
                    } else if (a._distanceKm != null) {
                        return -1;
                    } else if (b._distanceKm != null) {
                        return 1;
                    }
                    return 0;
                }
            });

            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(productList.isEmpty() ? View.VISIBLE : View.GONE);

            // Aggregations after the final list
            updateStatsBar();
        });
    }

    private void updateStatsBar() {
        if (productList.isEmpty()) {
            tvStats.setText("");
            return;
        }
        int count = productList.size();
        double min = Double.MAX_VALUE, max = 0, sum = 0;
        for (Product p : productList) {
            double price = p.price;
            if (price < min) min = price;
            if (price > max) max = price;
            sum += price;
        }
        double avg = sum / count;

        String stats = "Results: " + count
                + "  •  Min: " + currency.format(min)
                + "  •  Avg: " + currency.format(avg)
                + "  •  Max: " + currency.format(max);

        tvStats.setText(stats);
    }
}
