package com.example.dnervecairo.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.models.LeaderboardEntry;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    
    private final List<LeaderboardEntry> entries;
    private final String currentDriverId;
    private int lastAnimatedPosition = -1;

    public LeaderboardAdapter(List<LeaderboardEntry> entries, String currentDriverId) {
        this.entries = entries;
        this.currentDriverId = currentDriverId;
    }

    // Backwards compatibility constructor
    public LeaderboardAdapter(List<LeaderboardEntry> entries) {
        this(entries, null);
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
        
        // Rank
        holder.tvRank.setText("#" + entry.getRank());
        holder.tvRank.setVisibility(View.VISIBLE);
        
        // Avatar initials
        if (holder.tvInitials != null) {
            holder.tvInitials.setText(getInitials(entry.getDriverName()));
            
            // Set avatar background color based on tier
            if (holder.avatarBackground != null) {
                int bgColor = getTierBackgroundColor(entry.getTier());
                holder.avatarBackground.getBackground().setTint(bgColor);
            }
        }
        
        // Driver info
        holder.tvDriverName.setText(entry.getDriverName());
        holder.tvTier.setText(entry.getTier());
        holder.tvPoints.setText(String.valueOf(entry.getPoints()));
        
        // Tier color for tier text
        int tierColor = getTierColor(entry.getTier(), holder.itemView);
        holder.tvTier.setTextColor(tierColor);
        
        // Highlight if this is the current user
        boolean isCurrentUser = currentDriverId != null && 
                                entry.getDriverId() != null && 
                                entry.getDriverId().equals(currentDriverId);
        
        if (isCurrentUser) {
            // Highlighted card for current user
            holder.cardView.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent));
            holder.cardView.setStrokeWidth(3);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.surface_highlight));
            
            // Add "You" indicator
            holder.tvDriverName.setText(entry.getDriverName() + " (You)");
        } else {
            // Normal card
            holder.cardView.setStrokeWidth(0);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.surface));
        }
        
        // Entry animation
        if (position > lastAnimatedPosition) {
            animateEntry(holder.itemView, position);
            lastAnimatedPosition = position;
        }
    }

    private void animateEntry(View view, int position) {
        view.setAlpha(0f);
        view.setTranslationX(100f);
        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(300)
            .setStartDelay(position * 50L)
            .setInterpolator(new DecelerateInterpolator())
            .start();
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        } else if (parts.length == 1 && parts[0].length() >= 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return "?";
    }

    private int getTierColor(String tier, View view) {
        if (tier == null) tier = "";
        
        int colorRes;
        if (tier.contains("Diamond")) {
            colorRes = R.color.tier_diamond;
        } else if (tier.contains("Platinum")) {
            colorRes = R.color.tier_platinum;
        } else if (tier.contains("Gold")) {
            colorRes = R.color.tier_gold;
        } else if (tier.contains("Silver")) {
            colorRes = R.color.tier_silver;
        } else {
            colorRes = R.color.tier_bronze;
        }
        return ContextCompat.getColor(view.getContext(), colorRes);
    }

    private int getTierBackgroundColor(String tier) {
        if (tier == null) tier = "";
        
        if (tier.contains("Diamond")) {
            return Color.parseColor("#B9F2FF");
        } else if (tier.contains("Platinum")) {
            return Color.parseColor("#E5E4E2");
        } else if (tier.contains("Gold")) {
            return Color.parseColor("#FFD700");
        } else if (tier.contains("Silver")) {
            return Color.parseColor("#C0C0C0");
        } else {
            return Color.parseColor("#CD7F32");
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvRank, tvDriverName, tvTier, tvPoints, tvInitials;
        View avatarBackground;
        ImageView ivMedal;

        ViewHolder(View view) {
            super(view);
            cardView = (MaterialCardView) view;
            tvRank = view.findViewById(R.id.tv_rank);
            tvDriverName = view.findViewById(R.id.tv_driver_name);
            tvTier = view.findViewById(R.id.tv_driver_tier);
            tvPoints = view.findViewById(R.id.tv_points);
            tvInitials = view.findViewById(R.id.tv_initials);
            avatarBackground = view.findViewById(R.id.avatar_background);
            ivMedal = view.findViewById(R.id.iv_medal);
        }
    }
}
