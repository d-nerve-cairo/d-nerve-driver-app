package com.example.dnervecairo.api.requests;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload for sending GPS updates during a live trip.
 * POST /api/v1/trips/{trip_id}/gps
 */
public class LiveGpsUpdateRequest {

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("speed_kph")
    private float speedKph;

    @SerializedName("accuracy_meters")
    private float accuracyMeters;

    @SerializedName("bearing")
    private Float bearing;

    public LiveGpsUpdateRequest(double latitude, double longitude,
                                float speedKph, float accuracyMeters, Float bearing) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.speedKph = speedKph;
        this.accuracyMeters = accuracyMeters;
        this.bearing = bearing;
    }

    // Getters
    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getSpeedKph() {
        return speedKph;
    }

    public float getAccuracyMeters() {
        return accuracyMeters;
    }

    public Float getBearing() {
        return bearing;
    }
}