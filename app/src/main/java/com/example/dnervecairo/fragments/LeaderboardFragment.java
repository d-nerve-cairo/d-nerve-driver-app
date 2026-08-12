package com.example.dnervecairo.fragments;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.dnervecairo.R;
import com.example.dnervecairo.adapters.LeaderboardAdapter;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.responses.LeaderboardResponse;
import com.example.dnervecairo.models.LeaderboardEntry;
import com.example.dnervecairo.utils.NetworkUtils;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardFragment extends Fragment {

    private static final String TAG = "LeaderboardFragment";

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvLeaderboard;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private TextView tvEmpty;
    private MaterialButton btnRetry;
    private MaterialCardView cardYourRank;
    private TextView tvYourRank, tvYourName, tvYourPoints, tvYourInitials, tvGapIndicator;
    private ChipGroup chipGroupSort;
    private TextView tvOthersLabel;

    // Podium views
    private View podiumLayout;
    private TextView tvFirstName, tvFirstPoints, tvFirstInitials;
    private TextView tvSecondName, tvSecondPoints, tvSecondInitials;
    private TextView tvThirdName, tvThirdPoints, tvThirdInitials;

    private PreferenceManager prefManager;
    private String currentSortBy = "total_points";
    private List<LeaderboardEntry> allEntries = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        prefManager = new PreferenceManager(requireContext());

        initViews(view);
        setupSwipeRefresh();
        setupSortChips();
        setupRetryButton();
        loadLeaderboard();

        return view;
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        rvLeaderboard = view.findViewById(R.id.rv_leaderboard);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        tvEmpty = view.findViewById(R.id.tv_empty);
        btnRetry = view.findViewById(R.id.btn_retry);
        cardYourRank = view.findViewById(R.id.card_your_rank);
        tvYourRank = view.findViewById(R.id.tv_your_rank);
        tvYourName = view.findViewById(R.id.tv_your_name);
        tvYourPoints = view.findViewById(R.id.tv_your_points);
        tvYourInitials = view.findViewById(R.id.tv_your_initials);
        tvGapIndicator = view.findViewById(R.id.tv_gap_indicator);
        chipGroupSort = view.findViewById(R.id.chip_group_sort);
        tvOthersLabel = view.findViewById(R.id.tv_top_drivers_label);

        // Podium views
        podiumLayout = view.findViewById(R.id.podium_layout);
        tvFirstName = view.findViewById(R.id.tv_first_name);
        tvFirstPoints = view.findViewById(R.id.tv_first_points);
        tvFirstInitials = view.findViewById(R.id.tv_first_initials);
        tvSecondName = view.findViewById(R.id.tv_second_name);
        tvSecondPoints = view.findViewById(R.id.tv_second_points);
        tvSecondInitials = view.findViewById(R.id.tv_second_initials);
        tvThirdName = view.findViewById(R.id.tv_third_name);
        tvThirdPoints = view.findViewById(R.id.tv_third_points);
        tvThirdInitials = view.findViewById(R.id.tv_third_initials);

        rvLeaderboard.setLayoutManager(new LinearLayoutManager(getContext()));

        // Debug logging
        Log.d(TAG, "Views initialized:");
        Log.d(TAG, "  podiumLayout: " + (podiumLayout != null));
        Log.d(TAG, "  tvFirstName: " + (tvFirstName != null));
        Log.d(TAG, "  tvFirstPoints: " + (tvFirstPoints != null));
        Log.d(TAG, "  tvFirstInitials: " + (tvFirstInitials != null));
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
            swipeRefresh.setOnRefreshListener(this::loadLeaderboard);
        }
    }

    private void setupSortChips() {
        if (chipGroupSort != null) {
            chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;

                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_points) {
                    currentSortBy = "total_points";
                } else if (checkedId == R.id.chip_trips) {
                    currentSortBy = "trips_completed";
                } else if (checkedId == R.id.chip_streak) {
                    currentSortBy = "current_streak";
                } else if (checkedId == R.id.chip_quality) {
                currentSortBy = "quality_avg";
            }Log.d(TAG, "Sort changed to: " + currentSortBy);
                loadLeaderboard();
            });
        }
    }

    private void setupRetryButton() {
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> loadLeaderboard());
        }
    }

    private void loadLeaderboard() {
        Log.d(TAG, "loadLeaderboard() called, sortBy: " + currentSortBy);
        showLoading();

        if (!prefManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in");
            showEmptyState(getString(R.string.leaderboard_login_required), false);
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Log.d(TAG, "No network available");
            showEmptyState(getString(R.string.leaderboard_offline), true);
            showYourRankFromCache();
            return;
        }

        Log.d(TAG, "Making API call...");
        ApiClient.getInstance().getApiService()
                .getLeaderboard(50, currentSortBy)  // Get more drivers for smart display
                .enqueue(new Callback<LeaderboardResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<LeaderboardResponse> call, @NonNull Response<LeaderboardResponse> response) {
                        hideRefreshing();
                        Log.d(TAG, "API response code: " + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "API success - entries count: " + response.body().getLeaderboard().size());
                            displayApiLeaderboard(response.body());
                        } else {
                            Log.e(TAG, "API failed - code: " + response.code());
                            showEmptyState(getString(R.string.leaderboard_load_failed), true);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LeaderboardResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "API error: " + t.getMessage(), t);
                        hideRefreshing();
                        showEmptyState(getString(R.string.leaderboard_network_error), true);
                    }
                });
    }

    private void hideRefreshing() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        rvLeaderboard.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        if (podiumLayout != null) podiumLayout.setVisibility(View.GONE);
        if (tvOthersLabel != null) tvOthersLabel.setVisibility(View.GONE);
    }

    private void showEmptyState(String message, boolean showRetry) {
        progressBar.setVisibility(View.GONE);
        rvLeaderboard.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
        btnRetry.setVisibility(showRetry ? View.VISIBLE : View.GONE);
        if (podiumLayout != null) podiumLayout.setVisibility(View.GONE);
        if (tvOthersLabel != null) tvOthersLabel.setVisibility(View.GONE);
    }

    private void showYourRankFromCache() {
        if (prefManager.hasDriverData()) {
            String name = prefManager.getDriverName();
            tvYourName.setText(name);
            tvYourPoints.setText(String.valueOf(prefManager.getDriverPoints()));
            tvYourRank.setText("#-");
            if (tvYourInitials != null) {
                tvYourInitials.setText(getInitials(name));
            }
            if (tvGapIndicator != null) {
                tvGapIndicator.setVisibility(View.GONE);
            }
            cardYourRank.setVisibility(View.VISIBLE);
        }
    }

    private void displayApiLeaderboard(LeaderboardResponse response) {
        Log.d(TAG, "displayApiLeaderboard called");
        progressBar.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        allEntries.clear();
        String currentDriverId = prefManager.getDriverId();
        int userRank = -1;
        int userPoints = 0;

        Log.d(TAG, "Current driver ID: " + currentDriverId);
        Log.d(TAG, "Processing " + response.getLeaderboard().size() + " entries");

        for (LeaderboardResponse.LeaderboardEntry entry : response.getLeaderboard()) {
            Log.d(TAG, "Entry: rank=" + entry.getRank() + ", name=" + entry.getName() + 
                       ", points=" + entry.getTotalPoints() + ", driverId=" + entry.getDriverId());
            
            LeaderboardEntry leaderboardEntry = new LeaderboardEntry(
                    entry.getRank(),
                    entry.getName(),
                    entry.getTier() + " Driver",
                    entry.getTotalPoints(),
                    entry.getDriverId()
            );
            allEntries.add(leaderboardEntry);

            if (entry.getDriverId() != null && entry.getDriverId().equals(currentDriverId)) {
                userRank = entry.getRank();
                userPoints = entry.getTotalPoints();
                tvYourName.setText(entry.getName());
                tvYourPoints.setText(String.valueOf(entry.getTotalPoints()));
                if (tvYourInitials != null) {
                    tvYourInitials.setText(getInitials(entry.getName()));
                }
                Log.d(TAG, "Found current user at rank: " + userRank);
            }
        }

        Log.d(TAG, "Total entries processed: " + allEntries.size());

        // Update your rank card
        updateYourRankCard(userRank, userPoints);

        // Display podium for top 3
        displayPodium();

        // Display list with smart logic
        displaySmartLeaderboard(currentDriverId, userRank);
    }

    private void updateYourRankCard(int userRank, int userPoints) {
        Log.d(TAG, "updateYourRankCard: rank=" + userRank + ", points=" + userPoints);
        
        if (userRank > 0) {
            tvYourRank.setText("#" + userRank);
            animateRankBadge(tvYourRank);

            // Show gap indicator
            if (tvGapIndicator != null && userRank > 1 && allEntries.size() >= userRank) {
                int pointsAhead = allEntries.get(userRank - 2).getPoints();
                int gap = pointsAhead - userPoints;
                if (gap > 0) {
                    tvGapIndicator.setText(gap + " pts to reach #" + (userRank - 1));
                } else {
                    tvGapIndicator.setText("Tied with #" + (userRank - 1));
                }
                tvGapIndicator.setVisibility(View.VISIBLE);
            } else if (tvGapIndicator != null && userRank == 1) {
                tvGapIndicator.setText("🏆 You're #1!");
                tvGapIndicator.setVisibility(View.VISIBLE);
            }
        } else {
            tvYourRank.setText("#-");
            if (prefManager.hasDriverData()) {
                tvYourName.setText(prefManager.getDriverName());
                tvYourPoints.setText(String.valueOf(prefManager.getDriverPoints()));
                if (tvYourInitials != null) {
                    tvYourInitials.setText(getInitials(prefManager.getDriverName()));
                }
            }
            if (tvGapIndicator != null) {
                tvGapIndicator.setVisibility(View.GONE);
            }
        }
    }

    private void displayPodium() {
        Log.d(TAG, "displayPodium() called");
        Log.d(TAG, "  podiumLayout is null: " + (podiumLayout == null));
        Log.d(TAG, "  allEntries.size(): " + allEntries.size());

        if (podiumLayout == null) {
            Log.e(TAG, "ERROR: podiumLayout is null!");
            return;
        }

        if (allEntries.size() < 3) {
            Log.d(TAG, "Not enough entries for podium (need 3, have " + allEntries.size() + ")");
            podiumLayout.setVisibility(View.GONE);
            return;
        }

        try {
            // First place (center)
            LeaderboardEntry first = allEntries.get(0);
            Log.d(TAG, "Setting first place: " + first.getDriverName());
            if (tvFirstName != null) {
                tvFirstName.setText(getFirstName(first.getDriverName()));
                Log.d(TAG, "  tvFirstName set to: " + getFirstName(first.getDriverName()));
            }
            if (tvFirstPoints != null) {
                tvFirstPoints.setText(first.getPoints() + " pts");
                Log.d(TAG, "  tvFirstPoints set to: " + first.getPoints() + " pts");
            }
            if (tvFirstInitials != null) {
                tvFirstInitials.setText(getInitials(first.getDriverName()));
                Log.d(TAG, "  tvFirstInitials set to: " + getInitials(first.getDriverName()));
            }

            // Second place (left)
            LeaderboardEntry second = allEntries.get(1);
            Log.d(TAG, "Setting second place: " + second.getDriverName());
            if (tvSecondName != null) tvSecondName.setText(getFirstName(second.getDriverName()));
            if (tvSecondPoints != null) tvSecondPoints.setText(second.getPoints() + " pts");
            if (tvSecondInitials != null) tvSecondInitials.setText(getInitials(second.getDriverName()));

            // Third place (right)
            LeaderboardEntry third = allEntries.get(2);
            Log.d(TAG, "Setting third place: " + third.getDriverName());
            if (tvThirdName != null) tvThirdName.setText(getFirstName(third.getDriverName()));
            if (tvThirdPoints != null) tvThirdPoints.setText(third.getPoints() + " pts");
            if (tvThirdInitials != null) tvThirdInitials.setText(getInitials(third.getDriverName()));

            // Show podium
            podiumLayout.setVisibility(View.VISIBLE);
            Log.d(TAG, "Podium visibility set to VISIBLE");

            // Animate podium entrance
            animatePodiumEntrance();

        } catch (Exception e) {
            Log.e(TAG, "Error in displayPodium: " + e.getMessage(), e);
            podiumLayout.setVisibility(View.GONE);
        }
    }

    private void displaySmartLeaderboard(String currentDriverId, int userRank) {
        Log.d(TAG, "displaySmartLeaderboard: userRank=" + userRank + ", totalEntries=" + allEntries.size());
        
        List<LeaderboardEntry> displayList = new ArrayList<>();
        int totalDrivers = allEntries.size();

        if (totalDrivers <= 3) {
            // Only show podium, hide list
            Log.d(TAG, "Only " + totalDrivers + " drivers, hiding list");
            if (tvOthersLabel != null) tvOthersLabel.setVisibility(View.GONE);
            rvLeaderboard.setVisibility(View.GONE);
            return;
        }

        // Show "Other Drivers" label and list
        if (tvOthersLabel != null) tvOthersLabel.setVisibility(View.VISIBLE);
        rvLeaderboard.setVisibility(View.VISIBLE);

        if (totalDrivers <= 10) {
            // Show drivers #4 onwards (podium has #1-3)
            for (int i = 3; i < totalDrivers; i++) {
                displayList.add(allEntries.get(i));
            }
            Log.d(TAG, "Showing drivers 4-" + totalDrivers);
        } else {
            // More than 10 drivers - use smart display
            if (userRank <= 0 || userRank <= 10) {
                // User in top 10 or not found - show #4-10
                for (int i = 3; i < Math.min(10, totalDrivers); i++) {
                    displayList.add(allEntries.get(i));
                }
                Log.d(TAG, "User in top 10, showing drivers 4-10");
            } else {
                // User not in top 10 - show #4-6, then context around user
                for (int i = 3; i < Math.min(6, totalDrivers); i++) {
                    displayList.add(allEntries.get(i));
                }

                // Add drivers around user
                int startContext = Math.max(6, userRank - 3);
                int endContext = Math.min(totalDrivers, userRank + 2);

                for (int i = startContext; i < endContext; i++) {
                    if (i >= 3) {
                        LeaderboardEntry entry = allEntries.get(i);
                        if (!displayList.contains(entry)) {
                            displayList.add(entry);
                        }
                    }
                }
                Log.d(TAG, "Smart display: showing context around rank " + userRank);
            }
        }

        Log.d(TAG, "Final display list size: " + displayList.size());

        LeaderboardAdapter adapter = new LeaderboardAdapter(displayList, currentDriverId);
        rvLeaderboard.setAdapter(adapter);
        animateListEntrance();
    }

    private void animatePodiumEntrance() {
        if (podiumLayout == null) return;

        View secondColumn = podiumLayout.findViewById(R.id.podium_second);
        View firstColumn = podiumLayout.findViewById(R.id.podium_first);
        View thirdColumn = podiumLayout.findViewById(R.id.podium_third);

        Log.d(TAG, "Animating podium - second:" + (secondColumn != null) + 
                   ", first:" + (firstColumn != null) + ", third:" + (thirdColumn != null));

        if (secondColumn != null) animateColumnRise(secondColumn, 100);
        if (firstColumn != null) animateColumnRise(firstColumn, 200);
        if (thirdColumn != null) animateColumnRise(thirdColumn, 150);
    }

    private void animateColumnRise(View view, long delay) {
        view.setTranslationY(100f);
        view.setAlpha(0f);
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateRankBadge(TextView tv) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tv, "scaleX", 0.5f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tv, "scaleY", 0.5f, 1.2f, 1f);
        scaleX.setDuration(500);
        scaleY.setDuration(500);
        scaleX.start();
        scaleY.start();
    }

    private void animateListEntrance() {
        rvLeaderboard.setAlpha(0f);
        rvLeaderboard.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(400)
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

    private String getFirstName(String name) {
        if (name == null || name.isEmpty()) return "Driver";
        String[] parts = name.trim().split("\\s+");
        return parts[0];
    }

    // onResume reload removed — data loads once in onCreateView.
    // User can pull-to-refresh or change sort chip to trigger a new fetch.
}
