package com.example.dnervecairo.models;

import com.google.gson.annotations.SerializedName;

public class Trip {
    @SerializedName("id")
    private int id;

    @SerializedName("driver_id")
    private int driverId;

    @SerializedName("duration_minutes")
    private int durationMinutes;

    @SerializedName("distance_km")
    private double distanceKm;

    @SerializedName("gps_points_count")
    private int gpsPointsCount;

    @SerializedName("quality_score")
    private int qualityScore;

    @SerializedName("points_earned")
    private int pointsEarned;

    @SerializedName("created_at")
    private String createdAt;

    // Getters
    public int getId() { return id; }
    public int getDriverId() { return driverId; }
    public int getDurationMinutes() { return durationMinutes; }
    public double getDistanceKm() { return distanceKm; }
    public int getGpsPointsCount() { return gpsPointsCount; }
    public int getQualityScore() { return qualityScore; }
    public int getPointsEarned() { return pointsEarned; }
    public String getCreatedAt() { return createdAt; }
}