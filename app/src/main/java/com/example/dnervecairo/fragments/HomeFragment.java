package com.example.dnervecairo.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.dnervecairo.MainActivity;
import com.example.dnervecairo.R;
import com.example.dnervecairo.activities.StartTripActivity;
import com.example.dnervecairo.activities.TripHistoryActivity;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.database.CachedDriverEntity;
import com.example.dnervecairo.utils.OfflineManager;
import com.example.dnervecairo.utils.PreferenceManager;
import com.example.dnervecairo.viewmodels.SharedDriverViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;

public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvPoints, tvTier, tvTierProgress, tvTripsCount, tvQuality, tvStreak;
    private TextView tvAvatarInitials;
    private ProgressBar progressTier;
    private SwipeRefreshLayout swipeRefresh;
    private ImageView ivStreakIcon;
    private View shimmerOverlay;
    private LinearLayout contentLayout;
    private PreferenceManager prefManager;
    private OfflineManager offlineManager;
    
    private ValueAnimator streakPulseAnimator;
    private SharedDriverViewModel driverViewModel;

    // Tier thresholds matching backend
    private static final int TIER_SILVER = 500;
    private static final int TIER_GOLD = 2000;
    private static final int TIER_PLATINUM = 5000;
    private static final int TIER_DIAMOND = 10000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        prefManager = new PreferenceManager(requireContext());
        offlineManager = new OfflineManager(requireContext());

        // Initialize shared ViewModel (scoped to Activity)
        driverViewModel = new ViewModelProvider(requireActivity()).get(SharedDriverViewModel.class);

        initViews(view);
        setupSwipeRefresh();
        setupTripHistoryCard(view);
        setupRewardsCard(view);
        setupStartTripButton(view);

        // Observe shared driver data
        driverViewModel.getDriverData().observe(getViewLifecycleOwner(), driver -> {
            hideRefreshing();
            hideShimmer();
            if (driver != null) {
                displayDriverData(driver);
                offlineManager.cacheDriverData(driver);
                prefManager.saveDriverData(
                        driver.getName(),
                        driver.getTotalPoints(),
                        driver.getTier(),
                        driver.getTripsCompleted(),
                        driver.getQualityAvg(),
                        driver.getCurrentStreak()
                );
            }
        });

        driverViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                hideRefreshing();
                hideShimmer();
                // Fall back to cached data
                String driverId = prefManager.getDriverId();
                if (driverId != null) {
                    loadCachedData(driverId);
                } else {
                    displaySavedOrDefaultData();
                }
            }
        });

        showShimmer();
        loadDriverData();

        return view;
    }

    private void initViews(View view) {
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvAvatarInitials = view.findViewById(R.id.tv_avatar_initials);
        tvPoints = view.findViewById(R.id.tv_points);
        tvTier = view.findViewById(R.id.tv_tier);
        tvTierProgress = view.findViewById(R.id.tv_tier_progress);
        tvTripsCount = view.findViewById(R.id.tv_trips_count);
        tvQuality = view.findViewById(R.id.tv_quality);
        tvStreak = view.findViewById(R.id.tv_streak);
        progressTier = view.findViewById(R.id.progress_tier);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        ivStreakIcon = view.findViewById(R.id.iv_streak_icon);
        shimmerOverlay = view.findViewById(R.id.shimmer_overlay);
        contentLayout = view.findViewById(R.id.content_layout);
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
            swipeRefresh.setOnRefreshListener(() -> {
                String driverId = prefManager.getDriverId();
                if (driverId != null) {
                    driverViewModel.forceRefresh(driverId);
                }
            });
        }
    }

    private void setupTripHistoryCard(View view) {
        View cardTripHistory = view.findViewById(R.id.card_trip_history);
        cardTripHistory.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), TripHistoryActivity.class));
        });
    }

    private void setupStartTripButton(View view) {
        MaterialButton btnStartTrip = view.findViewById(R.id.btn_start_trip);
        btnStartTrip.setOnClickListener(v -> {
            // Button press animation
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                Intent intent = new Intent(getActivity(), StartTripActivity.class);
                startActivity(intent);
            }).start();
        });
    }

    private void setupRewardsCard(View view) {
        com.google.android.material.card.MaterialCardView cardRewards = view.findViewById(R.id.card_rewards);
        cardRewards.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(R.id.nav_rewards);
            }
        });
    }

    // =========================================================================
    // SHIMMER LOADING
    // =========================================================================
    
    private void showShimmer() {
        if (shimmerOverlay != null) {
            shimmerOverlay.setVisibility(View.VISIBLE);
            // Animate shimmer
            ObjectAnimator shimmerAnim = ObjectAnimator.ofFloat(shimmerOverlay, "alpha", 0.3f, 0.7f);
            shimmerAnim.setDuration(800);
            shimmerAnim.setRepeatMode(ValueAnimator.REVERSE);
            shimmerAnim.setRepeatCount(ValueAnimator.INFINITE);
            shimmerAnim.start();
        }
    }
    
    private void hideShimmer() {
        if (shimmerOverlay != null) {
            shimmerOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> shimmerOverlay.setVisibility(View.GONE))
                .start();
        }
    }

    // =========================================================================
    // TIME-BASED GREETING
    // =========================================================================
    
    private String getTimeBasedGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 12) {
            return getString(R.string.greeting_morning);
        } else if (hour >= 12 && hour < 17) {
            return getString(R.string.greeting_afternoon);
        } else if (hour >= 17 && hour < 21) {
            return getString(R.string.greeting_evening);
        } else {
            return getString(R.string.greeting_night);
        }
    }
    
    private void updateGreeting(String driverName) {
        String greeting = getTimeBasedGreeting();
        String firstName = driverName != null ? driverName.split(" ")[0] : "Driver";
        tvGreeting.setText(greeting + ", " + firstName + "!");
        
        // Update avatar initials
        if (tvAvatarInitials != null && driverName != null) {
            String initials = getInitials(driverName);
            tvAvatarInitials.setText(initials);
        }
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

    // =========================================================================
    // STREAK FIRE ANIMATION
    // =========================================================================
    
    private void startStreakPulse() {
        if (ivStreakIcon == null) return;
        
        stopStreakPulse();
        
        // Pulsing glow effect
        streakPulseAnimator = ValueAnimator.ofFloat(1f, 1.2f);
        streakPulseAnimator.setDuration(600);
        streakPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        streakPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        streakPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        streakPulseAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            ivStreakIcon.setScaleX(scale);
            ivStreakIcon.setScaleY(scale);
            ivStreakIcon.setAlpha(0.7f + (scale - 1f) * 1.5f);
        });
        streakPulseAnimator.start();
    }
    
    private void stopStreakPulse() {
        if (streakPulseAnimator != null) {
            streakPulseAnimator.cancel();
            streakPulseAnimator = null;
        }
        if (ivStreakIcon != null) {
            ivStreakIcon.setScaleX(1f);
            ivStreakIcon.setScaleY(1f);
            ivStreakIcon.setAlpha(1f);
        }
    }

    // =========================================================================
    // POINTS COUNTER ANIMATION
    // =========================================================================
    
    private void animatePointsCounter(int targetPoints) {
        ValueAnimator animator = ValueAnimator.ofInt(0, targetPoints);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            tvPoints.setText(String.valueOf(value));
        });
        animator.start();
    }

    // =========================================================================
    // DATA LOADING
    // =========================================================================

    private void loadDriverData() {
        if (!prefManager.isLoggedIn()) {
            displaySavedOrDefaultData();
            hideRefreshing();
            hideShimmer();
            return;
        }

        String driverId = prefManager.getDriverId();
        if (driverId != null) {
            // Delegates to ViewModel — will skip fetch if data is fresh
            driverViewModel.loadDriverData(driverId);
        } else {
            hideRefreshing();
            hideShimmer();
            displaySavedOrDefaultData();
        }
    }

    private void hideRefreshing() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void loadCachedData(String driverId) {
        offlineManager.getCachedDriver(driverId, cachedDriver -> {
            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                if (cachedDriver != null) {
                    displayCachedData(cachedDriver);
                } else {
                    displaySavedOrDefaultData();
                }
            });
        });
    }

    private void displayDriverData(DriverResponse driver) {
        // Time-based greeting with name
        updateGreeting(driver.getName());
        
        // Animate points counter
        animatePointsCounter(driver.getTotalPoints());
        
        String tier = driver.getTier();
        tvTier.setText(tier + " Driver");
        updateTierColor(tier);
        
        tvTripsCount.setText(String.valueOf(driver.getTripsCompleted()));
        
        // Quality display fix
        double qualityPercent = driver.getQualityAvg();
        if (qualityPercent <= 1.0) {
            qualityPercent = qualityPercent * 100;
        }
        tvQuality.setText(String.format("%.0f%%", qualityPercent));
        
        int streak = driver.getCurrentStreak();
        tvStreak.setText(String.valueOf(streak));
        
        // Animate streak fire if active
        if (streak > 0) {
            startStreakPulse();
        } else {
            stopStreakPulse();
        }
        
        updateTierProgress(driver.getTotalPoints(), tier);
        
        // Entrance animation for cards
        animateCardsEntrance();
    }

    private void displayCachedData(CachedDriverEntity driver) {
        updateGreeting(driver.getName());
        animatePointsCounter(driver.getTotalPoints());
        
        String tier = driver.getTier();
        tvTier.setText(tier + " Driver");
        updateTierColor(tier);
        
        tvTripsCount.setText(String.valueOf(driver.getTripsCompleted()));
        
        double qualityPercent = driver.getQualityAvg();
        if (qualityPercent <= 1.0) {
            qualityPercent = qualityPercent * 100;
        }
        tvQuality.setText(String.format("%.0f%%", qualityPercent));
        
        int streak = driver.getCurrentStreak();
        tvStreak.setText(String.valueOf(streak));
        
        if (streak > 0) {
            startStreakPulse();
        } else {
            stopStreakPulse();
        }
        
        updateTierProgress(driver.getTotalPoints(), tier);
        animateCardsEntrance();
    }

    private void displaySavedOrDefaultData() {
        if (prefManager.hasDriverData()) {
            updateGreeting(prefManager.getDriverName());
            animatePointsCounter(prefManager.getDriverPoints());
            
            String tier = prefManager.getDriverTier();
            tvTier.setText(tier + " Driver");
            updateTierColor(tier);
            
            tvTripsCount.setText(String.valueOf(prefManager.getDriverTrips()));
            
            double qualityPercent = prefManager.getDriverQuality();
            if (qualityPercent <= 1.0) {
                qualityPercent = qualityPercent * 100;
            }
            tvQuality.setText(String.format("%.0f%%", qualityPercent));
            
            int streak = prefManager.getDriverStreak();
            tvStreak.setText(String.valueOf(streak));
            
            if (streak > 0) {
                startStreakPulse();
            } else {
                stopStreakPulse();
            }
            
            updateTierProgress(prefManager.getDriverPoints(), tier);
        } else {
            updateGreeting("Driver");
            tvPoints.setText("0");
            tvTier.setText("Bronze Driver");
            tvTripsCount.setText("0");
            tvQuality.setText("0%");
            tvStreak.setText("0");
            progressTier.setProgress(0);
            tvTierProgress.setText(getString(R.string.home_complete_trips_hint));
            updateTierColor("Bronze");
            stopStreakPulse();
        }
        animateCardsEntrance();
    }
    
    // =========================================================================
    // CARD ENTRANCE ANIMATION
    // =========================================================================
    
    private void animateCardsEntrance() {
        if (contentLayout == null) return;
        
        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            View child = contentLayout.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(50f);
            child.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(i * 80L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }
    }

    private void updateTierColor(String tier) {
        if (tier == null) tier = "Bronze";
        
        int tierColor;
        switch (tier.toLowerCase()) {
            case "silver":
                tierColor = Color.parseColor("#C0C0C0");
                break;
            case "gold":
                tierColor = Color.parseColor("#FFD700");
                break;
            case "platinum":
                tierColor = Color.parseColor("#E5E4E2");
                break;
            case "diamond":
                tierColor = Color.parseColor("#B9F2FF");
                break;
            default: // Bronze
                tierColor = Color.parseColor("#CD7F32");
        }
        
        tvTier.setTextColor(tierColor);
        progressTier.setProgressTintList(android.content.res.ColorStateList.valueOf(tierColor));
    }

    private void updateTierProgress(int points, String tier) {
        if (tier == null) tier = "Bronze";

        int currentTierMin;
        int nextTierPoints;
        String nextTier;

        switch (tier.toLowerCase()) {
            case "bronze":
                currentTierMin = 0;
                nextTierPoints = TIER_SILVER;
                nextTier = "Silver";
                break;
            case "silver":
                currentTierMin = TIER_SILVER;
                nextTierPoints = TIER_GOLD;
                nextTier = "Gold";
                break;
            case "gold":
                currentTierMin = TIER_GOLD;
                nextTierPoints = TIER_PLATINUM;
                nextTier = "Platinum";
                break;
            case "platinum":
                currentTierMin = TIER_PLATINUM;
                nextTierPoints = TIER_DIAMOND;
                nextTier = "Diamond";
                break;
            case "diamond":
                progressTier.setMax(100);
                progressTier.setProgress(100);
                tvTierProgress.setText(getString(R.string.home_max_tier_reached));
                return;
            default:
                currentTierMin = 0;
                nextTierPoints = TIER_SILVER;
                nextTier = "Silver";
        }

        int progressInTier = points - currentTierMin;
        int tierRange = nextTierPoints - currentTierMin;
        
        progressTier.setMax(tierRange);
        progressTier.setProgress(progressInTier);
        tvTierProgress.setText(String.format("%d / %d to %s", points, nextTierPoints, nextTier));
    }

    // onResume reload removed — ViewModel observer handles data updates.
    // Fragment stays alive via hide/show, so onResume only fires on app foreground.
    
    @Override
    public void onPause() {
        super.onPause();
        stopStreakPulse();
    }
}
