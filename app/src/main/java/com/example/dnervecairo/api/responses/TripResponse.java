package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

public class TripResponse {

    @SerializedName("trip_id")
    private String tripId;

    @SerializedName("driver_id")
    private String driverId;

    @SerializedName("route_id")
    private String routeId;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("duration_minutes")
    private double durationMinutes;

    @SerializedName("num_points")
    private int numPoints;

    @SerializedName("quality_score")
    private double qualityScore;

    @SerializedName("points_earned")
    private int pointsEarned;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("gps_points_json")
    private String gpsPointsJson;

    // Getters
    public String getTripId() {
        return tripId;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public int getDurationMinutes() {
        return (int) durationMinutes;
    }

    public int getGpsPointsCount() {
        return numPoints;
    }

    public double getQualityScore() {
        return qualityScore;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // Distance not provided by backend, calculate estimate
    public double getDistanceKm() {
        // Rough estimate: ~0.5km per minute of travel
        return durationMinutes * 0.5;
    }

    public String getGpsPointsJson() {
        return gpsPointsJson;
    }
}