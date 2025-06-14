package com.example.cab_approval_system;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Simplified FCM solution using Firebase Database triggers instead of direct FCM API calls
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "cab_approval_notifications";
    private Context context;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    /**
     * Save notification to Firebase Database, which will trigger cloud functions
     * to send the actual FCM message
     */
    public void sendNotificationThroughDatabase(int requestId, String approverEmail, String requesterEmail) {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app");
        DatabaseReference notificationsRef = database.getReference("Notifications");

        // For requester
        Map<String, Object> requesterNotification = new HashMap<>();
        requesterNotification.put("recipient", requesterEmail);
        requesterNotification.put("title", "Ride Requested");
        requesterNotification.put("message", "Your ride request with ID " + requestId + " has been submitted.");
        requesterNotification.put("timestamp", System.currentTimeMillis());
        requesterNotification.put("status", "pending");
        requesterNotification.put("requestId", requestId);

        // For approver
        Map<String, Object> approverNotification = new HashMap<>();
        approverNotification.put("recipient", approverEmail);
        approverNotification.put("title", "Request Approval Pending");
        approverNotification.put("message", "New ride request (ID: " + requestId + ") is pending your approval.");
        approverNotification.put("timestamp", System.currentTimeMillis());
        approverNotification.put("status", "pending");
        approverNotification.put("requestId", requestId);

        // Save both notifications to Firebase
        String requesterNotificationId = notificationsRef.push().getKey();
        String approverNotificationId = notificationsRef.push().getKey();

        if (requesterNotificationId != null && approverNotificationId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("/" + requesterNotificationId, requesterNotification);
            updates.put("/" + approverNotificationId, approverNotification);

            notificationsRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Notifications saved to database successfully");
                        // Show local notification as backup
                        showLocalNotification("Ride Requested",
                                "Your ride request with ID " + requestId + " has been submitted.");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving notifications to database", e);
                    });
        }
    }

    /**
     * Create notification channel for Android O and above
     */
    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Cab Approval Notifications";
            String description = "Notifications for cab approval requests";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Show a local notification as backup
     */
    public void showLocalNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Permission not granted for showing notifications", e);
        }
    }

    /**
     * Update FCM token in Firebase database for current user
     */
    public static void updateFCMToken(String email) {
        if (email == null || email.isEmpty()) {
            Log.e(TAG, "Cannot update FCM token: email is null or empty");
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();

                    // Save token to Firebase for this user
                    DatabaseReference usersRef = FirebaseDatabase.getInstance("https://cab-approval-system-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .getReference("Registration_data");

                    usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                snapshot.getRef().child("fcm_token").setValue(token)
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated successfully"))
                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM token", e));
                                break; // Update only the first match
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "Error updating FCM token: " + databaseError.getMessage());
                        }
                    });
                });
    }
}