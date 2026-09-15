package com.siddhant.bazarhub;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.*;

import java.util.*;

public class HomeFragment extends Fragment {

    private RecyclerView rv;
    private TextView tvEmpty;
    private ProductAdapter adapter;
    private Spinner spCategory;
    private EditText etSearch, etMinPrice, etMaxPrice;
    private Button btnFilter, btnClearFilters, btnMyListings;

    private List<DocumentSnapshot> allApprovedDocs = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home_filters, container, false);

        rv = v.findViewById(R.id.rv);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        spCategory = v.findViewById(R.id.spCategoryFilter);
        etSearch = v.findViewById(R.id.etSearch);
        etMinPrice = v.findViewById(R.id.etMinPrice);
        etMaxPrice = v.findViewById(R.id.etMaxPrice);
        btnFilter = v.findViewById(R.id.btnApplyFilter);
        btnClearFilters = v.findViewById(R.id.btnClearFilter);
        btnMyListings = v.findViewById(R.id.btnMyListings);

        rv.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ProductAdapter(null, requireContext());
        rv.setAdapter(adapter);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"All","Mobiles","Electronics","Vehicles","Home & Furniture","Fashion","Books","Other"});
        spCategory.setAdapter(catAdapter);

        btnFilter.setOnClickListener(x -> applyFilters());
        btnClearFilters.setOnClickListener(x -> {
            etSearch.setText("");
            etMinPrice.setText("");
            etMaxPrice.setText("");
            spCategory.setSelection(0);
            applyFilters();
        });

        btnMyListings.setOnClickListener(x -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new MyListingsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // init fused location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        requestLocation();

        // initial load: approved only and last created
        queryApproved();

        return v;
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission if not granted
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        currentLocation = loc;
                        applyFilters(); // re-apply filters with location
                    }
                });
    }

    private void queryApproved() {
        Fire.db().collection("products")
                .whereEqualTo("status","approved")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) return;
                    if (snap != null) {
                        allApprovedDocs.clear();
                        allApprovedDocs.addAll(snap.getDocuments());
                        applyFilters(); // show initial data
                    }
                });
    }

    private void applyFilters() {
        String cat = spCategory.getSelectedItem().toString();
        String search = etSearch.getText().toString().trim().toLowerCase();
        String minStr = etMinPrice.getText().toString().trim();
        String maxStr = etMaxPrice.getText().toString().trim();

        List<DocumentSnapshot> filtered = new ArrayList<>();

        for (DocumentSnapshot doc : allApprovedDocs) {
            Product p = doc.toObject(Product.class);
            if (p == null) continue;

            boolean remove = false;

            // Category filter
            if (!"All".equals(cat) && (p.category == null || !p.category.equalsIgnoreCase(cat)))
                remove = true;

            // Search keyword filter
            if (!TextUtils.isEmpty(search) &&
                    !((p.title != null && p.title.toLowerCase().contains(search)) ||
                            (p.description != null && p.description.toLowerCase().contains(search))))
                remove = true;

            // Price filter
            if (!TextUtils.isEmpty(minStr)) {
                try { if (p.price < Double.parseDouble(minStr)) remove = true; } catch (Exception ignored) {}
            }
            if (!TextUtils.isEmpty(maxStr)) {
                try { if (p.price > Double.parseDouble(maxStr)) remove = true; } catch (Exception ignored) {}
            }

            // Compute distance if location available
            if (currentLocation != null && p.latitude != 0 && p.longitude != 0) {
                float[] results = new float[1];
                Location.distanceBetween(
                        currentLocation.getLatitude(), currentLocation.getLongitude(),
                        p.latitude, p.longitude,
                        results
                );
                p._distanceKm = results[0] / 1000.0; // meters → km
            } else {
                p._distanceKm = null;
            }

            if (!remove) filtered.add(doc);
        }

        // sort by distance if available
        if (currentLocation != null) {
            filtered.sort((d1, d2) -> {
                Product p1 = d1.toObject(Product.class);
                Product p2 = d2.toObject(Product.class);
                if (p1 == null || p2 == null) return 0;
                Double dKm1 = p1._distanceKm;
                Double dKm2 = p2._distanceKm;
                if (dKm1 == null && dKm2 == null) return 0;
                if (dKm1 == null) return 1;
                if (dKm2 == null) return -1;
                return Double.compare(dKm1, dKm2);
            });
        }

        adapter.setDocs(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // Handle location permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation();
        }
    }
}
