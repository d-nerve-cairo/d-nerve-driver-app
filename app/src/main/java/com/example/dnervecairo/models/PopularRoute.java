package com.example.dnervecairo.models;

public class PopularRoute {
    private String routeId;
    private String startName;
    private String endName;
    private int estimatedMinutes;
    private int popularity;
    private float distanceKm = 0;

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

    // Setter
    public void setDistanceKm(float distanceKm) { this.distanceKm = distanceKm; }

    // Method
    public String getDisplayName() {
        return startName + " → " + endName;
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