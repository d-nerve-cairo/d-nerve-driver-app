package com.example.dnervecairo.api.requests;

import com.google.gson.annotations.SerializedName;

public class WithdrawalRequest {

    @SerializedName("amount")
    private double amount;

    @SerializedName("payment_method")
    private String paymentMethod;

    @SerializedName("account_number")
    private String accountNumber;

    public WithdrawalRequest(double amount, String paymentMethod, String accountNumber) {
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.accountNumber = accountNumber;
    }

    // Getters
    public double getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getAccountNumber() { return accountNumber; }
}