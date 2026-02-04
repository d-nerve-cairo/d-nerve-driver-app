package com.example.dnervecairo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {

    private static final String PREF_NAME = "DNervePrefs";
    private static final String KEY_DRIVER_ID = "driver_id";
    private static final String KEY_DRIVER_NAME = "driver_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_DRIVER_POINTS = "driver_points";
    private static final String KEY_DRIVER_TIER = "driver_tier";
    private static final String KEY_DRIVER_TRIPS = "driver_trips";
    private static final String KEY_DRIVER_QUALITY = "driver_quality";
    private static final String KEY_DRIVER_STREAK = "driver_streak";
    private static final String KEY_DRIVER_DATA_TIMESTAMP = "driver_data_timestamp";
    private static final String KEY_DRIVER_PHONE = "driver_phone";
    private static final String KEY_DRIVER_VEHICLE_TYPE = "driver_vehicle_type";
    private static final String KEY_DRIVER_PLATE = "driver_plate";
    private static final String KEY_MEMBER_SINCE = "member_since";

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ========== AUTHENTICATION ==========

    public void saveDriverId(String id) {
        prefs.edit().putString(KEY_DRIVER_ID, id).apply();
    }

    public String getDriverId() {
        return prefs.getString(KEY_DRIVER_ID, null);
    }

    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // ========== DRIVER DATA (for offline display) ==========

    public void saveDriverData(String name, int totalPoints, String tier,
                               int tripsCompleted, double qualityAvg, int streak) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_DRIVER_NAME, name);
        editor.putInt(KEY_DRIVER_POINTS, totalPoints);
        editor.putString(KEY_DRIVER_TIER, tier);
        editor.putInt(KEY_DRIVER_TRIPS, tripsCompleted);
        editor.putFloat(KEY_DRIVER_QUALITY, (float) qualityAvg);
        editor.putInt(KEY_DRIVER_STREAK, streak);
        editor.putLong(KEY_DRIVER_DATA_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
    }

    public void saveProfileData(String phone, String vehicleType, String licensePlate, String memberSince) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_DRIVER_PHONE, phone);
        editor.putString(KEY_DRIVER_VEHICLE_TYPE, vehicleType);
        editor.putString(KEY_DRIVER_PLATE, licensePlate);
        editor.putString(KEY_MEMBER_SINCE, memberSince);
        editor.apply();
    }

    public String getDriverName() {
        return prefs.getString(KEY_DRIVER_NAME, "Driver");
    }

    public void saveDriverName(String name) {
        prefs.edit().putString(KEY_DRIVER_NAME, name).apply();
    }

    public int getDriverPoints() {
        return prefs.getInt(KEY_DRIVER_POINTS, 0);
    }

    public String getDriverTier() {
        return prefs.getString(KEY_DRIVER_TIER, "Bronze");
    }

    public int getDriverTrips() {
        return prefs.getInt(KEY_DRIVER_TRIPS, 0);
    }

    public float getDriverQuality() {
        return prefs.getFloat(KEY_DRIVER_QUALITY, 0f);
    }

    public int getDriverStreak() {
        return prefs.getInt(KEY_DRIVER_STREAK, 0);
    }

    public String getDriverPhone() {
        return prefs.getString(KEY_DRIVER_PHONE, "N/A");
    }

    public String getDriverVehicleType() {
        return prefs.getString(KEY_DRIVER_VEHICLE_TYPE, "Microbus");
    }

    public String getDriverPlate() {
        return prefs.getString(KEY_DRIVER_PLATE, "N/A");
    }

    public String getMemberSince() {
        String date = prefs.getString(KEY_MEMBER_SINCE, null);
        if (date == null) return "N/A";
        try {
            return date.substring(0, 10);
        } catch (Exception e) {
            return "N/A";
        }
    }

    public long getDriverDataTimestamp() {
        return prefs.getLong(KEY_DRIVER_DATA_TIMESTAMP, 0);
    }

    public boolean hasDriverData() {
        return prefs.contains(KEY_DRIVER_NAME) && prefs.getString(KEY_DRIVER_NAME, null) != null;
    }

    // ========== TEST & UTILITY ==========

    public void setTestDriver() {
        saveDriverId("driver_20260124201814");
        saveDriverName("Test Driver");
        setLoggedIn(true);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}