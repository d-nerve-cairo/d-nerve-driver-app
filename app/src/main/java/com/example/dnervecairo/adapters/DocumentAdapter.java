package com.example.dnervecairo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.api.responses.DocumentResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private List<DocumentResponse> documents = new ArrayList<>();
    private final OnDocumentClickListener listener;

    public interface OnDocumentClickListener {
        void onUploadClick(DocumentResponse document);
        void onViewClick(DocumentResponse document);
    }

    public DocumentAdapter(OnDocumentClickListener listener) {
        this.listener = listener;
    }

    public void setDocuments(List<DocumentResponse> documents) {
        this.documents = documents != null ? documents : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        holder.bind(documents.get(position));
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    class DocumentViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ImageView ivIcon, ivStatus;
        private final TextView tvTitle, tvStatus, tvRejectionReason;
        private final MaterialButton btnAction;
        private final View layoutRequired;

        DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivIcon = itemView.findViewById(R.id.iv_icon);
            ivStatus = itemView.findViewById(R.id.iv_status);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvRejectionReason = itemView.findViewById(R.id.tv_rejection_reason);
            btnAction = itemView.findViewById(R.id.btn_action);
            layoutRequired = itemView.findViewById(R.id.badge_required);
        }

        void bind(DocumentResponse doc) {
            // Set title based on document type
            tvTitle.setText(getDocumentTitle(doc.getDocumentType()));
            ivIcon.setImageResource(getDocumentIcon(doc.getDocumentType()));

            // Required badge
            boolean isRequired = isRequiredDocument(doc.getDocumentType());
            if (layoutRequired != null) {
                layoutRequired.setVisibility(isRequired ? View.VISIBLE : View.GONE);
            }

            // Set status with proper colors
            String status = doc.getStatus();
            if (status == null) status = "not_uploaded";
            
            switch (status) {
                case "not_uploaded":
                    setStatusUI("Not Uploaded", R.color.text_secondary, R.drawable.ic_upload);
                    setActionButton("Upload", true, doc);
                    tvRejectionReason.setVisibility(View.GONE);
                    break;

                case "pending":
                    setStatusUI("Pending Review", R.color.warning, R.drawable.ic_pending);
                    setActionButton("Replace", true, doc);
                    tvRejectionReason.setVisibility(View.GONE);
                    break;

                case "approved":
                    setStatusUI("Approved", R.color.success, R.drawable.ic_check_circle);
                    setActionButton("View", false, doc);
                    tvRejectionReason.setVisibility(View.GONE);
                    break;

                case "rejected":
                    setStatusUI("Rejected", R.color.error, R.drawable.ic_error);
                    setActionButton("Re-upload", true, doc);
                    if (doc.getRejectionReason() != null && !doc.getRejectionReason().isEmpty()) {
                        tvRejectionReason.setText("Reason: " + doc.getRejectionReason());
                        tvRejectionReason.setVisibility(View.VISIBLE);
                    } else {
                        tvRejectionReason.setVisibility(View.GONE);
                    }
                    break;

                default:
                    setStatusUI("Unknown", R.color.text_secondary, R.drawable.ic_upload);
                    setActionButton("Upload", true, doc);
                    tvRejectionReason.setVisibility(View.GONE);
            }
        }

        private void setStatusUI(String text, int colorRes, int iconRes) {
            int color = ContextCompat.getColor(itemView.getContext(), colorRes);
            tvStatus.setText(text);
            tvStatus.setTextColor(color);
            ivStatus.setImageResource(iconRes);
            ivStatus.setColorFilter(color);
        }

        private void setActionButton(String text, boolean isUpload, DocumentResponse doc) {
            btnAction.setText(text);
            btnAction.setOnClickListener(v -> {
                animateClick(v);
                v.postDelayed(() -> {
                    if (isUpload) {
                        listener.onUploadClick(doc);
                    } else {
                        listener.onViewClick(doc);
                    }
                }, 100);
            });
        }

        private void animateClick(View v) {
            // Animate the entire card for better feedback
            cardView.animate()
                .scaleX(0.98f).scaleY(0.98f)
                .setDuration(50)
                .withEndAction(() ->
                    cardView.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(50)
                        .start()
                ).start();
        }

        private String getDocumentTitle(String type) {
            if (type == null) return "Document";
            switch (type) {
                case "profile_photo": return "Profile Photo";
                case "national_id": return "National ID";
                case "drivers_license": return "Driver's License";
                case "vehicle_registration": return "Vehicle Registration";
                case "vehicle_photo": return "Vehicle Photo";
                default: return formatTypeName(type);
            }
        }

        private String formatTypeName(String type) {
            // Convert snake_case to Title Case
            String[] words = type.split("_");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (word.length() > 0) {
                    sb.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
                }
            }
            return sb.toString().trim();
        }

        private int getDocumentIcon(String type) {
            if (type == null) return R.drawable.ic_description;
            switch (type) {
                case "profile_photo": return R.drawable.ic_person;
                case "national_id": return R.drawable.ic_badge;
                case "drivers_license": return R.drawable.ic_card;
                case "vehicle_registration": return R.drawable.ic_description;
                case "vehicle_photo": return R.drawable.ic_directions_bus;
                default: return R.drawable.ic_description;
            }
        }

        private boolean isRequiredDocument(String type) {
            if (type == null) return true;
            return !type.equals("vehicle_photo");
        }
    }
}
