package com.example.dnervecairo.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dnervecairo.R;
import com.example.dnervecairo.adapters.PopularRoutesAdapter;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.requests.ETARequest;
import com.example.dnervecairo.api.responses.ETAResponse;
import com.example.dnervecairo.api.responses.RouteResponse;
import com.example.dnervecairo.models.PopularRoute;
import com.example.dnervecairo.utils.NetworkUtils;
import com.example.dnervecairo.utils.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartTripActivity extends AppCompatActivity {

    private static final String TAG = "StartTripActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // UI Elements
    private TextView tvCurrentLocation;
    private TextView tvPredictedETA;  // Show ML prediction
    private TextInputEditText etStartLocation, etDestination;
    private RecyclerView rvPopularRoutes;
    private MaterialButton btnStartTrip;
    private MaterialCardView cardCurrentLocation;
    private View progressLoading;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private String currentLocationName = "";
    private double currentLat = 0, currentLng = 0;

    // Data
    private String selectedRouteId = null;
    private String selectedRouteName = null;
    private String startLocationName = "";
    private String destinationName = "";
    private float selectedRouteDistance = 0;  // Store route distance
    private int predictedETAMinutes = 0;      // Store ML prediction

    // Route coordinates for map display
    private double routeOriginLat = 0;
    private double routeOriginLon = 0;
    private double routeDestLat = 0;
    private double routeDestLon = 0;

    private PreferenceManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_trip);

        prefManager = new PreferenceManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupToolbar();
        setupListeners();
        getCurrentLocation();
        loadPopularRoutes();
    }

    private void initViews() {
        tvCurrentLocation = findViewById(R.id.tv_current_location);
        tvPredictedETA = findViewById(R.id.tv_predicted_eta);  // NEW
        etStartLocation = findViewById(R.id.et_start_location);
        etDestination = findViewById(R.id.et_destination);
        rvPopularRoutes = findViewById(R.id.rv_popular_routes);
        btnStartTrip = findViewById(R.id.btn_start_trip);
        cardCurrentLocation = findViewById(R.id.card_current_location);
        progressLoading = findViewById(R.id.progress_loading);

        rvPopularRoutes.setLayoutManager(new LinearLayoutManager(this));

        // Hide ETA initially
        if (tvPredictedETA != null) {
            tvPredictedETA.setVisibility(View.GONE);
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        // Use current location as start
        cardCurrentLocation.setOnClickListener(v -> {
            if (!currentLocationName.isEmpty()) {
                etStartLocation.setText(currentLocationName);
                startLocationName = currentLocationName;
            }
        });

        // Text change listeners
        etStartLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                startLocationName = s.toString();
                selectedRouteId = null; // Custom route
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
                selectedRouteId = null; // Custom route
                updateStartButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Start trip button
        btnStartTrip.setOnClickListener(v -> startTrip());
    }

    private void updateStartButton() {
        boolean canStart = !startLocationName.trim().isEmpty() && !destinationName.trim().isEmpty();
        btnStartTrip.setEnabled(canStart);

        if (canStart) {
            String buttonText = "Start Trip: " + startLocationName + " → " + destinationName;
            if (predictedETAMinutes > 0) {
                buttonText += " (~" + predictedETAMinutes + " min)";
            }
            btnStartTrip.setText(buttonText);
        } else {
            btnStartTrip.setText("Select Route to Start");
        }
    }

    // =========================================================================
    // ML ETA PREDICTION
    // =========================================================================

    private void fetchPredictedETA(float distanceKm, int fallbackMinutes) {
        // Determine if peak hour (7-9 AM or 5-8 PM)
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

                            // Update UI with ML-predicted ETA
                            showPredictedETA(predictedETAMinutes, true);
                            updateStartButton();
                        } else {
                            // Use fallback
                            predictedETAMinutes = fallbackMinutes;
                            showPredictedETA(fallbackMinutes, false);
                            updateStartButton();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ETAResponse> call, @NonNull Throwable t) {
                        Log.w(TAG, "ML prediction failed: " + t.getMessage());
                        // Use fallback
                        predictedETAMinutes = fallbackMinutes;
                        showPredictedETA(fallbackMinutes, false);
                        updateStartButton();
                    }
                });
    }

    private void showPredictedETA(int minutes, boolean isMLPrediction) {
        if (tvPredictedETA != null) {
            if (isMLPrediction) {
                tvPredictedETA.setText("AI Predicted ETA: " + minutes + " min");
                tvPredictedETA.setTextColor(getResources().getColor(R.color.primary, null));
            } else {
                tvPredictedETA.setText("Estimated: " + minutes + " min");
                tvPredictedETA.setTextColor(getResources().getColor(R.color.text_secondary, null));
            }
            tvPredictedETA.setVisibility(View.VISIBLE);
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
                        tvCurrentLocation.setText("Location unavailable");
                    }
                })
                .addOnFailureListener(this, e -> {
                    tvCurrentLocation.setText("Location unavailable");
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

                tvCurrentLocation.setText("📍 " + currentLocationName);
            } else {
                tvCurrentLocation.setText("📍 Current Location");
                currentLocationName = "Current Location";
            }
        } catch (IOException e) {
            tvCurrentLocation.setText("📍 Current Location");
            currentLocationName = "Current Location";
        }
    }

    // =========================================================================
    // ROUTES LOADING
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
                            displayRoutes(response.body());
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

    private void displayRoutes(List<RouteResponse> routes) {
        List<PopularRoute> popularRoutes = new ArrayList<>();

        for (RouteResponse route : routes) {
            PopularRoute pr = new PopularRoute(
                    route.getRouteId(),
                    route.getStartName(),
                    route.getEndName(),
                    route.getEstimatedDuration(),
                    route.getPopularity()
            );
            pr.setDistanceKm(route.getDistanceKm());  // Store distance for ETA

            // NEW: Store coordinates from API response
            pr.setOriginLat(route.getOriginLat());
            pr.setOriginLon(route.getOriginLon());
            pr.setDestLat(route.getDestLat());
            pr.setDestLon(route.getDestLon());

            popularRoutes.add(pr);
        }

        PopularRoutesAdapter adapter = new PopularRoutesAdapter(popularRoutes, route -> {
            // Route selected
            selectedRouteId = route.getRouteId();
            selectedRouteName = route.getStartName() + " → " + route.getEndName();
            startLocationName = route.getStartName();
            destinationName = route.getEndName();
            selectedRouteDistance = route.getDistanceKm();

            // NEW: Store route coordinates for map
            routeOriginLat = route.getOriginLat();
            routeOriginLon = route.getOriginLon();
            routeDestLat = route.getDestLat();
            routeDestLon = route.getDestLon();

            Log.d(TAG, "Selected route: " + selectedRouteName);
            Log.d(TAG, "Coordinates: (" + routeOriginLat + "," + routeOriginLon + ") -> (" + routeDestLat + "," + routeDestLon + ")");

            etStartLocation.setText(route.getStartName());
            etDestination.setText(route.getEndName());

            // Fetch ML-predicted ETA
            if (selectedRouteDistance > 0) {
                fetchPredictedETA(selectedRouteDistance, route.getEstimatedMinutes());
            } else {
                // Fallback: estimate distance from duration (assume 20 km/h average)
                float estimatedDistance = route.getEstimatedMinutes() * 20f / 60f;
                fetchPredictedETA(estimatedDistance, route.getEstimatedMinutes());
            }

            updateStartButton();
        });

        rvPopularRoutes.setAdapter(adapter);
    }

    private void loadOfflineRoutes() {
        // Show some common Cairo routes as fallback - NOW WITH COORDINATES
        List<PopularRoute> defaultRoutes = new ArrayList<>();
        defaultRoutes.add(new PopularRoute("route_1", "Ramses", "Giza", 45, 85, 15f,
                30.0619, 31.2466, 30.0131, 31.2089));
        defaultRoutes.add(new PopularRoute("route_2", "Tahrir", "Maadi", 35, 72, 12f,
                30.0444, 31.2357, 29.9602, 31.2569));
        defaultRoutes.add(new PopularRoute("route_3", "Heliopolis", "Downtown", 40, 68, 8.5f,
                30.0866, 31.3225, 30.0459, 31.2394));
        defaultRoutes.add(new PopularRoute("route_4", "Nasr City", "Mohandessin", 50, 55, 15f,
                30.0511, 31.3656, 30.0609, 31.2003));
        defaultRoutes.add(new PopularRoute("route_5", "Shubra", "Zamalek", 30, 45, 6f,
                30.0986, 31.2422, 30.0609, 31.2194));

        PopularRoutesAdapter adapter = new PopularRoutesAdapter(defaultRoutes, route -> {
            selectedRouteId = route.getRouteId();
            selectedRouteName = route.getStartName() + " → " + route.getEndName();
            startLocationName = route.getStartName();
            destinationName = route.getEndName();
            selectedRouteDistance = route.getDistanceKm();

            // NEW: Store route coordinates for map
            routeOriginLat = route.getOriginLat();
            routeOriginLon = route.getOriginLon();
            routeDestLat = route.getDestLat();
            routeDestLon = route.getDestLon();

            etStartLocation.setText(route.getStartName());
            etDestination.setText(route.getEndName());

            // Fetch ML-predicted ETA
            fetchPredictedETA(selectedRouteDistance, route.getEstimatedMinutes());

            updateStartButton();
        });

        rvPopularRoutes.setAdapter(adapter);
    }

    // =========================================================================
    // START TRIP
    // =========================================================================

    private void startTrip() {
        if (startLocationName.trim().isEmpty() || destinationName.trim().isEmpty()) {
            Toast.makeText(this, "Please select start and destination", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, TripActivity.class);
        intent.putExtra(TripActivity.EXTRA_ROUTE_ID, selectedRouteId);
        intent.putExtra(TripActivity.EXTRA_ROUTE_NAME, startLocationName + " → " + destinationName);
        intent.putExtra(TripActivity.EXTRA_START_NAME, startLocationName);
        intent.putExtra(TripActivity.EXTRA_END_NAME, destinationName);
        intent.putExtra(TripActivity.EXTRA_START_LAT, currentLat);
        intent.putExtra(TripActivity.EXTRA_START_LNG, currentLng);
        intent.putExtra("predicted_eta", predictedETAMinutes);  // Pass to TripActivity

        // NEW: Pass route coordinates for map display
        intent.putExtra("route_origin_lat", routeOriginLat);
        intent.putExtra("route_origin_lon", routeOriginLon);
        intent.putExtra("route_dest_lat", routeDestLat);
        intent.putExtra("route_dest_lon", routeDestLon);
        intent.putExtra("route_distance_km", selectedRouteDistance);

        Log.d(TAG, "Starting trip with coordinates: origin(" + routeOriginLat + "," + routeOriginLon +
                ") dest(" + routeDestLat + "," + routeDestLon + ")");

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