package com.example.dnervecairo.api.requests;

import com.google.gson.annotations.SerializedName;

public class UpdateDriverRequest {

    @SerializedName("name")
    private String name;

    @SerializedName("vehicle_type")
    private String vehicleType;

    @SerializedName("license_plate")
    private String licensePlate;

    public UpdateDriverRequest(String name, String vehicleType, String licensePlate) {
        this.name = name;
        this.vehicleType = vehicleType;
        this.licensePlate = licensePlate;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getLicensePlate() {
        return licensePlate;
    }
}