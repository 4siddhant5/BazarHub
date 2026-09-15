package com.siddhant.bazarhub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private RecyclerView rvMyProducts;
    private ProductAdapter adapter;
    private final List<Product> myProducts = new ArrayList<>();
    private ImageView ivProfilePic;

    // UI refs
    private TextView tvName, tvEmail, tvRole, tvAvgRating;
    private Button btnLogout, btnEditProfile, btnMyOrders; // <-- Added MyOrders

    // Dialog preview holder for picked photo
    private ImageView dialogPhotoPreview;
    private Uri selectedImageUri = null;

    // Image picker launcher
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (dialogPhotoPreview != null && selectedImageUri != null) {
                        Glide.with(requireContext()).load(selectedImageUri)
                                .placeholder(R.drawable.ic_user_placeholder)
                                .circleCrop()
                                .into(dialogPhotoPreview);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_profile, c, false);

        // Views
        tvName = v.findViewById(R.id.tvName);
        tvEmail = v.findViewById(R.id.tvEmail);
        tvRole = v.findViewById(R.id.tvRole);
        rvMyProducts = v.findViewById(R.id.rvMyProducts);
        ivProfilePic = v.findViewById(R.id.ivProfilePic);
        btnLogout = v.findViewById(R.id.btnLogout);
        tvAvgRating = v.findViewById(R.id.tvAvgRating);
        btnEditProfile = v.findViewById(R.id.btnEditProfile);
        btnMyOrders = v.findViewById(R.id.btnMyOrders); // NEW

        // RecyclerView
        rvMyProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyProducts.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public boolean animateAdd(@NonNull RecyclerView.ViewHolder holder) {
                holder.itemView.setAlpha(0f);
                holder.itemView.animate().alpha(1f).setDuration(300).start();
                return super.animateAdd(holder);
            }
        });
        adapter = new ProductAdapter(myProducts, requireContext());
        rvMyProducts.setAdapter(adapter);

        FirebaseUser u = Fire.auth().getCurrentUser();
        if (u != null) {
            // name/email
            tvName.setText(u.getDisplayName() != null ? u.getDisplayName() : "Bazar User");
            tvEmail.setText(u.getEmail());

            // photo
            if (u.getPhotoUrl() != null) {
                Glide.with(requireContext())
                        .load(u.getPhotoUrl())
                        .placeholder(R.drawable.ic_user_placeholder)
                        .circleCrop()
                        .into(ivProfilePic);
            } else {
                ivProfilePic.setImageResource(R.drawable.ic_user_placeholder);
            }

            // role + avg rating
            Fire.db().collection("users").document(u.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        String role = doc.getString("role");
                        tvRole.setText(role != null ? role : "user");

                        Double avg = doc.getDouble("avgRating");
                        Long count = doc.getLong("ratingCount");
                        if (avg != null && count != null && count > 0) {
                            tvAvgRating.setVisibility(View.VISIBLE);
                            tvAvgRating.setText(String.format(Locale.getDefault(),
                                    "Rating: %.1f (%d)", avg, count));
                        } else {
                            tvAvgRating.setVisibility(View.GONE);
                        }
                    });

            // user’s products
            Fire.db().collection("products")
                    .whereEqualTo("sellerId", u.getUid())
                    .get()
                    .addOnSuccessListener(query -> {
                        myProducts.clear();
                        for (QueryDocumentSnapshot doc : query) {
                            Product p = doc.toObject(Product.class);
                            if (p != null) myProducts.add(p);
                        }
                        adapter.notifyDataSetChanged();
                    });
        }

        // logout
        btnLogout.setOnClickListener(v1 -> {
            Fire.auth().signOut();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        // Edit profile
        btnEditProfile.setOnClickListener(v12 -> openEditProfileDialog());

        // 👉 My Orders
        btnMyOrders.setOnClickListener(v13 -> {
            Intent intent = new Intent(getActivity(), OrdersActivity.class);
            startActivity(intent);
        });

        return v;
    }

    // ==== Edit Profile flow (name + photo) ====
    private void openEditProfileDialog() {
        View dv = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        TextView etName = dv.findViewById(R.id.etDisplayName);
        ImageView ivPreview = dv.findViewById(R.id.ivDialogPhoto);
        Button btnChangePhoto = dv.findViewById(R.id.btnChangePhoto);

        FirebaseUser u = Fire.auth().getCurrentUser();
        if (u == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // set initial
        etName.setText(u.getDisplayName() != null ? u.getDisplayName() : "");
        if (u.getPhotoUrl() != null) {
            Glide.with(requireContext()).load(u.getPhotoUrl())
                    .placeholder(R.drawable.ic_user_placeholder)
                    .circleCrop().into(ivPreview);
        } else {
            ivPreview.setImageResource(R.drawable.ic_user_placeholder);
        }

        dialogPhotoPreview = ivPreview;
        selectedImageUri = null;

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(dv)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (d, w) -> {
                    dialogPhotoPreview = null;
                    selectedImageUri = null;
                })
                .create();

        dialog.setOnShowListener(dlg -> {
            Button btnSave = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            btnSave.setOnClickListener(v -> {
                String newName = etName.getText().toString().trim();
                saveProfileChanges(newName, dialog);
            });
        });

        btnChangePhoto.setOnClickListener(v -> pickImageFromGallery());

        dialog.show();
    }

    private void pickImageFromGallery() {
        Intent pick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pick.setType("image/*");
        imagePickerLauncher.launch(pick);
    }

    private void saveProfileChanges(String newName, android.app.AlertDialog dialog) {
        FirebaseUser u = Fire.auth().getCurrentUser();
        if (u == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        if (selectedImageUri != null) {
            StorageReference ref = FirebaseStorage.getInstance()
                    .getReference("profile_photos/" + u.getUid() + ".jpg");

            ref.putFile(selectedImageUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(uri -> {
                        applyProfileUpdates(u, newName, uri.toString(), dialog);
                    })
                    .addOnFailureListener(e -> {
                        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(requireContext(), "Photo upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            String currentPhoto = (u.getPhotoUrl() != null) ? u.getPhotoUrl().toString() : null;
            applyProfileUpdates(u, newName, currentPhoto, dialog);
        }
    }

    private void applyProfileUpdates(FirebaseUser u, @Nullable String newName, @Nullable String photoUrl, android.app.AlertDialog dialog) {
        UserProfileChangeRequest.Builder up = new UserProfileChangeRequest.Builder();
        if (newName != null && !newName.isEmpty()) up.setDisplayName(newName);
        if (photoUrl != null && !photoUrl.isEmpty()) up.setPhotoUri(Uri.parse(photoUrl));

        u.updateProfile(up.build())
                .addOnSuccessListener(v -> {
                    Map<String, Object> map = new HashMap<>();
                    if (newName != null && !newName.isEmpty()) map.put("name", newName);
                    if (photoUrl != null && !photoUrl.isEmpty()) map.put("photoUrl", photoUrl);

                    if (map.isEmpty()) {
                        finalizeProfileUI(u, newName, photoUrl, dialog);
                        return;
                    }

                    Fire.db().collection("users").document(u.getUid())
                            .set(map, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(v2 -> finalizeProfileUI(u, newName, photoUrl, dialog))
                            .addOnFailureListener(e -> {
                                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                Toast.makeText(requireContext(), "Saved to auth, Firestore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(requireContext(), "Auth update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void finalizeProfileUI(FirebaseUser u, @Nullable String newName, @Nullable String photoUrl, android.app.AlertDialog dialog) {
        try {
            Tasks.await(u.reload());
        } catch (Exception ignored) {}

        if (newName != null && !newName.isEmpty()) {
            tvName.setText(newName);
        } else if (u.getDisplayName() != null) {
            tvName.setText(u.getDisplayName());
        }

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(requireContext()).load(photoUrl).placeholder(R.drawable.ic_user_placeholder).circleCrop().into(ivProfilePic);
        }

        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
        dialog.dismiss();

        dialogPhotoPreview = null;
        selectedImageUri = null;
    }
}
