package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("driver")
    private LoginResponse.DriverData driver;

    public String getMessage() { return message; }
    public LoginResponse.DriverData getDriver() { return driver; }
}
