package com.example.dnervecairo.api.requests;

import com.google.gson.annotations.SerializedName;

public class ETARequest {

    @SerializedName("distance_km")
    private float distanceKm;

    @SerializedName("hour")
    private int hour;

    @SerializedName("is_peak")
    private int isPeak;

    public ETARequest(float distanceKm, int hour, int isPeak) {
        this.distanceKm = distanceKm;
        this.hour = hour;
        this.isPeak = isPeak;
    }

    // Getters
    public float getDistanceKm() { return distanceKm; }
    public int getHour() { return hour; }
    public int getIsPeak() { return isPeak; }
}