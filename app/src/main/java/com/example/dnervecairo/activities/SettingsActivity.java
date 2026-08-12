package com.example.dnervecairo.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.dnervecairo.BuildConfig;
import com.example.dnervecairo.R;
import com.example.dnervecairo.utils.PreferenceManager;
import com.example.dnervecairo.utils.SettingsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private SettingsManager settingsManager;
    private PreferenceManager prefManager;

    // GPS Settings
    private Spinner spinnerGpsAccuracy;
    private Spinner spinnerUpdateInterval;

    // Notification Settings
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchTripReminders;
    private SwitchMaterial switchRewardAlerts;

    // Data Settings
    private SwitchMaterial switchAutoSync;
    private SwitchMaterial switchWifiOnly;

    // Live Mode
    private SwitchMaterial switchLiveMode;

    // Appearance Settings
    private SwitchMaterial switchDarkMode;
    private Spinner spinnerLanguage;

    // Profile Header
    private TextView tvDriverName;
    private TextView tvDriverId;
    private ImageView ivProfileAvatar;

    // Cards for animation
    private MaterialCardView cardProfile;
    private MaterialCardView cardGps;
    private MaterialCardView cardNotifications;
    private MaterialCardView cardData;
    private MaterialCardView cardAppearance;
    private MaterialCardView cardAbout;
    private MaterialButton btnLogout;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isInitializing = true;
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved language BEFORE setContentView
        settingsManager = new SettingsManager(this);
        applyLanguage(settingsManager.getLanguage());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefManager = new PreferenceManager(this);

        initViews();
        setupToolbar();
        setupProfileHeader();
        setupSpinners();
        setupSwitches();
        setupLanguageSpinner();
        setupButtons();
        loadSettings();

        if (isFirstLoad) {
            prepareEntranceAnimations();
            playEntranceAnimations();
        }

        isInitializing = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void initViews() {
        // GPS
        spinnerGpsAccuracy = findViewById(R.id.spinner_gps_accuracy);
        spinnerUpdateInterval = findViewById(R.id.spinner_update_interval);

        // Notifications
        switchNotifications = findViewById(R.id.switch_notifications);
        switchTripReminders = findViewById(R.id.switch_trip_reminders);
        switchRewardAlerts = findViewById(R.id.switch_reward_alerts);

        // Data
        switchAutoSync = findViewById(R.id.switch_auto_sync);
        switchWifiOnly = findViewById(R.id.switch_wifi_only);

        // Appearance
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        spinnerLanguage = findViewById(R.id.spinner_language);
        switchLiveMode = findViewById(R.id.switch_live_mode);

        // Profile Header
        tvDriverName = findViewById(R.id.tv_driver_name);
        tvDriverId = findViewById(R.id.tv_driver_id);
        ivProfileAvatar = findViewById(R.id.iv_profile_avatar);

        // Cards
        cardProfile = findViewById(R.id.card_profile);
        cardGps = findViewById(R.id.card_gps);
        cardNotifications = findViewById(R.id.card_notifications);
        cardData = findViewById(R.id.card_data);
        cardAppearance = findViewById(R.id.card_appearance);
        cardAbout = findViewById(R.id.card_about);
        btnLogout = findViewById(R.id.btn_logout);

        // Set app version
        TextView tvVersion = findViewById(R.id.tv_app_version);
        if (tvVersion != null) {
            tvVersion.setText(BuildConfig.VERSION_NAME);
        }
    }

    private void prepareEntranceAnimations() {
        View[] cards = {cardProfile, cardGps, cardNotifications, cardData, cardAppearance, cardAbout, btnLogout};
        for (View card : cards) {
            if (card != null) {
                card.setAlpha(0f);
                card.setTranslationY(30f);
            }
        }
    }

    private void playEntranceAnimations() {
        if (!isFirstLoad) return;
        isFirstLoad = false;

        View[] cards = {cardProfile, cardGps, cardNotifications, cardData, cardAppearance, cardAbout, btnLogout};

        for (int i = 0; i < cards.length; i++) {
            View card = cards[i];
            if (card != null) {
                final int delay = i * 80;
                handler.postDelayed(() -> {
                    card.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(350)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                }, delay);
            }
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            animateClick(v);
            handler.postDelayed(this::finish, 100);
        });
    }

    private void setupProfileHeader() {
        if (tvDriverName != null) {
            String name = prefManager.getDriverName();
            tvDriverName.setText(name != null && !name.isEmpty() ? name : "Driver");
        }

        if (tvDriverId != null) {
            String driverId = prefManager.getDriverId();
            if (driverId != null && driverId.length() > 10) {
                // Show last 8 characters
                tvDriverId.setText("ID: ..." + driverId.substring(driverId.length() - 8));
            } else {
                tvDriverId.setText("ID: " + (driverId != null ? driverId : "Unknown"));
            }
        }

        // Profile card click - direct user to bottom navigation
        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> {
                animateClick(v);
                showToast("View your profile in the Profile tab");
            });
        }
    }

    private void setupSpinners() {
        // GPS Accuracy options
        String[] gpsOptions = {
                "Battery Saver",
                "Balanced",
                "High Accuracy"
        };
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
        String[] intervalOptions = {
                "5 seconds",
                "10 seconds",
                "15 seconds",
                "30 seconds"
        };
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

                // Visual feedback
                updateSwitchAlpha(switchTripReminders, isChecked);
                updateSwitchAlpha(switchRewardAlerts, isChecked);
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
                updateSwitchAlpha(switchWifiOnly, isChecked);
            }
        });

        switchWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isInitializing) {
                settingsManager.setWifiOnlyEnabled(isChecked);
            }
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isInitializing) {
                settingsManager.setDarkModeEnabled(isChecked);
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                );
            }
        });

        if (switchLiveMode != null) {
            switchLiveMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isInitializing) {
                    prefManager.setLiveModeEnabled(isChecked);
                    showToast(isChecked
                            ? "Live Mode on — commuters can see your trip"
                            : "Live Mode off — batch mode active");
                }
            });
        }
    }

    private void updateSwitchAlpha(View view, boolean enabled) {
        if (view != null) {
            view.animate()
                    .alpha(enabled ? 1f : 0.5f)
                    .setDuration(200)
                    .start();
        }
    }

    private void setupLanguageSpinner() {
        String[] languages = {"English", "العربية"};
        String[] languageCodes = {"en", "ar"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        // Set current selection
        String currentLang = settingsManager.getLanguage();
        spinnerLanguage.setSelection(currentLang.equals("ar") ? 1 : 0);

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isInitializing) {
                    String selectedLang = languageCodes[position];
                    if (!selectedLang.equals(settingsManager.getLanguage())) {
                        settingsManager.setLanguage(selectedLang);
                        applyLanguage(selectedLang);
                        recreate();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupButtons() {
        // Terms of Service
        View btnTerms = findViewById(R.id.btn_terms);
        if (btnTerms != null) {
            btnTerms.setOnClickListener(v -> {
                animateClick(v);
                handler.postDelayed(() -> openUrl("https://dnerve.com/terms"), 100);
            });
        }

        // Privacy Policy
        View btnPrivacy = findViewById(R.id.btn_privacy);
        if (btnPrivacy != null) {
            btnPrivacy.setOnClickListener(v -> {
                animateClick(v);
                handler.postDelayed(() -> openUrl("https://dnerve.com/privacy"), 100);
            });
        }

        // Logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                animateClick(v);
                handler.postDelayed(this::showLogoutDialog, 100);
            });
        }
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
        switchTripReminders.setAlpha(notificationsEnabled ? 1f : 0.5f);
        switchRewardAlerts.setChecked(settingsManager.isRewardAlertsEnabled());
        switchRewardAlerts.setEnabled(notificationsEnabled);
        switchRewardAlerts.setAlpha(notificationsEnabled ? 1f : 0.5f);

        // Data Settings
        boolean autoSyncEnabled = settingsManager.isAutoSyncEnabled();
        switchAutoSync.setChecked(autoSyncEnabled);
        switchWifiOnly.setChecked(settingsManager.isWifiOnlyEnabled());
        switchWifiOnly.setEnabled(autoSyncEnabled);
        switchWifiOnly.setAlpha(autoSyncEnabled ? 1f : 0.5f);

        // Dark Mode Settings
        switchDarkMode.setChecked(settingsManager.isDarkModeEnabled());

        // Live Mode
        if (switchLiveMode != null) {
            switchLiveMode.setChecked(prefManager.isLiveModeEnabled());
        }
    }

    private void animateClick(View v) {
        if (v == null) return;
        v.animate()
                .scaleX(0.97f).scaleY(0.97f)
                .setDuration(50)
                .withEndAction(() ->
                        v.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(50)
                                .start()
                ).start();
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
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm_message)
                .setIcon(R.drawable.ic_logout)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void performLogout() {
        // Show loading briefly
        showToast(getString(R.string.logging_out));

        handler.postDelayed(() -> {
            // Clear preferences
            prefManager.clearAll();
            settingsManager.clearAll();

            // Navigate to login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 500);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void applyLanguage(String languageCode) {
        java.util.Locale locale = new java.util.Locale(languageCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}