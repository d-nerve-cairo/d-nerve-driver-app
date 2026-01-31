package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LeaderboardResponse {
    @SerializedName("leaderboard")
    private List<LeaderboardEntry> leaderboard;

    @SerializedName("total_drivers")
    private int totalDrivers;

    public List<LeaderboardEntry> getLeaderboard() { return leaderboard; }
    public int getTotalDrivers() { return totalDrivers; }

    public static class LeaderboardEntry {
        @SerializedName("rank")
        private int rank;

        @SerializedName("driver_id")
        private String driverId;

        @SerializedName("name")
        private String name;

        @SerializedName("total_points")
        private int totalPoints;

        @SerializedName("tier")
        private String tier;

        @SerializedName("trips_completed")
        private int tripsCompleted;

        @SerializedName("quality_avg")
        private double qualityAvg;

        // Getters
        public int getRank() { return rank; }
        public String getDriverId() { return driverId; }
        public String getName() { return name; }
        public int getTotalPoints() { return totalPoints; }
        public String getTier() { return tier; }
        public int getTripsCompleted() { return tripsCompleted; }
        public double getQualityAvg() { return qualityAvg; }
    }
}