package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

public class DriverResponse {
    @SerializedName("driver_id")
    private String driverId;

    @SerializedName("name")
    private String name;

    @SerializedName("phone")
    private String phone;

    @SerializedName("vehicle_type")
    private String vehicleType;

    @SerializedName("license_plate")
    private String licensePlate;

    @SerializedName("total_points")
    private int totalPoints;

    @SerializedName("current_tier")  // <-- FIXED: was "tier"
    private String tier;

    @SerializedName("trips_completed")
    private int tripsCompleted;

    @SerializedName("quality_avg")
    private double qualityAvg;

    @SerializedName("current_streak")
    private int currentStreak;

    @SerializedName("member_since")  // <-- FIXED: was "created_at"
    private String createdAt;

    // Getters
    public String getDriverId() { return driverId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getVehicleType() { return vehicleType; }
    public String getLicensePlate() { return licensePlate; }
    public int getTotalPoints() { return totalPoints; }
    public String getTier() { return tier != null ? tier : "Bronze"; }  // <-- Added null safety
    public int getTripsCompleted() { return tripsCompleted; }
    public double getQualityAvg() { return qualityAvg; }
    public int getCurrentStreak() { return currentStreak; }
    public String getCreatedAt() { return createdAt; }
}