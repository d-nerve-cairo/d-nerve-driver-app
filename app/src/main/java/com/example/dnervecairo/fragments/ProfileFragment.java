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
import com.example.dnervecairo.activities.LoginActivity;
import com.example.dnervecairo.activities.SettingsActivity;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.responses.DriverResponse;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        prefManager = new PreferenceManager(requireContext());

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
        MaterialButton btnSettings = view.findViewById(R.id.btn_settings);
        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);

        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditProfileActivity.class));
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
            displaySampleData();
            return;
        }

        String driverId = prefManager.getDriverId();

        ApiClient.getInstance().getApiService()
                .getDriver(driverId)
                .enqueue(new Callback<DriverResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DriverResponse> call, @NonNull Response<DriverResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            displayDriverProfile(response.body());
                        } else {
                            displaySampleData();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DriverResponse> call, @NonNull Throwable t) {
                        Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                        displaySampleData();
                    }
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
        tvEmail.setText("N/A"); // API doesn't return email
        tvVehicleType.setText(driver.getVehicleType());
        tvPlate.setText(driver.getLicensePlate());
    }

    private void displaySampleData() {
        tvName.setText("Ahmed Driver");
        tvTier.setText("🥉 Bronze Driver");
        tvMemberSince.setText("Member since Jan 2026");
        tvTotalTrips.setText("47");
        tvTotalPoints.setText("450");
        tvAvgQuality.setText("87%");
        tvPhone.setText("+20 123 456 7890");
        tvEmail.setText("ahmed@example.com");
        tvVehicleType.setText("Microbus - Toyota HiAce");
        tvPlate.setText("ج ن و  1234");
    }

    private String getTierEmoji(String tier) {
        switch (tier.toLowerCase()) {
            case "platinum": return "💎";
            case "gold": return "🥇";
            case "silver": return "🥈";
            default: return "🥉";
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null) return "Jan 2026";
        // Simple formatting - you can improve this
        try {
            return dateStr.substring(0, 10);
        } catch (Exception e) {
            return "Jan 2026";
        }
    }

    private void logout() {
        prefManager.clear();
        Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();

        // Go to login screen
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}