package com.example.dnervecairo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        this.documents = documents;
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

        DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivIcon = itemView.findViewById(R.id.iv_icon);
            ivStatus = itemView.findViewById(R.id.iv_status);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvRejectionReason = itemView.findViewById(R.id.tv_rejection_reason);
            btnAction = itemView.findViewById(R.id.btn_action);
        }

        void bind(DocumentResponse doc) {
            // Set title based on document type
            tvTitle.setText(getDocumentTitle(doc.getDocumentType()));
            ivIcon.setImageResource(getDocumentIcon(doc.getDocumentType()));

            // Set status
            String status = doc.getStatus();
            switch (status) {
                case "not_uploaded":
                    tvStatus.setText("Not Uploaded");
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.text_secondary, null));
                    ivStatus.setImageResource(R.drawable.ic_upload);
                    ivStatus.setColorFilter(itemView.getContext().getResources().getColor(R.color.text_secondary, null));
                    btnAction.setText("Upload");
                    btnAction.setOnClickListener(v -> listener.onUploadClick(doc));
                    tvRejectionReason.setVisibility(View.GONE);
                    break;

                case "pending":
                    tvStatus.setText("Pending Review");
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.warning, null));
                    ivStatus.setImageResource(R.drawable.ic_pending);
                    ivStatus.setColorFilter(itemView.getContext().getResources().getColor(R.color.warning, null));
                    btnAction.setText("Replace");
                    btnAction.setOnClickListener(v -> listener.onUploadClick(doc));
                    tvRejectionReason.setVisibility(View.GONE);
                    break;

                case "approved":
                    tvStatus.setText("Approved");
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.success, null));
                    ivStatus.setImageResource(R.drawable.ic_check_circle);
                    ivStatus.setColorFilter(itemView.getContext().getResources().getColor(R.color.success, null));
                    btnAction.setText("View");
                    btnAction.setOnClickListener(v -> listener.onViewClick(doc));
                    tvRejectionReason.setVisibility(View.GONE);
                    break;

                case "rejected":
                    tvStatus.setText("Rejected");
                    tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.error, null));
                    ivStatus.setImageResource(R.drawable.ic_error);
                    ivStatus.setColorFilter(itemView.getContext().getResources().getColor(R.color.error, null));
                    btnAction.setText("Re-upload");
                    btnAction.setOnClickListener(v -> listener.onUploadClick(doc));
                    if (doc.getRejectionReason() != null) {
                        tvRejectionReason.setText("Reason: " + doc.getRejectionReason());
                        tvRejectionReason.setVisibility(View.VISIBLE);
                    }
                    break;
            }

            // Required badge
            boolean isRequired = isRequiredDocument(doc.getDocumentType());
            // Could show a "Required" badge if needed
        }

        private String getDocumentTitle(String type) {
            switch (type) {
                case "profile_photo": return "Profile Photo";
                case "national_id": return "National ID";
                case "drivers_license": return "Driver's License";
                case "vehicle_registration": return "Vehicle Registration";
                case "vehicle_photo": return "Vehicle Photo (Optional)";
                default: return type;
            }
        }

        private int getDocumentIcon(String type) {
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
            return !type.equals("vehicle_photo");
        }
    }
}