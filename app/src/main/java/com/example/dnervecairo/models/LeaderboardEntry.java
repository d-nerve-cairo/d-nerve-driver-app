package com.example.dnervecairo.models;

public class LeaderboardEntry {
    private final int rank;
    private final String driverName;
    private final String tier;
    private final int points;

    public LeaderboardEntry(int rank, String driverName, String tier, int points) {
        this.rank = rank;
        this.driverName = driverName;
        this.tier = tier;
        this.points = points;
    }

    public int getRank() { return rank; }
    public String getDriverName() { return driverName; }
    public String getTier() { return tier; }
    public int getPoints() { return points; }
}