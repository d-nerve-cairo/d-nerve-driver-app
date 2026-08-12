package com.example.dnervecairo.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.dnervecairo.R;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.requests.UpdateDriverRequest;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;

    // Views
    private TextInputEditText etName, etPhone, etEmail, etLicensePlate, etEmergencyContact, etBio;
    private TextInputLayout tilName, tilEmail, tilLicensePlate, tilEmergencyContact, tilBio;
    private AutoCompleteTextView dropdownVehicleType;
    private MaterialButton btnSave;
    private View progressOverlay;
    private ImageView ivAvatar;
    
    // Cards for animation
    private MaterialCardView cardPhoto, cardPersonal, cardVehicle, cardEmergency;

    // Data
    private PreferenceManager prefManager;
    private String driverId;
    private Uri selectedPhotoUri;
    private Uri cameraImageUri;
    private boolean hasUnsavedChanges = false;
    private boolean isFirstLoad = true;
    
    // Original values for change detection
    private String originalName = "";
    private String originalEmail = "";
    private String originalVehicleType = "";
    private String originalLicensePlate = "";
    private String originalEmergencyContact = "";
    private String originalBio = "";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final String[] vehicleTypes = {"Microbus", "Minibus", "Bus"};

    // Activity result launchers
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        setProfilePhoto(imageUri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                    setProfilePhoto(cameraImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        prefManager = new PreferenceManager(this);
        driverId = prefManager.getDriverId();

        initViews();
        setupToolbar();
        setupVehicleTypeDropdown();
        setupTextWatchers();
        setupButtons();
        setupBackPressHandler();
        prepareEntranceAnimations();
        loadCurrentProfile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void initViews() {
        // Text inputs
        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etEmail = findViewById(R.id.et_email);
        etLicensePlate = findViewById(R.id.et_license_plate);
        etEmergencyContact = findViewById(R.id.et_emergency_contact);
        etBio = findViewById(R.id.et_bio);
        
        // Input layouts
        tilName = findViewById(R.id.til_name);
        tilEmail = findViewById(R.id.til_email);
        tilLicensePlate = findViewById(R.id.til_license_plate);
        tilEmergencyContact = findViewById(R.id.til_emergency_contact);
        tilBio = findViewById(R.id.til_bio);
        
        // Other views
        dropdownVehicleType = findViewById(R.id.dropdown_vehicle_type);
        btnSave = findViewById(R.id.btn_save);
        progressOverlay = findViewById(R.id.progress_overlay);
        ivAvatar = findViewById(R.id.iv_avatar);
        
        // Cards
        cardPhoto = findViewById(R.id.card_photo);
        cardPersonal = findViewById(R.id.card_personal);
        cardVehicle = findViewById(R.id.card_vehicle);
        cardEmergency = findViewById(R.id.card_emergency);
    }

    private void prepareEntranceAnimations() {
        View[] cards = {cardPhoto, cardPersonal, cardVehicle, cardEmergency, btnSave};
        for (View card : cards) {
            if (card != null) {
                card.setAlpha(0f);
                card.setTranslationY(40f);
            }
        }
    }

    private void playEntranceAnimations() {
        if (!isFirstLoad) return;
        isFirstLoad = false;

        View[] cards = {cardPhoto, cardPersonal, cardVehicle, cardEmergency, btnSave};
        
        for (int i = 0; i < cards.length; i++) {
            View card = cards[i];
            if (card != null) {
                final int delay = i * 100;
                handler.postDelayed(() -> {
                    card.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                }, delay);
            }
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            animateClick(v);
            handler.postDelayed(this::handleBackPress, 100);
        });
    }

    private void setupVehicleTypeDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                vehicleTypes
        );
        dropdownVehicleType.setAdapter(adapter);
        
        dropdownVehicleType.setOnItemClickListener((parent, view, position, id) -> {
            checkForChanges();
        });
    }

    private void setupTextWatchers() {
        TextWatcher changeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                checkForChanges();
            }
        };

        if (etName != null) etName.addTextChangedListener(changeWatcher);
        if (etEmail != null) etEmail.addTextChangedListener(changeWatcher);
        if (etLicensePlate != null) etLicensePlate.addTextChangedListener(changeWatcher);
        if (etEmergencyContact != null) etEmergencyContact.addTextChangedListener(changeWatcher);
        if (etBio != null) etBio.addTextChangedListener(changeWatcher);
        
        // Character counter for bio
        if (etBio != null && tilBio != null) {
            tilBio.setCounterEnabled(true);
            tilBio.setCounterMaxLength(200);
        }
        
        // Character counter for name
        if (tilName != null) {
            tilName.setCounterEnabled(true);
            tilName.setCounterMaxLength(50);
        }
    }

    private void checkForChanges() {
        String currentName = getText(etName);
        String currentEmail = getText(etEmail);
        String currentVehicleType = dropdownVehicleType.getText().toString().trim();
        String currentLicensePlate = getText(etLicensePlate);
        String currentEmergencyContact = getText(etEmergencyContact);
        String currentBio = getText(etBio);

        hasUnsavedChanges = !currentName.equals(originalName) ||
                           !currentEmail.equals(originalEmail) ||
                           !currentVehicleType.equals(originalVehicleType) ||
                           !currentLicensePlate.equals(originalLicensePlate) ||
                           !currentEmergencyContact.equals(originalEmergencyContact) ||
                           !currentBio.equals(originalBio) ||
                           selectedPhotoUri != null;
    }

    private String getText(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> {
            animateClick(v);
            handler.postDelayed(this::saveProfile, 100);
        });

        View btnChangePhoto = findViewById(R.id.btn_change_photo);
        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> {
                animateClick(v);
                handler.postDelayed(this::showPhotoPickerDialog, 100);
            });
        }
        
        // Avatar click also opens photo picker
        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(v -> {
                animateClick(v);
                handler.postDelayed(this::showPhotoPickerDialog, 100);
            });
        }
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    private void handleBackPress() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog();
        } else {
            finish();
        }
    }

    private void showUnsavedChangesDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Are you sure you want to leave?")
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton("Discard", (dialog, which) -> finish())
                .setNegativeButton("Keep Editing", null)
                .setNeutralButton("Save", (dialog, which) -> saveProfile())
                .show();
    }

    private void showPhotoPickerDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_image_source, null);

        View btnCamera = view.findViewById(R.id.btn_camera);
        View btnGallery = view.findViewById(R.id.btn_gallery);

        btnCamera.setOnClickListener(v -> {
            animateClick(v);
            handler.postDelayed(() -> {
                dialog.dismiss();
                openCamera();
            }, 100);
        });

        btnGallery.setOnClickListener(v -> {
            animateClick(v);
            handler.postDelayed(() -> {
                dialog.dismiss();
                openGallery();
            }, 100);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = new File(getCacheDir(), "profile_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        cameraLauncher.launch(intent);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                showToast("Camera permission required");
            }
        }
    }

    private void setProfilePhoto(Uri uri) {
        selectedPhotoUri = uri;
        
        // Show preview
        if (ivAvatar != null) {
            ivAvatar.setImageURI(uri);
            ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            
            // Animate the change
            ivAvatar.setAlpha(0f);
            ivAvatar.animate().alpha(1f).setDuration(300).start();
        }
        
        checkForChanges();
        showToast("Photo selected - Save to apply changes");
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
                            playEntranceAnimations();
                        } else {
                            showToast("Failed to load profile");
                            playEntranceAnimations();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DriverResponse> call, @NonNull Throwable t) {
                        showLoading(false);
                        showToast("Network error");
                        playEntranceAnimations();
                    }
                });
    }

    private void populateFields(DriverResponse driver) {
        // Store original values
        originalName = driver.getName() != null ? driver.getName() : "";
        originalEmail = driver.getEmail() != null ? driver.getEmail() : "";
        originalVehicleType = driver.getVehicleType() != null ? driver.getVehicleType() : "";
        originalLicensePlate = driver.getLicensePlate() != null ? driver.getLicensePlate() : "";
        originalEmergencyContact = driver.getEmergencyContact() != null ? driver.getEmergencyContact() : "";
        originalBio = driver.getBio() != null ? driver.getBio() : "";

        // Populate fields
        if (etName != null) etName.setText(originalName);
        if (etPhone != null) etPhone.setText(driver.getPhone());
        if (etEmail != null) etEmail.setText(originalEmail);
        if (etLicensePlate != null) etLicensePlate.setText(originalLicensePlate);
        if (etEmergencyContact != null) etEmergencyContact.setText(originalEmergencyContact);
        if (etBio != null) etBio.setText(originalBio);

        // Set vehicle type dropdown
        if (!originalVehicleType.isEmpty()) {
            dropdownVehicleType.setText(originalVehicleType, false);
        }

        hasUnsavedChanges = false;
    }

    private void saveProfile() {
        // Clear previous errors
        clearErrors();

        // Get values
        String name = getText(etName);
        String email = getText(etEmail);
        String vehicleType = dropdownVehicleType.getText().toString().trim();
        String licensePlate = getText(etLicensePlate);
        String emergencyContact = getText(etEmergencyContact);
        String bio = getText(etBio);

        // Validation
        boolean isValid = true;

        if (name.isEmpty()) {
            tilName.setError("Name is required");
            if (isValid) etName.requestFocus();
            isValid = false;
        } else if (name.length() < 2) {
            tilName.setError("Name must be at least 2 characters");
            if (isValid) etName.requestFocus();
            isValid = false;
        }

        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email address");
            if (isValid) etEmail.requestFocus();
            isValid = false;
        }

        if (vehicleType.isEmpty()) {
            showToast("Please select vehicle type");
            isValid = false;
        }

        if (licensePlate.isEmpty()) {
            tilLicensePlate.setError("License plate is required");
            if (isValid) etLicensePlate.requestFocus();
            isValid = false;
        }

        if (!emergencyContact.isEmpty() && emergencyContact.length() < 8) {
            tilEmergencyContact.setError("Invalid phone number");
            if (isValid) etEmergencyContact.requestFocus();
            isValid = false;
        }

        if (!isValid) return;

        // Save to server
        showLoading(true);
        btnSave.setEnabled(false);

        UpdateDriverRequest request = new UpdateDriverRequest(name, vehicleType, licensePlate);
        request.setEmail(email);
        request.setEmergencyContact(emergencyContact);
        request.setBio(bio);

        ApiClient.getInstance().getApiService()
                .updateDriver(driverId, request)
                .enqueue(new Callback<DriverResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DriverResponse> call,
                                           @NonNull Response<DriverResponse> response) {
                        showLoading(false);
                        btnSave.setEnabled(true);

                        if (response.isSuccessful()) {
                            // Upload photo if selected
                            if (selectedPhotoUri != null) {
                                uploadProfilePhoto();
                            } else {
                                showSuccessAndFinish();
                            }
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

    private void uploadProfilePhoto() {
        // TODO: Implement actual photo upload when API is available
        // For now, just show success
        showSuccessAndFinish();
    }

    private void showSuccessAndFinish() {
        hasUnsavedChanges = false;
        
        // Animate button to show success
        btnSave.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_check_circle));
        btnSave.setText("Saved!");
        btnSave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.success));
        
        btnSave.animate()
            .scaleX(1.05f).scaleY(1.05f)
            .setDuration(150)
            .withEndAction(() -> {
                btnSave.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(150)
                    .start();
            }).start();

        handler.postDelayed(() -> {
            showToast("Profile updated successfully");
            setResult(RESULT_OK);
            finish();
        }, 800);
    }

    private void clearErrors() {
        if (tilName != null) tilName.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilLicensePlate != null) tilLicensePlate.setError(null);
        if (tilEmergencyContact != null) tilEmergencyContact.setError(null);
        if (tilBio != null) tilBio.setError(null);
    }

    private void animateClick(View v) {
        if (v == null) return;
        v.animate()
            .scaleX(0.95f).scaleY(0.95f)
            .setDuration(50)
            .withEndAction(() ->
                v.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(50)
                    .start()
            ).start();
    }

    private void showLoading(boolean show) {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
