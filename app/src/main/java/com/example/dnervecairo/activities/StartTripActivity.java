package com.example.dnervecairo.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.adapters.PopularRoutesAdapter;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.requests.ETARequest;
// NEW: Live trip imports
import com.example.dnervecairo.api.requests.StartLiveTripRequest;
import com.example.dnervecairo.api.responses.ETAResponse;
// NEW: Live trip response
import com.example.dnervecairo.api.responses.LiveTripStartResponse;
import com.example.dnervecairo.api.responses.RouteResponse;
import com.example.dnervecairo.models.PopularRoute;
import com.example.dnervecairo.utils.NetworkUtils;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartTripActivity extends AppCompatActivity {

    private static final String TAG = "StartTripActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // Distance thresholds (in km)
    private static final float EXACT_MATCH_RADIUS = 1.5f;
    private static final float NEARBY_RADIUS = 5.0f;

    // UI Elements
    private TextView tvCurrentLocation;
    private TextView tvPredictedETA;
    private TextView tvRoutesHeader;
    private TextInputEditText etStartLocation, etDestination;
    private RecyclerView rvPopularRoutes;
    private MaterialButton btnStartTrip;
    private MaterialCardView cardCurrentLocation, cardETA;
    private View progressLoading;
    private NestedScrollView scrollView;
    private LinearLayout layoutButtonContainer;
    private View buttonTopAnchor;

    // Passenger counter
    private TextView tvPassengerCount;
    private ImageView btnPassengerMinus, btnPassengerPlus;
    private int passengerCount = 1;

    private Chip chipFromHere, chipPopular, chipAll;
    private String currentFilter = "from_here";

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private String currentLocationName = "";
    private String normalizedLocationName = "";
    private double currentLat = 0, currentLng = 0;

    // Data
    private String selectedRouteId = null;
    private String startLocationName = "";
    private String destinationName = "";
    private float selectedRouteDistance = 0;
    private int predictedETAMinutes = 0;

    // All routes for filtering
    private List<PopularRoute> allRoutes = new ArrayList<>();
    private PopularRoutesAdapter routesAdapter;

    // Button scroll state
    private boolean isButtonAtTop = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Route coordinates for map display
    private double routeOriginLat = 0;
    private double routeOriginLon = 0;
    private double routeDestLat = 0;
    private double routeDestLon = 0;

    private PreferenceManager prefManager;

    // =========================================================================
    // CAIRO LOCATION NAME MAPPINGS (for fuzzy matching)
    // =========================================================================

    private static final Map<String, String[]> LOCATION_ALIASES = new HashMap<>();
    static {
        // Format: Canonical Name -> [aliases including Arabic]
        LOCATION_ALIASES.put("abbassia", new String[]{"abbassia", "abbasiya", "el abbasiya", "العباسية", "عباسية"});
        LOCATION_ALIASES.put("ramses", new String[]{"ramses", "ramsis", "ramses square", "رمسيس", "ميدان رمسيس"});
        LOCATION_ALIASES.put("tahrir", new String[]{"tahrir", "tahrir square", "التحرير", "ميدان التحرير"});
        LOCATION_ALIASES.put("giza", new String[]{"giza", "giza square", "الجيزة", "ميدان الجيزة"});
        LOCATION_ALIASES.put("ataba", new String[]{"ataba", "attaba", "el ataba", "العتبة"});
        LOCATION_ALIASES.put("maadi", new String[]{"maadi", "el maadi", "المعادي", "معادي"});
        LOCATION_ALIASES.put("heliopolis", new String[]{"heliopolis", "masr el gedida", "مصر الجديدة", "هليوبوليس"});
        LOCATION_ALIASES.put("nasr city", new String[]{"nasr city", "nasr", "مدينة نصر", "نصر"});
        LOCATION_ALIASES.put("shubra", new String[]{"shubra", "shoubra", "شبرا"});
        LOCATION_ALIASES.put("mohandessin", new String[]{"mohandessin", "mohandiseen", "المهندسين"});
        LOCATION_ALIASES.put("dokki", new String[]{"dokki", "el dokki", "الدقي"});
        LOCATION_ALIASES.put("ain shams", new String[]{"ain shams", "ein shams", "عين شمس"});
        LOCATION_ALIASES.put("zeitoun", new String[]{"zeitoun", "zaytoun", "الزيتون"});
        LOCATION_ALIASES.put("imbaba", new String[]{"imbaba", "imbaba", "امبابة", "إمبابة"});
        LOCATION_ALIASES.put("dar el salam", new String[]{"dar el salam", "dar elsalam", "دار السلام"});
        LOCATION_ALIASES.put("6th october", new String[]{"6th october", "6 october", "october", "السادس من أكتوبر", "أكتوبر"});
        LOCATION_ALIASES.put("new cairo", new String[]{"new cairo", "el tagamoa", "tagamoa", "التجمع", "القاهرة الجديدة"});
        LOCATION_ALIASES.put("helwan", new String[]{"helwan", "حلوان"});
        LOCATION_ALIASES.put("zamalek", new String[]{"zamalek", "الزمالك"});
        LOCATION_ALIASES.put("downtown", new String[]{"downtown", "wust el balad", "وسط البلد"});
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_trip);

        prefManager = new PreferenceManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupToolbar();
        setupListeners();
        setupFilterChips();
        setupScrollBehavior();
        getCurrentLocation();
        loadPopularRoutes();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void initViews() {
        tvCurrentLocation = findViewById(R.id.tv_current_location);
        tvPredictedETA = findViewById(R.id.tv_predicted_eta);
        tvRoutesHeader = findViewById(R.id.tv_routes_header);
        etStartLocation = findViewById(R.id.et_start_location);
        etDestination = findViewById(R.id.et_destination);
        rvPopularRoutes = findViewById(R.id.rv_popular_routes);
        btnStartTrip = findViewById(R.id.btn_start_trip);
        cardCurrentLocation = findViewById(R.id.card_current_location);
        cardETA = findViewById(R.id.card_eta);
        progressLoading = findViewById(R.id.progress_loading);
        scrollView = findViewById(R.id.scroll_view);
        layoutButtonContainer = findViewById(R.id.layout_button_container);
        buttonTopAnchor = findViewById(R.id.button_top_anchor);

        // Passenger counter
        tvPassengerCount = findViewById(R.id.tv_passenger_count);
        btnPassengerMinus = findViewById(R.id.btn_passenger_minus);
        btnPassengerPlus = findViewById(R.id.btn_passenger_plus);

        // Filter chips (updated IDs)
        chipFromHere = findViewById(R.id.chip_from_here);
        chipPopular = findViewById(R.id.chip_popular);
        chipAll = findViewById(R.id.chip_all);

        rvPopularRoutes.setLayoutManager(new LinearLayoutManager(this));
        rvPopularRoutes.setNestedScrollingEnabled(false);

        // Hide ETA initially
        if (cardETA != null) {
            cardETA.setVisibility(View.GONE);
        }

        // Initialize empty adapter
        routesAdapter = new PopularRoutesAdapter(new ArrayList<>(), this::onRouteSelected);
        rvPopularRoutes.setAdapter(routesAdapter);

        // Initialize passenger display
        updatePassengerDisplay();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            animateClick(v);
            handler.postDelayed(this::finish, 100);
        });
    }

    private void setupListeners() {
        // Use current location as start
        cardCurrentLocation.setOnClickListener(v -> {
            if (!currentLocationName.isEmpty()) {
                etStartLocation.setText(currentLocationName);
                startLocationName = currentLocationName;
                animateClick(v);
            }
        });

        // Passenger counter
        if (btnPassengerMinus != null) {
            btnPassengerMinus.setOnClickListener(v -> {
                if (passengerCount > 1) {
                    passengerCount--;
                    updatePassengerDisplay();
                    animateClick(v);
                }
            });
        }

        if (btnPassengerPlus != null) {
            btnPassengerPlus.setOnClickListener(v -> {
                if (passengerCount < 20) {
                    passengerCount++;
                    updatePassengerDisplay();
                    animateClick(v);
                }
            });
        }

        // Text change listeners
        etStartLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                startLocationName = s.toString();
                if (s.length() > 0) selectedRouteId = null;
                updateStartButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                destinationName = s.toString();
                if (s.length() > 0) selectedRouteId = null;
                updateStartButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Start trip button
        btnStartTrip.setOnClickListener(v -> {
            animateClick(v);
            handler.postDelayed(this::startTrip, 100);
        });
    }

    private void setupFilterChips() {
        if (chipFromHere != null) {
            chipFromHere.setOnClickListener(v -> {
                currentFilter = "from_here";
                filterAndDisplayRoutes();
            });
        }

        if (chipPopular != null) {
            chipPopular.setOnClickListener(v -> {
                currentFilter = "popular";
                filterAndDisplayRoutes();
            });
        }

        if (chipAll != null) {
            chipAll.setOnClickListener(v -> {
                currentFilter = "all";
                filterAndDisplayRoutes();
            });
        }
    }

    /**
     * Setup scroll behavior for floating button     *  moves from bottom to top when scrolling down
     */
    private void setupScrollBehavior() {
        if (scrollView == null || btnStartTrip == null) return;

        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    int scrollThreshold = 300; // px

                    if (scrollY > scrollThreshold && !isButtonAtTop) {
                        // Scrolled down - move button to top
                        moveButtonToTop();
                    } else if (scrollY <= scrollThreshold && isButtonAtTop) {
                        // Scrolled up - move button to bottom
                        moveButtonToBottom();
                    }
                });
    }

    private void moveButtonToTop() {
        if (isButtonAtTop || btnStartTrip == null) return;
        isButtonAtTop = true;

        // Animate button sliding up and becoming a top bar
        btnStartTrip.animate()
                .translationY(-btnStartTrip.getHeight() - 100)
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    // Show top anchor button
                    if (buttonTopAnchor != null) {
                        buttonTopAnchor.setVisibility(View.VISIBLE);
                        buttonTopAnchor.setAlpha(0f);
                        buttonTopAnchor.animate()
                                .alpha(1f)
                                .setDuration(200)
                                .start();
                    }
                })
                .start();
    }

    private void moveButtonToBottom() {
        if (!isButtonAtTop || btnStartTrip == null) return;
        isButtonAtTop = false;

        // Hide top button first
        if (buttonTopAnchor != null) {
            buttonTopAnchor.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> buttonTopAnchor.setVisibility(View.GONE))
                    .start();
        }

        // Animate button sliding back down
        btnStartTrip.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void animateClick(View v) {
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

    private void updatePassengerDisplay() {
        if (tvPassengerCount != null) {
            tvPassengerCount.setText(String.valueOf(passengerCount));
        }
        if (btnPassengerMinus != null) {
            btnPassengerMinus.setAlpha(passengerCount > 1 ? 1f : 0.3f);
        }
        if (btnPassengerPlus != null) {
            btnPassengerPlus.setAlpha(passengerCount < 20 ? 1f : 0.3f);
        }
    }

    private void updateStartButton() {
        boolean canStart = !startLocationName.trim().isEmpty() && !destinationName.trim().isEmpty();
        btnStartTrip.setEnabled(canStart);

        // Update top button too if it exists
        View topButton = findViewById(R.id.btn_start_trip_top);
        if (topButton != null) {
            topButton.setEnabled(canStart);
        }

        if (canStart) {
            String routeText = startLocationName + " → " + destinationName;
            if (routeText.length() > 25) {
                routeText = routeText.substring(0, 22) + "...";
            }
            btnStartTrip.setText("Start: " + routeText);
        } else {
            btnStartTrip.setText("Select Route to Start");
        }
    }

    // =========================================================================
    // ROUTE SELECTION CALLBACK
    // =========================================================================

    private void onRouteSelected(PopularRoute route) {
        String selectedRouteName = route.getStartName() + " → " + route.getEndName();
        startLocationName = route.getStartName();
        destinationName = route.getEndName();
        selectedRouteDistance = route.getDistanceKm();

        routeOriginLat = route.getOriginLat();
        routeOriginLon = route.getOriginLon();
        routeDestLat = route.getDestLat();
        routeDestLon = route.getDestLon();

        Log.d(TAG, "Selected route: " + selectedRouteName +
                " (match: " + route.getMatchType() + ", dist: " + route.getDistanceFromDriver() + "km)");

        etStartLocation.setText(route.getStartName());
        etDestination.setText(route.getEndName());

        selectedRouteId = route.getRouteId();

        // Fetch ML-predicted ETA
        if (selectedRouteDistance > 0) {
            fetchPredictedETA(selectedRouteDistance, route.getEstimatedMinutes());
        } else {
            float estimatedDistance = route.getEstimatedMinutes() * 20f / 60f;
            fetchPredictedETA(estimatedDistance, route.getEstimatedMinutes());
        }

        updateStartButton();
    }

    // =========================================================================
    // FUZZY LOCATION MATCHING
    // =========================================================================

    /**
     * Normalize a location name for matching
     */
    private String normalizeLocationName(String name) {
        if (name == null) return "";

        String normalized = name.toLowerCase(Locale.ROOT)
                .replace("el ", "")
                .replace("al ", "")
                .replace("square", "")
                .replace("ميدان", "")
                .trim();

        // Check if it matches any known aliases
        for (Map.Entry<String, String[]> entry : LOCATION_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (normalized.contains(alias.toLowerCase()) ||
                        alias.toLowerCase().contains(normalized)) {
                    return entry.getKey();
                }
            }
        }

        return normalized;
    }

    /**
     * Check if two location names match (fuzzy)
     */
    private boolean locationNamesMatch(String name1, String name2) {
        String norm1 = normalizeLocationName(name1);
        String norm2 = normalizeLocationName(name2);

        if (norm1.isEmpty() || norm2.isEmpty()) return false;

        // Direct match
        if (norm1.equals(norm2)) return true;

        // Partial match (one contains the other)
        if (norm1.contains(norm2) || norm2.contains(norm1)) return true;

        return false;
    }

    // =========================================================================
    // ML ETA PREDICTION
    // =========================================================================

    private void fetchPredictedETA(float distanceKm, int fallbackMinutes) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int isPeak = ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 20)) ? 1 : 0;

        Log.d(TAG, "Fetching ML ETA: distance=" + distanceKm + "km, hour=" + hour + ", isPeak=" + isPeak);

        ETARequest request = new ETARequest(distanceKm, hour, isPeak);

        ApiClient.getInstance().getApiService()
                .predictETA(request)
                .enqueue(new Callback<ETAResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ETAResponse> call,
                                           @NonNull Response<ETAResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            float eta = response.body().getPredictedDurationMinutes();
                            predictedETAMinutes = Math.round(eta);
                            Log.d(TAG, "ML ETA prediction: " + predictedETAMinutes + " min");
                            showPredictedETA(predictedETAMinutes, true, isPeak == 1);
                        } else {
                            predictedETAMinutes = fallbackMinutes;
                            showPredictedETA(fallbackMinutes, false, false);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ETAResponse> call, @NonNull Throwable t) {
                        Log.w(TAG, "ML prediction failed: " + t.getMessage());
                        predictedETAMinutes = fallbackMinutes;
                        showPredictedETA(fallbackMinutes, false, false);
                    }
                });
    }

    private void showPredictedETA(int minutes, boolean isMLPrediction, boolean isPeakHour) {
        if (cardETA != null && tvPredictedETA != null) {
            cardETA.setVisibility(View.VISIBLE);
            cardETA.setAlpha(0f);
            cardETA.animate().alpha(1f).setDuration(300);

            String etaText;
            if (isMLPrediction) {
                etaText = "🤖 " + minutes + " min";
                if (isPeakHour) {
                    etaText += " (Peak)";
                }
                tvPredictedETA.setTextColor(getResources().getColor(R.color.primary, null));
            } else {
                etaText = "~" + minutes + " min";
                tvPredictedETA.setTextColor(getResources().getColor(R.color.text_secondary, null));
            }
            tvPredictedETA.setText(etaText);
        }
    }

    // =========================================================================
    // LOCATION
    // =========================================================================

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        tvCurrentLocation.setText("Detecting location...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLng = location.getLongitude();
                        reverseGeocode(location);
                    } else {
                        tvCurrentLocation.setText("📍 Location unavailable");
                        currentLocationName = "";
                        filterAndDisplayRoutes();
                    }
                })
                .addOnFailureListener(this, e -> {
                    tvCurrentLocation.setText("📍 Location unavailable");
                    currentLocationName = "";
                    filterAndDisplayRoutes();
                });
    }

    private void reverseGeocode(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String locality = address.getLocality();
                String subLocality = address.getSubLocality();
                String thoroughfare = address.getThoroughfare();

                if (subLocality != null) {
                    currentLocationName = subLocality;
                } else if (thoroughfare != null) {
                    currentLocationName = thoroughfare;
                } else if (locality != null) {
                    currentLocationName = locality;
                } else {
                    currentLocationName = "Current Location";
                }

                normalizedLocationName = normalizeLocationName(currentLocationName);
                tvCurrentLocation.setText("📍 " + currentLocationName);

                Log.d(TAG, "Driver location: " + currentLocationName + " → normalized: " + normalizedLocationName);
            } else {
                tvCurrentLocation.setText("📍 Current Location");
                currentLocationName = "Current Location";
                normalizedLocationName = "";
            }
        } catch (IOException e) {
            tvCurrentLocation.setText("📍 Current Location");
            currentLocationName = "Current Location";
            normalizedLocationName = "";
        }

        // Re-filter routes with new location
        if (!allRoutes.isEmpty()) {
            calculateDistancesAndFilter();
        }
    }

    // =========================================================================
    // ROUTES LOADING & SMART FILTERING
    // =========================================================================

    private void loadPopularRoutes() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            loadOfflineRoutes();
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);

        ApiClient.getInstance().getApiService()
                .getRoutes()
                .enqueue(new Callback<List<RouteResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<RouteResponse>> call,
                                           @NonNull Response<List<RouteResponse>> response) {
                        progressLoading.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            processRoutes(response.body());
                        } else {
                            loadOfflineRoutes();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<RouteResponse>> call, @NonNull Throwable t) {
                        progressLoading.setVisibility(View.GONE);
                        loadOfflineRoutes();
                    }
                });
    }

    private void processRoutes(List<RouteResponse> routes) {
        allRoutes.clear();

        for (RouteResponse route : routes) {
            PopularRoute pr = new PopularRoute(
                    route.getRouteId(),
                    route.getStartName(),
                    route.getEndName(),
                    route.getEstimatedDuration(),
                    route.getPopularity()
            );
            pr.setDistanceKm(route.getDistanceKm());
            pr.setOriginLat(route.getOriginLat());
            pr.setOriginLon(route.getOriginLon());
            pr.setDestLat(route.getDestLat());
            pr.setDestLon(route.getDestLon());

            allRoutes.add(pr);
        }

        calculateDistancesAndFilter();
    }

    private void calculateDistancesAndFilter() {
        // Calculate distance from driver to each route's origin
        for (PopularRoute route : allRoutes) {
            float dist = Float.MAX_VALUE;

            // Method 1: Coordinate-based distance
            if (currentLat != 0 && currentLng != 0 && route.hasCoordinates()) {
                dist = calculateDistance(currentLat, currentLng,
                        route.getOriginLat(), route.getOriginLon());
            }

            // Method 2: Name-based matching (fuzzy)
            // If names match but coordinates don't show close, use name match
            if (dist > EXACT_MATCH_RADIUS && !normalizedLocationName.isEmpty()) {
                if (locationNamesMatch(currentLocationName, route.getStartName())) {
                    // Name match - set distance to very small
                    dist = 0.5f; // Treat as 500m away
                    Log.d(TAG, "Fuzzy name match: " + currentLocationName + " ≈ " + route.getStartName());
                }
            }

            route.setDistanceFromDriver(dist);
        }

        filterAndDisplayRoutes();
    }

    /**
     * Smart filtering based on driver's location
     */
    private void filterAndDisplayRoutes() {
        List<PopularRoute> exactMatches = new ArrayList<>();
        List<PopularRoute> nearbyRoutes = new ArrayList<>();
        List<PopularRoute> allOther = new ArrayList<>();

        for (PopularRoute route : allRoutes) {
            if (route.getDistanceFromDriver() <= EXACT_MATCH_RADIUS) {
                exactMatches.add(route);
            } else if (route.getDistanceFromDriver() <= NEARBY_RADIUS) {
                nearbyRoutes.add(route);
            } else {
                allOther.add(route);
            }
        }

        // Sort each category
        Collections.sort(exactMatches, (r1, r2) ->
                Float.compare(r1.getDistanceFromDriver(), r2.getDistanceFromDriver()));
        Collections.sort(nearbyRoutes, (r1, r2) ->
                Float.compare(r1.getDistanceFromDriver(), r2.getDistanceFromDriver()));
        Collections.sort(allOther, (r1, r2) ->
                Integer.compare(r2.getPopularity(), r1.getPopularity()));

        // Display based on current filter
        switch (currentFilter) {
            case "from_here":
                displaySmartRoutes(exactMatches, nearbyRoutes);
                break;

            case "popular":
                List<PopularRoute> popularSorted = new ArrayList<>(allRoutes);
                Collections.sort(popularSorted, (r1, r2) ->
                        Integer.compare(r2.getPopularity(), r1.getPopularity()));
                displaySimpleList(popularSorted, "🔥 Popular Routes");
                break;

            case "all":
            default:
                List<PopularRoute> allSorted = new ArrayList<>(allRoutes);
                Collections.sort(allSorted, (r1, r2) ->
                        r1.getStartName().compareTo(r2.getStartName()));
                displaySimpleList(allSorted, "All Routes");
                break;
        }
    }

    /**
     * Display routes with smart sections (From Your Location + Nearby)
     */
    private void displaySmartRoutes(List<PopularRoute> exactMatches, List<PopularRoute> nearbyRoutes) {
        if (tvRoutesHeader != null) {
            if (!exactMatches.isEmpty()) {
                tvRoutesHeader.setText("🎯 Routes from your area");
            } else if (!nearbyRoutes.isEmpty()) {
                tvRoutesHeader.setText("📍 Nearby routes");
            } else {
                tvRoutesHeader.setText("No routes found nearby");
            }
        }

        // Use sectioned display
        routesAdapter.updateWithSections(exactMatches, nearbyRoutes, currentLocationName);

        Log.d(TAG, "Smart filter: " + exactMatches.size() + " exact, " + nearbyRoutes.size() + " nearby");
    }

    /**
     * Display simple list without sections
     */
    private void displaySimpleList(List<PopularRoute> routes, String headerText) {
        if (tvRoutesHeader != null) {
            tvRoutesHeader.setText(headerText + " (" + routes.size() + ")");
        }

        routesAdapter.updateItems(routes);
    }

    private void loadOfflineRoutes() {
        allRoutes.clear();

        allRoutes.add(new PopularRoute("route_1", "Ramses Square", "Giza Square", 45, 85, 15f,
                30.0619, 31.2466, 30.0131, 31.2089));
        allRoutes.add(new PopularRoute("route_2", "Tahrir Square", "Maadi", 35, 72, 12f,
                30.0444, 31.2357, 29.9602, 31.2569));
        allRoutes.add(new PopularRoute("route_3", "Heliopolis", "Nasr City", 25, 68, 5f,
                30.0866, 31.3225, 30.0511, 31.3656));
        allRoutes.add(new PopularRoute("route_4", "Nasr City", "Mohandessin", 50, 55, 15f,
                30.0511, 31.3656, 30.0609, 31.2003));
        allRoutes.add(new PopularRoute("route_5", "Shubra", "Imbaba", 20, 45, 4f,
                30.0986, 31.2422, 30.0758, 31.2078));
        allRoutes.add(new PopularRoute("route_23", "Abbassia", "Heliopolis", 15, 60, 4f,
                30.0722, 31.2833, 30.0866, 31.3225));
        allRoutes.add(new PopularRoute("route_25", "Ataba Square", "Abbassia", 12, 55, 3.5f,
                30.0531, 31.2469, 30.0722, 31.2833));

        calculateDistancesAndFilter();
    }

    /**
     * Calculate distance between two coordinates in kilometers
     */
    private float calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000f;
    }

    // =========================================================================
    // START TRIP (with Live Mode support)
    // =========================================================================

    /**
     * Start trip - checks for live mode and initiates accordingly
     */
    private void startTrip() {
        if (startLocationName.trim().isEmpty() || destinationName.trim().isEmpty()) {
            Toast.makeText(this, "Please select start and destination", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if driver is logged in
        if (!prefManager.isLoggedIn()) {
            Log.w(TAG, "Driver not logged in, starting in batch mode");
            launchTripActivity(null, false);
            return;
        }

        // Check if live mode is enabled and network is available
        boolean isLiveModeEnabled = prefManager.isLiveModeEnabled();
        boolean isOnline = NetworkUtils.isNetworkAvailable(this);

        if (isLiveModeEnabled && isOnline) {
            // Start live trip via API
            startLiveTrip();
        } else {
            // Fallback to batch mode
            String reason = !isLiveModeEnabled ? "Live mode disabled" : "Offline";
            Log.d(TAG, "Starting trip in batch mode: " + reason);
            launchTripActivity(null, false);
        }
    }

    /**
     * Start a live trip by calling the backend API
     */
    private void startLiveTrip() {
        // Disable button and show loading
        btnStartTrip.setEnabled(false);
        btnStartTrip.setText("Starting...");
        progressLoading.setVisibility(View.VISIBLE);

        String driverId = prefManager.getDriverId();

        // Use current location if route origin not set
        double originLat = routeOriginLat != 0 ? routeOriginLat : currentLat;
        double originLon = routeOriginLon != 0 ? routeOriginLon : currentLng;

        StartLiveTripRequest request = new StartLiveTripRequest(
                driverId,
                selectedRouteId,
                passengerCount,
                originLat,
                originLon
        );

        Log.d(TAG, "Starting live trip: driver=" + driverId + ", route=" + selectedRouteId);

        ApiClient.getInstance().getApiService()
                .startLiveTrip(request)
                .enqueue(new Callback<LiveTripStartResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<LiveTripStartResponse> call,
                                           @NonNull Response<LiveTripStartResponse> response) {
                        progressLoading.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            LiveTripStartResponse tripResponse = response.body();

                            if (tripResponse.isActive()) {
                                String tripId = tripResponse.getTripId();
                                Log.d(TAG, "Live trip started: " + tripId);

                                // Save for crash recovery
                                prefManager.saveActiveTripData(
                                        tripId,
                                        selectedRouteId,
                                        System.currentTimeMillis()
                                );

                                Toast.makeText(StartTripActivity.this,
                                        "🟢 Live trip started", Toast.LENGTH_SHORT).show();

                                launchTripActivity(tripId, true);
                            } else {
                                Log.w(TAG, "Live trip not active, falling back to batch");
                                launchTripActivity(null, false);
                            }
                        } else {
                            Log.w(TAG, "Live trip API failed: " + response.code());
                            handleLiveTripError();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LiveTripStartResponse> call, @NonNull Throwable t) {
                        progressLoading.setVisibility(View.GONE);
                        Log.e(TAG, "Live trip request failed: " + t.getMessage());
                        handleLiveTripError();
                    }
                });
    }

    /**
     * Handle live trip start failure - fallback to batch mode
     */
    private void handleLiveTripError() {
        Toast.makeText(this, "Starting in offline mode", Toast.LENGTH_SHORT).show();
        launchTripActivity(null, false);
    }

    /**
     * Launch TripActivity with appropriate mode
     *
     * @param liveTripId The server-assigned trip ID (null for batch mode)
     * @param isLiveMode Whether this is a live streaming trip
     */
    private void launchTripActivity(String liveTripId, boolean isLiveMode) {
        Intent intent = new Intent(this, TripActivity.class);

        // Existing extras
        intent.putExtra(TripActivity.EXTRA_ROUTE_ID, selectedRouteId);
        intent.putExtra(TripActivity.EXTRA_ROUTE_NAME, startLocationName + " → " + destinationName);
        intent.putExtra(TripActivity.EXTRA_START_NAME, startLocationName);
        intent.putExtra(TripActivity.EXTRA_END_NAME, destinationName);
        intent.putExtra(TripActivity.EXTRA_START_LAT, currentLat);
        intent.putExtra(TripActivity.EXTRA_START_LNG, currentLng);
        intent.putExtra("predicted_eta", predictedETAMinutes);
        intent.putExtra("passenger_count", passengerCount);

        // Route coordinates for map display
        intent.putExtra("route_origin_lat", routeOriginLat);
        intent.putExtra("route_origin_lon", routeOriginLon);
        intent.putExtra("route_dest_lat", routeDestLat);
        intent.putExtra("route_dest_lon", routeDestLon);
        intent.putExtra("route_distance_km", selectedRouteDistance);

        // NEW: Live trip extras
        intent.putExtra("is_live_mode", isLiveMode);
        if (liveTripId != null) {
            intent.putExtra("live_trip_id", liveTripId);
        }

        Log.d(TAG, "Launching TripActivity: liveMode=" + isLiveMode +
                ", tripId=" + liveTripId + ", passengers=" + passengerCount);

        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }
}