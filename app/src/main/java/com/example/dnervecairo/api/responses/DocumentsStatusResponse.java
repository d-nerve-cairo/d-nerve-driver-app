package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DocumentsStatusResponse {

    @SerializedName("driver_id")
    private String driverId;

    @SerializedName("verification_status")
    private String verificationStatus;

    @SerializedName("documents_uploaded")
    private int documentsUploaded;

    @SerializedName("documents_required")
    private int documentsRequired;

    @SerializedName("documents")
    private List<DocumentResponse> documents;

    // Getters
    public String getDriverId() { return driverId; }
    public String getVerificationStatus() { return verificationStatus; }
    public int getDocumentsUploaded() { return documentsUploaded; }
    public int getDocumentsRequired() { return documentsRequired; }
    public List<DocumentResponse> getDocuments() { return documents; }
}