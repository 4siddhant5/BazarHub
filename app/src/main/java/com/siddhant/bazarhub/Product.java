package com.siddhant.bazarhub;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude; // <- to avoid persisting helper fields
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Product {
    public String id;
    public String sellerId;
    public String sellerName;
    public String sellerPhone;

    public String title;
    public String description;
    public String category;
    public double price;
    public double discountPercent; // optional
    public String condition; // new | used | refurbished
    public int stock;
    public boolean isAvailable;

    // Location fields
    public String location;        // Human-readable location name
    public Double latitude;        // Geo latitude
    public Double longitude;       // Geo longitude

    public List<String> images; // download URLs
    public String status; // pending | approved | rejected
    public Timestamp createdAt;

    // Ratings
    public double avgRating = 0.0;
    public long ratingCount = 0;
    public Map<String, Integer> userRatings = new HashMap<>(); // userId -> rating

    // Wishlist
    public List<String> likedBy; // userIds

    // Search fields
    public String searchableTitle;
    public String searchableCategory;

    // ---- Helper (not stored in Firestore)
    @Exclude public Double _distanceKm;

    public Product() {}

    public Product(String id, String sellerId, String sellerName, String sellerPhone,
                   String title, String description, String category, double price,
                   double discountPercent, String condition, int stock, String location,
                   Double latitude, Double longitude, List<String> images, String status) {

        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.sellerPhone = sellerPhone;

        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.discountPercent = discountPercent;
        this.condition = condition;
        this.stock = stock;
        this.isAvailable = stock > 0;

        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.images = images;
        this.status = status;
        this.createdAt = Timestamp.now();

        this.searchableTitle = title != null ? title.toLowerCase() : null;
        this.searchableCategory = category != null ? category.toLowerCase() : null;
    }

    public double getDiscountedPrice() {
        return price - (price * discountPercent / 100.0);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("sellerId", sellerId);
        m.put("sellerName", sellerName);
        m.put("sellerPhone", sellerPhone);

        m.put("title", title);
        m.put("description", description);
        m.put("category", category);
        m.put("price", price);
        m.put("discountPercent", discountPercent);
        m.put("condition", condition);
        m.put("stock", stock);
        m.put("isAvailable", isAvailable);

        // Location fields
        m.put("location", location);
        m.put("latitude", latitude);
        m.put("longitude", longitude);

        m.put("images", images);
        m.put("status", status);
        m.put("createdAt", createdAt);

        m.put("avgRating", avgRating);
        m.put("ratingCount", ratingCount);
        m.put("userRatings", userRatings);

        m.put("likedBy", likedBy);

        m.put("searchableTitle", searchableTitle);
        m.put("searchableCategory", searchableCategory);

        // _distanceKm intentionally not saved
        return m;
    }
}
