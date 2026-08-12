package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

// =========================================================================
    // Driver profile response from API
    // Updated with rating system, emergency contact, and bio fields
// =========================================================================

public class DriverResponse {

    @SerializedName("driver_id")
    private String driverId;

    @SerializedName("name")
    private String name;

    @SerializedName("phone")
    private String phone;

    @SerializedName("email")
    private String email;

    @SerializedName("vehicle_type")
    private String vehicleType;

    @SerializedName("license_plate")
    private String licensePlate;

    // Profile fields
    @SerializedName("emergency_contact")
    private String emergencyContact;

    @SerializedName("bio")
    private String bio;

    // Rating fields
    @SerializedName("avg_rating")
    private float avgRating;

    @SerializedName("total_ratings")
    private int totalRatings;

    // Gamification
    @SerializedName("total_points")
    private int totalPoints;

    @SerializedName("tier")
    private String tier;

    @SerializedName("current_tier")
    private String currentTier;

    @SerializedName("trips_completed")
    private int tripsCompleted;

    @SerializedName("quality_avg")
    private float qualityAvg;

    @SerializedName("current_streak")
    private int currentStreak;

    // Rewards
    @SerializedName("rewards_available_egp")
    private float rewardsAvailableEgp;

    // Timestamps
    @SerializedName("member_since")
    private String memberSince;

    @SerializedName("created_at")
    private String createdAt;

    // =========================================================================
    // GETTERS
    // =========================================================================

    public String getDriverId() { return driverId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getVehicleType() { return vehicleType; }
    public String getLicensePlate() { return licensePlate; }
    public String getEmergencyContact() { return emergencyContact; }
    public String getBio() { return bio; }
    public float getAvgRating() { return avgRating; }
    public int getTotalRatings() { return totalRatings; }
    public int getTotalPoints() { return totalPoints; }
    public String getTier() { return tier != null ? tier : currentTier; }
    public String getCurrentTier() { return currentTier != null ? currentTier : tier; }
    public int getTripsCompleted() { return tripsCompleted; }
    public float getQualityAvg() { return qualityAvg; }
    public int getCurrentStreak() { return currentStreak; }
    public float getRewardsAvailableEgp() { return rewardsAvailableEgp; }
    public String getMemberSince() { return memberSince; }
    public String getCreatedAt() { return createdAt; }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    public boolean hasRatings() {
        return totalRatings > 0;
    }

    public String getFormattedRating() {
        if (totalRatings == 0) {
            return "No ratings";
        }
        return String.format("%.1f ★ (%d)", avgRating, totalRatings);
    }

    public int getRatingPercentage() {
        return Math.round((avgRating / 5.0f) * 100);
    }

    public boolean hasEmergencyContact() {
        return emergencyContact != null && !emergencyContact.isEmpty();
    }

    public boolean hasBio() {
        return bio != null && !bio.isEmpty();
    }

    public boolean isProfileComplete() {
        return name != null && !name.isEmpty() &&
               vehicleType != null && !vehicleType.isEmpty() &&
               licensePlate != null && !licensePlate.isEmpty();
    }
}
