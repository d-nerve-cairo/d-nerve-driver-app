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

    @SerializedName("driver_id")
    private String driverId;

    // Full constructor with driverId
    public LeaderboardEntry(int rank, String driverName, String tier, int points, String driverId) {
        this.rank = rank;
        this.driverName = driverName;
        this.tier = tier;
        this.points = points;
        this.driverId = driverId;
    }

    // Legacy constructor for backwards compatibility
    public LeaderboardEntry(int rank, String driverName, String tier, int points) {
        this(rank, driverName, tier, points, null);
    }

    // Getters
    public int getRank() { return rank; }
    public String getDriverName() { return driverName; }
    public String getTier() { return tier; }
    public int getPoints() { return points; }
    public String getDriverId() { return driverId; }

    // Setters
    public void setDriverId(String driverId) { this.driverId = driverId; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LeaderboardEntry that = (LeaderboardEntry) obj;
        return rank == that.rank && 
               (driverId != null ? driverId.equals(that.driverId) : that.driverId == null);
    }
    
    @Override
    public int hashCode() {
        int result = rank;
        result = 31 * result + (driverId != null ? driverId.hashCode() : 0);
        return result;
    }
}
