package com.siddhant.bazarhub;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String productId;
    private String productName;
    private String productImage;
    private double price;
    private int quantity;
    private String sellerId;

    public CartItem() {
        // Required empty constructor for Firebase
    }

    public CartItem(String productId, String productName, String productImage, double price, int quantity, String sellerId) {
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.price = price;
        this.quantity = quantity;
        this.sellerId = sellerId;
    }

    // Getters & Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
}
