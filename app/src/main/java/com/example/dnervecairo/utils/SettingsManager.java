package com.example.dnervecairo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {

    private static final String PREF_NAME = "dnerve_settings";

    // GPS Settings
    private static final String KEY_GPS_ACCURACY = "gps_accuracy";
    private static final String KEY_UPDATE_INTERVAL = "update_interval";

    // Notification Settings
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_TRIP_REMINDERS = "trip_reminders";
    private static final String KEY_REWARD_ALERTS = "reward_alerts";

    // Data Settings
    private static final String KEY_AUTO_SYNC = "auto_sync";
    private static final String KEY_WIFI_ONLY = "wifi_only";

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // GPS Accuracy: 0 = Battery Saver, 1 = Balanced, 2 = High Accuracy
    public int getGpsAccuracy() {
        return prefs.getInt(KEY_GPS_ACCURACY, 2); // Default: High Accuracy
    }

    public void setGpsAccuracy(int accuracy) {
        prefs.edit().putInt(KEY_GPS_ACCURACY, accuracy).apply();
    }

    // Update Interval in seconds: 5, 10, 15, 30
    public int getUpdateInterval() {
        return prefs.getInt(KEY_UPDATE_INTERVAL, 5); // Default: 5 seconds
    }

    public void setUpdateInterval(int seconds) {
        prefs.edit().putInt(KEY_UPDATE_INTERVAL, seconds).apply();
    }

    // Notifications
    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public boolean isTripRemindersEnabled() {
        return prefs.getBoolean(KEY_TRIP_REMINDERS, true);
    }

    public void setTripRemindersEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_TRIP_REMINDERS, enabled).apply();
    }

    public boolean isRewardAlertsEnabled() {
        return prefs.getBoolean(KEY_REWARD_ALERTS, true);
    }

    public void setRewardAlertsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REWARD_ALERTS, enabled).apply();
    }

    // Data Settings
    public boolean isAutoSyncEnabled() {
        return prefs.getBoolean(KEY_AUTO_SYNC, true);
    }

    public void setAutoSyncEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply();
    }

    public boolean isWifiOnlyEnabled() {
        return prefs.getBoolean(KEY_WIFI_ONLY, false);
    }

    public void setWifiOnlyEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, enabled).apply();
    }

    // Clear all settings
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}