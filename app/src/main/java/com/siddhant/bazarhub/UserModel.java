package com.siddhant.bazarhub;

public class UserModel {
    private String id;
    private String name;
    private String email;
    private String role;
    private boolean isBlocked;

    public UserModel() {} // Firestore needs empty constructor

    public UserModel(String id, String name, String email, String role, boolean isBlocked) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.isBlocked = isBlocked;
    }

    // Getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
}
