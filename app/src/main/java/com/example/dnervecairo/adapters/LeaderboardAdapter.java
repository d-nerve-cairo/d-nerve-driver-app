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
import com.example.dnervecairo.models.LeaderboardEntry;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardEntry> entries;

    public LeaderboardAdapter(List<LeaderboardEntry> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardEntry entry = entries.get(position);

        holder.tvRank.setText(String.valueOf(entry.getRank()));
        holder.tvDriverName.setText(entry.getDriverName());
        holder.tvTier.setText(entry.getTier());
        holder.tvPoints.setText(String.valueOf(entry.getPoints()));

        // Show medal for top 3
        if (entry.getRank() <= 3) {
            holder.ivMedal.setVisibility(View.VISIBLE);
            int color;
            switch (entry.getRank()) {
                case 1:
                    color = ContextCompat.getColor(holder.itemView.getContext(), R.color.tier_gold);
                    break;
                case 2:
                    color = ContextCompat.getColor(holder.itemView.getContext(), R.color.tier_silver);
                    break;
                default:
                    color = ContextCompat.getColor(holder.itemView.getContext(), R.color.tier_bronze);
            }
            holder.ivMedal.setColorFilter(color);
            holder.tvRank.setVisibility(View.GONE);
        } else {
            holder.ivMedal.setVisibility(View.GONE);
            holder.tvRank.setVisibility(View.VISIBLE);
        }

        // Color tier text
        int tierColor;
        switch (entry.getTier()) {
            case "Platinum Driver":
                tierColor = R.color.tier_platinum;
                break;
            case "Gold Driver":
                tierColor = R.color.tier_gold;
                break;
            case "Silver Driver":
                tierColor = R.color.tier_silver;
                break;
            default:
                tierColor = R.color.tier_bronze;
        }
        holder.tvTier.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), tierColor));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvDriverName, tvTier, tvPoints;
        ImageView ivMedal;

        ViewHolder(View view) {
            super(view);
            tvRank = view.findViewById(R.id.tv_rank);
            tvDriverName = view.findViewById(R.id.tv_driver_name);
            tvTier = view.findViewById(R.id.tv_driver_tier);
            tvPoints = view.findViewById(R.id.tv_points);
            ivMedal = view.findViewById(R.id.iv_medal);
        }
    }
}