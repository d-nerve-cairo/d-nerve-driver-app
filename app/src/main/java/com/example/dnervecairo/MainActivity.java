package com.example.dnervecairo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.dnervecairo.fragments.HomeFragment;
import com.example.dnervecairo.fragments.LeaderboardFragment;
import com.example.dnervecairo.fragments.RewardsFragment;
import com.example.dnervecairo.fragments.ProfileFragment;
import com.example.dnervecairo.utils.SettingsManager;

/**
 * Main activity with cached fragment switching.
 * Uses hide/show instead of replace to keep fragments alive across tab switches.
 * This eliminates redundant API calls and preserves scroll position & state.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    // Cached fragment instances — created once, reused forever
    private Fragment homeFragment;
    private Fragment leaderboardFragment;
    private Fragment rewardsFragment;
    private Fragment profileFragment;
    private Fragment activeFragment;

    private static final String TAG_HOME = "frag_home";
    private static final String TAG_LEADERBOARD = "frag_leaderboard";
    private static final String TAG_REWARDS = "frag_rewards";
    private static final String TAG_PROFILE = "frag_profile";

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

        bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            setupFragments();
        } else {
            // Restore fragment references after config change
            restoreFragments();
        }

        // Handle navigation clicks
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                switchFragment(homeFragment);
            } else if (itemId == R.id.nav_leaderboard) {
                switchFragment(leaderboardFragment);
            } else if (itemId == R.id.nav_rewards) {
                switchFragment(rewardsFragment);
            } else if (itemId == R.id.nav_profile) {
                switchFragment(profileFragment);
            }

            return true;
        });
    }

    /**
     * Create all fragments once and add them to the container.
     * Only the home fragment is shown initially; others are hidden.
     */
    private void setupFragments() {
        FragmentManager fm = getSupportFragmentManager();

        homeFragment = new HomeFragment();
        leaderboardFragment = new LeaderboardFragment();
        rewardsFragment = new RewardsFragment();
        profileFragment = new ProfileFragment();

        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.fragment_container, profileFragment, TAG_PROFILE).hide(profileFragment);
        ft.add(R.id.fragment_container, rewardsFragment, TAG_REWARDS).hide(rewardsFragment);
        ft.add(R.id.fragment_container, leaderboardFragment, TAG_LEADERBOARD).hide(leaderboardFragment);
        ft.add(R.id.fragment_container, homeFragment, TAG_HOME); // Visible by default
        ft.commit();

        activeFragment = homeFragment;
    }

    /**
     * Restore cached fragment references after a configuration change (rotation, theme switch).
     */
    private void restoreFragments() {
        FragmentManager fm = getSupportFragmentManager();

        homeFragment = fm.findFragmentByTag(TAG_HOME);
        leaderboardFragment = fm.findFragmentByTag(TAG_LEADERBOARD);
        rewardsFragment = fm.findFragmentByTag(TAG_REWARDS);
        profileFragment = fm.findFragmentByTag(TAG_PROFILE);

        // Determine which fragment is currently visible
        if (homeFragment != null && homeFragment.isVisible()) {
            activeFragment = homeFragment;
        } else if (leaderboardFragment != null && leaderboardFragment.isVisible()) {
            activeFragment = leaderboardFragment;
        } else if (rewardsFragment != null && rewardsFragment.isVisible()) {
            activeFragment = rewardsFragment;
        } else if (profileFragment != null && profileFragment.isVisible()) {
            activeFragment = profileFragment;
        } else {
            // Fallback: show home
            activeFragment = homeFragment;
            if (homeFragment != null) {
                getSupportFragmentManager().beginTransaction().show(homeFragment).commit();
            }
        }
    }

    /**
     * Switch to a target fragment using hide/show.
     * No fragment is destroyed — state is preserved.
     */
    private void switchFragment(Fragment target) {
        if (target == null || target == activeFragment) return;

        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();

        activeFragment = target;
    }

    private void applyLanguage() {
        SettingsManager settingsManager = new SettingsManager(this);
        String languageCode = settingsManager.getLanguage();
        java.util.Locale locale = java.util.Locale.forLanguageTag(languageCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    /**
     * Switch to a specific tab in the bottom navigation.
     * Called from fragments to navigate between tabs.
     * @param tabId The menu item ID (e.g., R.id.nav_rewards)
     */
    public void switchToTab(int tabId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(tabId);
        }
    }
}
