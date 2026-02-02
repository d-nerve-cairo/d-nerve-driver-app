package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

public class BadgeResponse {

    @SerializedName("badge_id")
    private String badgeId;

    @SerializedName("name")
    private String name;

    @SerializedName("name_ar")
    private String nameAr;

    @SerializedName("description")
    private String description;

    @SerializedName("description_ar")
    private String descriptionAr;

    @SerializedName("icon")
    private String icon;

    @SerializedName("category")
    private String category;

    @SerializedName("requirement_type")
    private String requirementType;

    @SerializedName("requirement_value")
    private int requirementValue;

    @SerializedName("current_value")
    private int currentValue;

    @SerializedName("progress_percent")
    private float progressPercent;

    @SerializedName("is_earned")
    private boolean isEarned;

    @SerializedName("earned_at")
    private String earnedAt;

    @SerializedName("points_reward")
    private int pointsReward;

    // Getters
    public String getBadgeId() { return badgeId; }
    public String getName() { return name; }
    public String getNameAr() { return nameAr; }
    public String getDescription() { return description; }
    public String getDescriptionAr() { return descriptionAr; }
    public String getIcon() { return icon; }
    public String getCategory() { return category; }
    public String getRequirementType() { return requirementType; }
    public int getRequirementValue() { return requirementValue; }
    public int getCurrentValue() { return currentValue; }
    public float getProgressPercent() { return progressPercent; }
    public boolean isEarned() { return isEarned; }
    public String getEarnedAt() { return earnedAt; }
    public int getPointsReward() { return pointsReward; }
}