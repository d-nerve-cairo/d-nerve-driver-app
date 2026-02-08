package com.example.dnervecairo.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ProgressBar;
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

    private RecyclerView recyclerView;
    private DocumentAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView tvVerificationStatus, tvProgress;
    private LinearProgressIndicator progressIndicator;

    private PreferenceManager prefManager;
    private String currentDocumentType;
    private Uri cameraImageUri;

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
        loadDocuments();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_documents);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        tvVerificationStatus = findViewById(R.id.tv_verification_status);
        tvProgress = findViewById(R.id.tv_progress);
        progressIndicator = findViewById(R.id.progress_indicator);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new DocumentAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(this::loadDocuments);
    }

    private void loadDocuments() {
        progressBar.setVisibility(View.VISIBLE);

        ApiClient.getInstance().getApiService()
                .getDriverDocuments(prefManager.getDriverId())
                .enqueue(new Callback<DocumentsStatusResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DocumentsStatusResponse> call,
                                           @NonNull Response<DocumentsStatusResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            displayDocuments(response.body());
                        } else {
                            Toast.makeText(DocumentsActivity.this,
                                    "Failed to load documents", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DocumentsStatusResponse> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(DocumentsActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
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
        progressIndicator.setProgress(uploaded);

        // Status text and color
        switch (status) {
            case "verified":
                tvVerificationStatus.setText("✅ Verified");
                tvVerificationStatus.setTextColor(getResources().getColor(R.color.success, null));
                break;
            case "pending":
                tvVerificationStatus.setText("🕐 Pending Review");
                tvVerificationStatus.setTextColor(getResources().getColor(R.color.warning, null));
                break;
            case "rejected":
                tvVerificationStatus.setText("❌ Rejected");
                tvVerificationStatus.setTextColor(getResources().getColor(R.color.error, null));
                break;
            default:
                tvVerificationStatus.setText("📋 Incomplete");
                tvVerificationStatus.setTextColor(getResources().getColor(R.color.text_secondary, null));
        }

        adapter.setDocuments(data.getDocuments());
    }

    @Override
    public void onUploadClick(DocumentResponse document) {
        currentDocumentType = document.getDocumentType();
        showImageSourceDialog();
    }

    @Override
    public void onViewClick(DocumentResponse document) {
        // TODO: Open document viewer
        Toast.makeText(this, "View: " + document.getFileName(), Toast.LENGTH_SHORT).show();
    }

    private void showImageSourceDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_image_source, null);

        view.findViewById(R.id.btn_camera).setOnClickListener(v -> {
            dialog.dismiss();
            openCamera();
        });

        view.findViewById(R.id.btn_gallery).setOnClickListener(v -> {
            dialog.dismiss();
            openGallery();
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
            }
        }
    }

    private void uploadDocument(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            // Convert URI to File
            File file = createTempFileFromUri(imageUri);
            if (file == null) {
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
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
                            progressBar.setVisibility(View.GONE);

                            if (response.isSuccessful() && response.body() != null) {
                                Toast.makeText(DocumentsActivity.this,
                                        "✅ " + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                                loadDocuments(); // Refresh list
                            } else {
                                Toast.makeText(DocumentsActivity.this,
                                        "Upload failed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<DocumentUploadResponse> call, @NonNull Throwable t) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(DocumentsActivity.this,
                                    "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
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
}