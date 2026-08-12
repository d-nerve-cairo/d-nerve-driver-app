package com.example.dnervecairo.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.dnervecairo.MainActivity;
import com.example.dnervecairo.R;
import com.example.dnervecairo.services.DNerveFirebaseMessagingService;

/**
 * Helper class for showing local notifications from within the app
 */
public class NotificationHelper {

    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    /**
     * Create notification channels for Android O+
     */
    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Trip channel
            NotificationChannel tripChannel = new NotificationChannel(
                    DNerveFirebaseMessagingService.CHANNEL_TRIP,
                    "Trip Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            tripChannel.setDescription("Notifications about your trips");
            notificationManager.createNotificationChannel(tripChannel);

            // Rewards channel
            NotificationChannel rewardsChannel = new NotificationChannel(
                    DNerveFirebaseMessagingService.CHANNEL_REWARDS,
                    "Rewards & Withdrawals",
                    NotificationManager.IMPORTANCE_HIGH
            );
            rewardsChannel.setDescription("Notifications about rewards and withdrawals");
            notificationManager.createNotificationChannel(rewardsChannel);

            // Achievements channel
            NotificationChannel achievementsChannel = new NotificationChannel(
                    DNerveFirebaseMessagingService.CHANNEL_ACHIEVEMENTS,
                    "Achievements",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            achievementsChannel.setDescription("Badge and achievement notifications");
            notificationManager.createNotificationChannel(achievementsChannel);

            // General channel
            NotificationChannel generalChannel = new NotificationChannel(
                    DNerveFirebaseMessagingService.CHANNEL_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General app notifications");
            notificationManager.createNotificationChannel(generalChannel);
        }
    }

    /**
     * Show trip complete notification
     */
    public void showTripCompleteNotification(int pointsEarned, String routeName) {
        String title = "Trip Complete! 🏁";
        String body = routeName != null 
                ? "You earned " + pointsEarned + " points on " + routeName + "!"
                : "You earned " + pointsEarned + " points. Great job!";
        
        showNotification(title, body, DNerveFirebaseMessagingService.CHANNEL_TRIP, 1001);
    }

    /**
     * Show achievement unlocked notification
     */
    public void showAchievementNotification(String badgeName, String badgeDescription) {
        String title = "Achievement Unlocked! 🏆";
        String body = "You earned the \"" + badgeName + "\" badge!";
        
        showNotification(title, body, DNerveFirebaseMessagingService.CHANNEL_ACHIEVEMENTS, 1002);
    }

    /**
     * Show withdrawal status notification
     */
    public void showWithdrawalNotification(String status, double amount) {
        String title;
        String body;
        
        switch (status.toLowerCase()) {
            case "approved":
            case "completed":
                title = "Withdrawal Approved! 💰";
                body = String.format("Your withdrawal of %.2f EGP has been processed.", amount);
                break;
            case "rejected":
                title = "Withdrawal Update";
                body = "Your withdrawal request could not be processed. Please contact support.";
                break;
            default:
                title = "Withdrawal Processing";
                body = String.format("Your withdrawal of %.2f EGP is being processed.", amount);
                break;
        }
        
        showNotification(title, body, DNerveFirebaseMessagingService.CHANNEL_REWARDS, 1003);
    }

    /**
     * Show streak reminder notification
     */
    public void showStreakReminderNotification(int currentStreak) {
        String title = "Keep Your Streak! 🔥";
        String body = "Drive today to maintain your " + currentStreak + "-day streak!";
        
        showNotification(title, body, DNerveFirebaseMessagingService.CHANNEL_GENERAL, 1004);
    }

    /**
     * Show tier upgrade notification
     */
    public void showTierUpgradeNotification(String newTier) {
        String emoji;
        switch (newTier.toLowerCase()) {
            case "silver": emoji = "🥈"; break;
            case "gold": emoji = "🥇"; break;
            case "platinum": emoji = "💎"; break;
            default: emoji = "🎉"; break;
        }
        
        String title = "Tier Upgraded! " + emoji;
        String body = "Congratulations! You're now a " + newTier + " Driver!";
        
        showNotification(title, body, DNerveFirebaseMessagingService.CHANNEL_ACHIEVEMENTS, 1005);
    }

    /**
     * Show generic notification
     */
    public void showNotification(String title, String body, String channelId, int notificationId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_directions_bus)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        notificationManager.notify(notificationId, builder.build());
    }
}
