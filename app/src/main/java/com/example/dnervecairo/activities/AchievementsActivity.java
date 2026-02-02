package com.example.dnervecairo.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.adapters.BadgeAdapter;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.responses.BadgeResponse;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AchievementsActivity extends AppCompatActivity {

    private RecyclerView rvBadges;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView tvEarnedCount;
    private TabLayout tabLayout;

    private BadgeAdapter adapter;
    private PreferenceManager prefManager;
    private String driverId;

    private List<BadgeResponse> allBadges = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        prefManager = new PreferenceManager(this);
        driverId = prefManager.getDriverId();

        initViews();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        loadBadges();
    }

    private void initViews() {
        rvBadges = findViewById(R.id.rv_badges);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyState = findViewById(R.id.tv_empty_state);
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

    private void loadBadges() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        ApiClient.getInstance().getApiService()
                .getBadgeProgress(driverId)
                .enqueue(new Callback<List<BadgeResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<BadgeResponse>> call,
                                           @NonNull Response<List<BadgeResponse>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            allBadges = response.body();
                            updateEarnedCount();
                            filterBadges();
                        } else {
                            showError("Failed to load badges");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<BadgeResponse>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        showError("Network error: " + t.getMessage());
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
            tvEmptyState.setVisibility(View.VISIBLE);
            rvBadges.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvBadges.setVisibility(View.VISIBLE);
            adapter.setBadges(filtered);
        }
    }

    private void updateEarnedCount() {
        int earned = 0;
        for (BadgeResponse badge : allBadges) {
            if (badge.isEarned()) earned++;
        }
        tvEarnedCount.setText(earned + " / " + allBadges.size() + " Earned");
    }

    private void showError(String message) {
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
        rvBadges.setVisibility(View.GONE);
    }
}