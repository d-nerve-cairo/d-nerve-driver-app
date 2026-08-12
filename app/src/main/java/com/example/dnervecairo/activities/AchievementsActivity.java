package com.example.dnervecairo.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.dnervecairo.R;
import com.example.dnervecairo.adapters.BadgeAdapter;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.responses.BadgeResponse;
import com.example.dnervecairo.utils.NotificationHelper;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AchievementsActivity extends AppCompatActivity {

    private RecyclerView rvBadges;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private TextView tvEarnedCount;
    private TabLayout tabLayout;

    private BadgeAdapter adapter;
    private PreferenceManager prefManager;
    private NotificationHelper notificationHelper;
    private String driverId;

    private List<BadgeResponse> allBadges = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        prefManager = new PreferenceManager(this);
        notificationHelper = new NotificationHelper(this);
        driverId = prefManager.getDriverId();

        initViews();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupSwipeRefresh();
        
        // First check for new badges, then load all badges
        checkAndAwardBadges();
    }

    private void initViews() {
        rvBadges = findViewById(R.id.rv_badges);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        tvEarnedCount = findViewById(R.id.tv_earned_count);
        tabLayout = findViewById(R.id.tab_layout);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentFilter = "all"; break;
                    case 1: currentFilter = "trips"; break;
                    case 2: currentFilter = "quality"; break;
                    case 3: currentFilter = "streak"; break;
                    case 4: currentFilter = "points"; break;
                }
                filterBadges();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new BadgeAdapter();
        rvBadges.setLayoutManager(new LinearLayoutManager(this));
        rvBadges.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> {
            checkAndAwardBadges();
        });
    }

    /**
     * Check and award any new badges, then load all badges
     */
    private void checkAndAwardBadges() {
        showLoading(true);

        ApiClient.getInstance().getApiService()
                .checkBadges(driverId)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call,
                                           @NonNull Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject result = response.body();
                            
                            // Check if any new badges were earned
                            if (result.has("newly_earned")) {
                                JsonArray newlyEarned = result.getAsJsonArray("newly_earned");
                                if (newlyEarned.size() > 0) {
                                    // Show notification for each new badge
                                    for (int i = 0; i < newlyEarned.size(); i++) {
                                        JsonObject badge = newlyEarned.get(i).getAsJsonObject();
                                        String badgeName = badge.get("name").getAsString();
                                        String badgeDesc = badge.has("description") ? 
                                                badge.get("description").getAsString() : "";
                                        
                                        notificationHelper.showAchievementNotification(badgeName, badgeDesc);
                                    }
                                    
                                    // Show toast
                                    Toast.makeText(AchievementsActivity.this,
                                            "🏆 " + newlyEarned.size() + " new badge(s) earned!",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                        
                        // Now load all badges
                        loadBadges();
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        // Still try to load badges even if check fails
                        loadBadges();
                    }
                });
    }

    private void loadBadges() {
        ApiClient.getInstance().getApiService()
                .getBadgeProgress(driverId)
                .enqueue(new Callback<List<BadgeResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<BadgeResponse>> call,
                                           @NonNull Response<List<BadgeResponse>> response) {
                        showLoading(false);
                        
                        if (response.isSuccessful() && response.body() != null) {
                            allBadges = response.body();
                            updateEarnedCount();
                            filterBadges();
                        } else {
                            showError(getString(R.string.error_generic));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<BadgeResponse>> call, @NonNull Throwable t) {
                        showLoading(false);
                        showError(getString(R.string.error_network));
                    }
                });
    }

    private void filterBadges() {
        List<BadgeResponse> filtered = new ArrayList<>();

        for (BadgeResponse badge : allBadges) {
            if (currentFilter.equals("all") || badge.getCategory().equals(currentFilter)) {
                filtered.add(badge);
            }
        }

        if (filtered.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            adapter.setBadges(filtered);
        }
    }

    private void updateEarnedCount() {
        int earned = 0;
        for (BadgeResponse badge : allBadges) {
            if (badge.isEarned()) earned++;
        }
        tvEarnedCount.setText(earned + " / " + allBadges.size() + " " + getString(R.string.achievements_earned));
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvBadges.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            emptyState.setVisibility(View.VISIBLE);
            rvBadges.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvBadges.setVisibility(View.VISIBLE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        showEmptyState(true);
    }
}
