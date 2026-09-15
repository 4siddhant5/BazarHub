package com.siddhant.bazarhub;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class AppUser {
    public String uid;
    public String name;
    public String email;
    public String role; // "admin" | "user"
    public Timestamp createdAt;
    public boolean blocked; // new field

    public AppUser() {
        // Firestore will auto-handle missing "blocked" → defaults to false
        this.blocked = false;
    }

    public AppUser(String uid, String name, String email, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = Timestamp.now();
        this.blocked = false; // default
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("uid", uid);
        m.put("name", name);
        m.put("email", email);
        m.put("role", role);
        m.put("createdAt", createdAt);
        m.put("blocked", blocked);
        return m;
    }
}
