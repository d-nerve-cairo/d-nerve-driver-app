package com.example.dnervecairo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.adapters.LeaderboardAdapter;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.responses.LeaderboardResponse;
import com.example.dnervecairo.models.LeaderboardEntry;
import com.example.dnervecairo.utils.NetworkUtils;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardFragment extends Fragment {

    private RecyclerView rvLeaderboard;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private TextView tvEmpty;
    private MaterialButton btnRetry;
    private MaterialCardView cardYourRank;
    private TextView tvYourRank, tvYourName, tvYourPoints;

    private PreferenceManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        prefManager = new PreferenceManager(requireContext());

        initViews(view);
        setupRetryButton();
        loadLeaderboard();

        return view;
    }

    private void initViews(View view) {
        rvLeaderboard = view.findViewById(R.id.rv_leaderboard);
        progressBar = view.findViewById(R.id.progress_bar);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        tvEmpty = view.findViewById(R.id.tv_empty);
        ImageView ivEmptyIcon = view.findViewById(R.id.iv_empty_icon);
        btnRetry = view.findViewById(R.id.btn_retry);
        cardYourRank = view.findViewById(R.id.card_your_rank);
        tvYourRank = view.findViewById(R.id.tv_your_rank);
        tvYourName = view.findViewById(R.id.tv_your_name);
        tvYourPoints = view.findViewById(R.id.tv_your_points);

        rvLeaderboard.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupRetryButton() {
        btnRetry.setOnClickListener(v -> loadLeaderboard());
    }

    private void loadLeaderboard() {
        // Show loading
        showLoading();

        // Check if logged in
        if (!prefManager.isLoggedIn()) {
            showEmptyState("Login to view leaderboard", false);
            return;
        }

        // Check if online
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showEmptyState("📴 Leaderboard requires internet connection", true);
            showYourRankFromCache();
            return;
        }

        ApiClient.getInstance().getApiService()
                .getLeaderboard(10, "total_points")
                .enqueue(new Callback<LeaderboardResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<LeaderboardResponse> call, @NonNull Response<LeaderboardResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            displayApiLeaderboard(response.body());
                        } else {
                            showEmptyState("Failed to load leaderboard", true);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LeaderboardResponse> call, @NonNull Throwable t) {
                        showEmptyState("📴 Network error - check your connection", true);
                    }
                });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        rvLeaderboard.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showEmptyState(String message, boolean showRetry) {
        progressBar.setVisibility(View.GONE);
        rvLeaderboard.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
        btnRetry.setVisibility(showRetry ? View.VISIBLE : View.GONE);
    }

    private void showYourRankFromCache() {
        // Show cached user data in "Your Rank" card
        if (prefManager.hasDriverData()) {
            tvYourName.setText(prefManager.getDriverName());
            tvYourPoints.setText(String.valueOf(prefManager.getDriverPoints()));
            tvYourRank.setText("#-");
            cardYourRank.setVisibility(View.VISIBLE);
        }
    }

    private void displayApiLeaderboard(LeaderboardResponse response) {
        progressBar.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        rvLeaderboard.setVisibility(View.VISIBLE);

        List<LeaderboardEntry> entries = new ArrayList<>();
        String currentDriverId = prefManager.getDriverId();
        int userRank = -1;

        for (LeaderboardResponse.LeaderboardEntry entry : response.getLeaderboard()) {
            entries.add(new LeaderboardEntry(
                    entry.getRank(),
                    entry.getName(),
                    entry.getTier() + " Driver",
                    entry.getTotalPoints()
            ));

            // Check if this is the current user
            if (entry.getDriverId() != null && entry.getDriverId().equals(currentDriverId)) {
                userRank = entry.getRank();
                tvYourName.setText(entry.getName());
                tvYourPoints.setText(String.valueOf(entry.getTotalPoints()));
            }
        }

        // Update your rank card
        if (userRank > 0) {
            tvYourRank.setText("#" + userRank);
        } else {
            tvYourRank.setText("#-");
            // Use cached data for name/points
            if (prefManager.hasDriverData()) {
                tvYourName.setText(prefManager.getDriverName());
                tvYourPoints.setText(String.valueOf(prefManager.getDriverPoints()));
            }
        }

        displayLeaderboard(entries);
    }

    private void displayLeaderboard(List<LeaderboardEntry> entries) {
        LeaderboardAdapter adapter = new LeaderboardAdapter(entries);
        rvLeaderboard.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLeaderboard();
    }
}