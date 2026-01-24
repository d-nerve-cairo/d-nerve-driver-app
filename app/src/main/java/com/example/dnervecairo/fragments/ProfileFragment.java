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
import com.example.dnervecairo.activities.SettingsActivity;
import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        setupButtons(view);
        loadProfileData(view);

        return view;
    }

    private void setupButtons(View view) {
        MaterialButton btnSettings = view.findViewById(R.id.btn_settings);
        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Logout clicked", Toast.LENGTH_SHORT).show();
            // TODO: Implement logout logic
        });
    }

    private void loadProfileData(View view) {
        // Sample data (later comes from API/SharedPreferences)
        TextView tvName = view.findViewById(R.id.tv_name);
        TextView tvTier = view.findViewById(R.id.tv_tier);
        TextView tvMemberSince = view.findViewById(R.id.tv_member_since);
        TextView tvTotalTrips = view.findViewById(R.id.tv_total_trips);
        TextView tvTotalPoints = view.findViewById(R.id.tv_total_points);
        TextView tvAvgQuality = view.findViewById(R.id.tv_avg_quality);
        TextView tvPhone = view.findViewById(R.id.tv_phone);
        TextView tvEmail = view.findViewById(R.id.tv_email);
        TextView tvVehicleType = view.findViewById(R.id.tv_vehicle_type);
        TextView tvPlate = view.findViewById(R.id.tv_plate);

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
}