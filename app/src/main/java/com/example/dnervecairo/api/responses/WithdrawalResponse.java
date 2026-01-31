package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

public class WithdrawalResponse {

    @SerializedName("withdrawal_id")
    private String withdrawalId;

    @SerializedName("driver_id")
    private String driverId;

    @SerializedName("amount")
    private double amount;

    @SerializedName("payment_method")
    private String paymentMethod;

    @SerializedName("account_number")
    private String accountNumber;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    // Getters
    public String getWithdrawalId() { return withdrawalId; }
    public String getDriverId() { return driverId; }
    public double getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getAccountNumber() { return accountNumber; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}