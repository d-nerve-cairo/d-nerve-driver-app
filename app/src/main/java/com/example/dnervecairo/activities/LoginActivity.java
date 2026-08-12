package com.example.dnervecairo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dnervecairo.MainActivity;
import com.example.dnervecairo.R;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.requests.LoginRequest;
import com.example.dnervecairo.api.responses.LoginResponse;
import com.example.dnervecairo.services.DNerveFirebaseMessagingService;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private CheckBox cbRememberMe;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvForgotPassword, tvRegister;
    
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
        loadSavedCredentials();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        cbRememberMe = findViewById(R.id.cb_remember_me);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvRegister = findViewById(R.id.tv_register);
    }

    private void setupListeners() {
        // Login button
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Forgot password
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Register link
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        // Clear errors on focus
        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilEmail.setError(null);
        });
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tilPassword.setError(null);
        });
    }

    private void loadSavedCredentials() {
        // Load remembered email if exists
        String savedEmail = prefManager.getSavedEmail();
        if (savedEmail != null && !savedEmail.isEmpty()) {
            etEmail.setText(savedEmail);
            cbRememberMe.setChecked(true);
        }
    }

    private void attemptLogin() {
        // Clear previous errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Validation
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_required));
            etEmail.requestFocus();
            return;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_password_required));
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_short));
            etPassword.requestFocus();
            return;
        }

        // Show loading
        setLoading(true);

        // Create login request
        LoginRequest request = LoginRequest.withEmail(email, password);

        ApiClient.getInstance().getApiService()
                .loginDriver(request)
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
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

                            // Remember email if checked
                            if (cbRememberMe.isChecked()) {
                                prefManager.saveEmail(email);
                            } else {
                                prefManager.clearSavedEmail();
                            }

                            // P5d: Upload FCM token to backend now that we have a driver_id
                            String cachedToken = prefManager.getFcmToken();
                            if (cachedToken != null) {
                                DNerveFirebaseMessagingService.uploadTokenToServer(
                                        driver.getDriverId(), cachedToken);
                            }

                            Toast.makeText(LoginActivity.this,
                                getString(R.string.auth_welcome_message, driver.getName()),
                                Toast.LENGTH_SHORT).show();
                            goToMain();
                        } else {
                            // Parse error message
                            String errorMsg = getString(R.string.error_login_failed);
                            if (response.code() == 401) {
                                errorMsg = getString(R.string.error_invalid_credentials);
                            } else if (response.code() == 404) {
                                errorMsg = getString(R.string.error_account_not_found);
                            }
                            tilPassword.setError(errorMsg);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, 
                            getString(R.string.error_network), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "" : getString(R.string.auth_login));
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
