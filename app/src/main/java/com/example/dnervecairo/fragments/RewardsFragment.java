package com.example.dnervecairo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dnervecairo.R;
import com.google.android.material.button.MaterialButton;

public class RewardsFragment extends Fragment {

    private static final int MIN_WITHDRAWAL = 500;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rewards, container, false);

        setupWithdrawButton(view);
        loadBalance(view);

        return view;
    }

    private void loadBalance(View view) {
        TextView tvBalance = view.findViewById(R.id.tv_balance);
        // Sample data (later from API)
        tvBalance.setText("450");
    }

    private void setupWithdrawButton(View view) {
        MaterialButton btnWithdraw = view.findViewById(R.id.btn_withdraw);

        btnWithdraw.setOnClickListener(v -> {
            int currentBalance = 450; // Later from API

            if (currentBalance >= MIN_WITHDRAWAL) {
                // Show withdrawal dialog
                Toast.makeText(getContext(), "Withdrawal requested!", Toast.LENGTH_SHORT).show();
            } else {
                int needed = MIN_WITHDRAWAL - currentBalance;
                Toast.makeText(getContext(),
                        "Need " + needed + " more points to withdraw",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}