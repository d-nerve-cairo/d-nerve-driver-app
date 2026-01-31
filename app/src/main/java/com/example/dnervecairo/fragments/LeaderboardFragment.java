package com.example.dnervecairo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.dnervecairo.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardFragment extends Fragment {

    private RecyclerView rvLeaderboard;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private PreferenceManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        prefManager = new PreferenceManager(requireContext());

        rvLeaderboard = view.findViewById(R.id.rv_leaderboard);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(getContext()));

        loadLeaderboard();

        return view;
    }

    private void loadLeaderboard() {
        // Check if logged in
        if (!prefManager.isLoggedIn()) {
            displaySampleData();
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
                            displaySampleData();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LeaderboardResponse> call, @NonNull Throwable t) {
                        Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                        displaySampleData();
                    }
                });
    }

    private void displayApiLeaderboard(LeaderboardResponse response) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        for (LeaderboardResponse.LeaderboardEntry entry : response.getLeaderboard()) {
            entries.add(new LeaderboardEntry(
                    entry.getRank(),
                    entry.getName(),
                    entry.getTier() + " Driver",
                    entry.getTotalPoints()
            ));
        }

        displayLeaderboard(entries);
    }

    private void displayLeaderboard(List<LeaderboardEntry> entries) {
        LeaderboardAdapter adapter = new LeaderboardAdapter(entries);
        rvLeaderboard.setAdapter(adapter);
    }

    private void displaySampleData() {
        List<LeaderboardEntry> entries = new ArrayList<>();
        entries.add(new LeaderboardEntry(1, "Ahmed Hassan", "Platinum Driver", 2850));
        entries.add(new LeaderboardEntry(2, "Mohamed Ali", "Gold Driver", 2340));
        entries.add(new LeaderboardEntry(3, "Omar Khaled", "Gold Driver", 1890));
        entries.add(new LeaderboardEntry(4, "You", "Bronze Driver", 450));
        entries.add(new LeaderboardEntry(5, "Mahmoud Samir", "Silver Driver", 420));
        entries.add(new LeaderboardEntry(6, "Youssef Ahmed", "Bronze Driver", 380));
        entries.add(new LeaderboardEntry(7, "Khaled Ibrahim", "Bronze Driver", 290));
        entries.add(new LeaderboardEntry(8, "Hassan Mostafa", "Bronze Driver", 210));
        entries.add(new LeaderboardEntry(9, "Ali Nasser", "Bronze Driver", 150));
        entries.add(new LeaderboardEntry(10, "Tarek Adel", "Bronze Driver", 90));

        displayLeaderboard(entries);
    }
}