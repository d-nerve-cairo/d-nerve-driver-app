package com.example.dnervecairo.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dnervecairo.R;
import com.example.dnervecairo.activities.AchievementsActivity;
import com.example.dnervecairo.activities.LoginActivity;
import com.example.dnervecairo.activities.SettingsActivity;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.database.CachedDriverEntity;
import com.example.dnervecairo.utils.NetworkUtils;
import com.example.dnervecairo.utils.OfflineManager;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.example.dnervecairo.activities.EditProfileActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvTier, tvMemberSince, tvTotalTrips, tvTotalPoints, tvAvgQuality;
    private TextView tvPhone, tvEmail, tvVehicleType, tvPlate;
    private PreferenceManager prefManager;
    private OfflineManager offlineManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        prefManager = new PreferenceManager(requireContext());
        offlineManager = new OfflineManager(requireContext());

        initViews(view);
        setupButtons(view);
        loadProfileData();

        return view;
    }

    private void initViews(View view) {
        tvName = view.findViewById(R.id.tv_name);
        tvTier = view.findViewById(R.id.tv_tier);
        tvMemberSince = view.findViewById(R.id.tv_member_since);
        tvTotalTrips = view.findViewById(R.id.tv_total_trips);
        tvTotalPoints = view.findViewById(R.id.tv_total_points);
        tvAvgQuality = view.findViewById(R.id.tv_avg_quality);
        tvPhone = view.findViewById(R.id.tv_phone);
        tvEmail = view.findViewById(R.id.tv_email);
        tvVehicleType = view.findViewById(R.id.tv_vehicle_type);
        tvPlate = view.findViewById(R.id.tv_plate);
    }

    private void setupButtons(View view) {
        MaterialButton btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        MaterialButton btnAchievements = view.findViewById(R.id.btn_achievements);
        MaterialButton btnSettings = view.findViewById(R.id.btn_settings);
        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);

        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditProfileActivity.class));
        });

        btnAchievements.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AchievementsActivity.class));
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            logout();
        });
    }

    private void loadProfileData() {
        if (!prefManager.isLoggedIn()) {
            displaySavedOrDefaultData();
            return;
        }

        String driverId = prefManager.getDriverId();

        // Check if online
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            // Online - fetch from API
            ApiClient.getInstance().getApiService()
                    .getDriver(driverId)
                    .enqueue(new Callback<DriverResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<DriverResponse> call, @NonNull Response<DriverResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                DriverResponse driver = response.body();
                                displayDriverProfile(driver);
                                // Cache for offline use
                                offlineManager.cacheDriverData(driver);
                                // Save to SharedPreferences
                                prefManager.saveDriverData(
                                        driver.getName(),
                                        driver.getTotalPoints(),
                                        driver.getTier(),
                                        driver.getTripsCompleted(),
                                        driver.getQualityAvg(),
                                        driver.getCurrentStreak()
                                );
                                // Save additional profile data
                                prefManager.saveProfileData(
                                        driver.getPhone(),
                                        driver.getVehicleType(),
                                        driver.getLicensePlate(),
                                        driver.getCreatedAt()
                                );
                            } else {
                                loadCachedData(driverId);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<DriverResponse> call, @NonNull Throwable t) {
                            loadCachedData(driverId);
                        }
                    });
        } else {
            // Offline - load from cache
            loadCachedData(driverId);
        }
    }

    private void loadCachedData(String driverId) {
        // First try Room database
        offlineManager.getCachedDriver(driverId, cachedDriver -> {
            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                if (cachedDriver != null) {
                    displayCachedProfile(cachedDriver);
                } else {
                    // Fallback to SharedPreferences
                    displaySavedOrDefaultData();
                }
            });
        });
    }

    private void displayDriverProfile(DriverResponse driver) {
        tvName.setText(driver.getName());
        tvTier.setText(getTierEmoji(driver.getTier()) + " " + driver.getTier() + " Driver");
        tvMemberSince.setText("Member since " + formatDate(driver.getCreatedAt()));
        tvTotalTrips.setText(String.valueOf(driver.getTripsCompleted()));
        tvTotalPoints.setText(String.valueOf(driver.getTotalPoints()));
        tvAvgQuality.setText(String.format("%.0f%%", driver.getQualityAvg()));
        tvPhone.setText(driver.getPhone());
        tvEmail.setText("N/A");
        tvVehicleType.setText(driver.getVehicleType());
        tvPlate.setText(driver.getLicensePlate());
    }

    private void displayCachedProfile(CachedDriverEntity driver) {
        tvName.setText(driver.getName());
        tvTier.setText(getTierEmoji(driver.getTier()) + " " + driver.getTier() + " Driver");
        tvMemberSince.setText("Member since (cached)");
        tvTotalTrips.setText(String.valueOf(driver.getTripsCompleted()));
        tvTotalPoints.setText(String.valueOf(driver.getTotalPoints()));
        tvAvgQuality.setText(String.format("%.0f%%", driver.getQualityAvg() * 100));
        tvPhone.setText(driver.getPhone() != null ? driver.getPhone() : "N/A");
        tvEmail.setText("N/A");
        tvVehicleType.setText(driver.getVehicleType() != null ? driver.getVehicleType() : "Microbus");
        tvPlate.setText(driver.getLicensePlate() != null ? driver.getLicensePlate() : "N/A");
    }

    private void displaySavedOrDefaultData() {
        if (prefManager.hasDriverData()) {
            // Show last known data from SharedPreferences
            tvName.setText(prefManager.getDriverName());
            tvTier.setText(getTierEmoji(prefManager.getDriverTier()) + " " + prefManager.getDriverTier() + " Driver");
            tvMemberSince.setText("Member since " + prefManager.getMemberSince());
            tvTotalTrips.setText(String.valueOf(prefManager.getDriverTrips()));
            tvTotalPoints.setText(String.valueOf(prefManager.getDriverPoints()));
            tvAvgQuality.setText(String.format("%.0f%%", prefManager.getDriverQuality()));
            tvPhone.setText(prefManager.getDriverPhone());
            tvEmail.setText("N/A");
            tvVehicleType.setText(prefManager.getDriverVehicleType());
            tvPlate.setText(prefManager.getDriverPlate());
        } else {
            // No data ever saved - show placeholder
            tvName.setText("Driver");
            tvTier.setText("🥉 Bronze Driver");
            tvMemberSince.setText("Not available");
            tvTotalTrips.setText("0");
            tvTotalPoints.setText("0");
            tvAvgQuality.setText("0%");
            tvPhone.setText("N/A");
            tvEmail.setText("N/A");
            tvVehicleType.setText("N/A");
            tvPlate.setText("N/A");
        }
    }

    private String getTierEmoji(String tier) {
        if (tier == null) return "🥉";
        switch (tier.toLowerCase()) {
            case "platinum": return "💎";
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
        Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData(); // Refresh data when returning to screen
    }
}