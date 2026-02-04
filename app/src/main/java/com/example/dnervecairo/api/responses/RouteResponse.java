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

    public String getRouteId() { return routeId; }
    public String getStartName() { return startName; }
    public String getEndName() { return endName; }
    public int getEstimatedDuration() { return estimatedDuration; }
    public int getPopularity() { return popularity; }
}