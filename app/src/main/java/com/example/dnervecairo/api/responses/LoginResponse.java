package com.example.dnervecairo.api.responses;

import com.example.dnervecairo.models.Driver;
import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("token")
    private String token;

    @SerializedName("driver")
    private Driver driver;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public String getToken() { return token; }
    public Driver getDriver() { return driver; }
    public String getMessage() { return message; }
}