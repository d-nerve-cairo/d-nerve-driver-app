package com.example.dnervecairo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dnervecairo.MainActivity;
import com.example.dnervecairo.R;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.api.responses.DriversListResponse;  // ADD THIS
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etPhone;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private PreferenceManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefManager = new PreferenceManager(this);

        // Check if already logged in
        if (prefManager.isLoggedIn()) {
            goToMain();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        etPhone = findViewById(R.id.et_phone);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        TextView tvRegister = findViewById(R.id.tv_register);
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void attemptLogin() {
        String phone = etPhone.getText().toString().trim();

        // Validation
        if (phone.isEmpty()) {
            etPhone.setError("Phone required");
            return;
        }
        if (phone.length() < 10) {
            etPhone.setError("Invalid phone number");
            return;
        }

        // Show loading
        setLoading(true);

        // Search for driver by phone using the list endpoint
        ApiClient.getInstance().getApiService()
                .getDrivers()
                .enqueue(new Callback<DriversListResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DriversListResponse> call, @NonNull Response<DriversListResponse> response) {
                        setLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            // Find driver with matching phone
                            DriverResponse foundDriver = null;
                            for (DriverResponse driver : response.body().getDrivers()) {
                                if (phone.equals(driver.getPhone())) {
                                    foundDriver = driver;
                                    break;
                                }
                            }

                            if (foundDriver != null) {
                                // Save driver info
                                prefManager.saveDriverId(foundDriver.getDriverId());
                                prefManager.saveDriverName(foundDriver.getName());
                                prefManager.setLoggedIn(true);

                                Toast.makeText(LoginActivity.this, "Welcome back, " + foundDriver.getName() + "!", Toast.LENGTH_SHORT).show();
                                goToMain();
                            } else {
                                Toast.makeText(LoginActivity.this, "Phone not registered. Please register first.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Login failed. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DriversListResponse> call, @NonNull Throwable t) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}