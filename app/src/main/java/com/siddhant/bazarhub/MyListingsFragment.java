package com.siddhant.bazarhub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

public class MyListingsFragment extends Fragment {

    private RecyclerView rv;
    private ProductAdapter adapter;
    private FirebaseUser user;

    // ✅ Declare productList to fix the error
    private List<Product> productList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_my_listings, c, false);
        rv = v.findViewById(R.id.rvMyListings);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // ✅ Use the newly declared productList
        adapter = new ProductAdapter(productList, getContext());
        rv.setAdapter(adapter);

        user = Fire.auth().getCurrentUser();
        if (user == null) return v;

        Fire.db().collection("products")
                .whereEqualTo("sellerId", user.getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap != null) {
                        adapter.setDocs(snap.getDocuments());
                        // ✅ Update the local productList as well
                        productList.clear();
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Product p = doc.toObject(Product.class);
                            if (p != null) productList.add(p);
                        }
                    }
                });

        // Swipe to delete
        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                Product p = adapter.getAt(pos);
                if (p == null) { adapter.notifyItemChanged(pos); return; }

                Fire.db().collection("products").document(p.id).get().addOnSuccessListener(doc -> {
                    if (!doc.exists()) { adapter.notifyItemChanged(pos); return; }
                    String status = doc.getString("status");
                    String seller = doc.getString("sellerId");
                    if (user.getUid().equals(seller) || AdminList.isAdminEmail(user.getEmail())) {
                        Fire.db().collection("products").document(p.id).delete()
                                .addOnSuccessListener(x -> {
                                    Toast.makeText(getContext(),"Deleted",Toast.LENGTH_SHORT).show();
                                    // ✅ Remove from local list to prevent index errors
                                    productList.remove(pos);
                                    adapter.notifyItemRemoved(pos);
                                })
                                .addOnFailureListener(err -> {
                                    Toast.makeText(getContext(), err.getMessage(), Toast.LENGTH_LONG).show();
                                    adapter.notifyItemChanged(pos);
                                });
                    } else {
                        Toast.makeText(getContext(),"Not allowed to delete",Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(pos);
                    }
                }).addOnFailureListener(err -> {
                    adapter.notifyItemChanged(pos);
                    Toast.makeText(getContext(), err.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        };
        new ItemTouchHelper(cb).attachToRecyclerView(rv);

        return v;
    }
}
