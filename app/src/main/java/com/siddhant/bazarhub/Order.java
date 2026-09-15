package com.siddhant.bazarhub;

public class Order {
    private String orderId;
    private String productId;
    private String buyerId;
    private String sellerId;
    private String status; // pending, shipped, delivered, cancelled
    private long timestamp;

    // ✅ Extra fields
    private int quantity;
    private double totalPrice;

    // --- Required by Firestore ---
    public Order() {
    }

    // ✅ Constructor used in CheckoutActivity (6 params, defaults for quantity & price)
    public Order(String orderId, String productId, String buyerId, String sellerId,
                 String status, long timestamp) {
        this.orderId = orderId;
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.status = status;
        this.timestamp = timestamp;
        this.quantity = 0;     // default until set
        this.totalPrice = 0.0; // default until set
    }

    // ✅ Full constructor (8 params, when you want to set everything at once)
    public Order(String orderId, String productId, String buyerId, String sellerId,
                 String status, long timestamp, int quantity, double totalPrice) {
        this.orderId = orderId;
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.status = status;
        this.timestamp = timestamp;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    // --- Getters ---
    public String getOrderId() { return orderId; }
    public String getProductId() { return productId; }
    public String getBuyerId() { return buyerId; }
    public String getSellerId() { return sellerId; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return totalPrice; }

    // --- Setters ---
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setProductId(String productId) { this.productId = productId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public void setStatus(String status) { this.status = status; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    // ✅ Helper for quick local UI update
    public void setStatusLocal(String newStatus) {
        this.status = newStatus;
    }

    // --- Convenience methods ---
    public boolean isPending() { return "pending".equalsIgnoreCase(status); }
    public boolean isShipped() { return "shipped".equalsIgnoreCase(status); }
    public boolean isDelivered() { return "delivered".equalsIgnoreCase(status); }
    public boolean isCancelled() { return "cancelled".equalsIgnoreCase(status); }

    // Useful for Cancel button check
    public boolean isCancellable() {
        return isPending(); // only pending orders can be cancelled
    }
}
