package com.example.dnervecairo.api.requests;

import com.google.gson.annotations.SerializedName;

public class UpdateDriverRequest {

    @SerializedName("name")
    private String name;

    @SerializedName("vehicle_type")
    private String vehicleType;

    @SerializedName("license_plate")
    private String licensePlate;

    @SerializedName("email")
    private String email;

    @SerializedName("emergency_contact")
    private String emergencyContact;

    @SerializedName("bio")
    private String bio;

    // Constructor for basic fields (backwards compatible)
    public UpdateDriverRequest(String name, String vehicleType, String licensePlate) {
        this.name = name;
        this.vehicleType = vehicleType;
        this.licensePlate = licensePlate;
    }

    // Full constructor
    public UpdateDriverRequest(String name, String vehicleType, String licensePlate, 
                               String email, String emergencyContact, String bio) {
        this.name = name;
        this.vehicleType = vehicleType;
        this.licensePlate = licensePlate;
        this.email = email;
        this.emergencyContact = emergencyContact;
        this.bio = bio;
    }

    // Getters
    public String getName() { return name; }
    public String getVehicleType() { return vehicleType; }
    public String getLicensePlate() { return licensePlate; }
    public String getEmail() { return email; }
    public String getEmergencyContact() { return emergencyContact; }
    public String getBio() { return bio; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public void setEmail(String email) { this.email = email; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public void setBio(String bio) { this.bio = bio; }
}
