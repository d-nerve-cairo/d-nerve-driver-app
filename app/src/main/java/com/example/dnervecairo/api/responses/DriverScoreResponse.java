package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

public class DriverScoreResponse {
    @SerializedName("driver_id")
    private String driverId;

    @SerializedName("total_points")
    private int totalPoints;

    @SerializedName("available_points")
    private int availablePoints;

    @SerializedName("tier")
    private String tier;

    @SerializedName("trips_completed")
    private int tripsCompleted;

    @SerializedName("quality_avg")
    private double qualityAvg;

    @SerializedName("current_streak")
    private int currentStreak;

    @SerializedName("rank")
    private int rank;

    // Getters
    public String getDriverId() { return driverId; }
    public int getTotalPoints() { return totalPoints; }
    public int getAvailablePoints() { return availablePoints; }
    public String getTier() { return tier; }
    public int getTripsCompleted() { return tripsCompleted; }
    public double getQualityAvg() { return qualityAvg; }
    public int getCurrentStreak() { return currentStreak; }
    public int getRank() { return rank; }
}