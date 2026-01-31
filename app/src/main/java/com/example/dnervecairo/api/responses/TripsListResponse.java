package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TripsListResponse {
    @SerializedName("trips")
    private List<TripResponse> trips;

    @SerializedName("total")
    private int total;

    public List<TripResponse> getTrips() {
        return trips;
    }

    public int getTotal() {
        return total;
    }
}