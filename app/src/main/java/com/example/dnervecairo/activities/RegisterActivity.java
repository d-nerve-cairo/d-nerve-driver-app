package com.example.dnervecairo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dnervecairo.MainActivity;
import com.example.dnervecairo.R;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.requests.DriverRegistration;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etVehicleType, etLicensePlate;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private PreferenceManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        prefManager = new PreferenceManager(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etVehicleType = findViewById(R.id.et_vehicle_type);
        etLicensePlate = findViewById(R.id.et_license_plate);
        btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        // Back button
        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(v -> finish());

        // Register button
        btnRegister.setOnClickListener(v -> attemptRegister());

        // Login link
        TextView tvLogin = findViewById(R.id.tv_login);
        tvLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String vehicleType = etVehicleType.getText().toString().trim();
        String licensePlate = etLicensePlate.getText().toString().trim();

        // Validation
        if (name.isEmpty()) {
            etName.setError("Name required");
            return;
        }
        if (name.length() < 2) {
            etName.setError("Name too short");
            return;
        }
        if (phone.isEmpty()) {
            etPhone.setError("Phone required");
            return;
        }
        if (phone.length() < 10) {
            etPhone.setError("Invalid phone number");
            return;
        }
        if (vehicleType.isEmpty()) {
            etVehicleType.setError("Vehicle type required");
            return;
        }
        if (licensePlate.isEmpty()) {
            etLicensePlate.setError("License plate required");
            return;
        }

        // Show loading
        setLoading(true);

        DriverRegistration request = new DriverRegistration(name, phone, vehicleType, licensePlate);

        ApiClient.getInstance().getApiService()
                .registerDriver(request)
                .enqueue(new Callback<DriverResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DriverResponse> call, @NonNull Response<DriverResponse> response) {
                        setLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            DriverResponse driver = response.body();

                            // Save driver info
                            prefManager.saveDriverId(driver.getDriverId());
                            prefManager.saveDriverName(name);
                            prefManager.setLoggedIn(true);

                            Toast.makeText(RegisterActivity.this, "Welcome to D-Nerve, " + name + "!", Toast.LENGTH_SHORT).show();
                            goToMain();
                        } else {
                            // Show specific error message
                            String errorMsg = "Registration failed";
                            if (response.code() == 400) {
                                errorMsg = "Phone number already registered. Please login instead.";
                            }
                            Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DriverResponse> call, @NonNull Throwable t) {
                        setLoading(false);
                        Toast.makeText(RegisterActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}