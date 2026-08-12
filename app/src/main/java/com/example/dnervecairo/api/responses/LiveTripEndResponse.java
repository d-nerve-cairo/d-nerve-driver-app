package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

/**
 * Response from ending a live trip.
 * POST /api/v1/trips/{trip_id}/end
 */
public class LiveTripEndResponse {

    @SerializedName("trip_id")
    private String tripId;

    @SerializedName("status")
    private String status;

    @SerializedName("duration_minutes")
    private float durationMinutes;

    @SerializedName("gps_points_count")
    private int gpsPointsCount;

    @SerializedName("quality_score")
    private float qualityScore;

    @SerializedName("points_earned")
    private int pointsEarned;

    @SerializedName("driver_total_points")
    private int driverTotalPoints;

    @SerializedName("driver_tier")
    private String driverTier;

    @SerializedName("message")
    private String message;

    // Getters
    public String getTripId() {
        return tripId;
    }

    public String getStatus() {
        return status;
    }

    public float getDurationMinutes() {
        return durationMinutes;
    }

    public int getGpsPointsCount() {
        return gpsPointsCount;
    }

    public float getQualityScore() {
        return qualityScore;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public int getDriverTotalPoints() {
        return driverTotalPoints;
    }

    public String getDriverTier() {
        return driverTier;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Check if the trip was successfully completed
     */
    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }

    /**
     * Get quality score as percentage (0-100)
     */
    public int getQualityScorePercent() {
        return Math.round(qualityScore * 100);
    }
}