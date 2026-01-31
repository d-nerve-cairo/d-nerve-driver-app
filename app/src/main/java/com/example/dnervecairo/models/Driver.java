package com.example.dnervecairo.models;

import com.google.gson.annotations.SerializedName;

public class Driver {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("phone")
    private String phone;

    @SerializedName("email")
    private String email;

    @SerializedName("total_points")
    private int totalPoints;

    @SerializedName("tier")
    private String tier;

    @SerializedName("total_trips")
    private int totalTrips;

    @SerializedName("avg_quality")
    private double avgQuality;

    @SerializedName("current_streak")
    private int currentStreak;

    @SerializedName("vehicle_type")
    private String vehicleType;

    @SerializedName("plate_number")
    private String plateNumber;

    @SerializedName("created_at")
    private String createdAt;

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public int getTotalPoints() { return totalPoints; }
    public String getTier() { return tier; }
    public int getTotalTrips() { return totalTrips; }
    public double getAvgQuality() { return avgQuality; }
    public int getCurrentStreak() { return currentStreak; }
    public String getVehicleType() { return vehicleType; }
    public String getPlateNumber() { return plateNumber; }
    public String getCreatedAt() { return createdAt; }
}