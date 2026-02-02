package com.example.dnervecairo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.api.responses.BadgeResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private List<BadgeResponse> badges = new ArrayList<>();

    public void setBadges(List<BadgeResponse> badges) {
        this.badges = badges;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        holder.bind(badges.get(position));
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivBadgeIcon;
        private final ImageView ivEarnedCheck;
        private final TextView tvBadgeName;
        private final TextView tvBadgeDesc;
        private final TextView tvProgress;
        private final TextView tvReward;
        private final ProgressBar progressBar;
        private final View cardView;

        BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBadgeIcon = itemView.findViewById(R.id.iv_badge_icon);
            ivEarnedCheck = itemView.findViewById(R.id.iv_earned_check);
            tvBadgeName = itemView.findViewById(R.id.tv_badge_name);
            tvBadgeDesc = itemView.findViewById(R.id.tv_badge_desc);
            tvProgress = itemView.findViewById(R.id.tv_progress);
            tvReward = itemView.findViewById(R.id.tv_reward);
            progressBar = itemView.findViewById(R.id.progress_bar);
            cardView = itemView.findViewById(R.id.card_badge);
        }

        void bind(BadgeResponse badge) {
            // Check current language
            String lang = Locale.getDefault().getLanguage();
            boolean isArabic = lang.equals("ar");

            // Set name and description based on language
            String name = isArabic && badge.getNameAr() != null ? badge.getNameAr() : badge.getName();
            String desc = isArabic && badge.getDescriptionAr() != null ? badge.getDescriptionAr() : badge.getDescription();

            tvBadgeName.setText(name);
            tvBadgeDesc.setText(desc);

            // Set icon based on category
            int iconRes = R.drawable.ic_badge;
            switch (badge.getCategory()) {
                case "trips":
                    iconRes = R.drawable.ic_directions_bus;
                    break;
                case "quality":
                    iconRes = R.drawable.ic_verified;
                    break;
                case "streak":
                    iconRes = R.drawable.ic_local_fire_department;
                    break;
                case "points":
                    iconRes = R.drawable.ic_stars;
                    break;
            }
            ivBadgeIcon.setImageResource(iconRes);

            // Show earned status
            if (badge.isEarned()) {
                ivEarnedCheck.setVisibility(View.VISIBLE);
                ivBadgeIcon.setAlpha(1.0f);
                progressBar.setVisibility(View.GONE);
                tvProgress.setText("Earned!");
                tvProgress.setTextColor(itemView.getContext().getResources().getColor(R.color.success));
            } else {
                ivEarnedCheck.setVisibility(View.GONE);
                ivBadgeIcon.setAlpha(0.4f);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress((int) badge.getProgressPercent());
                tvProgress.setText(String.format(Locale.US, "%d / %d (%.0f%%)",
                        badge.getCurrentValue(), badge.getRequirementValue(), badge.getProgressPercent()));
                tvProgress.setTextColor(itemView.getContext().getResources().getColor(R.color.text_secondary));
            }

            // Reward points
            tvReward.setText("+" + badge.getPointsReward() + " pts");
        }
    }
}