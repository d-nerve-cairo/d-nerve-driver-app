package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DriversListResponse {
    @SerializedName("drivers")
    private List<DriverResponse> drivers;

    @SerializedName("total")
    private int total;

    public List<DriverResponse> getDrivers() {
        return drivers;
    }

    public int getTotal() {
        return total;
    }
}