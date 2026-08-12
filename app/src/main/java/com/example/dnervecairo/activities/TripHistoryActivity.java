package com.example.dnervecairo.activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.dnervecairo.R;
import com.example.dnervecairo.adapters.TripHistoryAdapter;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.responses.TripResponse;
import com.example.dnervecairo.api.responses.TripsListResponse;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripHistoryActivity extends AppCompatActivity {

    // Views
    private RecyclerView recyclerView;
    private TripHistoryAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyState;
    private LinearLayout errorState;
    private ProgressBar progressBar;
    private MaterialButton btnRetry;
    private MaterialButton btnStartTrip;

    // Stats views
    private MaterialCardView cardStats;
    private TextView tvTotalTrips, tvTotalPoints, tvTotalDistance;

    // Filter chips
    private ChipGroup chipGroupFilter;
    private Chip chipAll, chipWeek, chipMonth;

    // Data
    private PreferenceManager prefManager;
    private int offset = 0;
    private final int limit = 50;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private List<TripResponse> allTrips = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isFirstLoad = true;

    // Filter constants
    private static final int FILTER_ALL = 0;
    private static final int FILTER_WEEK = 1;
    private static final int FILTER_MONTH = 2;
    private int currentFilter = FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_history);

        prefManager = new PreferenceManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        setupButtons();
        setupFilterChips();
        prepareEntranceAnimations();
        loadTrips(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_trips);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        emptyState = findViewById(R.id.empty_state);
        errorState = findViewById(R.id.error_state);
        progressBar = findViewById(R.id.progress_bar);
        btnRetry = findViewById(R.id.btn_retry);
        btnStartTrip = findViewById(R.id.btn_start_trip);

        // Stats - these may be null if not in layout
        cardStats = findViewById(R.id.card_stats);
        tvTotalTrips = findViewById(R.id.tv_total_trips);
        tvTotalPoints = findViewById(R.id.tv_total_points);
        tvTotalDistance = findViewById(R.id.tv_total_distance);

        // Filter chips - these may be null if not in layout
        chipGroupFilter = findViewById(R.id.chip_group_filter);
        chipAll = findViewById(R.id.chip_all);
        chipWeek = findViewById(R.id.chip_week);
        chipMonth = findViewById(R.id.chip_month);
    }

    private void prepareEntranceAnimations() {
        if (cardStats != null) {
            cardStats.setAlpha(0f);
            cardStats.setTranslationY(20f);
        }
        if (chipGroupFilter != null) {
            chipGroupFilter.setAlpha(0f);
        }
    }

    private void playEntranceAnimations() {
        if (!isFirstLoad) return;
        isFirstLoad = false;

        // Stats card
        if (cardStats != null) {
            cardStats.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }

        // Filter chips
        handler.postDelayed(() -> {
            if (chipGroupFilter != null) {
                chipGroupFilter.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
            }
        }, 200);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                animateButtonClick(v);
                handler.postDelayed(this::finish, 100);
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new TripHistoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Pagination scroll listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && hasMoreData) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadTrips(false);
                    }
                }
            }
        });

        // Click listener - opens trip map
        adapter.setOnTripClickListener(trip -> {
            Intent intent = new Intent(this, TripMapActivity.class);
            intent.putExtra(TripMapActivity.EXTRA_TRIP_ID, trip.getTripId());
            intent.putExtra(TripMapActivity.EXTRA_GPS_POINTS, trip.getGpsPointsJson());
            intent.putExtra(TripMapActivity.EXTRA_DURATION, (double) trip.getDurationMinutes());
            intent.putExtra(TripMapActivity.EXTRA_DISTANCE, trip.getDistanceKm());
            intent.putExtra(TripMapActivity.EXTRA_POINTS_EARNED, trip.getPointsEarned());
            intent.putExtra(TripMapActivity.EXTRA_QUALITY, trip.getQualityScore());
            intent.putExtra(TripMapActivity.EXTRA_DATE, trip.getStartTime());
            startActivity(intent);
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> loadTrips(true));
    }

    private void setupButtons() {
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> {
                animateButtonClick(v);
                handler.postDelayed(() -> {
                    showLoading();
                    loadTrips(true);
                }, 100);
            });
        }

        if (btnStartTrip != null) {
            btnStartTrip.setOnClickListener(v -> {
                animateButtonClick(v);
                handler.postDelayed(() -> {
                    startActivity(new Intent(this, StartTripActivity.class));
                    finish();
                }, 100);
            });
        }
    }

    private void setupFilterChips() {
        if (chipAll != null) {
            chipAll.setOnClickListener(v -> {
                currentFilter = FILTER_ALL;
                filterAndDisplayTrips();
            });
        }
        if (chipWeek != null) {
            chipWeek.setOnClickListener(v -> {
                currentFilter = FILTER_WEEK;
                filterAndDisplayTrips();
            });
        }
        if (chipMonth != null) {
            chipMonth.setOnClickListener(v -> {
                currentFilter = FILTER_MONTH;
                filterAndDisplayTrips();
            });
        }
    }

    private void animateButtonClick(View v) {
        if (v == null) return;
        v.animate()
            .scaleX(0.95f).scaleY(0.95f)
            .setDuration(50)
            .withEndAction(() ->
                v.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(50)
                    .start()
            ).start();
    }

    private void loadTrips(boolean refresh) {
        if (isLoading) return;
        isLoading = true;

        if (refresh) {
            offset = 0;
            hasMoreData = true;
            allTrips.clear();
            showLoading();
        }

        String driverId = prefManager.getDriverId();

        ApiClient.getInstance().getApiService()
                .getTripHistory(driverId, limit, offset)
                .enqueue(new Callback<TripsListResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TripsListResponse> call,
                                           @NonNull Response<TripsListResponse> response) {
                        isLoading = false;
                        hideLoading();

                        if (response.isSuccessful() && response.body() != null) {
                            TripsListResponse data = response.body();
                            List<TripResponse> trips = data.getTrips();

                            if (trips != null) {
                                if (refresh) {
                                    allTrips = new ArrayList<>(trips);
                                } else {
                                    allTrips.addAll(trips);
                                }

                                offset += trips.size();
                                hasMoreData = trips.size() == limit;
                            }

                            // Update stats
                            updateStats(data.getTotal());

                            // Filter and display
                            filterAndDisplayTrips();

                            // Play animations
                            playEntranceAnimations();

                            // Animate list items
                            if (refresh) {
                                animateListItems();
                            }
                        } else {
                            if (refresh && adapter.getItemCount() == 0) {
                                showErrorState();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TripsListResponse> call, @NonNull Throwable t) {
                        isLoading = false;
                        hideLoading();

                        if (adapter.getItemCount() == 0) {
                            showErrorState();
                        }
                    }
                });
    }

    private void filterAndDisplayTrips() {
        List<TripResponse> filtered = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        Date now = new Date();

        for (TripResponse trip : allTrips) {
            Date tripDate = parseDate(trip.getStartTime());
            if (tripDate == null) {
                filtered.add(trip); // Include if can't parse date
                continue;
            }

            switch (currentFilter) {
                case FILTER_WEEK:
                    cal.setTime(now);
                    cal.add(Calendar.DAY_OF_YEAR, -7);
                    if (tripDate.after(cal.getTime())) {
                        filtered.add(trip);
                    }
                    break;
                case FILTER_MONTH:
                    cal.setTime(now);
                    cal.add(Calendar.MONTH, -1);
                    if (tripDate.after(cal.getTime())) {
                        filtered.add(trip);
                    }
                    break;
                default: // FILTER_ALL
                    filtered.add(trip);
                    break;
            }
        }

        if (filtered.isEmpty()) {
            showEmptyState();
        } else {
            showContent();
            adapter.setTrips(filtered);
        }
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return format.parse(dateStr.split("\\.")[0]);
        } catch (ParseException e) {
            return null;
        }
    }

    private void updateStats(int totalCount) {
        int totalPoints = 0;
        double totalDistance = 0;

        for (TripResponse trip : allTrips) {
            totalPoints += trip.getPointsEarned();
            totalDistance += trip.getDistanceKm();
        }

        // Update total trips (use server count or local count)
        if (tvTotalTrips != null) {
            int displayCount = totalCount > 0 ? totalCount : allTrips.size();
            animateCounter(tvTotalTrips, 0, displayCount, "");
        }
        
        if (tvTotalPoints != null) {
            animateCounter(tvTotalPoints, 0, totalPoints, " pts");
        }
        
        if (tvTotalDistance != null) {
            tvTotalDistance.setText(String.format(Locale.US, "%.1f km", totalDistance));
        }
    }

    private void animateCounter(TextView textView, int from, int to, String suffix) {
        if (textView == null) return;
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(600);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            textView.setText(String.format(Locale.US, "%d%s", value, suffix));
        });
        animator.start();
    }

    private void animateListItems() {
        recyclerView.post(() -> {
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                if (child != null) {
                    child.setAlpha(0f);
                    child.setTranslationY(50f);
                    final int delay = i * 50;
                    handler.postDelayed(() -> {
                        child.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(300)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                    }, delay);
                }
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        swipeRefresh.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        if (errorState != null) errorState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private void showContent() {
        swipeRefresh.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        if (errorState != null) errorState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        swipeRefresh.setVisibility(View.GONE);
        if (errorState != null) errorState.setVisibility(View.GONE);
    }

    private void showErrorState() {
        if (errorState != null) {
            errorState.setVisibility(View.VISIBLE);
            swipeRefresh.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        }
    }
}
