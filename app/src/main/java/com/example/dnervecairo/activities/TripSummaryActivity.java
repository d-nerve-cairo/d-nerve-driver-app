package com.example.dnervecairo.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dnervecairo.R;
import com.example.dnervecairo.utils.NotificationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

public class TripSummaryActivity extends AppCompatActivity {

    // Intent extras keys
    public static final String EXTRA_DURATION = "duration";
    public static final String EXTRA_DISTANCE = "distance";
    public static final String EXTRA_GPS_POINTS = "gps_points";
    public static final String EXTRA_ROUTE_NAME = "route_name";
    public static final String EXTRA_PASSENGER_COUNT = "passenger_count";
    public static final String EXTRA_SAVED_OFFLINE = "saved_offline";

    // NEW: Live mode intent extras
    public static final String EXTRA_IS_LIVE_MODE = "is_live_mode";
    public static final String EXTRA_SERVER_POINTS_EARNED = "server_points_earned";
    public static final String EXTRA_SERVER_QUALITY_SCORE = "server_quality_score";
    public static final String EXTRA_SERVER_DRIVER_TIER = "server_driver_tier";
    public static final String EXTRA_SERVER_TOTAL_POINTS = "server_total_points";

    // Views for animation
    private ImageView ivSuccessIcon;
    private TextView tvTripComplete;
    private TextView tvPointsEarned;
    private MaterialCardView cardRouteInfo;
    private MaterialCardView cardPoints;
    private MaterialCardView cardDetails;
    private MaterialCardView cardBreakdown;
    private MaterialButton btnBackHome;

    // NEW: Live mode flag
    private boolean isLiveMode = false;

    private NotificationHelper notificationHelper;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_summary);

        // Initialize notification helper
        notificationHelper = new NotificationHelper(this);

        // Get trip data from intent
        int durationMinutes = getIntent().getIntExtra(EXTRA_DURATION, 0);
        double distance = getIntent().getDoubleExtra(EXTRA_DISTANCE, 0.0);
        int gpsPoints = getIntent().getIntExtra(EXTRA_GPS_POINTS, 0);
        String routeName = getIntent().getStringExtra(EXTRA_ROUTE_NAME);
        int passengerCount = getIntent().getIntExtra(EXTRA_PASSENGER_COUNT, 0);
        boolean savedOffline = getIntent().getBooleanExtra(EXTRA_SAVED_OFFLINE, false);

        // NEW: Get live mode data
        isLiveMode = getIntent().getBooleanExtra(EXTRA_IS_LIVE_MODE, false);
        int serverPointsEarned = getIntent().getIntExtra(EXTRA_SERVER_POINTS_EARNED, 0);
        int serverQualityScore = getIntent().getIntExtra(EXTRA_SERVER_QUALITY_SCORE, 0);
        String serverDriverTier = getIntent().getStringExtra(EXTRA_SERVER_DRIVER_TIER);
        int serverTotalPoints = getIntent().getIntExtra(EXTRA_SERVER_TOTAL_POINTS, 0);

        // Determine points and quality based on mode
        int totalPoints;
        int qualityScore;
        int basePoints;
        int distanceBonus;
        int qualityBonus;
        int peakBonus;
        int passengerBonus;

        if (isLiveMode && serverPointsEarned > 0) {
            // NEW: Use server-provided values for live trips
            totalPoints = serverPointsEarned;
            qualityScore = serverQualityScore;

            // For live mode, we show simplified breakdown
            // Server handles all calculations, so we just show the total
            basePoints = 0;
            distanceBonus = 0;
            qualityBonus = 0;
            peakBonus = 0;
            passengerBonus = 0;
        } else {
            // Existing: Calculate points locally for batch mode
            basePoints = 10;
            distanceBonus = calculateDistanceBonus(distance);
            qualityBonus = calculateQualityBonus(gpsPoints, durationMinutes);
            peakBonus = calculatePeakBonus();
            passengerBonus = calculatePassengerBonus(passengerCount);
            totalPoints = basePoints + distanceBonus + qualityBonus + peakBonus + passengerBonus;
            qualityScore = calculateQualityScore(gpsPoints, durationMinutes);
        }

        // Initialize views
        initViews();

        // Update UI
        displayRouteInfo(routeName, passengerCount);
        displayTripDetails(durationMinutes, distance, gpsPoints, qualityScore);

        // NEW: Different breakdown display for live vs batch mode
        if (isLiveMode && serverPointsEarned > 0) {
            displayLiveModePoints(totalPoints, serverDriverTier, serverTotalPoints);
        } else {
            displayPointsBreakdown(basePoints, distanceBonus, qualityBonus, peakBonus, passengerBonus, totalPoints);
        }

        displayOfflineStatus(savedOffline);

        // Start animations
        startEntranceAnimations(totalPoints);

        // Show trip complete notification
        showTripCompleteNotification(totalPoints, routeName);

        // Back button
        btnBackHome.setOnClickListener(v -> {
            animateButtonClick(v);
            handler.postDelayed(this::finish, 150);
        });
    }

    private void initViews() {
        ivSuccessIcon = findViewById(R.id.iv_success_icon);
        tvTripComplete = findViewById(R.id.tv_trip_complete);
        tvPointsEarned = findViewById(R.id.tv_points_earned);
        cardRouteInfo = findViewById(R.id.card_route_info);
        cardPoints = findViewById(R.id.card_points);
        cardDetails = findViewById(R.id.card_details);
        cardBreakdown = findViewById(R.id.card_breakdown);
        btnBackHome = findViewById(R.id.btn_back_home);

        // Set initial state for animation
        if (ivSuccessIcon != null) {
            ivSuccessIcon.setScaleX(0f);
            ivSuccessIcon.setScaleY(0f);
            ivSuccessIcon.setAlpha(0f);
        }
        if (tvTripComplete != null) {
            tvTripComplete.setAlpha(0f);
            tvTripComplete.setTranslationY(20f);
        }
        if (cardRouteInfo != null) {
            cardRouteInfo.setAlpha(0f);
            cardRouteInfo.setTranslationY(30f);
        }
        if (cardPoints != null) {
            cardPoints.setAlpha(0f);
            cardPoints.setScaleX(0.8f);
            cardPoints.setScaleY(0.8f);
        }
        if (cardDetails != null) {
            cardDetails.setAlpha(0f);
            cardDetails.setTranslationY(30f);
        }
        if (cardBreakdown != null) {
            cardBreakdown.setAlpha(0f);
            cardBreakdown.setTranslationY(30f);
        }
        if (btnBackHome != null) {
            btnBackHome.setAlpha(0f);
            btnBackHome.setTranslationY(30f);
        }
    }

    private void startEntranceAnimations(int totalPoints) {
        // 1. Success icon - bounce in
        handler.postDelayed(() -> {
            if (ivSuccessIcon != null) {
                ivSuccessIcon.animate()
                        .scaleX(1f).scaleY(1f)
                        .alpha(1f)
                        .setDuration(500)
                        .setInterpolator(new OvershootInterpolator(1.5f))
                        .start();
            }
        }, 100);

        // 2. "Trip Complete" text
        handler.postDelayed(() -> {
            if (tvTripComplete != null) {
                tvTripComplete.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        }, 300);

        // 3. Route info card
        handler.postDelayed(() -> {
            if (cardRouteInfo != null && cardRouteInfo.getVisibility() == View.VISIBLE) {
                cardRouteInfo.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        }, 400);

        // 4. Points card - scale up with counter animation
        handler.postDelayed(() -> {
            if (cardPoints != null) {
                cardPoints.animate()
                        .alpha(1f)
                        .scaleX(1f).scaleY(1f)
                        .setDuration(500)
                        .setInterpolator(new OvershootInterpolator(1.2f))
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // Start counting animation
                                animatePointsCounter(totalPoints);
                            }
                        })
                        .start();
            }
        }, 500);

        // 5. Details card
        handler.postDelayed(() -> {
            if (cardDetails != null) {
                cardDetails.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        }, 700);

        // 6. Breakdown card
        handler.postDelayed(() -> {
            if (cardBreakdown != null) {
                cardBreakdown.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        }, 850);

        // 7. Back button
        handler.postDelayed(() -> {
            if (btnBackHome != null) {
                btnBackHome.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        }, 1000);
    }

    private void animatePointsCounter(int targetPoints) {
        if (tvPointsEarned == null) return;

        ValueAnimator animator = ValueAnimator.ofInt(0, targetPoints);
        animator.setDuration(800);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            tvPointsEarned.setText(String.format(Locale.getDefault(), "+%d", value));
        });
        animator.start();
    }

    private void animateButtonClick(View v) {
        v.animate()
                .scaleX(0.95f).scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() ->
                        v.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(100)
                                .start()
                ).start();
    }

    private void showTripCompleteNotification(int pointsEarned, String routeName) {
        notificationHelper.showTripCompleteNotification(pointsEarned, routeName);
    }

    private void displayRouteInfo(String routeName, int passengerCount) {
        TextView tvRouteName = findViewById(R.id.tv_route_name);
        TextView tvPassengerCount = findViewById(R.id.tv_passenger_count);
        LinearLayout layoutRouteInfo = findViewById(R.id.layout_route_info);

        if (routeName != null && !routeName.isEmpty()) {
            tvRouteName.setText(routeName);
            if (cardRouteInfo != null) cardRouteInfo.setVisibility(View.VISIBLE);
            if (layoutRouteInfo != null) layoutRouteInfo.setVisibility(View.VISIBLE);
        } else {
            if (cardRouteInfo != null) cardRouteInfo.setVisibility(View.GONE);
        }

        if (tvPassengerCount != null) {
            tvPassengerCount.setText(String.valueOf(passengerCount));
        }
    }

    private void displayTripDetails(int duration, double distance, int gpsPoints, int quality) {
        TextView tvDuration = findViewById(R.id.tv_duration);
        TextView tvDistance = findViewById(R.id.tv_distance);
        TextView tvGpsPoints = findViewById(R.id.tv_gps_points);
        TextView tvQuality = findViewById(R.id.tv_quality);
        TextView tvQualityLabel = findViewById(R.id.tv_quality_label);

        if (tvDuration != null) {
            tvDuration.setText(String.format(Locale.getDefault(), "%d min", duration));
        }
        if (tvDistance != null) {
            tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", distance));
        }
        if (tvGpsPoints != null) {
            tvGpsPoints.setText(String.valueOf(gpsPoints));
        }
        if (tvQuality != null) {
            tvQuality.setText(String.format(Locale.getDefault(), "%d%%", quality));

            // Color and label based on quality score
            String qualityText;
            int colorRes;

            if (quality >= 90) {
                qualityText = "Excellent";
                colorRes = R.color.success;
            } else if (quality >= 70) {
                qualityText = "Good";
                colorRes = R.color.success;
            } else if (quality >= 50) {
                qualityText = "Fair";
                colorRes = R.color.warning;
            } else {
                qualityText = "Poor";
                colorRes = R.color.error;
            }

            tvQuality.setTextColor(getResources().getColor(colorRes, null));
            if (tvQualityLabel != null) {
                tvQualityLabel.setText(qualityText);
                tvQualityLabel.setTextColor(getResources().getColor(colorRes, null));
            }
        }
    }

    // =========================================================================
    // NEW: Display for Live Mode (server-provided values)
    // =========================================================================

    /**
     * Display points for live mode trips using server-provided values.
     * Shows tier badge and total driver points instead of detailed breakdown.
     */
    private void displayLiveModePoints(int pointsEarned, String driverTier, int totalDriverPoints) {
        TextView tvBasePoints = findViewById(R.id.tv_base_points);
        TextView tvTotalPoints = findViewById(R.id.tv_total_points);

        // Hide individual bonus rows for live mode (server calculates everything)
        LinearLayout layoutDistanceBonus = findViewById(R.id.layout_distance_bonus);
        LinearLayout layoutQualityBonus = findViewById(R.id.layout_quality_bonus);
        LinearLayout layoutPeakBonus = findViewById(R.id.layout_peak_bonus);
        LinearLayout layoutPassengerBonus = findViewById(R.id.layout_passenger_bonus);

        if (layoutDistanceBonus != null) layoutDistanceBonus.setVisibility(View.GONE);
        if (layoutQualityBonus != null) layoutQualityBonus.setVisibility(View.GONE);
        if (layoutPeakBonus != null) layoutPeakBonus.setVisibility(View.GONE);
        if (layoutPassengerBonus != null) layoutPassengerBonus.setVisibility(View.GONE);

        // Set initial points display (will be animated)
        if (tvPointsEarned != null) {
            tvPointsEarned.setText("+0");
        }

        // Show "Server Calculated" as base
        if (tvBasePoints != null) {
            tvBasePoints.setText(String.valueOf(pointsEarned));
        }

        if (tvTotalPoints != null) {
            tvTotalPoints.setText(String.valueOf(pointsEarned));
        }

        // NEW: Display driver tier if available
        TextView tvDriverTier = findViewById(R.id.tv_driver_tier);
        TextView tvTotalDriverPoints = findViewById(R.id.tv_total_driver_points);
        LinearLayout layoutTierInfo = findViewById(R.id.layout_tier_info);

        if (driverTier != null && !driverTier.isEmpty()) {
            // Show tier badge if the view exists
            if (tvDriverTier != null) {
                String tierEmoji = getTierEmoji(driverTier);
                tvDriverTier.setText(tierEmoji + " " + driverTier);
                tvDriverTier.setVisibility(View.VISIBLE);
            }

            // Show total driver points if view exists
            if (tvTotalDriverPoints != null && totalDriverPoints > 0) {
                tvTotalDriverPoints.setText(String.format(Locale.getDefault(),
                        "Total: %,d pts", totalDriverPoints));
                tvTotalDriverPoints.setVisibility(View.VISIBLE);
            }

            // Show tier info layout if exists
            if (layoutTierInfo != null) {
                layoutTierInfo.setVisibility(View.VISIBLE);
            }
        }

        // NEW: Update "Trip Complete" text for live mode
        if (tvTripComplete != null) {
            tvTripComplete.setText("🟢 Live Trip Complete!");
        }
    }

    /**
     * Get emoji for driver tier
     */
    private String getTierEmoji(String tier) {
        if (tier == null) return "🚐";

        switch (tier.toLowerCase()) {
            case "gold":
                return "🥇";
            case "silver":
                return "🥈";
            case "bronze":
                return "🥉";
            case "platinum":
                return "💎";
            case "diamond":
                return "💠";
            default:
                return "🚐";
        }
    }

    // =========================================================================
    // Existing: Display for Batch Mode (local calculation)
    // =========================================================================

    private void displayPointsBreakdown(int base, int distance, int quality, int peak, int passenger, int total) {
        TextView tvBasePoints = findViewById(R.id.tv_base_points);
        TextView tvDistanceBonus = findViewById(R.id.tv_distance_bonus);
        TextView tvQualityBonus = findViewById(R.id.tv_quality_bonus);
        TextView tvPeakBonus = findViewById(R.id.tv_peak_bonus);
        TextView tvPassengerBonus = findViewById(R.id.tv_passenger_bonus);
        TextView tvTotalPoints = findViewById(R.id.tv_total_points);

        LinearLayout layoutDistanceBonus = findViewById(R.id.layout_distance_bonus);
        LinearLayout layoutQualityBonus = findViewById(R.id.layout_quality_bonus);
        LinearLayout layoutPeakBonus = findViewById(R.id.layout_peak_bonus);
        LinearLayout layoutPassengerBonus = findViewById(R.id.layout_passenger_bonus);

        // Set initial points display (will be animated)
        if (tvPointsEarned != null) {
            tvPointsEarned.setText("+0");
        }

        if (tvBasePoints != null) {
            tvBasePoints.setText(String.valueOf(base));
        }

        // Distance bonus
        if (layoutDistanceBonus != null && tvDistanceBonus != null) {
            if (distance > 0) {
                layoutDistanceBonus.setVisibility(View.VISIBLE);
                tvDistanceBonus.setText(String.format(Locale.getDefault(), "+%d", distance));
            } else {
                layoutDistanceBonus.setVisibility(View.GONE);
            }
        }

        // Quality bonus
        if (layoutQualityBonus != null && tvQualityBonus != null) {
            if (quality > 0) {
                layoutQualityBonus.setVisibility(View.VISIBLE);
                tvQualityBonus.setText(String.format(Locale.getDefault(), "+%d", quality));
            } else {
                layoutQualityBonus.setVisibility(View.GONE);
            }
        }

        // Peak bonus
        if (layoutPeakBonus != null && tvPeakBonus != null) {
            if (peak > 0) {
                layoutPeakBonus.setVisibility(View.VISIBLE);
                tvPeakBonus.setText(String.format(Locale.getDefault(), "+%d", peak));
            } else {
                layoutPeakBonus.setVisibility(View.GONE);
            }
        }

        // Passenger bonus
        if (layoutPassengerBonus != null && tvPassengerBonus != null) {
            if (passenger > 0) {
                layoutPassengerBonus.setVisibility(View.VISIBLE);
                tvPassengerBonus.setText(String.format(Locale.getDefault(), "+%d", passenger));
            } else {
                layoutPassengerBonus.setVisibility(View.GONE);
            }
        }

        if (tvTotalPoints != null) {
            tvTotalPoints.setText(String.valueOf(total));
        }
    }

    private void displayOfflineStatus(boolean savedOffline) {
        TextView tvOfflineStatus = findViewById(R.id.tv_offline_status);
        if (tvOfflineStatus != null) {
            if (savedOffline) {
                tvOfflineStatus.setVisibility(View.VISIBLE);
            } else {
                tvOfflineStatus.setVisibility(View.GONE);
            }
        }
    }

    private int calculateQualityScore(int gpsPoints, int durationMinutes) {
        if (durationMinutes <= 0) return 0;
        // Expected: ~2 points per minute (every 30 seconds)
        int expectedPoints = durationMinutes * 2;
        int score = (int) ((gpsPoints / (float) expectedPoints) * 100);
        return Math.min(score, 100); // Cap at 100%
    }

    private int calculateDistanceBonus(double distanceKm) {
        // 1 point per km, max 10
        return Math.min((int) distanceKm, 10);
    }

    private int calculateQualityBonus(int gpsPoints, int durationMinutes) {
        int quality = calculateQualityScore(gpsPoints, durationMinutes);
        if (quality >= 90) return 5;
        if (quality >= 70) return 3;
        if (quality >= 50) return 1;
        return 0;
    }

    private int calculatePeakBonus() {
        // Check if current time is peak hour (7-9 AM or 5-8 PM)
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 20)) {
            return 3;
        }
        return 0;
    }

    private int calculatePassengerBonus(int passengerCount) {
        // 1 point per passenger, max 10
        return Math.min(passengerCount, 10);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}