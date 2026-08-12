package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

/**
 * Response from starting a live trip.
 * POST /api/v1/trips/start
 */
public class LiveTripStartResponse {

    @SerializedName("trip_id")
    private String tripId;

    @SerializedName("status")
    private String status;

    @SerializedName("route_id")
    private String routeId;

    @SerializedName("driver_name")
    private String driverName;

    @SerializedName("started_at")
    private String startedAt;

    @SerializedName("message")
    private String message;

    // Getters
    public String getTripId() {
        return tripId;
    }

    public String getStatus() {
        return status;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Check if the trip was successfully started
     */
    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }
}