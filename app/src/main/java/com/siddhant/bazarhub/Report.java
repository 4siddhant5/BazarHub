package com.siddhant.bazarhub;

import com.google.firebase.Timestamp;

public class Report {
    private String id;         // Firestore document ID
    private String productId;
    private String reporterId;
    private String sellerId;
    private String reason;
    private Timestamp timestamp;

    // Empty constructor required by Firestore
    public Report() {}

    // Full constructor
    public Report(String id, String productId, String reporterId, String sellerId, String reason, Timestamp timestamp) {
        this.id = id;
        this.productId = productId;
        this.reporterId = reporterId;
        this.sellerId = sellerId;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getReporterId() {
        return reporterId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getReason() {
        return reason;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    // Setters (needed if you plan to update Firestore fields dynamically)
    public void setId(String id) {
        this.id = id;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
