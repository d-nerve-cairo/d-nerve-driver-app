package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WithdrawalHistoryResponse {

    @SerializedName("withdrawals")
    private List<WithdrawalResponse> withdrawals;

    @SerializedName("total")
    private int total;

    public List<WithdrawalResponse> getWithdrawals() {
        return withdrawals;
    }

    public int getTotal() {
        return total;
    }
}