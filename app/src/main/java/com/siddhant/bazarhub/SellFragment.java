package com.siddhant.bazarhub;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SellFragment extends Fragment {

    private EditText etTitle, etDesc, etPrice, etLocation;
    private Spinner spCategory;
    private Button btnPick, btnPost;
    private ProgressBar progress;
    private RecyclerView rvImages;
    private final List<Uri> pickedUris = new ArrayList<>();
    private ImageAdapter imageAdapter;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMultiple;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_sell, c, false);
        etTitle = v.findViewById(R.id.etTitle);
        etDesc = v.findViewById(R.id.etDesc);
        etPrice = v.findViewById(R.id.etPrice);
        etLocation = v.findViewById(R.id.etLocation);
        spCategory = v.findViewById(R.id.spCategory);
        btnPick = v.findViewById(R.id.btnPickImages);
        btnPost = v.findViewById(R.id.btnPost);
        progress = v.findViewById(R.id.progress);
        rvImages = v.findViewById(R.id.rvImages);

        // categories
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Mobiles","Electronics","Vehicles","Home & Furniture","Fashion","Books","Other"});
        spCategory.setAdapter(catAdapter);

        rvImages.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        imageAdapter = new ImageAdapter(pickedUris);
        rvImages.setAdapter(imageAdapter);

        pickMultiple = registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5),
                uris -> {
                    pickedUris.clear();
                    pickedUris.addAll(uris);
                    imageAdapter.notifyDataSetChanged();
                });

        btnPick.setOnClickListener(v1 ->
                pickMultiple.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build())
        );

        btnPost.setOnClickListener(v12 -> doPost());

        return v;
    }

    private void doPost() {
        String title = etTitle.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String category = (String) spCategory.getSelectedItem();

        if (TextUtils.isEmpty(title)) { etTitle.setError("Required"); return; }
        if (TextUtils.isEmpty(desc)) { etDesc.setError("Required"); return; }
        if (TextUtils.isEmpty(priceStr)) { etPrice.setError("Required"); return; }
        if (pickedUris.isEmpty()) { Toast.makeText(getContext(),"Pick at least 1 image",Toast.LENGTH_LONG).show(); return; }

        double price;
        try { price = Double.parseDouble(priceStr); } catch (Exception e) { etPrice.setError("Invalid"); return; }

        FirebaseUser u = Fire.auth().getCurrentUser();
        if (u == null) { Toast.makeText(getContext(),"Login again",Toast.LENGTH_LONG).show(); return; }

        setLoading(true);

        String productId = Fire.db().collection("products").document().getId(); // pre-generate id
        StorageReference baseRef = Fire.storage().getReference().child("products").child(productId);

        // Upload images sequentially and collect download URLs
        List<String> downloadUrls = new ArrayList<>();

        uploadNextImage(0, baseRef, downloadUrls, () -> {
            // Save product doc
            Product p = new Product(
                    productId,
                    u.getUid(),
                    title,
                    desc,
                    category,
                    price,
                    location,
                    downloadUrls,
                    "pending"
            );
            Fire.db().collection("products").document(productId)
                    .set(p.toMap())
                    .addOnSuccessListener(v -> {
                        setLoading(false);
                        Toast.makeText(getContext(),"Submitted for approval",Toast.LENGTH_LONG).show();
                        clearForm();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }

    private void uploadNextImage(int index, StorageReference baseRef, List<String> outUrls, Runnable onDone) {
        if (index >= pickedUris.size()) {
            onDone.run(); return;
        }
        Uri uri = pickedUris.get(index);
        String name = "img_" + index + "_" + UUID.randomUUID() + ".jpg";
        StorageReference ref = baseRef.child(name);
        ref.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return ref.getDownloadUrl();
        }).addOnSuccessListener(dl -> {
            outUrls.add(dl.toString());
            uploadNextImage(index + 1, baseRef, outUrls, onDone);
        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(getContext(),"Upload failed: "+e.getMessage(),Toast.LENGTH_LONG).show();
        });
    }

    private void clearForm() {
        etTitle.setText(""); etDesc.setText(""); etPrice.setText(""); etLocation.setText("");
        pickedUris.clear(); imageAdapter.notifyDataSetChanged();
        spCategory.setSelection(0);
    }

    private void setLoading(boolean b) {
        progress.setVisibility(b ? View.VISIBLE : View.GONE);
        btnPost.setEnabled(!b); btnPick.setEnabled(!b);
    }

    // tiny adapter to preview selected images
    static class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.VH> {
        private final List<Uri> uris;
        ImageAdapter(List<Uri> uris){ this.uris = uris; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            ImageView iv = new ImageView(p.getContext());
            int size = (int)(p.getResources().getDisplayMetrics().density * 96);
            iv.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            int pad = (int)(p.getResources().getDisplayMetrics().density * 4);
            iv.setPadding(pad,pad,pad,pad);
            return new VH(iv);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int i) {
            Glide.with(h.iv.getContext()).load(uris.get(i)).into(h.iv);
        }
        @Override public int getItemCount(){ return uris.size(); }
        static class VH extends RecyclerView.ViewHolder { ImageView iv; VH(@NonNull View v){ super(v); iv=(ImageView)v; } }
    }
}
