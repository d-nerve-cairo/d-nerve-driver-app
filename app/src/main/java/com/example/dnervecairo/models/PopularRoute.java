package com.example.dnervecairo.models;

/**
 * PopularRoute model with smart location matching support
 */
public class PopularRoute {
    
    // Match types for filtering
    public enum MatchType {
        EXACT_MATCH,    // Route origin matches driver location (< 1.5km)
        NEARBY,         // Route origin is nearby (1.5 - 5km)
        OTHER           // All other routes
    }
    
    private String routeId;
    private String startName;
    private String endName;
    private int estimatedMinutes;
    private int popularity;
    private float distanceKm = 0;
    
    // Distance from driver's current location to route origin (in km)
    private float distanceFromDriver = 0;
    
    // Match type based on location
    private MatchType matchType = MatchType.OTHER;

    // Route coordinates for map display
    private double originLat = 0;
    private double originLon = 0;
    private double destLat = 0;
    private double destLon = 0;

    public PopularRoute(String routeId, String startName, String endName,
                        int estimatedMinutes, int popularity) {
        this.routeId = routeId;
        this.startName = startName;
        this.endName = endName;
        this.estimatedMinutes = estimatedMinutes;
        this.popularity = popularity;
    }

    // Constructor with coordinates (for offline routes)
    public PopularRoute(String routeId, String startName, String endName,
                        int estimatedMinutes, int popularity, float distanceKm,
                        double originLat, double originLon, double destLat, double destLon) {
        this.routeId = routeId;
        this.startName = startName;
        this.endName = endName;
        this.estimatedMinutes = estimatedMinutes;
        this.popularity = popularity;
        this.distanceKm = distanceKm;
        this.originLat = originLat;
        this.originLon = originLon;
        this.destLat = destLat;
        this.destLon = destLon;
    }

    // Getters
    public String getRouteId() { return routeId; }
    public String getStartName() { return startName; }
    public String getEndName() { return endName; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public int getPopularity() { return popularity; }
    public float getDistanceKm() { return distanceKm; }
    public float getDistanceFromDriver() { return distanceFromDriver; }
    public MatchType getMatchType() { return matchType; }

    // Setters
    public void setDistanceKm(float distanceKm) { this.distanceKm = distanceKm; }
    public void setDistanceFromDriver(float distanceFromDriver) { 
        this.distanceFromDriver = distanceFromDriver;
        updateMatchType();
    }
    public void setMatchType(MatchType matchType) { this.matchType = matchType; }
    
    /**
     * Update match type based on distance from driver
     */
    private void updateMatchType() {
        if (distanceFromDriver <= 1.5f) {
            matchType = MatchType.EXACT_MATCH;
        } else if (distanceFromDriver <= 5.0f) {
            matchType = MatchType.NEARBY;
        } else {
            matchType = MatchType.OTHER;
        }
    }

    // Display methods
    public String getDisplayName() {
        return startName + " → " + endName;
    }
    
    /**
     * Get formatted distance to pickup point
     */
    public String getFormattedDistanceToPickup() {
        if (distanceFromDriver <= 0) {
            return "";
        }
        if (distanceFromDriver < 0.1f) {
            return "You're here!";
        }
        if (distanceFromDriver < 1) {
            return String.format("%.0f m to pickup", distanceFromDriver * 1000);
        }
        return String.format("%.1f km to pickup", distanceFromDriver);
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public String getFormattedDistanceFromDriver() {
        return getFormattedDistanceToPickup();
    }
    
    /**
     * Check if this is an exact location match
     */
    public boolean isExactMatch() {
        return matchType == MatchType.EXACT_MATCH;
    }
    
    /**
     * Check if this route is nearby
     */
    public boolean isNearby() {
        return matchType == MatchType.NEARBY;
    }

    // Coordinate getters
    public double getOriginLat() { return originLat; }
    public double getOriginLon() { return originLon; }
    public double getDestLat() { return destLat; }
    public double getDestLon() { return destLon; }

    // Coordinate setters
    public void setOriginLat(double originLat) { this.originLat = originLat; }
    public void setOriginLon(double originLon) { this.originLon = originLon; }
    public void setDestLat(double destLat) { this.destLat = destLat; }
    public void setDestLon(double destLon) { this.destLon = destLon; }

    // Helper to check if coordinates are set
    public boolean hasCoordinates() {
        return originLat != 0 && originLon != 0 && destLat != 0 && destLon != 0;
    }
}
