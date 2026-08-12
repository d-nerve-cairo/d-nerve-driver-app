package com.example.dnervecairo.api.requests;

import com.google.gson.annotations.SerializedName;

public class DriverRegistration {

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("password")
    private String password;

    @SerializedName("vehicle_type")
    private String vehicleType;

    @SerializedName("license_plate")
    private String licensePlate;

    @SerializedName("country_code")
    private String countryCode;

    // Full constructor with all fields
    public DriverRegistration(String name, String email, String phone, String password,
                              String vehicleType, String licensePlate, String countryCode) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.vehicleType = vehicleType;
        this.licensePlate = licensePlate;
        this.countryCode = countryCode;
    }

    // Legacy constructor for backwards compatibility
    public DriverRegistration(String name, String phone, String vehicleType, String licensePlate) {
        this.name = name;
        this.phone = phone;
        this.vehicleType = vehicleType;
        this.licensePlate = licensePlate;
        this.email = null;
        this.password = null;
        this.countryCode = "+20";
    }

    // Getters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public String getVehicleType() { return vehicleType; }
    public String getLicensePlate() { return licensePlate; }
    public String getCountryCode() { return countryCode; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setPassword(String password) { this.password = password; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
}
