package com.example.dnervecairo.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_driver")
public class CachedDriverEntity {

    @PrimaryKey
    @NonNull
    private String driverId;

    private String name;
    private String phone;
    private String vehicleType;
    private String licensePlate;
    private int totalPoints;
    private String tier;
    private int tripsCompleted;
    private double qualityAvg;
    private int currentStreak;
    private long lastUpdated;

    // Constructor
    public CachedDriverEntity(@NonNull String driverId, String name, String phone,
                              String vehicleType, String licensePlate, int totalPoints,
                              String tier, int tripsCompleted, double qualityAvg,
                              int currentStreak, long lastUpdated) {
        this.driverId = driverId;
        this.name = name;
        this.phone = phone;
        this.vehicleType = vehicleType;
        this.licensePlate = licensePlate;
        this.totalPoints = totalPoints;
        this.tier = tier;
        this.tripsCompleted = tripsCompleted;
        this.qualityAvg = qualityAvg;
        this.currentStreak = currentStreak;
        this.lastUpdated = lastUpdated;
    }

    // Getters
    @NonNull
    public String getDriverId() { return driverId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getVehicleType() { return vehicleType; }
    public String getLicensePlate() { return licensePlate; }
    public int getTotalPoints() { return totalPoints; }
    public String getTier() { return tier; }
    public int getTripsCompleted() { return tripsCompleted; }
    public double getQualityAvg() { return qualityAvg; }
    public int getCurrentStreak() { return currentStreak; }
    public long getLastUpdated() { return lastUpdated; }

    // Setters
    public void setDriverId(@NonNull String driverId) { this.driverId = driverId; }
    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    public void setTier(String tier) { this.tier = tier; }
    public void setTripsCompleted(int tripsCompleted) { this.tripsCompleted = tripsCompleted; }
    public void setQualityAvg(double qualityAvg) { this.qualityAvg = qualityAvg; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}