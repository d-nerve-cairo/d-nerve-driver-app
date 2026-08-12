package com.example.dnervecairo.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.dnervecairo.R;
import com.example.dnervecairo.adapters.WithdrawalAdapter;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.requests.WithdrawalRequest;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.api.responses.PointsHistoryResponse;
import com.example.dnervecairo.api.responses.WithdrawalHistoryResponse;
import com.example.dnervecairo.api.responses.WithdrawalResponse;
import com.example.dnervecairo.utils.NotificationHelper;
import com.example.dnervecairo.utils.PreferenceManager;
import com.example.dnervecairo.viewmodels.SharedDriverViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RewardsFragment extends Fragment {

    private static final double MIN_WITHDRAWAL = 5.0;

    // Views
    private TextView tvBalance, tvEgpValue, tvMinWithdrawal;
    private TextView tvTotalEarned, tvTotalWithdrawn;
    private TextView tvTierName, tvMinProgress;
    private ImageView ivTierIcon;
    private MaterialButton btnWithdraw;
    private MaterialButton btnQuick5, btnQuick10, btnQuick20;
    private RecyclerView rvHistory;
    private LinearLayout emptyState, layoutMinProgress, layoutQuickWithdraw;
    private ProgressBar progressMinWithdrawal;
    private View loadingOverlay;
    private SwipeRefreshLayout swipeRefresh;
    private BarChart chartWeekly;

    // Cards for animation
    private MaterialCardView cardBalance, cardStats, cardChart, cardHowItWorks, cardHistory;

    // Animation
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ObjectAnimator pulseAnimator;
    private boolean isFirstLoad = true;

    private WithdrawalAdapter adapter;
    private PreferenceManager prefManager;
    private NotificationHelper notificationHelper;
    private String driverId;
    private SharedDriverViewModel driverViewModel;

    private double availableBalance = 0;
    private int totalPoints = 0;
    private double totalWithdrawnAmount = 0;
    private String currentTier = "Bronze";

    private final String[] paymentMethods = {
            "Vodafone Cash",
            "Orange Money",
            "Etisalat Cash",
            "Fawry",
            "Bank Transfer"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rewards, container, false);

        prefManager = new PreferenceManager(requireContext());
        notificationHelper = new NotificationHelper(requireContext());
        driverId = prefManager.getDriverId();
        driverViewModel = new ViewModelProvider(requireActivity()).get(SharedDriverViewModel.class);

        initViews(view);
        setupRecyclerView();
        setupWithdrawButton();
        setupQuickWithdrawButtons();
        setupSwipeRefresh();
        setupChart();

        // Prepare for entrance animations
        if (isFirstLoad) {
            prepareEntranceAnimations();
        }

        // Observe shared driver data
        driverViewModel.getDriverData().observe(getViewLifecycleOwner(), driver -> {
            if (driver != null) {
                updateUI(driver);
                playEntranceAnimations();
            } else {
                showToast(getString(R.string.error_generic));
            }
            // Load rewards-specific data (only used by this fragment)
            loadWithdrawalHistory();
            loadPointsHistory();
        });

        driverViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null) {
                showLoading(loading);
            }
        });

        // Trigger initial load
        loadRewardsData();

        return view;
    }

    // onResume reload removed — ViewModel observer handles driver data updates.
    // Withdrawal and points history load when driver data arrives via observer.

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        handler.removeCallbacksAndMessages(null);
    }

    private void initViews(View view) {
        tvBalance = view.findViewById(R.id.tv_balance);
        tvEgpValue = view.findViewById(R.id.tv_egp_value);
        tvMinWithdrawal = view.findViewById(R.id.tv_min_withdrawal);
        tvTotalEarned = view.findViewById(R.id.tv_total_earned);
        tvTotalWithdrawn = view.findViewById(R.id.tv_total_withdrawn);
        tvTierName = view.findViewById(R.id.tv_tier_name);
        tvMinProgress = view.findViewById(R.id.tv_min_progress);
        ivTierIcon = view.findViewById(R.id.iv_tier_icon);
        btnWithdraw = view.findViewById(R.id.btn_withdraw);
        btnQuick5 = view.findViewById(R.id.btn_quick_5);
        btnQuick10 = view.findViewById(R.id.btn_quick_10);
        btnQuick20 = view.findViewById(R.id.btn_quick_20);
        rvHistory = view.findViewById(R.id.rv_history);
        emptyState = view.findViewById(R.id.empty_state);
        layoutMinProgress = view.findViewById(R.id.layout_min_progress);
        layoutQuickWithdraw = view.findViewById(R.id.layout_quick_withdraw);
        progressMinWithdrawal = view.findViewById(R.id.progress_min_withdrawal);
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        chartWeekly = view.findViewById(R.id.chart_weekly);

        // Cards for animation
        cardBalance = view.findViewById(R.id.card_balance);
        cardStats = view.findViewById(R.id.card_stats);
        cardChart = view.findViewById(R.id.card_chart);
        cardHowItWorks = view.findViewById(R.id.card_how_it_works);
        cardHistory = view.findViewById(R.id.card_history);
    }

    private void prepareEntranceAnimations() {
        // Set initial states for animation
        View[] cards = {cardBalance, cardStats, cardChart, cardHowItWorks, cardHistory};
        for (View card : cards) {
            if (card != null) {
                card.setAlpha(0f);
                card.setTranslationY(30f);
            }
        }
    }

    private void playEntranceAnimations() {
        if (!isFirstLoad) return;
        isFirstLoad = false;

        View[] cards = {cardBalance, cardStats, cardChart, cardHowItWorks, cardHistory};
        int delay = 0;

        for (View card : cards) {
            if (card != null) {
                final int currentDelay = delay;
                handler.postDelayed(() -> {
                    card.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                }, currentDelay);
                delay += 100;
            }
        }
    }

    private void setupRecyclerView() {
        if (rvHistory != null) {
            adapter = new WithdrawalAdapter();
            rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvHistory.setAdapter(adapter);
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
            swipeRefresh.setOnRefreshListener(() -> {
                if (driverId != null) {
                    driverViewModel.forceRefresh(driverId);
                }
            });
        }
    }

    private void setupWithdrawButton() {
        btnWithdraw.setOnClickListener(v -> {
            animateButtonClick(v);
            handler.postDelayed(() -> {
                if (availableBalance >= MIN_WITHDRAWAL) {
                    showWithdrawDialog(null);
                } else {
                    double needed = MIN_WITHDRAWAL - availableBalance;
                    Toast.makeText(getContext(),
                            String.format(Locale.US, "Need %.2f more EGP to withdraw", needed),
                            Toast.LENGTH_SHORT).show();
                }
            }, 100);
        });
    }

    private void setupQuickWithdrawButtons() {
        if (btnQuick5 != null) {
            btnQuick5.setOnClickListener(v -> {
                animateButtonClick(v);
                handler.postDelayed(() -> {
                    if (availableBalance >= 5) {
                        showWithdrawDialog(5.0);
                    } else {
                        showInsufficientBalanceToast(5.0);
                    }
                }, 100);
            });
        }
        if (btnQuick10 != null) {
            btnQuick10.setOnClickListener(v -> {
                animateButtonClick(v);
                handler.postDelayed(() -> {
                    if (availableBalance >= 10) {
                        showWithdrawDialog(10.0);
                    } else {
                        showInsufficientBalanceToast(10.0);
                    }
                }, 100);
            });
        }
        if (btnQuick20 != null) {
            btnQuick20.setOnClickListener(v -> {
                animateButtonClick(v);
                handler.postDelayed(() -> {
                    if (availableBalance >= 20) {
                        showWithdrawDialog(20.0);
                    } else {
                        showInsufficientBalanceToast(20.0);
                    }
                }, 100);
            });
        }
    }

    private void animateButtonClick(View v) {
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

    private void showInsufficientBalanceToast(double amount) {
        Toast.makeText(getContext(),
                String.format(Locale.US, "Need %.2f EGP, you have %.2f EGP", amount, availableBalance),
                Toast.LENGTH_SHORT).show();
    }

    private void setupChart() {
        if (chartWeekly == null) return;

        chartWeekly.setDrawBarShadow(false);
        chartWeekly.setDrawValueAboveBar(true);
        chartWeekly.getDescription().setEnabled(false);
        chartWeekly.setMaxVisibleValueCount(7);
        chartWeekly.setPinchZoom(false);
        chartWeekly.setDrawGridBackground(false);
        chartWeekly.setScaleEnabled(false);
        chartWeekly.getLegend().setEnabled(false);
        chartWeekly.getAxisRight().setEnabled(false);
        chartWeekly.setNoDataText(getString(R.string.rewards_no_earnings_data));
        chartWeekly.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        chartWeekly.setExtraBottomOffset(8f);

        XAxis xAxis = chartWeekly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        xAxis.setTextSize(11f);

        chartWeekly.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        chartWeekly.getAxisLeft().setAxisMinimum(0f);
        chartWeekly.getAxisLeft().setDrawGridLines(true);
        chartWeekly.getAxisLeft().setGridColor(ContextCompat.getColor(requireContext(), R.color.divider));
        chartWeekly.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.US, "%.0f", value);
            }
        });
    }

    private void loadRewardsData() {
        if (driverId != null) {
            // Delegates to ViewModel — observer will call updateUI + load history
            driverViewModel.loadDriverData(driverId);
        }
    }

    private void updateUI(DriverResponse driver) {
        int previousPoints = totalPoints;
        totalPoints = driver.getTotalPoints();
        availableBalance = totalPoints / 10.0;
        currentTier = driver.getTier();

        // Animate balance counter
        if (previousPoints != totalPoints) {
            animateBalanceCounter(previousPoints, totalPoints);
        } else {
            tvBalance.setText(String.valueOf(totalPoints));
        }

        // Update EGP equivalent with animation
        if (tvEgpValue != null) {
            tvEgpValue.setText(String.format(Locale.US, "= %.2f EGP", availableBalance));
        }

        // Update Tier Badge with animation
        updateTierBadge(currentTier);

        // Update Progress to Min Withdrawal
        updateMinWithdrawalProgress();

        // Update Quick Withdraw Buttons visibility
        updateQuickWithdrawButtons();

        // Start/stop withdraw button pulse based on eligibility
        updateWithdrawButtonPulse();

        // Update earning stats
        if (tvTotalEarned != null) {
            tvTotalEarned.setText(String.format(Locale.US, "%d pts", totalPoints));
        }

        if (tvTotalWithdrawn != null) {
            tvTotalWithdrawn.setText(String.format(Locale.US, "%.2f EGP", totalWithdrawnAmount));
        }

        // Enable/disable withdraw button
        btnWithdraw.setEnabled(availableBalance >= MIN_WITHDRAWAL);
        btnWithdraw.setAlpha(availableBalance >= MIN_WITHDRAWAL ? 1.0f : 0.6f);
    }

    private void animateBalanceCounter(int from, int to) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(800);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            if (tvBalance != null) {
                tvBalance.setText(String.valueOf(value));
            }
        });
        animator.start();

        // Scale animation on balance
        if (tvBalance != null) {
            tvBalance.animate()
                .scaleX(1.1f).scaleY(1.1f)
                .setDuration(200)
                .withEndAction(() ->
                    tvBalance.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(200)
                        .start()
                ).start();
        }
    }

    private void updateWithdrawButtonPulse() {
        if (btnWithdraw == null) return;

        if (availableBalance >= MIN_WITHDRAWAL) {
            // Start pulse animation
            if (pulseAnimator == null || !pulseAnimator.isRunning()) {
                pulseAnimator = ObjectAnimator.ofFloat(btnWithdraw, "alpha", 1f, 0.7f, 1f);
                pulseAnimator.setDuration(1500);
                pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
                pulseAnimator.start();
            }
        } else {
            // Stop pulse
            if (pulseAnimator != null) {
                pulseAnimator.cancel();
                btnWithdraw.setAlpha(0.6f);
            }
        }
    }

    private void updateTierBadge(String tier) {
        if (tvTierName == null) return;

        tvTierName.setText(tier);

        // Set tier icon color based on tier
        int tierColor;
        switch (tier.toLowerCase()) {
            case "silver":
                tierColor = Color.parseColor("#C0C0C0");
                break;
            case "gold":
                tierColor = Color.parseColor("#FFD700");
                break;
            case "platinum":
                tierColor = Color.parseColor("#E5E4E2");
                break;
            case "diamond":
                tierColor = Color.parseColor("#B9F2FF");
                break;
            default: // Bronze
                tierColor = Color.parseColor("#CD7F32");
        }

        if (ivTierIcon != null) {
            ivTierIcon.setColorFilter(tierColor);
            
            // Shine animation on tier icon
            ivTierIcon.animate()
                .rotationBy(360f)
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }
    }

    private void updateMinWithdrawalProgress() {
        if (layoutMinProgress == null || progressMinWithdrawal == null || tvMinProgress == null) return;

        if (availableBalance < MIN_WITHDRAWAL) {
            layoutMinProgress.setVisibility(View.VISIBLE);

            int targetProgress = (int) ((availableBalance / MIN_WITHDRAWAL) * 100);
            
            // Animate progress bar
            ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressMinWithdrawal, "progress", 0, targetProgress);
            progressAnimator.setDuration(800);
            progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            progressAnimator.start();

            tvMinProgress.setText(String.format(Locale.US, "%.2f / %.2f EGP to withdraw",
                    availableBalance, MIN_WITHDRAWAL));
        } else {
            layoutMinProgress.setVisibility(View.GONE);
        }
    }

    private void updateQuickWithdrawButtons() {
        if (layoutQuickWithdraw == null) return;

        if (availableBalance >= MIN_WITHDRAWAL) {
            layoutQuickWithdraw.setVisibility(View.VISIBLE);

            // Staggered fade-in for buttons
            View[] buttons = {btnQuick5, btnQuick10, btnQuick20};
            int delay = 0;
            for (View btn : buttons) {
                if (btn != null) {
                    btn.setAlpha(0f);
                    final int d = delay;
                    handler.postDelayed(() -> {
                        btn.animate().alpha(1f).setDuration(200).start();
                    }, d);
                    delay += 100;
                }
            }

            // Enable/disable based on balance
            if (btnQuick5 != null) {
                btnQuick5.setEnabled(availableBalance >= 5);
                btnQuick5.setAlpha(availableBalance >= 5 ? 1.0f : 0.5f);
            }
            if (btnQuick10 != null) {
                btnQuick10.setEnabled(availableBalance >= 10);
                btnQuick10.setAlpha(availableBalance >= 10 ? 1.0f : 0.5f);
            }
            if (btnQuick20 != null) {
                btnQuick20.setEnabled(availableBalance >= 20);
                btnQuick20.setAlpha(availableBalance >= 20 ? 1.0f : 0.5f);
            }
        } else {
            layoutQuickWithdraw.setVisibility(View.GONE);
        }
    }

    private void loadPointsHistory() {
        ApiClient.getInstance().getApiService()
                .getPointsHistory(driverId, 100, 0)
                .enqueue(new Callback<PointsHistoryResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<PointsHistoryResponse> call,
                                           @NonNull Response<PointsHistoryResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            updateChartWithRealData(response.body().getTransactions());
                        } else {
                            showEmptyChart();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PointsHistoryResponse> call, @NonNull Throwable t) {
                        showEmptyChart();
                    }
                });
    }

    private void updateChartWithRealData(List<PointsHistoryResponse.PointsTransaction> transactions) {
        if (chartWeekly == null || getContext() == null) return;

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        String[] days = new String[7];
        Map<String, Integer> earningsMap = new HashMap<>();
        Calendar cal = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String dayName = dayFormat.format(cal.getTime());
            String dateKey = dateKeyFormat.format(cal.getTime());
            days[6 - i] = dayName;
            earningsMap.put(dateKey, 0);
        }

        if (transactions != null) {
            for (PointsHistoryResponse.PointsTransaction t : transactions) {
                if (t.getPoints() > 0 && t.getTimestamp() != null) {
                    try {
                        String timestamp = t.getTimestamp().split("\\.")[0];
                        Date transDate = parseFormat.parse(timestamp);
                        if (transDate != null) {
                            String dateKey = dateKeyFormat.format(transDate);
                            if (earningsMap.containsKey(dateKey)) {
                                earningsMap.put(dateKey, earningsMap.get(dateKey) + t.getPoints());
                            }
                        }
                    } catch (Exception e) {
                        // Skip malformed timestamps
                    }
                }
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        cal = Calendar.getInstance();
        boolean hasData = false;

        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String dateKey = dateKeyFormat.format(cal.getTime());
            int points = earningsMap.getOrDefault(dateKey, 0);
            entries.add(new BarEntry(6 - i, points));
            if (points > 0) hasData = true;
        }

        if (!hasData) {
            showEmptyChart();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Points");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.accent));
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        dataSet.setValueTextSize(10f);
        dataSet.setDrawValues(true);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return String.format(Locale.US, "%.0f", value);
            }
        });

        // Gradient colors for bars based on value
        int[] colors = new int[entries.size()];
        float maxValue = 0;
        for (BarEntry entry : entries) {
            if (entry.getY() > maxValue) maxValue = entry.getY();
        }
        for (int i = 0; i < entries.size(); i++) {
            float ratio = maxValue > 0 ? entries.get(i).getY() / maxValue : 0;
            if (ratio > 0.7f) {
                colors[i] = ContextCompat.getColor(requireContext(), R.color.success);
            } else if (ratio > 0.3f) {
                colors[i] = ContextCompat.getColor(requireContext(), R.color.accent);
            } else {
                colors[i] = ContextCompat.getColor(requireContext(), R.color.primary);
            }
        }
        dataSet.setColors(colors);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        chartWeekly.setData(data);
        chartWeekly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        chartWeekly.animateY(800);
        chartWeekly.invalidate();
    }

    private void showEmptyChart() {
        if (chartWeekly == null || getContext() == null) return;

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        String[] days = new String[7];
        ArrayList<BarEntry> entries = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            days[6 - i] = dayFormat.format(cal.getTime());
            entries.add(new BarEntry(6 - i, 0));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Points");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.divider));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        chartWeekly.setData(data);
        chartWeekly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        chartWeekly.invalidate();
    }

    private void loadWithdrawalHistory() {
        ApiClient.getInstance().getApiService()
                .getWithdrawalHistory(driverId)
                .enqueue(new Callback<WithdrawalHistoryResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WithdrawalHistoryResponse> call,
                                           @NonNull Response<WithdrawalHistoryResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            List<WithdrawalResponse> withdrawals = response.body().getWithdrawals();

                            if (withdrawals != null && !withdrawals.isEmpty() && adapter != null) {
                                adapter.setWithdrawals(withdrawals);
                                if (rvHistory != null) rvHistory.setVisibility(View.VISIBLE);
                                if (emptyState != null) emptyState.setVisibility(View.GONE);

                                totalWithdrawnAmount = 0;
                                for (WithdrawalResponse w : withdrawals) {
                                    totalWithdrawnAmount += w.getAmount();
                                }

                                if (tvTotalWithdrawn != null) {
                                    tvTotalWithdrawn.setText(String.format(Locale.US, "%.2f EGP", totalWithdrawnAmount));
                                }
                                if (tvTotalEarned != null) {
                                    int totalEarnedPoints = totalPoints + (int) (totalWithdrawnAmount * 10);
                                    tvTotalEarned.setText(String.format(Locale.US, "%d pts", totalEarnedPoints));
                                }
                            } else {
                                if (rvHistory != null) rvHistory.setVisibility(View.GONE);
                                if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WithdrawalHistoryResponse> call, @NonNull Throwable t) {
                        showLoading(false);
                        if (rvHistory != null) rvHistory.setVisibility(View.GONE);
                        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showWithdrawDialog(@Nullable Double prefilledAmount) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_withdraw, null);

        TextView tvAvailable = dialogView.findViewById(R.id.tv_available_balance);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        AutoCompleteTextView dropdownMethod = dialogView.findViewById(R.id.dropdown_payment_method);
        TextInputEditText etAccount = dialogView.findViewById(R.id.et_account);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        tvAvailable.setText(String.format(Locale.US, "Available: %.2f EGP (%d points)", availableBalance, totalPoints));

        if (prefilledAmount != null) {
            etAmount.setText(String.format(Locale.US, "%.0f", prefilledAmount));
        }

        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, paymentMethods);
        dropdownMethod.setAdapter(methodAdapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            String method = dropdownMethod.getText().toString().trim();
            String account = etAccount.getText() != null ? etAccount.getText().toString().trim() : "";

            if (amountStr.isEmpty()) {
                etAmount.setError(getString(R.string.error_enter_amount));
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                etAmount.setError(getString(R.string.error_invalid_amount));
                return;
            }

            if (amount < MIN_WITHDRAWAL) {
                etAmount.setError(String.format(Locale.US, "Minimum is %.0f EGP", MIN_WITHDRAWAL));
                return;
            }

            if (amount > availableBalance) {
                etAmount.setError(getString(R.string.error_insufficient_balance));
                return;
            }

            if (method.isEmpty()) {
                showToast(getString(R.string.error_select_payment_method));
                return;
            }

            if (account.isEmpty()) {
                etAccount.setError(getString(R.string.error_enter_account));
                return;
            }

            dialog.dismiss();
            showWithdrawalConfirmation(amount, method, account);
        });

        dialog.show();
    }

    private void showWithdrawalConfirmation(double amount, String method, String account) {
        if (getContext() == null) return;

        int pointsToDeduct = (int) (amount * 10);
        String maskedAccount = maskAccountNumber(account);

        String message = getString(R.string.withdrawal_confirm_message,
                String.format(Locale.US, "%.2f", amount),
                method,
                maskedAccount,
                pointsToDeduct);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.withdrawal_confirm_title)
                .setMessage(message)
                .setIcon(R.drawable.ic_withdraw)
                .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                    processWithdrawal(amount, method, account);
                })
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private String maskAccountNumber(String account) {
        if (account == null || account.length() <= 4) {
            return account;
        }
        int visibleDigits = 4;
        String lastDigits = account.substring(account.length() - visibleDigits);
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < account.length() - visibleDigits; i++) {
            masked.append("•");
        }
        masked.append(lastDigits);
        return masked.toString();
    }

    private void processWithdrawal(double amount, String method, String account) {
        showLoading(true);

        WithdrawalRequest request = new WithdrawalRequest(amount, method, account);

        ApiClient.getInstance().getApiService()
                .requestWithdrawal(driverId, request)
                .enqueue(new Callback<WithdrawalResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WithdrawalResponse> call,
                                           @NonNull Response<WithdrawalResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            showSuccessAnimation();
                            showToast(getString(R.string.withdrawal_success));
                            notificationHelper.showWithdrawalNotification("pending", amount);
                            loadRewardsData();
                        } else {
                            showToast(getString(R.string.withdrawal_failed));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WithdrawalResponse> call, @NonNull Throwable t) {
                        showLoading(false);
                        showToast(getString(R.string.error_network));
                    }
                });
    }

    private void showSuccessAnimation() {
        if (cardBalance != null) {
            // Celebrate with a bounce
            cardBalance.animate()
                .scaleX(1.05f).scaleY(1.05f)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() ->
                    cardBalance.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(200)
                        .start()
                ).start();
        }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
