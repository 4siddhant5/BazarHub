package com.siddhant.bazarhub;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public class Fire {
    private static FirebaseAuth auth;
    private static FirebaseFirestore db;
    private static FirebaseStorage storage;

    public static FirebaseAuth auth() {
        if (auth == null) auth = FirebaseAuth.getInstance();
        return auth;
    }

    public static FirebaseFirestore db() {
        if (db == null) db = FirebaseFirestore.getInstance();
        return db;
    }

    public static FirebaseStorage storage() {
        if (storage == null) storage = FirebaseStorage.getInstance();
        return storage;
    }
}
