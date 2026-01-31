package com.example.dnervecairo.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dnervecairo.R;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.requests.UpdateDriverRequest;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etLicensePlate;
    private AutoCompleteTextView dropdownVehicleType;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private PreferenceManager prefManager;
    private String driverId;

    private final String[] vehicleTypes = {"Microbus", "Minibus", "Bus"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        prefManager = new PreferenceManager(this);
        driverId = prefManager.getDriverId();

        initViews();
        setupToolbar();
        setupVehicleTypeDropdown();
        setupButtons();
        loadCurrentProfile();
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etLicensePlate = findViewById(R.id.et_license_plate);
        dropdownVehicleType = findViewById(R.id.dropdown_vehicle_type);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupVehicleTypeDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                vehicleTypes
        );
        dropdownVehicleType.setAdapter(adapter);
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> saveProfile());

        findViewById(R.id.btn_change_photo).setOnClickListener(v -> {
            Toast.makeText(this, "Photo upload coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCurrentProfile() {
        showLoading(true);

        ApiClient.getInstance().getApiService()
                .getDriver(driverId)
                .enqueue(new Callback<DriverResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DriverResponse> call,
                                           @NonNull Response<DriverResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            populateFields(response.body());
                        } else {
                            showToast("Failed to load profile");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DriverResponse> call, @NonNull Throwable t) {
                        showLoading(false);
                        showToast("Network error");
                    }
                });
    }

    private void populateFields(DriverResponse driver) {

        // Debug logging
        android.util.Log.d("EditProfile", "Name: " + driver.getName());
        android.util.Log.d("EditProfile", "Phone: " + driver.getPhone());
        android.util.Log.d("EditProfile", "Vehicle: " + driver.getVehicleType());
        android.util.Log.d("EditProfile", "Plate: " + driver.getLicensePlate());


        etName.setText(driver.getName());
        etPhone.setText(driver.getPhone());
        etLicensePlate.setText(driver.getLicensePlate());

        // Set vehicle type dropdown
        String vehicleType = driver.getVehicleType();
        if (vehicleType != null) {
            dropdownVehicleType.setText(vehicleType, false);
        }
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String vehicleType = dropdownVehicleType.getText().toString().trim();
        String licensePlate = etLicensePlate.getText().toString().trim();

        // Validation
        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (vehicleType.isEmpty()) {
            showToast("Please select vehicle type");
            return;
        }

        if (licensePlate.isEmpty()) {
            etLicensePlate.setError("License plate is required");
            etLicensePlate.requestFocus();
            return;
        }

        // Save to server
        showLoading(true);
        btnSave.setEnabled(false);

        UpdateDriverRequest request = new UpdateDriverRequest(name, vehicleType, licensePlate);

        ApiClient.getInstance().getApiService()
                .updateDriver(driverId, request)
                .enqueue(new Callback<DriverResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DriverResponse> call,
                                           @NonNull Response<DriverResponse> response) {
                        showLoading(false);
                        btnSave.setEnabled(true);

                        if (response.isSuccessful()) {
                            showToast("Profile updated successfully");
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            showToast("Failed to update profile");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DriverResponse> call, @NonNull Throwable t) {
                        showLoading(false);
                        btnSave.setEnabled(true);
                        showToast("Network error: " + t.getMessage());
                    }
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}