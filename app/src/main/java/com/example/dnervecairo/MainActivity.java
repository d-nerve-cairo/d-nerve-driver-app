package com.example.dnervecairo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.dnervecairo.fragments.HomeFragment;
import com.example.dnervecairo.fragments.LeaderboardFragment;
import com.example.dnervecairo.fragments.RewardsFragment;
import com.example.dnervecairo.fragments.ProfileFragment;
import com.example.dnervecairo.utils.SettingsManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved language
        applyLanguage();

        // Apply saved theme
        SettingsManager settingsManager = new SettingsManager(this);
        AppCompatDelegate.setDefaultNightMode(
                settingsManager.isDarkModeEnabled()
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Handle navigation clicks
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_leaderboard) {
                fragment = new LeaderboardFragment();
            } else if (itemId == R.id.nav_rewards) {
                fragment = new RewardsFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            return loadFragment(fragment);
        });
    }

    private void applyLanguage() {
        SettingsManager settingsManager = new SettingsManager(this);
        String languageCode = settingsManager.getLanguage();
        java.util.Locale locale = new java.util.Locale(languageCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}