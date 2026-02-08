package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

public class DocumentResponse {

    @SerializedName("document_id")
    private String documentId;

    @SerializedName("driver_id")
    private String driverId;

    @SerializedName("document_type")
    private String documentType;

    @SerializedName("status")
    private String status;

    @SerializedName("file_name")
    private String fileName;

    @SerializedName("rejection_reason")
    private String rejectionReason;

    @SerializedName("uploaded_at")
    private String uploadedAt;

    @SerializedName("reviewed_at")
    private String reviewedAt;

    // Getters
    public String getDocumentId() { return documentId; }
    public String getDriverId() { return driverId; }
    public String getDocumentType() { return documentType; }
    public String getStatus() { return status; }
    public String getFileName() { return fileName; }
    public String getRejectionReason() { return rejectionReason; }
    public String getUploadedAt() { return uploadedAt; }
    public String getReviewedAt() { return reviewedAt; }

    // Helper methods
    public boolean isUploaded() {
        return !"not_uploaded".equals(status);
    }

    public boolean isPending() {
        return "pending".equals(status);
    }

    public boolean isApproved() {
        return "approved".equals(status);
    }

    public boolean isRejected() {
        return "rejected".equals(status);
    }
}