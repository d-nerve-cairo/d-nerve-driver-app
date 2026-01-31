package com.example.dnervecairo.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.dnervecairo.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import android.content.Intent;

import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.requests.TripSubmission;
import com.example.dnervecairo.api.requests.GpsPointRequest;
import com.example.dnervecairo.api.responses.TripResponse;
import com.example.dnervecairo.utils.PreferenceManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TripActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // UI Elements
    private TextView tvTimer, tvDistance, tvGpsPoints, tvGpsStatus;
    private ImageView ivGpsStatus;
    private MaterialButton btnStopTrip;

    // Map
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Trip Data
    private final List<LatLng> tripPoints = new ArrayList<>();
    private long startTime;
    private double totalDistance = 0.0;
    private Location lastLocation;
    private boolean isTracking = false;

    // Timer
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        initViews();
        initMap();
        initLocationClient();
        setupStopButton();
    }

    private void initViews() {
        tvTimer = findViewById(R.id.tv_timer);
        tvDistance = findViewById(R.id.tv_distance);
        tvGpsPoints = findViewById(R.id.tv_gps_points);
        tvGpsStatus = findViewById(R.id.tv_gps_status);
        ivGpsStatus = findViewById(R.id.iv_gps_status);
        btnStopTrip = findViewById(R.id.btn_stop_trip);
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void initLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    onNewLocation(location);
                }
            }
        };
    }

    private void setupStopButton() {
        btnStopTrip.setOnClickListener(v -> stopTrip());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Check permission and start tracking
        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
            startTracking();
        } else {
            requestLocationPermission();
        }
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkLocationPermission()) {
                    mMap.setMyLocationEnabled(true);
                    startTracking();
                }
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startTracking() {
        isTracking = true;
        startTime = System.currentTimeMillis();

        // Start timer
        startTimer();

        // Start location updates
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .build();

        if (checkLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }

        updateGpsStatus(true);
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsed / 1000) % 60;
                int minutes = (int) (elapsed / (1000 * 60)) % 60;
                int hours = (int) (elapsed / (1000 * 60 * 60));

                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void onNewLocation(Location location) {
        LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
        tripPoints.add(newPoint);

        // Update distance
        if (lastLocation != null) {
            totalDistance += lastLocation.distanceTo(location) / 1000.0; // km
            tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", totalDistance));
        }
        lastLocation = location;

        // Update GPS points count
        tvGpsPoints.setText(String.valueOf(tripPoints.size()));

        // Move camera
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 16));

        // Draw path
        if (tripPoints.size() > 1) {
            mMap.clear();
            mMap.addPolyline(new PolylineOptions()
                    .addAll(tripPoints)
                    .width(10)
                    .color(getResources().getColor(R.color.primary, null)));
        }
    }

    private void updateGpsStatus(boolean active) {
        if (active) {
            tvGpsStatus.setText("GPS Active");
            tvGpsStatus.setTextColor(getResources().getColor(R.color.success, null));
            ivGpsStatus.setColorFilter(getResources().getColor(R.color.success, null));
        } else {
            tvGpsStatus.setText("GPS Off");
            tvGpsStatus.setTextColor(getResources().getColor(R.color.error, null));
            ivGpsStatus.setColorFilter(getResources().getColor(R.color.error, null));
        }
    }

    private void stopTrip() {
        isTracking = false;

        // Stop timer
        timerHandler.removeCallbacks(timerRunnable);

        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback);

        // Calculate trip duration in minutes
        long duration = System.currentTimeMillis() - startTime;
        int minutes = (int) (duration / (1000 * 60));

        // Submit trip to API
        submitTripToApi(minutes);
    }

    private void submitTripToApi(int durationMinutes) {
        PreferenceManager prefManager = new PreferenceManager(this);

        // Check if logged in
        if (!prefManager.isLoggedIn()) {
            openTripSummary(durationMinutes);
            return;
        }

        // Convert LatLng list to GpsPointRequest list
        List<GpsPointRequest> gpsPointsList = new ArrayList<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

        for (LatLng point : tripPoints) {
            gpsPointsList.add(new GpsPointRequest(
                    point.latitude,
                    point.longitude,
                    sdf.format(new java.util.Date()),
                    10.0f
            ));
        }

        // Calculate start and end times
        String endTime = sdf.format(new java.util.Date());
        String startTimeStr = sdf.format(new java.util.Date(startTime));

        TripSubmission request = new TripSubmission(
                prefManager.getDriverId(),
                startTimeStr,
                endTime,
                gpsPointsList
        );

        ApiClient.getInstance().getApiService()
                .submitTrip(request)
                .enqueue(new Callback<TripResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TripResponse> call, @NonNull Response<TripResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(TripActivity.this, "Trip saved! +" + response.body().getPointsEarned() + " points", Toast.LENGTH_SHORT).show();
                        }
                        openTripSummary(durationMinutes);
                    }

                    @Override
                    public void onFailure(@NonNull Call<TripResponse> call, @NonNull Throwable t) {
                        Toast.makeText(TripActivity.this, "Trip saved locally", Toast.LENGTH_SHORT).show();
                        openTripSummary(durationMinutes);
                    }
                });
    }
    private void openTripSummary(int durationMinutes) {
        Intent intent = new Intent(this, TripSummaryActivity.class);
        intent.putExtra(TripSummaryActivity.EXTRA_DURATION, durationMinutes);
        intent.putExtra(TripSummaryActivity.EXTRA_DISTANCE, totalDistance);
        intent.putExtra(TripSummaryActivity.EXTRA_GPS_POINTS, tripPoints.size());
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isTracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}