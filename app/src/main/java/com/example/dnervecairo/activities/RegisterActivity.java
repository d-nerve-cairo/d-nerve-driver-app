package com.example.dnervecairo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.example.dnervecairo.api.responses.LoginResponse;
import com.example.dnervecairo.api.responses.RegisterResponse;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    // Views
    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputLayout tilVehicleType, tilLicensePlate;
    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private TextInputEditText etLicensePlate;
    private AutoCompleteTextView actvCountryCode, actvVehicleType;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    
    private PreferenceManager prefManager;

    // Simplified country codes - Flag + Code only
    private static final String[] COUNTRY_CODES = {
            "🇪🇬 +20",
            "🇸🇦 +966",
            "🇦🇪 +971",
            "🇯🇴 +962",
            "🇱🇧 +961",
            "🇮🇶 +964",
            "🇰🇼 +965",
            "🇶🇦 +974",
            "🇧🇭 +973",
            "🇴🇲 +968",
            "🇾🇪 +967",
            "🇸🇾 +963",
            "🇵🇸 +970",
            "🇱🇾 +218",
            "🇸🇩 +249",
            "🇲🇦 +212",
            "🇹🇳 +216",
            "🇩🇿 +213"
    };

    private static final String[] VEHICLE_TYPES = {
            "Microbus",
            "Minibus",
            "Bus",
            "Van"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        prefManager = new PreferenceManager(this);

        initViews();
        setupDropdowns();
        setupListeners();
    }

    private void initViews() {
        // Input layouts
        tilName = findViewById(R.id.til_name);
        tilEmail = findViewById(R.id.til_email);
        tilPhone = findViewById(R.id.til_phone);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        tilVehicleType = findViewById(R.id.til_vehicle_type);
        tilLicensePlate = findViewById(R.id.til_license_plate);

        // Edit texts
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etLicensePlate = findViewById(R.id.et_license_plate);

        // Dropdowns
        actvCountryCode = findViewById(R.id.actv_country_code);
        actvVehicleType = findViewById(R.id.actv_vehicle_type);

        // Button & Progress
        btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupDropdowns() {
        // Country code dropdown
        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, COUNTRY_CODES);
        actvCountryCode.setAdapter(countryAdapter);
        actvCountryCode.setText(COUNTRY_CODES[0], false); // Default: Egypt 🇪🇬 +20

        // Vehicle type dropdown
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, VEHICLE_TYPES);
        actvVehicleType.setAdapter(vehicleAdapter);
        actvVehicleType.setText(VEHICLE_TYPES[0], false); // Default: Microbus
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

        // Clear errors on focus
        setupErrorClearListeners();
    }

    private void setupErrorClearListeners() {
        etName.setOnFocusChangeListener((v, focus) -> { if (focus) tilName.setError(null); });
        etEmail.setOnFocusChangeListener((v, focus) -> { if (focus) tilEmail.setError(null); });
        etPhone.setOnFocusChangeListener((v, focus) -> { if (focus) tilPhone.setError(null); });
        etPassword.setOnFocusChangeListener((v, focus) -> { if (focus) tilPassword.setError(null); });
        etConfirmPassword.setOnFocusChangeListener((v, focus) -> { if (focus) tilConfirmPassword.setError(null); });
        etLicensePlate.setOnFocusChangeListener((v, focus) -> { if (focus) tilLicensePlate.setError(null); });
    }

    private void attemptRegister() {
        // Clear previous errors
        clearErrors();

        // Get values
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String vehicleType = actvVehicleType.getText().toString();
        String licensePlate = etLicensePlate.getText().toString().trim();
        String countryCode = extractCountryCode(actvCountryCode.getText().toString());

        // Validation
        boolean isValid = true;

        if (name.isEmpty()) {
            tilName.setError(getString(R.string.error_name_required));
            isValid = false;
        } else if (name.length() < 2) {
            tilName.setError(getString(R.string.error_name_short));
            isValid = false;
        }

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_required));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            isValid = false;
        }

        if (phone.isEmpty()) {
            tilPhone.setError(getString(R.string.error_phone_required));
            isValid = false;
        } else if (phone.length() < 9) {
            tilPhone.setError(getString(R.string.error_phone_short));
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_password_required));
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_short));
            isValid = false;
        } else if (!password.matches(".*\\d.*")) {
            tilPassword.setError(getString(R.string.error_password_no_number));
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError(getString(R.string.error_confirm_password));
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            isValid = false;
        }

        if (licensePlate.isEmpty()) {
            tilLicensePlate.setError(getString(R.string.error_license_required));
            isValid = false;
        }

        if (!isValid) return;

        // Show loading
        setLoading(true);

        // Create registration request
        DriverRegistration request = new DriverRegistration(
                name, email, phone, password, vehicleType, licensePlate, countryCode
        );

        ApiClient.getInstance().getApiService()
                .registerDriver(request)
                .enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                        setLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            LoginResponse.DriverData driver = response.body().getDriver();

                            // Save driver info
                            prefManager.saveDriverId(driver.getDriverId());
                            prefManager.saveDriverName(driver.getName());
                            prefManager.setLoggedIn(true);
                            
                            // Save additional data
                            prefManager.saveDriverData(
                                driver.getName(),
                                driver.getTotalPoints(),
                                driver.getTier(),
                                driver.getTripsCompleted(),
                                driver.getQualityAvg(),
                                driver.getCurrentStreak()
                            );

                            Toast.makeText(RegisterActivity.this,
                                    getString(R.string.auth_register_success, driver.getName()),
                                    Toast.LENGTH_SHORT).show();
                            goToMain();
                        } else {
                            // Parse error
                            String errorMsg = getString(R.string.error_registration_failed);
                            if (response.code() == 400) {
                                // Try to parse specific error
                                try {
                                    String errorBody = response.errorBody().string();
                                    if (errorBody.contains("Email already")) {
                                        tilEmail.setError(getString(R.string.error_email_exists));
                                    } else if (errorBody.contains("Phone")) {
                                        tilPhone.setError(getString(R.string.error_phone_exists));
                                    } else {
                                        Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                        setLoading(false);
                        Toast.makeText(RegisterActivity.this,
                                getString(R.string.error_network),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void clearErrors() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilLicensePlate.setError(null);
    }

    private String extractCountryCode(String countryString) {
        // Extract "+XX" from "🇪🇬 +20"
        try {
            int start = countryString.indexOf('+');
            if (start != -1) {
                return countryString.substring(start).trim();
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return "+20"; // Default to Egypt
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "" : getString(R.string.auth_create_account));
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
