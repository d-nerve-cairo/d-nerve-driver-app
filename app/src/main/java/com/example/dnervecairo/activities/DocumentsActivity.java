package com.example.dnervecairo.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.dnervecairo.R;
import com.example.dnervecairo.adapters.DocumentAdapter;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.responses.DocumentResponse;
import com.example.dnervecairo.api.responses.DocumentsStatusResponse;
import com.example.dnervecairo.api.responses.DocumentUploadResponse;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentsActivity extends AppCompatActivity implements DocumentAdapter.OnDocumentClickListener {

    private static final int CAMERA_PERMISSION_REQUEST = 100;

    // Views
    private RecyclerView recyclerView;
    private DocumentAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private View progressOverlay;
    private TextView tvVerificationStatus, tvProgress;
    private LinearProgressIndicator progressIndicator;
    
    // Cards for animation
    private MaterialCardView cardStatus;
    private LinearLayout layoutDocumentsHeader;

    // Data
    private PreferenceManager prefManager;
    private String currentDocumentType;
    private Uri cameraImageUri;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isFirstLoad = true;

    // Activity result launchers
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        uploadDocument(imageUri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                    uploadDocument(cameraImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);

        prefManager = new PreferenceManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        prepareEntranceAnimations();
        loadDocuments();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_documents);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        progressOverlay = findViewById(R.id.progress_overlay);
        tvVerificationStatus = findViewById(R.id.tv_verification_status);
        tvProgress = findViewById(R.id.tv_progress);
        progressIndicator = findViewById(R.id.progress_indicator);
        cardStatus = findViewById(R.id.card_status);
        layoutDocumentsHeader = findViewById(R.id.layout_documents_header);
    }

    private void prepareEntranceAnimations() {
        if (cardStatus != null) {
            cardStatus.setAlpha(0f);
            cardStatus.setTranslationY(30f);
        }
        if (layoutDocumentsHeader != null) {
            layoutDocumentsHeader.setAlpha(0f);
        }
    }

    private void playEntranceAnimations() {
        if (!isFirstLoad) return;
        isFirstLoad = false;

        // Status card
        if (cardStatus != null) {
            cardStatus.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }

        // Documents header
        handler.postDelayed(() -> {
            if (layoutDocumentsHeader != null) {
                layoutDocumentsHeader.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
            }
        }, 200);

        // Animate list items
        handler.postDelayed(this::animateListItems, 300);
    }

    private void animateListItems() {
        recyclerView.post(() -> {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                if (child != null) {
                    child.setAlpha(0f);
                    child.setTranslationX(50f);
                    final int delay = i * 80;
                    handler.postDelayed(() -> {
                        child.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(350)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                    }, delay);
                }
            }
        });
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            animateClick(v);
            handler.postDelayed(this::finish, 100);
        });
    }

    private void setupRecyclerView() {
        adapter = new DocumentAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::loadDocuments);
    }

    private void loadDocuments() {
        progressOverlay.setVisibility(View.VISIBLE);

        ApiClient.getInstance().getApiService()
                .getDriverDocuments(prefManager.getDriverId())
                .enqueue(new Callback<DocumentsStatusResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DocumentsStatusResponse> call,
                                           @NonNull Response<DocumentsStatusResponse> response) {
                        progressOverlay.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            displayDocuments(response.body());
                            playEntranceAnimations();
                        } else {
                            showToast("Failed to load documents");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DocumentsStatusResponse> call, @NonNull Throwable t) {
                        progressOverlay.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        showToast("Network error");
                    }
                });
    }

    private void displayDocuments(DocumentsStatusResponse data) {
        // Update header
        String status = data.getVerificationStatus();
        int uploaded = data.getDocumentsUploaded();
        int required = data.getDocumentsRequired();

        tvProgress.setText(uploaded + "/" + required + " documents");
        progressIndicator.setMax(required);
        
        // Animate progress
        animateProgress(uploaded);

        // Status text and color
        switch (status) {
            case "verified":
                tvVerificationStatus.setText("✓ Verified");
                tvVerificationStatus.setTextColor(ContextCompat.getColor(this, R.color.success));
                break;
            case "pending":
                tvVerificationStatus.setText("⏳ Pending Review");
                tvVerificationStatus.setTextColor(ContextCompat.getColor(this, R.color.warning));
                break;
            case "rejected":
                tvVerificationStatus.setText("✗ Rejected");
                tvVerificationStatus.setTextColor(ContextCompat.getColor(this, R.color.error));
                break;
            default:
                tvVerificationStatus.setText("📋 Incomplete");
                tvVerificationStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }

        adapter.setDocuments(data.getDocuments());
    }

    private void animateProgress(int target) {
        handler.postDelayed(() -> {
            progressIndicator.setProgressCompat(target, true);
        }, 400);
    }

    @Override
    public void onUploadClick(DocumentResponse document) {
        currentDocumentType = document.getDocumentType();
        showImageSourceDialog();
    }

    @Override
    public void onViewClick(DocumentResponse document) {
        // Show document info
        String fileName = document.getFileName();
        if (fileName != null && !fileName.isEmpty()) {
            showToast("Document: " + fileName);
        } else {
            showToast("Document uploaded successfully");
        }
    }

    private void showImageSourceDialog() {
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
        File photoFile = new File(getCacheDir(), "document_" + System.currentTimeMillis() + ".jpg");
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

    private void uploadDocument(Uri imageUri) {
        progressOverlay.setVisibility(View.VISIBLE);
        showToast("Uploading...");

        try {
            // Convert URI to File
            File file = createTempFileFromUri(imageUri);
            if (file == null) {
                showToast("Failed to process image");
                progressOverlay.setVisibility(View.GONE);
                return;
            }

            // Create multipart request
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            RequestBody docType = RequestBody.create(MediaType.parse("text/plain"), currentDocumentType);

            ApiClient.getInstance().getApiService()
                    .uploadDocument(prefManager.getDriverId(), docType, filePart)
                    .enqueue(new Callback<DocumentUploadResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<DocumentUploadResponse> call,
                                               @NonNull Response<DocumentUploadResponse> response) {
                            progressOverlay.setVisibility(View.GONE);

                            if (response.isSuccessful() && response.body() != null) {
                                showToast("✓ " + response.body().getMessage());
                                // Animate success
                                showUploadSuccess();
                                loadDocuments(); // Refresh list
                            } else {
                                showToast("Upload failed");
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<DocumentUploadResponse> call, @NonNull Throwable t) {
                            progressOverlay.setVisibility(View.GONE);
                            showToast("Network error: " + t.getMessage());
                        }
                    });

        } catch (Exception e) {
            progressOverlay.setVisibility(View.GONE);
            showToast("Error: " + e.getMessage());
        }
    }

    private void showUploadSuccess() {
        // Brief success animation on status card
        if (cardStatus != null) {
            cardStatus.animate()
                .scaleX(1.02f).scaleY(1.02f)
                .setDuration(150)
                .withEndAction(() ->
                    cardStatus.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(150)
                        .start()
                ).start();
        }
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            
            File tempFile = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
