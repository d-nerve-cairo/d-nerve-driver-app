package com.example.dnervecairo.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dnervecairo.R;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class TripSummaryActivity extends AppCompatActivity {

    // Intent extras keys
    public static final String EXTRA_DURATION = "duration";
    public static final String EXTRA_DISTANCE = "distance";
    public static final String EXTRA_GPS_POINTS = "gps_points";
    public static final String EXTRA_ROUTE_NAME = "route_name";
    public static final String EXTRA_PASSENGER_COUNT = "passenger_count";
    public static final String EXTRA_SAVED_OFFLINE = "saved_offline";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_summary);

        // Get trip data from intent
        int durationMinutes = getIntent().getIntExtra(EXTRA_DURATION, 0);
        double distance = getIntent().getDoubleExtra(EXTRA_DISTANCE, 0.0);
        int gpsPoints = getIntent().getIntExtra(EXTRA_GPS_POINTS, 0);
        String routeName = getIntent().getStringExtra(EXTRA_ROUTE_NAME);
        int passengerCount = getIntent().getIntExtra(EXTRA_PASSENGER_COUNT, 0);
        boolean savedOffline = getIntent().getBooleanExtra(EXTRA_SAVED_OFFLINE, false);

        // Calculate points
        int basePoints = 10;
        int qualityBonus = calculateQualityBonus(gpsPoints, durationMinutes);
        int peakBonus = calculatePeakBonus();
        int passengerBonus = calculatePassengerBonus(passengerCount);
        int totalPoints = basePoints + qualityBonus + peakBonus + passengerBonus;

        // Calculate quality score
        int qualityScore = calculateQualityScore(gpsPoints, durationMinutes);

        // Update UI
        displayRouteInfo(routeName, passengerCount);
        displayTripDetails(durationMinutes, distance, gpsPoints, qualityScore);
        displayPointsBreakdown(basePoints, qualityBonus, peakBonus, passengerBonus, totalPoints);
        displayOfflineStatus(savedOffline);

        // Back button
        MaterialButton btnBackHome = findViewById(R.id.btn_back_home);
        btnBackHome.setOnClickListener(v -> finish());
    }

    private void displayRouteInfo(String routeName, int passengerCount) {
        TextView tvRouteName = findViewById(R.id.tv_route_name);
        TextView tvPassengerCount = findViewById(R.id.tv_passenger_count);
        LinearLayout layoutRouteInfo = findViewById(R.id.layout_route_info);

        if (routeName != null && !routeName.isEmpty()) {
            tvRouteName.setText(routeName);
            layoutRouteInfo.setVisibility(View.VISIBLE);
        } else {
            layoutRouteInfo.setVisibility(View.GONE);
        }

        tvPassengerCount.setText(String.valueOf(passengerCount));
    }

    private void displayTripDetails(int duration, double distance, int gpsPoints, int quality) {
        TextView tvDuration = findViewById(R.id.tv_duration);
        TextView tvDistance = findViewById(R.id.tv_distance);
        TextView tvGpsPoints = findViewById(R.id.tv_gps_points);
        TextView tvQuality = findViewById(R.id.tv_quality);

        tvDuration.setText(String.format(Locale.getDefault(), "%d min", duration));
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distance));
        tvGpsPoints.setText(String.valueOf(gpsPoints));
        tvQuality.setText(String.format(Locale.getDefault(), "%d%%", quality));

        // Color quality based on score
        if (quality >= 90) {
            tvQuality.setTextColor(getResources().getColor(R.color.quality_excellent, null));
        } else if (quality >= 70) {
            tvQuality.setTextColor(getResources().getColor(R.color.quality_good, null));
        } else if (quality >= 50) {
            tvQuality.setTextColor(getResources().getColor(R.color.quality_fair, null));
        } else {
            tvQuality.setTextColor(getResources().getColor(R.color.quality_poor, null));
        }
    }

    private void displayPointsBreakdown(int base, int quality, int peak, int passenger, int total) {
        TextView tvPointsEarned = findViewById(R.id.tv_points_earned);
        TextView tvBasePoints = findViewById(R.id.tv_base_points);
        TextView tvQualityBonus = findViewById(R.id.tv_quality_bonus);
        TextView tvPeakBonus = findViewById(R.id.tv_peak_bonus);
        TextView tvPassengerBonus = findViewById(R.id.tv_passenger_bonus);
        TextView tvTotalPoints = findViewById(R.id.tv_total_points);
        LinearLayout layoutPassengerBonus = findViewById(R.id.layout_passenger_bonus);

        tvPointsEarned.setText(String.format(Locale.getDefault(), "+%d", total));
        tvBasePoints.setText(String.valueOf(base));
        tvQualityBonus.setText(String.format(Locale.getDefault(), "+%d", quality));
        tvPeakBonus.setText(String.format(Locale.getDefault(), "+%d", peak));

        // Show passenger bonus only if there are passengers
        if (passenger > 0) {
            layoutPassengerBonus.setVisibility(View.VISIBLE);
            tvPassengerBonus.setText(String.format(Locale.getDefault(), "+%d", passenger));
        } else {
            layoutPassengerBonus.setVisibility(View.GONE);
        }

        tvTotalPoints.setText(String.valueOf(total));
    }

    private void displayOfflineStatus(boolean savedOffline) {
        TextView tvOfflineStatus = findViewById(R.id.tv_offline_status);
        if (savedOffline) {
            tvOfflineStatus.setVisibility(View.VISIBLE);
        } else {
            tvOfflineStatus.setVisibility(View.GONE);
        }
    }

    private int calculateQualityScore(int gpsPoints, int durationMinutes) {
        if (durationMinutes <= 0) return 0;
        // Expected: ~2 points per minute (every 30 seconds)
        int expectedPoints = durationMinutes * 2;
        int score = (int) ((gpsPoints / (float) expectedPoints) * 100);
        return Math.min(score, 100); // Cap at 100%
    }

    private int calculateQualityBonus(int gpsPoints, int durationMinutes) {
        int quality = calculateQualityScore(gpsPoints, durationMinutes);
        if (quality >= 90) return 5;
        if (quality >= 70) return 3;
        if (quality >= 50) return 1;
        return 0;
    }

    private int calculatePeakBonus() {
        // Check if current time is peak hour (7-9 AM or 5-8 PM)
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 20)) {
            return 3;
        }
        return 0;
    }

    private int calculatePassengerBonus(int passengerCount) {
        // 1 point per passenger, max 10
        return Math.min(passengerCount, 10);
    }
}