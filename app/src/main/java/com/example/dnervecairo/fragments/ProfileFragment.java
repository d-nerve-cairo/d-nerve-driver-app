package com.example.dnervecairo.fragments;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.dnervecairo.R;
import com.example.dnervecairo.activities.AchievementsActivity;
import com.example.dnervecairo.activities.DocumentsActivity;
import com.example.dnervecairo.activities.EditProfileActivity;
import com.example.dnervecairo.activities.LoginActivity;
import com.example.dnervecairo.activities.SettingsActivity;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.database.CachedDriverEntity;
import com.example.dnervecairo.utils.OfflineManager;
import com.example.dnervecairo.utils.PreferenceManager;
import com.example.dnervecairo.viewmodels.SharedDriverViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ProfileFragment extends Fragment {

    // Header views
    private TextView tvInitials, tvName, tvTier, tvMemberSince;
    private TextView tvTotalTrips, tvTotalPoints, tvAvgQuality;
    
    // Tier progress
    private ProgressBar tierProgressBar;
    private TextView tvTierProgress, tvTierNext;
    
    // Info views
    private TextView tvPhone, tvEmail, tvVehicleType, tvPlate;
    
    // Menu items
    private LinearLayout menuEditProfile, menuAchievements, menuDocuments, menuSettings;
    private MaterialButton btnLogout;

    private PreferenceManager prefManager;
    private OfflineManager offlineManager;
    private SharedDriverViewModel driverViewModel;
    private boolean returnedFromEdit = false;

    // Tier thresholds
    private static final int BRONZE_MAX = 500;
    private static final int SILVER_MAX = 2000;
    private static final int GOLD_MAX = 5000;
    private static final int PLATINUM_MAX = 10000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        prefManager = new PreferenceManager(requireContext());
        offlineManager = new OfflineManager(requireContext());
        driverViewModel = new ViewModelProvider(requireActivity()).get(SharedDriverViewModel.class);

        initViews(view);
        setupMenuItems();

        // Observe shared driver data
        driverViewModel.getDriverData().observe(getViewLifecycleOwner(), driver -> {
            if (driver != null) {
                displayDriverProfile(driver);
                offlineManager.cacheDriverData(driver);
                prefManager.saveDriverData(
                        driver.getName(),
                        driver.getTotalPoints(),
                        driver.getTier(),
                        driver.getTripsCompleted(),
                        driver.getQualityAvg(),
                        driver.getCurrentStreak()
                );
                prefManager.saveProfileData(
                        driver.getPhone(),
                        driver.getVehicleType(),
                        driver.getLicensePlate(),
                        driver.getCreatedAt()
                );
            }
        });

        driverViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                String driverId = prefManager.getDriverId();
                if (driverId != null) {
                    loadCachedData(driverId);
                } else {
                    displaySavedOrDefaultData();
                }
            }
        });

        loadProfileData();

        return view;
    }

    private void initViews(View view) {
        // Header
        tvInitials = view.findViewById(R.id.tv_initials);
        tvName = view.findViewById(R.id.tv_name);
        tvTier = view.findViewById(R.id.tv_tier);
        tvMemberSince = view.findViewById(R.id.tv_member_since);
        
        // Stats
        tvTotalTrips = view.findViewById(R.id.tv_total_trips);
        tvTotalPoints = view.findViewById(R.id.tv_total_points);
        tvAvgQuality = view.findViewById(R.id.tv_avg_quality);
        
        // Tier progress
        tierProgressBar = view.findViewById(R.id.tier_progress_bar);
        tvTierProgress = view.findViewById(R.id.tv_tier_progress);
        tvTierNext = view.findViewById(R.id.tv_tier_next);
        
        // Info
        tvPhone = view.findViewById(R.id.tv_phone);
        tvEmail = view.findViewById(R.id.tv_email);
        tvVehicleType = view.findViewById(R.id.tv_vehicle_type);
        tvPlate = view.findViewById(R.id.tv_plate);
        
        // Menu items
        menuEditProfile = view.findViewById(R.id.menu_edit_profile);
        menuAchievements = view.findViewById(R.id.menu_achievements);
        menuDocuments = view.findViewById(R.id.menu_documents);
        menuSettings = view.findViewById(R.id.menu_settings);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupMenuItems() {
        // Edit Profile
        if (menuEditProfile != null) {
            menuEditProfile.setOnClickListener(v -> {
                returnedFromEdit = true;
                startActivity(new Intent(getActivity(), EditProfileActivity.class));
            });
        }

        // Achievements
        if (menuAchievements != null) {
            menuAchievements.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), AchievementsActivity.class));
            });
        }

        // Documents
        if (menuDocuments != null) {
            menuDocuments.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), DocumentsActivity.class));
            });
        }

        // Settings
        if (menuSettings != null) {
            menuSettings.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
            });
        }

        // Logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        }
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setIcon(R.drawable.ic_logout)
                .setPositiveButton(R.string.logout_confirm, (dialog, which) -> logout())
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadProfileData() {
        if (!prefManager.isLoggedIn()) {
            displaySavedOrDefaultData();
            return;
        }

        String driverId = prefManager.getDriverId();
        if (driverId != null) {
            // Delegates to ViewModel — will skip fetch if data is fresh
            driverViewModel.loadDriverData(driverId);
        } else {
            displaySavedOrDefaultData();
        }
    }

    private void loadCachedData(String driverId) {
        offlineManager.getCachedDriver(driverId, cachedDriver -> {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (cachedDriver != null) {
                    displayCachedProfile(cachedDriver);
                } else {
                    displaySavedOrDefaultData();
                }
            });
        });
    }

    private void displayDriverProfile(DriverResponse driver) {
        // Header
        tvInitials.setText(getInitials(driver.getName()));
        tvName.setText(driver.getName());
        tvTier.setText(driver.getTier() + " Driver");
        setTierColor(driver.getTier());
        tvMemberSince.setText("Member since " + formatDate(driver.getCreatedAt()));
        
        // Stats
        tvTotalTrips.setText(String.valueOf(driver.getTripsCompleted()));
        tvTotalPoints.setText(String.valueOf(driver.getTotalPoints()));
        tvAvgQuality.setText(String.format("%.0f%%", driver.getQualityAvg() * 100));
        
        // Tier progress
        updateTierProgress(driver.getTotalPoints(), driver.getTier());
        
        // Info
        tvPhone.setText(driver.getPhone());
        tvEmail.setText("N/A");
        tvVehicleType.setText(driver.getVehicleType());
        tvPlate.setText(driver.getLicensePlate());
        
        // Animate stats
        animateStats();
    }

    private void displayCachedProfile(CachedDriverEntity driver) {
        tvInitials.setText(getInitials(driver.getName()));
        tvName.setText(driver.getName());
        tvTier.setText(driver.getTier() + " Driver");
        setTierColor(driver.getTier());
        tvMemberSince.setText("Member since (cached)");
        
        tvTotalTrips.setText(String.valueOf(driver.getTripsCompleted()));
        tvTotalPoints.setText(String.valueOf(driver.getTotalPoints()));
        tvAvgQuality.setText(String.format("%.0f%%", driver.getQualityAvg() * 100));
        
        updateTierProgress(driver.getTotalPoints(), driver.getTier());
        
        tvPhone.setText(driver.getPhone() != null ? driver.getPhone() : "N/A");
        tvEmail.setText("N/A");
        tvVehicleType.setText(driver.getVehicleType() != null ? driver.getVehicleType() : "Microbus");
        tvPlate.setText(driver.getLicensePlate() != null ? driver.getLicensePlate() : "N/A");
        
        animateStats();
    }

    private void displaySavedOrDefaultData() {
        if (prefManager.hasDriverData()) {
            String name = prefManager.getDriverName();
            String tier = prefManager.getDriverTier();
            int points = prefManager.getDriverPoints();
            
            tvInitials.setText(getInitials(name));
            tvName.setText(name);
            tvTier.setText(tier + " Driver");
            setTierColor(tier);
            tvMemberSince.setText("Member since " + prefManager.getMemberSince());
            
            tvTotalTrips.setText(String.valueOf(prefManager.getDriverTrips()));
            tvTotalPoints.setText(String.valueOf(points));
            tvAvgQuality.setText(String.format("%.0f%%", prefManager.getDriverQuality()));
            
            updateTierProgress(points, tier);
            
            tvPhone.setText(prefManager.getDriverPhone());
            tvEmail.setText("N/A");
            tvVehicleType.setText(prefManager.getDriverVehicleType());
            tvPlate.setText(prefManager.getDriverPlate());
        } else {
            tvInitials.setText("?");
            tvName.setText("Driver");
            tvTier.setText("Bronze Driver");
            tvMemberSince.setText("Not available");
            
            tvTotalTrips.setText("0");
            tvTotalPoints.setText("0");
            tvAvgQuality.setText("0%");
            
            updateTierProgress(0, "Bronze");
            
            tvPhone.setText("N/A");
            tvEmail.setText("N/A");
            tvVehicleType.setText("N/A");
            tvPlate.setText("N/A");
        }
        
        animateStats();
    }

    private void updateTierProgress(int points, String currentTier) {
        int tierMin, tierMax;
        String nextTier;
        int tierColor;

        switch (currentTier.toLowerCase()) {
            case "diamond":
                tierMin = PLATINUM_MAX;
                tierMax = PLATINUM_MAX;
                nextTier = null;
                tierColor = R.color.tier_diamond;
                break;
            case "platinum":
                tierMin = GOLD_MAX;
                tierMax = PLATINUM_MAX;
                nextTier = "Diamond";
                tierColor = R.color.tier_platinum;
                break;
            case "gold":
                tierMin = SILVER_MAX;
                tierMax = GOLD_MAX;
                nextTier = "Platinum";
                tierColor = R.color.tier_gold;
                break;
            case "silver":
                tierMin = BRONZE_MAX;
                tierMax = SILVER_MAX;
                nextTier = "Gold";
                tierColor = R.color.tier_silver;
                break;
            default: // Bronze
                tierMin = 0;
                tierMax = BRONZE_MAX;
                nextTier = "Silver";
                tierColor = R.color.tier_bronze;
                break;
        }

        // Calculate progress
        int progressPoints = points - tierMin;
        int tierRange = tierMax - tierMin;
        int progressPercent = tierRange > 0 ? (progressPoints * 100) / tierRange : 100;
        progressPercent = Math.min(100, Math.max(0, progressPercent));

        // Set progress bar
        if (tierProgressBar != null) {
            tierProgressBar.setMax(100);
            animateProgressBar(progressPercent);
            tierProgressBar.setProgressTintList(ContextCompat.getColorStateList(requireContext(), tierColor));
        }

        // Set progress text
        if (tvTierProgress != null) {
            tvTierProgress.setText(points + " / " + tierMax + " pts");
        }

        // Set next tier text
        if (tvTierNext != null) {
            if (nextTier != null) {
                int remaining = tierMax - points;
                tvTierNext.setText(remaining + " pts to " + nextTier + " " + getTierEmoji(nextTier));
                tvTierNext.setVisibility(View.VISIBLE);
            } else {
                tvTierNext.setText("🏆 Maximum tier reached!");
                tvTierNext.setVisibility(View.VISIBLE);
            }
        }
    }

    private void animateProgressBar(int targetProgress) {
        if (tierProgressBar == null) return;
        
        ObjectAnimator animator = ObjectAnimator.ofInt(tierProgressBar, "progress", 0, targetProgress);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private void animateStats() {
        View[] views = {tvTotalTrips, tvTotalPoints, tvAvgQuality};
        
        for (int i = 0; i < views.length; i++) {
            View v = views[i];
            if (v != null) {
                v.setAlpha(0f);
                v.setTranslationY(20f);
                v.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(i * 100L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            }
        }
    }

    private void setTierColor(String tier) {
        if (tvTier == null) return;
        
        int colorRes;
        switch (tier.toLowerCase()) {
            case "diamond":
                colorRes = R.color.tier_diamond;
                break;
            case "platinum":
                colorRes = R.color.tier_platinum;
                break;
            case "gold":
                colorRes = R.color.tier_gold;
                break;
            case "silver":
                colorRes = R.color.tier_silver;
                break;
            default:
                colorRes = R.color.tier_bronze;
                break;
        }
        
        // For visibility on gradient, we'll keep white but could add a badge background
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        } else if (parts.length == 1 && parts[0].length() >= 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return "?";
    }

    private String getTierEmoji(String tier) {
        if (tier == null) return "🥉";
        switch (tier.toLowerCase()) {
            case "diamond": return "💎";
            case "platinum": return "💠";
            case "gold": return "🥇";
            case "silver": return "🥈";
            default: return "🥉";
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null) return "N/A";
        try {
            return dateStr.substring(0, 10);
        } catch (Exception e) {
            return "N/A";
        }
    }

    private void logout() {
        prefManager.clear();
        Toast.makeText(getContext(), R.string.logout_success, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Only force-refresh if returning from EditProfile
        if (returnedFromEdit) {
            returnedFromEdit = false;
            String driverId = prefManager.getDriverId();
            if (driverId != null) {
                driverViewModel.forceRefresh(driverId);
            }
        }
    }
}
