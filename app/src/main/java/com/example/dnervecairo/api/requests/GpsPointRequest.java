package com.example.dnervecairo.api.requests;

import com.google.gson.annotations.SerializedName;

public class GpsPointRequest {
    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("accuracy_meters")
    private Float accuracyMeters;

    @SerializedName("speed_kph")
    private Float speedKph;

    public GpsPointRequest(double latitude, double longitude, String timestamp, Float accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.accuracyMeters = accuracy;
        this.speedKph = null;
    }
}