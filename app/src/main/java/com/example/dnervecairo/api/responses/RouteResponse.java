package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

public class RouteResponse {

    @SerializedName("route_id")
    private String routeId;

    @SerializedName("start_name")
    private String startName;

    @SerializedName("end_name")
    private String endName;

    @SerializedName("estimated_duration")
    private int estimatedDuration;

    @SerializedName("popularity")
    private int popularity;

    @SerializedName("distance_km")
    private float distanceKm;

    // Route coordinates for map display
    @SerializedName("origin_lat")
    private double originLat;

    @SerializedName("origin_lon")
    private double originLon;

    @SerializedName("dest_lat")
    private double destLat;

    @SerializedName("dest_lon")
    private double destLon;

    // Getters
    public String getRouteId() { return routeId; }
    public String getStartName() { return startName; }
    public String getEndName() { return endName; }
    public int getEstimatedDuration() { return estimatedDuration; }
    public int getPopularity() { return popularity; }
    public float getDistanceKm() { return distanceKm; }

    // Coordinate getters
    public double getOriginLat() { return originLat; }
    public double getOriginLon() { return originLon; }
    public double getDestLat() { return destLat; }
    public double getDestLon() { return destLon; }
}