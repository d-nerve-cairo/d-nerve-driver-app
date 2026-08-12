package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PointsHistoryResponse {

    @SerializedName("driver_id")
    private String driverId;

    @SerializedName("transactions")
    private List<PointsTransaction> transactions;

    @SerializedName("total")
    private int total;

    @SerializedName("limit")
    private int limit;

    @SerializedName("offset")
    private int offset;

    // Getters
    public String getDriverId() { return driverId; }
    public List<PointsTransaction> getTransactions() { return transactions; }
    public int getTotal() { return total; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }

    public static class PointsTransaction {
        @SerializedName("type")
        private String type;

        @SerializedName("points")
        private int points;

        @SerializedName("description")
        private String description;

        @SerializedName("balance_after")
        private int balanceAfter;

        @SerializedName("timestamp")
        private String timestamp;

        // Getters
        public String getType() { return type; }
        public int getPoints() { return points; }
        public String getDescription() { return description; }
        public int getBalanceAfter() { return balanceAfter; }
        public String getTimestamp() { return timestamp; }
    }
}
