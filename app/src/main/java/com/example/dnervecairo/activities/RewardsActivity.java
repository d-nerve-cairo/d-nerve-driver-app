package com.example.dnervecairo.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.adapters.WithdrawalAdapter;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.requests.WithdrawalRequest;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.api.responses.WithdrawalResponse;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.example.dnervecairo.api.responses.WithdrawalHistoryResponse;
import java.util.List;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RewardsActivity extends AppCompatActivity {

    private TextView tvBalance, tvPointsInfo, tvTotalEarned, tvTotalWithdrawn;
    private MaterialButton btnWithdraw;
    private RecyclerView rvWithdrawals;
    private LinearLayout emptyState;
    private FrameLayout loadingOverlay;

    private WithdrawalAdapter adapter;
    private PreferenceManager prefManager;
    private String driverId;

    private double availableBalance = 0;
    private int totalPoints = 0;

    private final String[] paymentMethods = {
            "Vodafone Cash",
            "Orange Money",
            "Etisalat Cash",
            "Fawry",
            "Bank Transfer"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rewards);

        prefManager = new PreferenceManager(this);
        driverId = prefManager.getDriverId();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupButtons();
        loadRewardsData();
    }

    private void initViews() {
        tvBalance = findViewById(R.id.tv_balance);
        tvPointsInfo = findViewById(R.id.tv_points_info);
        tvTotalEarned = findViewById(R.id.tv_total_earned);
        tvTotalWithdrawn = findViewById(R.id.tv_total_withdrawn);
        btnWithdraw = findViewById(R.id.btn_withdraw);
        rvWithdrawals = findViewById(R.id.rv_withdrawals);
        emptyState = findViewById(R.id.empty_state);
        loadingOverlay = findViewById(R.id.loading_overlay);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new WithdrawalAdapter();
        rvWithdrawals.setLayoutManager(new LinearLayoutManager(this));
        rvWithdrawals.setAdapter(adapter);
    }

    private void setupButtons() {
        btnWithdraw.setOnClickListener(v -> showWithdrawDialog());
    }

    private void loadRewardsData() {
        showLoading(true);

        // Load driver data
        ApiClient.getInstance().getApiService()
                .getDriver(driverId)
                .enqueue(new Callback<DriverResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DriverResponse> call,
                                           @NonNull Response<DriverResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            updateUI(response.body());
                        } else {
                            showToast("Failed to load rewards");
                        }
                        // Load withdrawal history after driver data
                        loadWithdrawalHistory();
                    }

                    @Override
                    public void onFailure(@NonNull Call<DriverResponse> call, @NonNull Throwable t) {
                        showLoading(false);
                        showToast("Network error");
                    }
                });
    }


    private void loadWithdrawalHistory() {
        ApiClient.getInstance().getApiService()
                .getWithdrawalHistory(driverId)
                .enqueue(new Callback<WithdrawalHistoryResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WithdrawalHistoryResponse> call,
                                           @NonNull Response<WithdrawalHistoryResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            List<WithdrawalResponse> withdrawals = response.body().getWithdrawals();

                            if (withdrawals != null && !withdrawals.isEmpty()) {
                                adapter.setWithdrawals(withdrawals);
                                rvWithdrawals.setVisibility(View.VISIBLE);
                                emptyState.setVisibility(View.GONE);

                                // Calculate total withdrawn
                                double totalWithdrawn = 0;
                                for (WithdrawalResponse w : withdrawals) {
                                    totalWithdrawn += w.getAmount();
                                }
                                tvTotalWithdrawn.setText(String.format(Locale.US, "%.2f", totalWithdrawn));
                            } else {
                                rvWithdrawals.setVisibility(View.GONE);
                                emptyState.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WithdrawalHistoryResponse> call, @NonNull Throwable t) {
                        showLoading(false);
                        // Silently fail - just show empty state
                        rvWithdrawals.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void updateUI(DriverResponse driver) {
        totalPoints = driver.getTotalPoints();

        // Calculate rewards (10 points = 1 EGP)
        double totalEarned = totalPoints / 10.0;

        // For now, assume no withdrawals (would need API for history)
        double totalWithdrawn = 0;
        availableBalance = totalEarned - totalWithdrawn;

        tvBalance.setText(String.format(Locale.US, "%.2f", availableBalance));
        tvPointsInfo.setText(String.format(Locale.US, "%d points = %.2f EGP", totalPoints, totalEarned));
        tvTotalEarned.setText(String.format(Locale.US, "%.2f", totalEarned));
        tvTotalWithdrawn.setText(String.format(Locale.US, "%.2f", totalWithdrawn));

        // Enable/disable withdraw button
        btnWithdraw.setEnabled(availableBalance >= 5);
        if (availableBalance < 5) {
            btnWithdraw.setText("Min 5 EGP required");
        } else {
            btnWithdraw.setText("Withdraw");
        }
    }

    private void showWithdrawDialog() {
        if (availableBalance < 5) {
            showToast("Minimum withdrawal is 5 EGP");
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_withdraw, null);

        TextView tvAvailable = dialogView.findViewById(R.id.tv_available_balance);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        AutoCompleteTextView dropdownMethod = dialogView.findViewById(R.id.dropdown_payment_method);
        TextInputEditText etAccount = dialogView.findViewById(R.id.et_account);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        tvAvailable.setText(String.format(Locale.US, "Available: %.2f EGP", availableBalance));

        // Setup payment method dropdown
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, paymentMethods);
        dropdownMethod.setAdapter(methodAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String method = dropdownMethod.getText().toString().trim();
            String account = etAccount.getText().toString().trim();

            // Validation
            if (amountStr.isEmpty()) {
                etAmount.setError("Enter amount");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount");
                return;
            }

            if (amount < 5) {
                etAmount.setError("Minimum is 5 EGP");
                return;
            }

            if (amount > availableBalance) {
                etAmount.setError("Exceeds available balance");
                return;
            }

            if (method.isEmpty()) {
                showToast("Select payment method");
                return;
            }

            if (account.isEmpty()) {
                etAccount.setError("Enter account number");
                return;
            }

            dialog.dismiss();
            processWithdrawal(amount, method, account);
        });

        dialog.show();
    }

    private void processWithdrawal(double amount, String method, String account) {
        showLoading(true);

        WithdrawalRequest request = new WithdrawalRequest(amount, method, account);

        ApiClient.getInstance().getApiService()
                .requestWithdrawal(driverId, request)
                .enqueue(new Callback<WithdrawalResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WithdrawalResponse> call,
                                           @NonNull Response<WithdrawalResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            showToast("Withdrawal request submitted!");
                            loadRewardsData(); // Refresh data
                        } else {
                            showToast("Withdrawal failed. Try again.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WithdrawalResponse> call, @NonNull Throwable t) {
                        showLoading(false);
                        showToast("Network error: " + t.getMessage());
                    }
                });
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}