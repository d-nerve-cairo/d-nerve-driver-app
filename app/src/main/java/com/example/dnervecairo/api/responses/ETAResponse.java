package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

public class ETAResponse {

    @SerializedName("predicted_duration_minutes")
    private float predictedDurationMinutes;

    @SerializedName("confidence_interval")
    private ConfidenceInterval confidenceInterval;

    @SerializedName("model_version")
    private String modelVersion;

    @SerializedName("timestamp")
    private String timestamp;

    public float getPredictedDurationMinutes() { return predictedDurationMinutes; }
    public ConfidenceInterval getConfidenceInterval() { return confidenceInterval; }
    public String getModelVersion() { return modelVersion; }
    public String getTimestamp() { return timestamp; }

    public static class ConfidenceInterval {
        @SerializedName("lower")
        private float lower;

        @SerializedName("upper")
        private float upper;

        public float getLower() { return lower; }
        public float getUpper() { return upper; }
    }
}