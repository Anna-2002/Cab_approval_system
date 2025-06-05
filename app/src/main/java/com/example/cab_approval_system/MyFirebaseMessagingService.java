package com.example.cab_approval_system;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "cab_approval_notifications";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d("FCM", "Message received: " + remoteMessage);

        String title = "Cab Approval System";  // Default fallback
        String body = "You have a new notification";

        // Handle notification payload (sent using 'notification' key)
        if (remoteMessage.getNotification() != null) {
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            if (notification.getTitle() != null) {
                title = notification.getTitle();
            }
            if (notification.getBody() != null) {
                body = notification.getBody();
            }
        }

        // Handle data payload (sent using 'data' key)
        if (!remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().get("title") != null) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().get("body") != null) {
                body = remoteMessage.getData().get("body");
            }
        }

        sendNotification(title, body);
    }

    private void sendNotification(String title, String messageBody) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Cab Approval Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title != null ? title : "Notification")
                .setContentText(messageBody != null ? messageBody : "")
                .setSmallIcon(R.drawable.ic_notification) // Ensure this icon exists in res/drawable
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }
}
