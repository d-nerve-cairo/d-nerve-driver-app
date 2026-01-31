package com.example.dnervecairo.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dnervecairo.BuildConfig;
import com.example.dnervecairo.R;
import com.example.dnervecairo.utils.PreferenceManager;
import com.example.dnervecairo.utils.SettingsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private SettingsManager settingsManager;
    private PreferenceManager prefManager;

    private Spinner spinnerGpsAccuracy;
    private Spinner spinnerUpdateInterval;
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchTripReminders;
    private SwitchMaterial switchRewardAlerts;
    private SwitchMaterial switchAutoSync;
    private SwitchMaterial switchWifiOnly;

    private boolean isInitializing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsManager = new SettingsManager(this);
        prefManager = new PreferenceManager(this);

        initViews();
        setupToolbar();
        setupSpinners();
        setupSwitches();
        setupButtons();
        loadSettings();

        isInitializing = false;
    }

    private void initViews() {
        spinnerGpsAccuracy = findViewById(R.id.spinner_gps_accuracy);
        spinnerUpdateInterval = findViewById(R.id.spinner_update_interval);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchTripReminders = findViewById(R.id.switch_trip_reminders);
        switchRewardAlerts = findViewById(R.id.switch_reward_alerts);
        switchAutoSync = findViewById(R.id.switch_auto_sync);
        switchWifiOnly = findViewById(R.id.switch_wifi_only);

        // Set app version
        TextView tvVersion = findViewById(R.id.tv_app_version);
        tvVersion.setText(BuildConfig.VERSION_NAME);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        // GPS Accuracy options
        String[] gpsOptions = {"Battery Saver", "Balanced", "High Accuracy"};
        ArrayAdapter<String> gpsAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, gpsOptions);
        gpsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGpsAccuracy.setAdapter(gpsAdapter);

        spinnerGpsAccuracy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isInitializing) {
                    settingsManager.setGpsAccuracy(position);
                    showToast("GPS accuracy updated");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Update Interval options
        String[] intervalOptions = {"5 seconds", "10 seconds", "15 seconds", "30 seconds"};
        int[] intervalValues = {5, 10, 15, 30};
        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, intervalOptions);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUpdateInterval.setAdapter(intervalAdapter);

        spinnerUpdateInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isInitializing) {
                    settingsManager.setUpdateInterval(intervalValues[position]);
                    showToast("Update interval updated");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSwitches() {
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isInitializing) {
                settingsManager.setNotificationsEnabled(isChecked);
                switchTripReminders.setEnabled(isChecked);
                switchRewardAlerts.setEnabled(isChecked);
            }
        });

        switchTripReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isInitializing) {
                settingsManager.setTripRemindersEnabled(isChecked);
            }
        });

        switchRewardAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isInitializing) {
                settingsManager.setRewardAlertsEnabled(isChecked);
            }
        });

        switchAutoSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isInitializing) {
                settingsManager.setAutoSyncEnabled(isChecked);
                switchWifiOnly.setEnabled(isChecked);
            }
        });

        switchWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isInitializing) {
                settingsManager.setWifiOnlyEnabled(isChecked);
            }
        });
    }

    private void setupButtons() {
        // Terms of Service
        findViewById(R.id.btn_terms).setOnClickListener(v -> {
            openUrl("https://dnerve.com/terms");
        });

        // Privacy Policy
        findViewById(R.id.btn_privacy).setOnClickListener(v -> {
            openUrl("https://dnerve.com/privacy");
        });

        // Logout
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void loadSettings() {
        // GPS Settings
        spinnerGpsAccuracy.setSelection(settingsManager.getGpsAccuracy());

        int interval = settingsManager.getUpdateInterval();
        int intervalPosition = 0;
        switch (interval) {
            case 5: intervalPosition = 0; break;
            case 10: intervalPosition = 1; break;
            case 15: intervalPosition = 2; break;
            case 30: intervalPosition = 3; break;
        }
        spinnerUpdateInterval.setSelection(intervalPosition);

        // Notification Settings
        boolean notificationsEnabled = settingsManager.isNotificationsEnabled();
        switchNotifications.setChecked(notificationsEnabled);
        switchTripReminders.setChecked(settingsManager.isTripRemindersEnabled());
        switchTripReminders.setEnabled(notificationsEnabled);
        switchRewardAlerts.setChecked(settingsManager.isRewardAlertsEnabled());
        switchRewardAlerts.setEnabled(notificationsEnabled);

        // Data Settings
        boolean autoSyncEnabled = settingsManager.isAutoSyncEnabled();
        switchAutoSync.setChecked(autoSyncEnabled);
        switchWifiOnly.setChecked(settingsManager.isWifiOnlyEnabled());
        switchWifiOnly.setEnabled(autoSyncEnabled);
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            showToast("Could not open link");
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        // Clear preferences
        prefManager.clearAll();
        settingsManager.clearAll();

        // Navigate to login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}