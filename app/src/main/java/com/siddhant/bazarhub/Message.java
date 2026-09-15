package com.siddhant.bazarhub;

import com.google.firebase.Timestamp;

public class Message {
    public String id;
    public String from;
    public String text;
    public String imageUrl;
    public Timestamp sentAt;

    public Message() {}
    public Message(String id, String from, String text, String imageUrl, Timestamp sentAt) {
        this.id = id; this.from = from; this.text = text; this.imageUrl = imageUrl; this.sentAt = sentAt;
    }
}
