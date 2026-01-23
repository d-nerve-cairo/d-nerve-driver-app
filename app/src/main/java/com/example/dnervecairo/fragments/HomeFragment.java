package com.example.dnervecairo.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.dnervecairo.R;
import com.example.dnervecairo.activities.TripActivity;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Button btnStartTrip = view.findViewById(R.id.btn_start_trip);
        btnStartTrip.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TripActivity.class);
            startActivity(intent);
        });

        return view;
    }
}