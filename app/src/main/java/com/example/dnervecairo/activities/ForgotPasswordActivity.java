package com.example.dnervecairo.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dnervecairo.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilCode, tilNewPassword;
    private TextInputEditText etEmail, etCode, etNewPassword;
    private MaterialButton btnSendCode, btnResetPassword;
    private ProgressBar progressBar;
    private View layoutStep1, layoutStep2;

    private boolean codeSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        setupListeners();
    }

    private void initViews() {
        // Back button
        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(v -> finish());

        // Step 1: Email
        layoutStep1 = findViewById(R.id.layout_step1);
        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);
        btnSendCode = findViewById(R.id.btn_send_code);

        // Step 2: Code & New Password
        layoutStep2 = findViewById(R.id.layout_step2);
        tilCode = findViewById(R.id.til_code);
        etCode = findViewById(R.id.et_code);
        tilNewPassword = findViewById(R.id.til_new_password);
        etNewPassword = findViewById(R.id.et_new_password);
        btnResetPassword = findViewById(R.id.btn_reset_password);

        progressBar = findViewById(R.id.progress_bar);

        // Initially show step 1
        layoutStep1.setVisibility(View.VISIBLE);
        layoutStep2.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnSendCode.setOnClickListener(v -> sendResetCode());
        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void sendResetCode() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email");
            return;
        }

        // Show loading
        setLoading(true);

        // Simulate API call (in production, call actual API)
        // For now, just show step 2 after a delay
        btnSendCode.postDelayed(() -> {
            setLoading(false);
            codeSent = true;
            
            // Show step 2
            layoutStep1.setVisibility(View.GONE);
            layoutStep2.setVisibility(View.VISIBLE);
            
            Toast.makeText(this, 
                "If this email is registered, you'll receive a reset code.\nFor testing, use: 123456", 
                Toast.LENGTH_LONG).show();
        }, 1500);
    }

    private void resetPassword() {
        String code = etCode.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString();

        boolean isValid = true;

        if (code.isEmpty()) {
            tilCode.setError("Enter the reset code");
            isValid = false;
        } else if (code.length() != 6) {
            tilCode.setError("Code must be 6 digits");
            isValid = false;
        }

        if (newPassword.isEmpty()) {
            tilNewPassword.setError("Enter new password");
            isValid = false;
        } else if (newPassword.length() < 6) {
            tilNewPassword.setError("Minimum 6 characters");
            isValid = false;
        } else if (!newPassword.matches(".*\\d.*")) {
            tilNewPassword.setError("Must contain at least one number");
            isValid = false;
        }

        if (!isValid) return;

        // Show loading
        setLoading(true);

        // Simulate API call
        btnResetPassword.postDelayed(() -> {
            setLoading(false);
            
            // For testing, accept code 123456
            if (code.equals("123456")) {
                Toast.makeText(this, "Password reset successfully! Please login.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                tilCode.setError("Invalid or expired code");
            }
        }, 1500);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSendCode.setEnabled(!loading);
        btnResetPassword.setEnabled(!loading);
    }
}
