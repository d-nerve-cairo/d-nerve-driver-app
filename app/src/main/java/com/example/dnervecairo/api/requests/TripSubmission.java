package com.example.dnervecairo.api.requests;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TripSubmission {
    @SerializedName("driver_id")
    private String driverId;

    @SerializedName("route_id")
    private String routeId;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("gps_points")
    private List<GpsPointRequest> gpsPoints;

    public TripSubmission(String driverId, String startTime, String endTime, List<GpsPointRequest> gpsPoints) {
        this.driverId = driverId;
        this.routeId = null;
        this.startTime = startTime;
        this.endTime = endTime;
        this.gpsPoints = gpsPoints;
    }

    // Getters
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

    public List<GpsPointRequest> getGpsPoints() {
        return gpsPoints;
    }
}