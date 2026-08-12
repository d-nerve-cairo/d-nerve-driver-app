package com.example.dnervecairo.api.requests;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("password")
    private String password;

    // Login with email
    public LoginRequest(String email, String password, boolean isEmail) {
        if (isEmail) {
            this.email = email;
            this.phone = null;
        } else {
            this.email = null;
            this.phone = email;
        }
        this.password = password;
    }

    // Static factory methods for clarity
    public static LoginRequest withEmail(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.email = email;
        request.password = password;
        return request;
    }

    public static LoginRequest withPhone(String phone, String password) {
        LoginRequest request = new LoginRequest();
        request.phone = phone;
        request.password = password;
        return request;
    }

    private LoginRequest() {}

    // Getters
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
}
