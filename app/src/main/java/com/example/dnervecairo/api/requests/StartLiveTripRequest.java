package com.example.dnervecairo.api.requests;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload for starting a live trip.
 * POST /api/v1/trips/start
 */
public class StartLiveTripRequest {

    @SerializedName("driver_id")
    private String driverId;

    @SerializedName("route_id")
    private String routeId;

    @SerializedName("passenger_count")
    private int passengerCount;

    @SerializedName("origin_lat")
    private double originLat;

    @SerializedName("origin_lon")
    private double originLon;

    public StartLiveTripRequest(String driverId, String routeId, int passengerCount,
                                double originLat, double originLon) {
        this.driverId = driverId;
        this.routeId = routeId;
        this.passengerCount = passengerCount;
        this.originLat = originLat;
        this.originLon = originLon;
    }

    // Getters
    public String getDriverId() {
        return driverId;
    }

    public String getRouteId() {
        return routeId;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public double getOriginLat() {
        return originLat;
    }

    public double getOriginLon() {
        return originLon;
    }
}