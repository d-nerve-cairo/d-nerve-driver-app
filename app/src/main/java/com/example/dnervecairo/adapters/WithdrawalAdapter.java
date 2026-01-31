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
import com.example.dnervecairo.api.responses.WithdrawalResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WithdrawalAdapter extends RecyclerView.Adapter<WithdrawalAdapter.ViewHolder> {

    private List<WithdrawalResponse> withdrawals = new ArrayList<>();

    public void setWithdrawals(List<WithdrawalResponse> withdrawals) {
        this.withdrawals = withdrawals;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_withdrawal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(withdrawals.get(position));
    }

    @Override
    public int getItemCount() {
        return withdrawals.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStatus;
        TextView tvMethod, tvDate, tvAmount, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStatus = itemView.findViewById(R.id.iv_status);
            tvMethod = itemView.findViewById(R.id.tv_method);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }

        void bind(WithdrawalResponse withdrawal) {
            tvMethod.setText(withdrawal.getPaymentMethod());
            tvAmount.setText(String.format(Locale.US, "-%.2f EGP", withdrawal.getAmount()));
            tvDate.setText(formatDate(withdrawal.getCreatedAt()));

            String status = withdrawal.getStatus();
            tvStatus.setText(capitalize(status));

            // Status colors
            int statusColor;
            switch (status.toLowerCase()) {
                case "completed":
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.success);
                    break;
                case "pending":
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.warning);
                    break;
                case "rejected":
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.error);
                    break;
                default:
                    statusColor = ContextCompat.getColor(itemView.getContext(), R.color.text_secondary);
            }
            tvStatus.setTextColor(statusColor);
        }

        private String formatDate(String dateStr) {
            if (dateStr == null) return "";
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                Date date = inputFormat.parse(dateStr.split("\\.")[0]);
                return outputFormat.format(date);
            } catch (Exception e) {
                return dateStr.substring(0, 10);
            }
        }

        private String capitalize(String str) {
            if (str == null || str.isEmpty()) return str;
            return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }
    }
}