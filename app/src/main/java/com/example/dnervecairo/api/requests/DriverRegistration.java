package com.example.dnervecairo.api.requests;

import com.google.gson.annotations.SerializedName;

public class DriverRegistration {
    @SerializedName("name")
    private String name;

    @SerializedName("phone")
    private String phone;

    @SerializedName("vehicle_type")
    private String vehicleType;

    @SerializedName("license_plate")
    private String licensePlate;

    public DriverRegistration(String name, String phone, String vehicleType, String licensePlate) {
        this.name = name;
        this.phone = phone;
        this.vehicleType = vehicleType;
        this.licensePlate = licensePlate;
    }
}