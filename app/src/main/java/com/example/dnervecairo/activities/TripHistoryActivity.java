package com.example.dnervecairo.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TripHistoryAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private TextView tvTotalTrips;

    private PreferenceManager prefManager;
    private int offset = 0;
    private final int limit = 20;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_history);

        prefManager = new PreferenceManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        loadTrips(true);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_trips);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        emptyState = findViewById(R.id.empty_state);
        progressBar = findViewById(R.id.progress_bar);
        tvTotalTrips = findViewById(R.id.tv_total_trips);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
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
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> loadTrips(true));
    }

    private void loadTrips(boolean refresh) {
        if (isLoading) return;
        isLoading = true;

        if (refresh) {
            offset = 0;
            hasMoreData = true;
            progressBar.setVisibility(View.VISIBLE);
        }

        String driverId = prefManager.getDriverId();

        ApiClient.getInstance().getApiService()
                .getTripHistory(driverId, limit, offset)
                .enqueue(new Callback<TripsListResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TripsListResponse> call,
                                           @NonNull Response<TripsListResponse> response) {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            TripsListResponse data = response.body();

                            if (refresh) {
                                adapter.setTrips(data.getTrips());
                            } else {
                                adapter.addTrips(data.getTrips());
                            }

                            offset += data.getTrips().size();
                            hasMoreData = data.getTrips().size() == limit;

                            // Update header
                            tvTotalTrips.setText(String.format("%d trips", data.getTotal()));

                            // Show/hide empty state
                            updateEmptyState(data.getTotal() == 0);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TripsListResponse> call, @NonNull Throwable t) {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(TripHistoryActivity.this,
                                "Failed to load trips", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}