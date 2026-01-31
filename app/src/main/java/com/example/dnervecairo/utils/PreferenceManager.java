package com.example.dnervecairo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {

    private static final String PREF_NAME = "DNervePrefs";
    private static final String KEY_DRIVER_ID = "driver_id";
    private static final String KEY_DRIVER_NAME = "driver_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Driver ID (String)
    public void saveDriverId(String id) {
        prefs.edit().putString(KEY_DRIVER_ID, id).apply();
    }

    public String getDriverId() {
        return prefs.getString(KEY_DRIVER_ID, null);
    }

    // Driver Name
    public void saveDriverName(String name) {
        prefs.edit().putString(KEY_DRIVER_NAME, name).apply();
    }

    public String getDriverName() {
        return prefs.getString(KEY_DRIVER_NAME, "Driver");
    }

    // Login State
    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Test Driver (for API testing)
    public void setTestDriver() {
        saveDriverId("driver_20260124201814");
        saveDriverName("Test Driver");
        setLoggedIn(true);
    }

    // Logout
    public void clear() {
        prefs.edit().clear().apply();
    }
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}