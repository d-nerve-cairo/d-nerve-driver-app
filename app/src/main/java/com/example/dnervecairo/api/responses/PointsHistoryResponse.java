package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PointsHistoryResponse {
    @SerializedName("history")
    private List<PointsTransaction> history;

    @SerializedName("total")
    private int total;

    public List<PointsTransaction> getHistory() { return history; }
    public int getTotal() { return total; }

    public static class PointsTransaction {
        @SerializedName("type")
        private String type;

        @SerializedName("points")
        private int points;

        @SerializedName("description")
        private String description;

        @SerializedName("timestamp")
        private String timestamp;

        // Getters
        public String getType() { return type; }
        public int getPoints() { return points; }
        public String getDescription() { return description; }
        public String getTimestamp() { return timestamp; }
    }
}