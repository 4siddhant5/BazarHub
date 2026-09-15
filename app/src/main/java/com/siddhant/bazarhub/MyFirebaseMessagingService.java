package com.siddhant.bazarhub;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL = "bazarhub_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();

        String title = (remoteMessage.getNotification() != null && remoteMessage.getNotification().getTitle() != null)
                ? remoteMessage.getNotification().getTitle()
                : data.get("title");

        String body = (remoteMessage.getNotification() != null && remoteMessage.getNotification().getBody() != null)
                ? remoteMessage.getNotification().getBody()
                : data.get("body");

        // Create notification channel
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "BazarHub", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
        }

        // Determine deep link target
        Intent intent;
        if (data.containsKey("chatId") && data.containsKey("userId")) {
            // Deep link → ChatActivity
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("chatId", data.get("chatId"));
            intent.putExtra("userId", data.get("userId"));

        } else if (data.containsKey("productId")) {
            // Deep link → ProductDetailsActivity
            intent = new Intent(this, ProductDetailsActivity.class);
            intent.putExtra("productId", data.get("productId"));

        } else {
            // Default → SplashActivity
            intent = new Intent(this, SplashActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Build notification
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: replace with your app icon
                .setContentTitle(title != null ? title : "BazarHub")
                .setContentText(body != null ? body : "")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        nm.notify((int) System.currentTimeMillis(), nb.build());
    }
}
