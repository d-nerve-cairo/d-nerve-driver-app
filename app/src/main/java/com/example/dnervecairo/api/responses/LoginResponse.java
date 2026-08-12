package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("driver")
    private DriverData driver;

    public String getMessage() { return message; }
    public DriverData getDriver() { return driver; }

    public static class DriverData {
        @SerializedName("driver_id")
        private String driverId;

        @SerializedName("name")
        private String name;

        @SerializedName("email")
        private String email;

        @SerializedName("phone")
        private String phone;

        @SerializedName("vehicle_type")
        private String vehicleType;

        @SerializedName("license_plate")
        private String licensePlate;

        @SerializedName("total_points")
        private int totalPoints;

        @SerializedName("tier")
        private String tier;

        @SerializedName("trips_completed")
        private int tripsCompleted;

        @SerializedName("quality_avg")
        private double qualityAvg;

        @SerializedName("current_streak")
        private int currentStreak;

        @SerializedName("rewards_available_egp")
        private double rewardsAvailable;

        @SerializedName("member_since")
        private String memberSince;

        @SerializedName("created_at")
        private String createdAt;

        // Getters
        public String getDriverId() { return driverId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getVehicleType() { return vehicleType; }
        public String getLicensePlate() { return licensePlate; }
        public int getTotalPoints() { return totalPoints; }
        public String getTier() { return tier; }
        public int getTripsCompleted() { return tripsCompleted; }
        public double getQualityAvg() { return qualityAvg; }
        public int getCurrentStreak() { return currentStreak; }
        public double getRewardsAvailable() { return rewardsAvailable; }
        public String getMemberSince() { return memberSince; }
        public String getCreatedAt() { return createdAt; }
    }
}
