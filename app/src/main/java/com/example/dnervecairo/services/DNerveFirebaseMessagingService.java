package com.example.dnervecairo.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.dnervecairo.MainActivity;
import com.example.dnervecairo.R;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.google.gson.JsonObject;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DNerveFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    
    // Notification Channels
    public static final String CHANNEL_TRIP = "trip_notifications";
    public static final String CHANNEL_REWARDS = "rewards_notifications";
    public static final String CHANNEL_ACHIEVEMENTS = "achievement_notifications";
    public static final String CHANNEL_GENERAL = "general_notifications";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM Token refreshed: " + token);

        // Save token locally
        PreferenceManager prefManager = new PreferenceManager(this);
        prefManager.saveFcmToken(token);

        // Upload to backend if driver is already logged in
        String driverId = prefManager.getDriverId();
        if (driverId != null) {
            uploadTokenToServer(driverId, token);
        }
    }

    /**
     * Fire-and-forget FCM token upload. Called on token refresh and after login.
     */
    public static void uploadTokenToServer(String driverId, String token) {
        ApiClient.getInstance().getApiService()
                .updateDriverFcmToken(driverId, token)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        Log.d("FCMService", "Token uploaded: " + response.isSuccessful());
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Log.w("FCMService", "Token upload failed (will retry on next token refresh)", t);
                    }
                });
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Notification - Title: " + title + ", Body: " + body);
            
            sendNotification(title, body, CHANNEL_GENERAL);
        }

        // Check if message contains data payload
        if (!remoteMessage.getData().isEmpty()) {
            Map<String, String> data = remoteMessage.getData();
            Log.d(TAG, "Data payload: " + data);
            
            handleDataMessage(data);
        }
    }

    /**
     * Handle data messages and show appropriate notifications
     */
    private void handleDataMessage(Map<String, String> data) {
        String type = data.get("type");
        String title = data.get("title");
        String body = data.get("body");
        
        if (type == null) {
            sendNotification(title, body, CHANNEL_GENERAL);
            return;
        }

        switch (type) {
            case "trip_complete":
                handleTripComplete(data);
                break;
            case "achievement":
                handleAchievement(data);
                break;
            case "withdrawal":
                handleWithdrawal(data);
                break;
            case "streak_reminder":
                handleStreakReminder(data);
                break;
            default:
                sendNotification(title, body, CHANNEL_GENERAL);
                break;
        }
    }

    private void handleTripComplete(Map<String, String> data) {
        String points = data.get("points");
        String title = "Trip Complete! 🏁";
        String body = "You earned " + points + " points. Great job!";
        sendNotification(title, body, CHANNEL_TRIP);
    }

    private void handleAchievement(Map<String, String> data) {
        String badgeName = data.get("badge_name");
        String title = "Achievement Unlocked! 🏆";
        String body = "You earned the \"" + badgeName + "\" badge!";
        sendNotification(title, body, CHANNEL_ACHIEVEMENTS);
    }

    private void handleWithdrawal(Map<String, String> data) {
        String status = data.get("status");
        String amount = data.get("amount");
        String title;
        String body;
        
        if ("approved".equals(status)) {
            title = "Withdrawal Approved! 💰";
            body = "Your withdrawal of " + amount + " EGP has been processed.";
        } else if ("rejected".equals(status)) {
            title = "Withdrawal Update";
            body = "Your withdrawal request could not be processed. Please contact support.";
        } else {
            title = "Withdrawal Processing";
            body = "Your withdrawal of " + amount + " EGP is being processed.";
        }
        
        sendNotification(title, body, CHANNEL_REWARDS);
    }

    private void handleStreakReminder(Map<String, String> data) {
        String streak = data.get("streak");
        String title = "Keep Your Streak! 🔥";
        String body = "Drive today to maintain your " + streak + "-day streak!";
        sendNotification(title, body, CHANNEL_GENERAL);
    }

    /**
     * Create and show a notification
     */
    private void sendNotification(String title, String messageBody, String channelId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_directions_bus)
                        .setContentTitle(title != null ? title : getString(R.string.app_name))
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels(notificationManager);
        }

        // Use unique ID for each notification
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    /**
     * Create notification channels for Android O+
     */
    private void createNotificationChannels(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Trip notifications channel
            NotificationChannel tripChannel = new NotificationChannel(
                    CHANNEL_TRIP,
                    "Trip Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            tripChannel.setDescription("Notifications about your trips");
            notificationManager.createNotificationChannel(tripChannel);

            // Rewards notifications channel
            NotificationChannel rewardsChannel = new NotificationChannel(
                    CHANNEL_REWARDS,
                    "Rewards & Withdrawals",
                    NotificationManager.IMPORTANCE_HIGH
            );
            rewardsChannel.setDescription("Notifications about rewards and withdrawals");
            notificationManager.createNotificationChannel(rewardsChannel);

            // Achievements notifications channel
            NotificationChannel achievementsChannel = new NotificationChannel(
                    CHANNEL_ACHIEVEMENTS,
                    "Achievements",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            achievementsChannel.setDescription("Badge and achievement notifications");
            notificationManager.createNotificationChannel(achievementsChannel);

            // General notifications channel
            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General app notifications");
            notificationManager.createNotificationChannel(generalChannel);
        }
    }
}
