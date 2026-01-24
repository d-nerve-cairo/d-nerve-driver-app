package com.example.dnervecairo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.adapters.LeaderboardAdapter;
import com.example.dnervecairo.models.LeaderboardEntry;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardFragment extends Fragment {

    private RecyclerView rvLeaderboard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        rvLeaderboard = view.findViewById(R.id.rv_leaderboard);
        setupLeaderboard();

        return view;
    }

    private void setupLeaderboard() {
        // Sample data (later this comes from API)
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

        LeaderboardAdapter adapter = new LeaderboardAdapter(entries);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(getContext()));
        rvLeaderboard.setAdapter(adapter);
    }
}