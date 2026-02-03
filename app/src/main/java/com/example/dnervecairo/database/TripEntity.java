package com.example.dnervecairo.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_trips")
public class TripEntity {

    @PrimaryKey
    @NonNull
    private String localTripId;

    private String driverId;
    private String routeId;
    private String startTime;
    private String endTime;
    private String gpsPointsJson;
    private int gpsPointsCount;
    private boolean isSynced;
    private long createdAt;

    // Constructor
    public TripEntity(@NonNull String localTripId, String driverId, String routeId,
                      String startTime, String endTime, String gpsPointsJson,
                      int gpsPointsCount, boolean isSynced, long createdAt) {
        this.localTripId = localTripId;
        this.driverId = driverId;
        this.routeId = routeId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.gpsPointsJson = gpsPointsJson;
        this.gpsPointsCount = gpsPointsCount;
        this.isSynced = isSynced;
        this.createdAt = createdAt;
    }

    // Getters
    @NonNull
    public String getLocalTripId() { return localTripId; }
    public String getDriverId() { return driverId; }
    public String getRouteId() { return routeId; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getGpsPointsJson() { return gpsPointsJson; }
    public int getGpsPointsCount() { return gpsPointsCount; }
    public boolean isSynced() { return isSynced; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setLocalTripId(@NonNull String localTripId) { this.localTripId = localTripId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setGpsPointsJson(String gpsPointsJson) { this.gpsPointsJson = gpsPointsJson; }
    public void setGpsPointsCount(int gpsPointsCount) { this.gpsPointsCount = gpsPointsCount; }
    public void setSynced(boolean synced) { isSynced = synced; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}