package com.example.dnervecairo.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dnervecairo.R;
import com.example.dnervecairo.activities.StartTripActivity;
import com.example.dnervecairo.activities.TripActivity;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.activities.TripHistoryActivity;
import com.example.dnervecairo.activities.RewardsActivity;
import com.example.dnervecairo.database.CachedDriverEntity;
import com.example.dnervecairo.utils.NetworkUtils;
import com.example.dnervecairo.utils.OfflineManager;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private TextView tvPoints, tvTier, tvTierProgress, tvTripsCount, tvQuality, tvStreak;
    private ProgressBar progressTier;
    private PreferenceManager prefManager;
    private OfflineManager offlineManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        prefManager = new PreferenceManager(requireContext());
        offlineManager = new OfflineManager(requireContext());

        initViews(view);
        setupTripHistoryCard(view);
        setupRewardsCard(view);
        setupStartTripButton(view);
        loadDriverData();

        return view;
    }

    private void initViews(View view) {
        tvPoints = view.findViewById(R.id.tv_points);
        tvTier = view.findViewById(R.id.tv_tier);
        tvTierProgress = view.findViewById(R.id.tv_tier_progress);
        tvTripsCount = view.findViewById(R.id.tv_trips_count);
        tvQuality = view.findViewById(R.id.tv_quality);
        tvStreak = view.findViewById(R.id.tv_streak);
        progressTier = view.findViewById(R.id.progress_tier);
    }

    private void setupTripHistoryCard(View view) {
        View cardTripHistory = view.findViewById(R.id.card_trip_history);
        cardTripHistory.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), TripHistoryActivity.class));
        });
    }

    private void setupStartTripButton(View view) {
        MaterialButton btnStartTrip = view.findViewById(R.id.btn_start_trip);
        btnStartTrip.setOnClickListener(v -> {
            // Go to route selection first
            Intent intent = new Intent(getActivity(), StartTripActivity.class);
            startActivity(intent);
        });
    }

    private void setupRewardsCard(View view) {
        com.google.android.material.card.MaterialCardView cardRewards = view.findViewById(R.id.card_rewards);
        cardRewards.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), RewardsActivity.class));
        });
    }

    private void loadDriverData() {
        // Check if logged in
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
                                displayDriverData(driver);
                                // Cache for offline use (Room)
                                offlineManager.cacheDriverData(driver);
                                // Also save to SharedPreferences (quick access)
                                prefManager.saveDriverData(
                                        driver.getName(),
                                        driver.getTotalPoints(),
                                        driver.getTier(),
                                        driver.getTripsCompleted(),
                                        driver.getQualityAvg(),
                                        driver.getCurrentStreak()
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
                    displayCachedData(cachedDriver);
                } else {
                    // Fallback to SharedPreferences
                    displaySavedOrDefaultData();
                }
            });
        });
    }

    private void displayDriverData(DriverResponse driver) {
        tvPoints.setText(String.valueOf(driver.getTotalPoints()));
        tvTier.setText(driver.getTier() + " Driver");
        tvTripsCount.setText(String.valueOf(driver.getTripsCompleted()));
        tvQuality.setText(String.format("%.0f%%", driver.getQualityAvg()));
        tvStreak.setText(String.valueOf(driver.getCurrentStreak()));

        // Update tier progress
        updateTierProgress(driver.getTotalPoints(), driver.getTier());
    }

    private void displayCachedData(CachedDriverEntity driver) {
        tvPoints.setText(String.valueOf(driver.getTotalPoints()));
        tvTier.setText(driver.getTier() + " Driver");
        tvTripsCount.setText(String.valueOf(driver.getTripsCompleted()));
        tvQuality.setText(String.format("%.0f%%", driver.getQualityAvg() * 100));
        tvStreak.setText(String.valueOf(driver.getCurrentStreak()));

        // Update tier progress
        updateTierProgress(driver.getTotalPoints(), driver.getTier());
    }

    private void displaySavedOrDefaultData() {
        if (prefManager.hasDriverData()) {
            // Show last known data from SharedPreferences
            tvPoints.setText(String.valueOf(prefManager.getDriverPoints()));
            tvTier.setText(prefManager.getDriverTier() + " Driver");
            tvTripsCount.setText(String.valueOf(prefManager.getDriverTrips()));
            tvQuality.setText(String.format("%.0f%%", prefManager.getDriverQuality()));
            tvStreak.setText(String.valueOf(prefManager.getDriverStreak()));
            updateTierProgress(prefManager.getDriverPoints(), prefManager.getDriverTier());
        } else {
            // No data ever saved - show placeholder
            tvPoints.setText("0");
            tvTier.setText("Bronze Driver");
            tvTripsCount.setText("0");
            tvQuality.setText("0%");
            tvStreak.setText("0");
            progressTier.setProgress(0);
            tvTierProgress.setText("Complete trips to earn points!");
        }
    }

    private void updateTierProgress(int points, String tier) {
        // Null safety
        if (tier == null) tier = "bronze";

        int nextTierPoints;
        String nextTier;

        switch (tier.toLowerCase()) {
            case "bronze":
                nextTierPoints = 1000;
                nextTier = "Silver";
                break;
            case "silver":
                nextTierPoints = 2500;
                nextTier = "Gold";
                break;
            case "gold":
                nextTierPoints = 5000;
                nextTier = "Platinum";
                break;
            default:
                nextTierPoints = points;
                nextTier = "Max";
        }

        progressTier.setMax(nextTierPoints);
        progressTier.setProgress(points);
        tvTierProgress.setText(points + " / " + nextTierPoints + " to " + nextTier);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDriverData(); // Refresh data when returning to screen
    }
}