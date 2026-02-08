package com.example.dnervecairo.api.responses;

import com.google.gson.annotations.SerializedName;

public class DocumentUploadResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("document")
    private DocumentResponse document;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public DocumentResponse getDocument() { return document; }
}