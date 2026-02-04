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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartTripActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // UI Elements
    private TextView tvCurrentLocation;
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
        etStartLocation = findViewById(R.id.et_start_location);
        etDestination = findViewById(R.id.et_destination);
        rvPopularRoutes = findViewById(R.id.rv_popular_routes);
        btnStartTrip = findViewById(R.id.btn_start_trip);
        cardCurrentLocation = findViewById(R.id.card_current_location);
        progressLoading = findViewById(R.id.progress_loading);

        rvPopularRoutes.setLayoutManager(new LinearLayoutManager(this));
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
            btnStartTrip.setText("Start Trip: " + startLocationName + " → " + destinationName);
        } else {
            btnStartTrip.setText("Select Route to Start");
        }
    }

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
            popularRoutes.add(new PopularRoute(
                    route.getRouteId(),
                    route.getStartName(),
                    route.getEndName(),
                    route.getEstimatedDuration(),
                    route.getPopularity()
            ));
        }

        PopularRoutesAdapter adapter = new PopularRoutesAdapter(popularRoutes, route -> {
            // Route selected
            selectedRouteId = route.getRouteId();
            selectedRouteName = route.getStartName() + " → " + route.getEndName();
            startLocationName = route.getStartName();
            destinationName = route.getEndName();

            etStartLocation.setText(route.getStartName());
            etDestination.setText(route.getEndName());

            updateStartButton();
        });

        rvPopularRoutes.setAdapter(adapter);
    }

    private void loadOfflineRoutes() {
        // Show some common Cairo routes as fallback
        List<PopularRoute> defaultRoutes = new ArrayList<>();
        defaultRoutes.add(new PopularRoute("route_1", "Ramses", "Giza", 45, 85));
        defaultRoutes.add(new PopularRoute("route_2", "Tahrir", "Maadi", 35, 72));
        defaultRoutes.add(new PopularRoute("route_3", "Heliopolis", "Downtown", 40, 68));
        defaultRoutes.add(new PopularRoute("route_4", "Nasr City", "Mohandessin", 50, 55));
        defaultRoutes.add(new PopularRoute("route_5", "Shubra", "Zamalek", 30, 45));

        PopularRoutesAdapter adapter = new PopularRoutesAdapter(defaultRoutes, route -> {
            selectedRouteId = route.getRouteId();
            selectedRouteName = route.getStartName() + " → " + route.getEndName();
            startLocationName = route.getStartName();
            destinationName = route.getEndName();

            etStartLocation.setText(route.getStartName());
            etDestination.setText(route.getEndName());

            updateStartButton();
        });

        rvPopularRoutes.setAdapter(adapter);
    }

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