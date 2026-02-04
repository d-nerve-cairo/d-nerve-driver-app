package com.example.dnervecairo.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.dnervecairo.R;
import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.requests.TripSubmission;
import com.example.dnervecairo.api.requests.GpsPointRequest;
import com.example.dnervecairo.api.responses.TripResponse;
import com.example.dnervecairo.utils.NetworkUtils;
import com.example.dnervecairo.utils.OfflineManager;
import com.example.dnervecairo.utils.PreferenceManager;
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
import com.google.android.material.progressindicator.LinearProgressIndicator;

import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "TripActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int MIN_GPS_POINTS = 3;
    private static final int AVERAGE_SPEED_KMH = 25; // Cairo traffic average

    // Route extras
    public static final String EXTRA_ROUTE_ID = "route_id";
    public static final String EXTRA_ROUTE_NAME = "route_name";
    public static final String EXTRA_START_NAME = "start_name";
    public static final String EXTRA_END_NAME = "end_name";
    public static final String EXTRA_START_LAT = "start_lat";
    public static final String EXTRA_START_LNG = "start_lng";

    // UI Elements
    private TextView tvTimer, tvDistance, tvGpsPoints, tvGpsStatus;
    private TextView tvOfflineIndicator, tvRouteName, tvEta, tvProgressPercent;
    private TextView tvRemainingDistance, tvSpeed, tvEstimatedPoints, tvPassengerCount;
    private ImageView ivGpsStatus;
    private MaterialButton btnStopTrip, btnPassengerMinus, btnPassengerPlus;
    private LinearProgressIndicator progressRoute;

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
    private float currentSpeed = 0f;
    private int passengerCount = 0;

    // Route Data
    private String routeId;
    private String routeName;
    private String startName;
    private String endName;
    private double totalRouteDistanceKm = 0.0;
    private LatLng destinationLatLng = null;

    // Timer
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // Offline
    private OfflineManager offlineManager;
    private boolean isOffline = false;

    // Executor for background tasks
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        offlineManager = new OfflineManager(this);

        // Get route info from intent
        routeId = getIntent().getStringExtra(EXTRA_ROUTE_ID);
        routeName = getIntent().getStringExtra(EXTRA_ROUTE_NAME);
        startName = getIntent().getStringExtra(EXTRA_START_NAME);
        endName = getIntent().getStringExtra(EXTRA_END_NAME);

        initViews();
        initMap();
        initLocationClient();
        setupButtons();
        checkNetworkStatus();
        displayRouteInfo();
    }

    private void initViews() {
        tvTimer = findViewById(R.id.tv_timer);
        tvDistance = findViewById(R.id.tv_distance);
        tvGpsPoints = findViewById(R.id.tv_gps_points);
        tvGpsStatus = findViewById(R.id.tv_gps_status);
        ivGpsStatus = findViewById(R.id.iv_gps_status);
        btnStopTrip = findViewById(R.id.btn_stop_trip);
        tvOfflineIndicator = findViewById(R.id.tv_offline_indicator);

        // New views
        tvRouteName = findViewById(R.id.tv_route_name);
        tvEta = findViewById(R.id.tv_eta);
        tvProgressPercent = findViewById(R.id.tv_progress_percent);
        progressRoute = findViewById(R.id.progress_route);
        tvRemainingDistance = findViewById(R.id.tv_remaining_distance);
        tvSpeed = findViewById(R.id.tv_speed);
        tvEstimatedPoints = findViewById(R.id.tv_estimated_points);
        tvPassengerCount = findViewById(R.id.tv_passenger_count);
        btnPassengerMinus = findViewById(R.id.btn_passenger_minus);
        btnPassengerPlus = findViewById(R.id.btn_passenger_plus);
    }

    private void displayRouteInfo() {
        if (routeName != null && !routeName.isEmpty()) {
            tvRouteName.setText(routeName);
        } else if (startName != null && endName != null) {
            tvRouteName.setText(startName + " → " + endName);
        } else {
            tvRouteName.setText("Recording Trip...");
        }

        tvEta.setText("ETA: --");
        tvProgressPercent.setText("0%");
        progressRoute.setProgress(0);
    }

    private void setupButtons() {
        btnStopTrip.setOnClickListener(v -> stopTrip());

        btnPassengerMinus.setOnClickListener(v -> {
            if (passengerCount > 0) {
                passengerCount--;
                tvPassengerCount.setText(String.valueOf(passengerCount));
            }
        });

        btnPassengerPlus.setOnClickListener(v -> {
            passengerCount++;
            tvPassengerCount.setText(String.valueOf(passengerCount));
        });
    }

    private void checkNetworkStatus() {
        isOffline = !NetworkUtils.isNetworkAvailable(this);
        updateOfflineIndicator();

        NetworkUtils.registerNetworkCallback(this, new NetworkUtils.NetworkCallback() {
            @Override
            public void onNetworkAvailable() {
                runOnUiThread(() -> {
                    isOffline = false;
                    updateOfflineIndicator();
                    offlineManager.syncNow();

                    // Fetch route distance if not already fetched
                    if (totalRouteDistanceKm == 0 && endName != null) {
                        fetchRouteDistance();
                    }
                });
            }

            @Override
            public void onNetworkLost() {
                runOnUiThread(() -> {
                    isOffline = true;
                    updateOfflineIndicator();
                });
            }
        });
    }

    private void updateOfflineIndicator() {
        if (tvOfflineIndicator != null) {
            if (isOffline) {
                tvOfflineIndicator.setVisibility(TextView.VISIBLE);
                tvOfflineIndicator.setText("📴 Offline Mode - Trip will sync when online");
            } else {
                tvOfflineIndicator.setVisibility(TextView.GONE);
            }
        }
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

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

        startTimer();
        updateGpsStatus(true);
        getLastKnownLocation();

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .setWaitForAccurateLocation(false)
                .build();

        if (checkLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }

        // Fetch route distance from Google Directions API
        if (!isOffline && endName != null) {
            fetchRouteDistance();
        }
    }

    private void getLastKnownLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null && tripPoints.isEmpty()) {
                            onNewLocation(location);
                            Toast.makeText(this, "📍 GPS location found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        // Last location not available
                    });
        }
    }

    private void fetchRouteDistance() {
        if (tripPoints.isEmpty() || endName == null) return;

        LatLng startPoint = tripPoints.get(0);
        String origin = startPoint.latitude + "," + startPoint.longitude;
        String destination = endName + ", Cairo, Egypt";

        executor.execute(() -> {
            try {
                // Get API key from manifest
                String apiKey = getApiKeyFromManifest();
                if (apiKey == null || apiKey.isEmpty()) {
                    Log.e(TAG, "Google Maps API key not found");
                    runOnUiThread(this::estimateRouteDistance);
                    return;
                }

                String urlStr = "https://maps.googleapis.com/maps/api/directions/json" +
                        "?origin=" + origin +
                        "&destination=" + java.net.URLEncoder.encode(destination, "UTF-8") +
                        "&mode=driving" +
                        "&key=" + apiKey;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    Log.e(TAG, "Directions API error: " + responseCode);
                    runOnUiThread(this::estimateRouteDistance);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                if (json.getString("status").equals("OK")) {
                    JSONArray routes = json.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        JSONArray legs = route.getJSONArray("legs");
                        if (legs.length() > 0) {
                            JSONObject leg = legs.getJSONObject(0);
                            int distanceMeters = leg.getJSONObject("distance").getInt("value");
                            totalRouteDistanceKm = distanceMeters / 1000.0;

                            // Get destination coordinates
                            JSONObject endLocation = leg.getJSONObject("end_location");
                            double endLat = endLocation.getDouble("lat");
                            double endLng = endLocation.getDouble("lng");
                            destinationLatLng = new LatLng(endLat, endLng);

                            Log.d(TAG, "Route distance: " + totalRouteDistanceKm + " km");

                            runOnUiThread(this::updateRouteProgress);
                        }
                    }
                } else {
                    Log.e(TAG, "Directions API status: " + json.getString("status"));
                    runOnUiThread(this::estimateRouteDistance);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching route distance: " + e.getMessage());
                runOnUiThread(this::estimateRouteDistance);
            }
        });
    }

    private String getApiKeyFromManifest() {
        try {
            android.content.pm.ApplicationInfo appInfo = getPackageManager()
                    .getApplicationInfo(getPackageName(), android.content.pm.PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return appInfo.metaData.getString("com.google.android.geo.API_KEY");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting API key: " + e.getMessage());
        }
        return null;
    }

    private void estimateRouteDistance() {
        // Option A: Simple estimate based on Cairo average trip
        if (totalRouteDistanceKm == 0) {
            totalRouteDistanceKm = 10.0; // Default 10km estimate
        }
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

        updateGpsStatus(true);

        // Update distance
        if (lastLocation != null) {
            totalDistance += lastLocation.distanceTo(location) / 1000.0;
            tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", totalDistance));
        }

        // Update speed
        if (location.hasSpeed()) {
            currentSpeed = location.getSpeed() * 3.6f; // m/s to km/h
        } else if (lastLocation != null) {
            float timeDiff = (location.getTime() - lastLocation.getTime()) / 1000f;
            if (timeDiff > 0) {
                float distanceM = lastLocation.distanceTo(location);
                currentSpeed = (distanceM / timeDiff) * 3.6f;
            }
        }
        tvSpeed.setText(String.format(Locale.getDefault(), "%.0f km/h", currentSpeed));

        lastLocation = location;

        // Update GPS points count
        tvGpsPoints.setText(String.valueOf(tripPoints.size()));

        // Update estimated points (based on distance and quality)
        int estimatedPoints = (int) (totalDistance * 5 + tripPoints.size() * 0.5);
        tvEstimatedPoints.setText("+" + estimatedPoints + " pts");

        // Update route progress
        updateRouteProgress();

        // Move camera
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 16));

            if (tripPoints.size() > 1) {
                mMap.clear();
                mMap.addPolyline(new PolylineOptions()
                        .addAll(tripPoints)
                        .width(10)
                        .color(getResources().getColor(R.color.primary, null)));
            }
        }
    }

    private void updateRouteProgress() {
        double remainingKm;
        int progressPercent;

        if (totalRouteDistanceKm > 0) {
            // Option B: Use actual route distance
            remainingKm = Math.max(0, totalRouteDistanceKm - totalDistance);
            progressPercent = (int) Math.min(100, (totalDistance / totalRouteDistanceKm) * 100);
        } else if (destinationLatLng != null && lastLocation != null) {
            // Fallback: Calculate straight-line distance to destination
            float[] results = new float[1];
            Location.distanceBetween(
                    lastLocation.getLatitude(), lastLocation.getLongitude(),
                    destinationLatLng.latitude, destinationLatLng.longitude,
                    results);
            remainingKm = results[0] / 1000.0;

            // Estimate total based on traveled + remaining
            double estimatedTotal = totalDistance + remainingKm;
            progressPercent = (int) Math.min(100, (totalDistance / estimatedTotal) * 100);
        } else {
            // Option A: Simple estimate
            remainingKm = Math.max(0, 10.0 - totalDistance);
            progressPercent = (int) Math.min(100, totalDistance * 10);
        }

        // Update UI
        tvRemainingDistance.setText(String.format(Locale.getDefault(), "%.1f km", remainingKm));
        tvProgressPercent.setText(progressPercent + "%");
        progressRoute.setProgress(progressPercent);

        // Calculate ETA
        int etaMinutes;
        if (currentSpeed > 5) {
            etaMinutes = (int) (remainingKm / currentSpeed * 60);
        } else {
            etaMinutes = (int) (remainingKm / AVERAGE_SPEED_KMH * 60);
        }

        if (etaMinutes > 0) {
            tvEta.setText("ETA: " + etaMinutes + "m");
        } else {
            tvEta.setText("Arriving");
        }
    }

    private void updateGpsStatus(boolean active) {
        if (active) {
            if (tripPoints.isEmpty()) {
                tvGpsStatus.setText("Waiting for GPS...");
                tvGpsStatus.setTextColor(getResources().getColor(R.color.warning, null));
                ivGpsStatus.setColorFilter(getResources().getColor(R.color.warning, null));
            } else {
                tvGpsStatus.setText("GPS Active");
                tvGpsStatus.setTextColor(getResources().getColor(R.color.success, null));
                ivGpsStatus.setColorFilter(getResources().getColor(R.color.success, null));
            }
        } else {
            tvGpsStatus.setText("GPS Off");
            tvGpsStatus.setTextColor(getResources().getColor(R.color.error, null));
            ivGpsStatus.setColorFilter(getResources().getColor(R.color.error, null));
        }
    }

    private void stopTrip() {
        isTracking = false;

        timerHandler.removeCallbacks(timerRunnable);
        fusedLocationClient.removeLocationUpdates(locationCallback);

        if (tripPoints.size() < MIN_GPS_POINTS) {
            Toast.makeText(this, "Trip too short - need at least " + MIN_GPS_POINTS + " GPS points. Please try again outdoors.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        long duration = System.currentTimeMillis() - startTime;
        int minutes = (int) (duration / (1000 * 60));

        submitTripToApi(minutes);
    }

    private void submitTripToApi(int durationMinutes) {
        PreferenceManager prefManager = new PreferenceManager(this);

        if (!prefManager.isLoggedIn()) {
            openTripSummary(durationMinutes, false);
            return;
        }

        List<GpsPointRequest> gpsPointsList = new ArrayList<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

        long tripDurationMs = System.currentTimeMillis() - startTime;
        long intervalMs = tripPoints.size() > 1 ? tripDurationMs / (tripPoints.size() - 1) : 0;

        for (int i = 0; i < tripPoints.size(); i++) {
            LatLng point = tripPoints.get(i);
            long pointTime = startTime + (i * intervalMs);
            gpsPointsList.add(new GpsPointRequest(
                    point.latitude,
                    point.longitude,
                    sdf.format(new java.util.Date(pointTime)),
                    10.0f
            ));
        }

        String endTime = sdf.format(new java.util.Date());
        String startTimeStr = sdf.format(new java.util.Date(startTime));

        TripSubmission request = new TripSubmission(
                prefManager.getDriverId(),
                startTimeStr,
                endTime,
                gpsPointsList
        );

        if (isOffline || !NetworkUtils.isNetworkAvailable(this)) {
            offlineManager.saveTripOffline(request);
            Toast.makeText(this, "📴 Trip saved offline - will sync when online", Toast.LENGTH_LONG).show();
            openTripSummary(durationMinutes, true);
            return;
        }

        ApiClient.getInstance().getApiService()
                .submitTrip(request)
                .enqueue(new Callback<TripResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TripResponse> call, @NonNull Response<TripResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(TripActivity.this, "Trip saved! +" + response.body().getPointsEarned() + " points", Toast.LENGTH_SHORT).show();
                            openTripSummary(durationMinutes, false);
                        } else {
                            offlineManager.saveTripOffline(request);
                            Toast.makeText(TripActivity.this, "Trip saved offline", Toast.LENGTH_SHORT).show();
                            openTripSummary(durationMinutes, true);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TripResponse> call, @NonNull Throwable t) {
                        offlineManager.saveTripOffline(request);
                        Toast.makeText(TripActivity.this, "📴 Trip saved offline - will sync when online", Toast.LENGTH_LONG).show();
                        openTripSummary(durationMinutes, true);
                    }
                });
    }

    private void openTripSummary(int durationMinutes, boolean savedOffline) {
        Intent intent = new Intent(this, TripSummaryActivity.class);
        intent.putExtra(TripSummaryActivity.EXTRA_DURATION, durationMinutes);
        intent.putExtra(TripSummaryActivity.EXTRA_DISTANCE, totalDistance);
        intent.putExtra(TripSummaryActivity.EXTRA_GPS_POINTS, tripPoints.size());
        intent.putExtra("saved_offline", savedOffline);
        intent.putExtra("route_name", routeName);
        intent.putExtra("passenger_count", passengerCount);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NetworkUtils.unregisterNetworkCallback(this);
        executor.shutdown();
        if (isTracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}