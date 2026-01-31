package com.example.dnervecairo.models;

import com.google.gson.annotations.SerializedName;

public class LeaderboardEntry {
    @SerializedName("rank")
    private int rank;

    @SerializedName("driver_name")
    private String driverName;

    @SerializedName("tier")
    private String tier;

    @SerializedName("points")
    private int points;

    // Constructor for local use
    public LeaderboardEntry(int rank, String driverName, String tier, int points) {
        this.rank = rank;
        this.driverName = driverName;
        this.tier = tier;
        this.points = points;
    }

    // Getters
    public int getRank() { return rank; }
    public String getDriverName() { return driverName; }
    public String getTier() { return tier; }
    public int getPoints() { return points; }
}