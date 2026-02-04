package com.example.dnervecairo.models;

public class PopularRoute {
    private String routeId;
    private String startName;
    private String endName;
    private int estimatedMinutes;
    private int popularity; // Percentage

    public PopularRoute(String routeId, String startName, String endName,
                        int estimatedMinutes, int popularity) {
        this.routeId = routeId;
        this.startName = startName;
        this.endName = endName;
        this.estimatedMinutes = estimatedMinutes;
        this.popularity = popularity;
    }

    public String getRouteId() { return routeId; }
    public String getStartName() { return startName; }
    public String getEndName() { return endName; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public int getPopularity() { return popularity; }

    public String getDisplayName() {
        return startName + " → " + endName;
    }
}