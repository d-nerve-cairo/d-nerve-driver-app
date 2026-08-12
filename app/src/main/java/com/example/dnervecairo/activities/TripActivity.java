package com.example.dnervecairo.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;  // NEW: For API level check
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
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
// NEW: Live trip imports
import com.example.dnervecairo.api.requests.LiveGpsUpdateRequest;
import com.example.dnervecairo.api.responses.LiveTripEndResponse;
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
import com.example.dnervecairo.utils.OsrmRouteHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.JsonObject;

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
    private static final int AVERAGE_SPEED_KMH = 25;

    // NEW: Live GPS streaming intervals
    private static final long GPS_INTERVAL_MOVING = 10000;  // 10 seconds when moving
    private static final long GPS_INTERVAL_STOPPED = 20000; // 20 seconds when stopped/slow
    private static final float SPEED_THRESHOLD_KMH = 5.0f;  // Threshold for moving vs stopped

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
    private TextView tvCurrentArea, tvGpsQuality;
    private ImageView ivGpsStatus;
    private View viewGpsPulse;
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
    private int passengerCount = 1;
    private int predictedEtaMinutes = 0;

    // Route Data
    private String routeId;
    private String routeName;
    private String startName;
    private String endName;
    private double totalRouteDistanceKm = 0.0;
    private LatLng destinationLatLng = null;

    // Route coordinates from intent
    private double routeOriginLat = 0;
    private double routeOriginLon = 0;
    private double routeDestLat = 0;
    private double routeDestLon = 0;
    private LatLng routeOriginLatLng = null;
    private LatLng routeDestLatLng = null;

    // Decoded route polyline
    private List<LatLng> routePolylinePoints = new ArrayList<>();

    // Timer
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // GPS pulse animation
    private final Handler pulseHandler = new Handler(Looper.getMainLooper());
    private Runnable pulseRunnable;

    // Offline
    private OfflineManager offlineManager;
    private boolean isOffline = false;

    // Executor for background tasks
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Geocoder for current area
    private Geocoder geocoder;
    private String lastAreaName = "";

    // Control when to start following driver
    private boolean shouldFollowDriver = false;
    private final Handler followHandler = new Handler(Looper.getMainLooper());

    // Vibrator for haptic feedback
    private Vibrator vibrator;

    // D5: Auto trip-end detection
    private boolean arrivalPromptShown = false;
    private final Handler arrivalHandler = new Handler(Looper.getMainLooper());
    private Runnable arrivalCountdownRunnable;

    // NEW: Live trip mode fields
    private boolean isLiveMode = false;
    private String liveTripId = null;
    private long lastGpsPostTime = 0;
    private int serverGpsPointsCount = 0;
    private PreferenceManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        offlineManager = new OfflineManager(this);
        geocoder = new Geocoder(this, Locale.getDefault());
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        prefManager = new PreferenceManager(this);  // NEW: Initialize here

        // Get route info from intent
        routeId = getIntent().getStringExtra(EXTRA_ROUTE_ID);
        routeName = getIntent().getStringExtra(EXTRA_ROUTE_NAME);
        startName = getIntent().getStringExtra(EXTRA_START_NAME);
        endName = getIntent().getStringExtra(EXTRA_END_NAME);

        // Get passenger count from StartTripActivity
        passengerCount = getIntent().getIntExtra("passenger_count", 1);
        predictedEtaMinutes = getIntent().getIntExtra("predicted_eta", 0);

        // Get route coordinates from intent
        routeOriginLat = getIntent().getDoubleExtra("route_origin_lat", 0);
        routeOriginLon = getIntent().getDoubleExtra("route_origin_lon", 0);
        routeDestLat = getIntent().getDoubleExtra("route_dest_lat", 0);
        routeDestLon = getIntent().getDoubleExtra("route_dest_lon", 0);
        totalRouteDistanceKm = getIntent().getFloatExtra("route_distance_km", 0);

        // NEW: Get live mode info from intent
        isLiveMode = getIntent().getBooleanExtra("is_live_mode", false);
        liveTripId = getIntent().getStringExtra("live_trip_id");

        // Create LatLng objects if coordinates are valid
        if (routeOriginLat != 0 && routeOriginLon != 0) {
            routeOriginLatLng = new LatLng(routeOriginLat, routeOriginLon);
        }
        if (routeDestLat != 0 && routeDestLon != 0) {
            routeDestLatLng = new LatLng(routeDestLat, routeDestLon);
            destinationLatLng = routeDestLatLng;
        }

        // NEW: Log trip mode
        Log.d(TAG, "Trip started - LiveMode: " + isLiveMode + ", TripId: " + liveTripId +
                ", Passengers: " + passengerCount + ", ETA: " + predictedEtaMinutes + " min");

        initViews();
        initMap();
        initLocationClient();
        setupButtons();
        checkNetworkStatus();
        displayRouteInfo();
        startGpsPulseAnimation();
    }

    private void initViews() {
        tvTimer = findViewById(R.id.tv_timer);
        tvDistance = findViewById(R.id.tv_distance);
        tvGpsPoints = findViewById(R.id.tv_gps_points);
        tvGpsStatus = findViewById(R.id.tv_gps_status);
        ivGpsStatus = findViewById(R.id.iv_gps_status);
        btnStopTrip = findViewById(R.id.btn_stop_trip);
        tvOfflineIndicator = findViewById(R.id.tv_offline_indicator);

        // Route info
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

        // New premium views
        tvCurrentArea = findViewById(R.id.tv_current_area);
        tvGpsQuality = findViewById(R.id.tv_gps_quality);
        viewGpsPulse = findViewById(R.id.view_gps_pulse);

        // Set initial passenger count from intent
        if (tvPassengerCount != null) {
            tvPassengerCount.setText(String.valueOf(passengerCount));
        }
    }

    private void displayRouteInfo() {
        if (routeName != null && !routeName.isEmpty()) {
            tvRouteName.setText(routeName);
        } else if (startName != null && endName != null) {
            tvRouteName.setText(startName + " → " + endName);
        } else {
            tvRouteName.setText("Recording Trip...");
        }

        // Show predicted ETA if available
        if (predictedEtaMinutes > 0) {
            tvEta.setText("ETA: " + predictedEtaMinutes + "m");
        } else {
            tvEta.setText("ETA: --");
        }

        tvProgressPercent.setText("0%");
        progressRoute.setProgress(0);

        // Show initial remaining distance if available
        if (totalRouteDistanceKm > 0) {
            tvRemainingDistance.setText(String.format(Locale.getDefault(), "%.1f km", totalRouteDistanceKm));
        }

        // Set current area
        if (tvCurrentArea != null) {
            // NEW: Show mode indicator
            if (isLiveMode) {
                tvCurrentArea.setText("🟢 Live trip starting...");
            } else {
                tvCurrentArea.setText("📍 Starting trip...");
            }
        }
    }

    private void setupButtons() {
        btnStopTrip.setOnClickListener(v -> {
            // Haptic feedback - FIXED: API level check
            vibrateHeavy();
            stopTrip();
        });

        btnPassengerMinus.setOnClickListener(v -> {
            if (passengerCount > 0) {
                passengerCount--;
                if (tvPassengerCount != null) {
                    tvPassengerCount.setText(String.valueOf(passengerCount));
                    animatePassengerChange(tvPassengerCount);
                }
                vibrateLight();
            }
        });

        btnPassengerPlus.setOnClickListener(v -> {
            if (passengerCount < 50) {
                passengerCount++;
                if (tvPassengerCount != null) {
                    tvPassengerCount.setText(String.valueOf(passengerCount));
                    animatePassengerChange(tvPassengerCount);
                }
                vibrateLight();
            }
        });
    }

    private void animatePassengerChange(View view) {
        view.animate()
                .scaleX(1.2f).scaleY(1.2f)
                .setDuration(100)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(100)
                                .start()
                ).start();
    }

    // FIXED: Added API level check for vibration
    private void vibrateHeavy() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    // Deprecated but works on older APIs
                    vibrator.vibrate(100);
                }
            }
        } catch (Exception e) {
            // Ignore vibration errors
        }
    }

    // FIXED: Added API level check for vibration
    private void vibrateLight() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    // Deprecated but works on older APIs
                    vibrator.vibrate(30);
                }
            }
        } catch (Exception e) {
            // Ignore vibration errors
        }
    }

    private void startGpsPulseAnimation() {
        if (viewGpsPulse == null) return;

        pulseRunnable = new Runnable() {
            @Override
            public void run() {
                viewGpsPulse.animate()
                        .scaleX(1.5f).scaleY(1.5f)
                        .alpha(0f)
                        .setDuration(1000)
                        .withEndAction(() -> {
                            viewGpsPulse.setScaleX(1f);
                            viewGpsPulse.setScaleY(1f);
                            viewGpsPulse.setAlpha(0.5f);
                        }).start();

                pulseHandler.postDelayed(this, 2000);
            }
        };
        pulseHandler.post(pulseRunnable);
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

    // MODIFIED: Updated to show live mode status
    private void updateOfflineIndicator() {
        if (tvOfflineIndicator != null) {
            if (isOffline) {
                if (isLiveMode) {
                    tvOfflineIndicator.setVisibility(View.VISIBLE);
                    tvOfflineIndicator.setText("📴 Offline - GPS will sync when online");
                } else {
                    tvOfflineIndicator.setVisibility(View.VISIBLE);
                    tvOfflineIndicator.setText("📴 Offline Mode - Trip will sync when online");
                }
            } else {
                tvOfflineIndicator.setVisibility(View.GONE);
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

        // Draw route markers and line
        drawRouteOnMap();

        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
            startTracking();
        } else {
            requestLocationPermission();
        }
    }

    private void drawRouteOnMap() {
        if (mMap == null) return;

        // Add start marker (green)
        if (routeOriginLatLng != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(routeOriginLatLng)
                    .title("Start: " + (startName != null ? startName : "Origin"))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }

        // Add end marker (red)
        if (routeDestLatLng != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(routeDestLatLng)
                    .title("End: " + (endName != null ? endName : "Destination"))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        // Draw road-following polyline if available
        if (!routePolylinePoints.isEmpty()) {
            mMap.addPolyline(new PolylineOptions()
                    .addAll(routePolylinePoints)
                    .width(8)
                    .color(Color.parseColor("#2196F3"))
                    .geodesic(true));
        } else if (routeOriginLatLng != null && routeDestLatLng != null) {
            mMap.addPolyline(new PolylineOptions()
                    .add(routeOriginLatLng, routeDestLatLng)
                    .width(8)
                    .color(Color.parseColor("#2196F3"))
                    .geodesic(true));
        }

        // Zoom to show route
        if (routeOriginLatLng != null && routeDestLatLng != null) {
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            boundsBuilder.include(routeOriginLatLng);
            boundsBuilder.include(routeDestLatLng);

            try {
                LatLngBounds bounds = boundsBuilder.build();
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            } catch (Exception e) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(routeOriginLatLng, 14));
            }

            // Start following driver after 5 seconds
            followHandler.postDelayed(() -> {
                shouldFollowDriver = true;
            }, 5000);
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
        updateGpsStatus(true, 0);
        getLastKnownLocation();

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .setWaitForAccurateLocation(false)
                .build();

        if (checkLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }

        // Fetch route distance
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
                    });
        }
    }

    private void fetchRouteDistance() {
        if (tripPoints.isEmpty()) return;

        LatLng startPoint = tripPoints.get(0);

        // Try to use route coordinates if available, otherwise estimate
        LatLng destLatLng = destinationLatLng;
        if (destLatLng == null) {
            runOnUiThread(this::estimateRouteDistance);
            return;
        }

        // Use OSRM (free, no API key required) instead of Google Directions API
        OsrmRouteHelper.fetchRoute(startPoint, destLatLng, new OsrmRouteHelper.RouteCallback() {
            @Override
            public void onRouteReady(List<LatLng> points) {
                if (points.isEmpty()) { estimateRouteDistance(); return; }
                routePolylinePoints = points;

                // Estimate total distance from the first OSRM route point to last
                if (totalRouteDistanceKm <= 0 && points.size() > 1) {
                    double dist = 0;
                    for (int i = 1; i < points.size(); i++) {
                        dist += haversineKm(points.get(i-1).latitude, points.get(i-1).longitude,
                                points.get(i).latitude, points.get(i).longitude);
                    }
                    totalRouteDistanceKm = dist;
                }

                updateRouteProgress();
                if (mMap != null && !routePolylinePoints.isEmpty()) {
                    mMap.clear();
                    drawRouteOnMap();
                    if (tripPoints.size() > 1) {
                        mMap.addPolyline(new PolylineOptions()
                                .addAll(tripPoints)
                                .width(10)
                                .color(getResources().getColor(R.color.primary, null)));
                    }
                }
            }

            private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
                float[] results = new float[1];
                android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results);
                return results[0] / 1000.0;
            }

            @Override
            public void onRouteFailed() {
                estimateRouteDistance();
            }
        });
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new LatLng((double) lat / 1E5, (double) lng / 1E5));
        }
        return poly;
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
        if (totalRouteDistanceKm == 0) {
            totalRouteDistanceKm = 10.0;
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

    // MODIFIED: Added live GPS streaming
    private void onNewLocation(Location location) {
        LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
        tripPoints.add(newPoint);

        // Update GPS status with accuracy
        float accuracy = location.getAccuracy();
        updateGpsStatus(true, accuracy);

        // Update distance
        if (lastLocation != null) {
            totalDistance += lastLocation.distanceTo(location) / 1000.0;
            tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", totalDistance));
        }

        // Update speed
        if (location.hasSpeed()) {
            currentSpeed = location.getSpeed() * 3.6f;
        } else if (lastLocation != null) {
            float timeDiff = (location.getTime() - lastLocation.getTime()) / 1000f;
            if (timeDiff > 0) {
                float distanceM = lastLocation.distanceTo(location);
                currentSpeed = (distanceM / timeDiff) * 3.6f;
            }
        }
        tvSpeed.setText(String.format(Locale.getDefault(), "%.0f km/h", currentSpeed));

        lastLocation = location;

        // Update GPS points
        tvGpsPoints.setText(String.valueOf(tripPoints.size()));

        // Update estimated points (distance + GPS quality bonus)
        int distancePoints = (int) (totalDistance * 10);
        int gpsBonus = tripPoints.size() / 2;
        int passengerBonus = passengerCount * 5;
        int estimatedPoints = distancePoints + gpsBonus + passengerBonus;
        tvEstimatedPoints.setText("+" + estimatedPoints + " pts");

        // Update route progress
        updateRouteProgress();

        // Update current area (every 10 points to save battery)
        if (tripPoints.size() % 10 == 0) {
            updateCurrentArea(location);
        }

        // NEW: Stream GPS to server if in live mode
        if (isLiveMode && liveTripId != null && !isOffline) {
            streamGpsToServer(location);
        }

        // Update map
        if (mMap != null) {
            mMap.clear();

            // Re-draw markers
            if (routeOriginLatLng != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(routeOriginLatLng)
                        .title("Start: " + (startName != null ? startName : "Origin"))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            }

            if (routeDestLatLng != null) {
                mMap.addMarker(new MarkerOptions()
                        .position(routeDestLatLng)
                        .title("End: " + (endName != null ? endName : "Destination"))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }

            // Draw route line
            if (!routePolylinePoints.isEmpty()) {
                mMap.addPolyline(new PolylineOptions()
                        .addAll(routePolylinePoints)
                        .width(6)
                        .color(Color.parseColor("#90CAF9"))
                        .geodesic(true));
            } else if (routeOriginLatLng != null && routeDestLatLng != null) {
                mMap.addPolyline(new PolylineOptions()
                        .add(routeOriginLatLng, routeDestLatLng)
                        .width(6)
                        .color(Color.parseColor("#90CAF9"))
                        .geodesic(true));
            }

            // Draw driver's path
            if (tripPoints.size() > 1) {
                mMap.addPolyline(new PolylineOptions()
                        .addAll(tripPoints)
                        .width(10)
                        .color(getResources().getColor(R.color.primary, null)));
            }

            // Follow driver
            if (shouldFollowDriver) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 16));
            }
        }
    }

    // =========================================================================
    // NEW: LIVE GPS STREAMING
    // =========================================================================

    /**
     * Stream GPS update to server based on speed-adaptive interval
     * - 10 seconds when moving (speed > 5 km/h)
     * - 20 seconds when stopped/slow (speed <= 5 km/h)
     */
    private void streamGpsToServer(Location location) {
        long now = System.currentTimeMillis();
        long interval = currentSpeed > SPEED_THRESHOLD_KMH ? GPS_INTERVAL_MOVING : GPS_INTERVAL_STOPPED;

        if (now - lastGpsPostTime < interval) {
            return; // Not time yet
        }

        lastGpsPostTime = now;

        LiveGpsUpdateRequest request = new LiveGpsUpdateRequest(
                location.getLatitude(),
                location.getLongitude(),
                currentSpeed,
                location.getAccuracy(),
                location.hasBearing() ? location.getBearing() : null
        );

        Log.d(TAG, "Streaming GPS: lat=" + location.getLatitude() +
                ", lon=" + location.getLongitude() + ", speed=" + currentSpeed);

        ApiClient.getInstance().getApiService()
                .sendGpsUpdate(liveTripId, request)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call,
                                           @NonNull Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // Update server GPS count if available
                            JsonObject body = response.body();
                            if (body.has("gps_points_count")) {
                                serverGpsPointsCount = body.get("gps_points_count").getAsInt();
                            }
                            Log.d(TAG, "GPS streamed successfully, server count: " + serverGpsPointsCount);
                        } else {
                            Log.w(TAG, "GPS stream failed: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        Log.w(TAG, "GPS stream error: " + t.getMessage());
                        // Don't interrupt user - GPS will be sent on next interval
                    }
                });
    }

    private void updateCurrentArea(Location location) {
        executor.execute(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(
                        location.getLatitude(), location.getLongitude(), 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String areaName = address.getSubLocality();
                    if (areaName == null) areaName = address.getThoroughfare();
                    if (areaName == null) areaName = address.getLocality();

                    if (areaName != null && !areaName.equals(lastAreaName)) {
                        lastAreaName = areaName;
                        final String finalArea = areaName;
                        runOnUiThread(() -> {
                            if (tvCurrentArea != null) {
                                // NEW: Show live indicator
                                String prefix = isLiveMode ? "🟢 " : "📍 ";
                                tvCurrentArea.setText(prefix + "Passing: " + finalArea);
                                // Animate the update
                                tvCurrentArea.setAlpha(0.5f);
                                tvCurrentArea.animate().alpha(1f).setDuration(300).start();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Geocoding failed: " + e.getMessage());
            }
        });
    }

    private void updateRouteProgress() {
        double remainingKm;
        int progressPercent;

        if (totalRouteDistanceKm > 0) {
            remainingKm = Math.max(0, totalRouteDistanceKm - totalDistance);
            progressPercent = (int) Math.min(100, (totalDistance / totalRouteDistanceKm) * 100);
        } else if (destinationLatLng != null && lastLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    lastLocation.getLatitude(), lastLocation.getLongitude(),
                    destinationLatLng.latitude, destinationLatLng.longitude,
                    results);
            remainingKm = results[0] / 1000.0;
            double estimatedTotal = totalDistance + remainingKm;
            progressPercent = (int) Math.min(100, (totalDistance / estimatedTotal) * 100);
        } else {
            remainingKm = Math.max(0, 10.0 - totalDistance);
            progressPercent = (int) Math.min(100, totalDistance * 10);
        }

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

        // D5: Auto trip-end — prompt when within 300m of destination
        if (!arrivalPromptShown && totalDistance > 1.0 && remainingKm <= 0.3 && destinationLatLng != null) {
            arrivalPromptShown = true;
            runOnUiThread(this::showArrivalDialog);
        }
    }

    private void updateGpsStatus(boolean active, float accuracy) {
        if (!active) {
            tvGpsStatus.setText("GPS Off");
            tvGpsStatus.setTextColor(getResources().getColor(R.color.error, null));
            ivGpsStatus.setColorFilter(getResources().getColor(R.color.error, null));
            if (tvGpsQuality != null) tvGpsQuality.setText("No Signal");
            return;
        }

        if (tripPoints.isEmpty()) {
            tvGpsStatus.setText("Waiting...");
            tvGpsStatus.setTextColor(getResources().getColor(R.color.warning, null));
            ivGpsStatus.setColorFilter(getResources().getColor(R.color.warning, null));
            if (tvGpsQuality != null) tvGpsQuality.setText("Searching");
            return;
        }

        // GPS is active - show quality based on accuracy
        // NEW: Show "Live" indicator if streaming
        if (isLiveMode && !isOffline) {
            tvGpsStatus.setText("🟢 Live");
        } else {
            tvGpsStatus.setText("GPS Active");
        }

        String quality;
        int color;

        if (accuracy < 10) {
            quality = "Excellent";
            color = getResources().getColor(R.color.success, null);
        } else if (accuracy < 25) {
            quality = "Good";
            color = getResources().getColor(R.color.success, null);
        } else if (accuracy < 50) {
            quality = "Fair";
            color = getResources().getColor(R.color.warning, null);
        } else {
            quality = "Weak";
            color = getResources().getColor(R.color.error, null);
        }

        tvGpsStatus.setTextColor(color);
        ivGpsStatus.setColorFilter(color);

        if (tvGpsQuality != null) {
            tvGpsQuality.setText(quality);
            tvGpsQuality.setTextColor(color);
        }
    }

    // =========================================================================
    // STOP TRIP (Modified for Live Mode)
    // =========================================================================

    // MODIFIED: Check for live mode
    private void stopTrip() {
        isTracking = false;

        timerHandler.removeCallbacks(timerRunnable);
        pulseHandler.removeCallbacks(pulseRunnable);
        fusedLocationClient.removeLocationUpdates(locationCallback);

        if (tripPoints.size() < MIN_GPS_POINTS) {
            Toast.makeText(this, "Trip too short - need at least " + MIN_GPS_POINTS + " GPS points", Toast.LENGTH_LONG).show();
            // NEW: Clear active trip data if live mode
            if (isLiveMode) {
                prefManager.clearActiveTripData();
            }
            finish();
            return;
        }

        long duration = System.currentTimeMillis() - startTime;
        int minutes = (int) (duration / (1000 * 60));

        // NEW: Choose submission method based on mode
        if (isLiveMode && liveTripId != null && !isOffline) {
            endLiveTrip(minutes);
        } else {
            submitTripToApi(minutes);
        }
    }

    // NEW: End live trip via API
    private void endLiveTrip(int durationMinutes) {
        Log.d(TAG, "Ending live trip: " + liveTripId);

        // Show loading state
        btnStopTrip.setEnabled(false);
        btnStopTrip.setText("Ending trip...");

        ApiClient.getInstance().getApiService()
                .endLiveTrip(liveTripId)
                .enqueue(new Callback<LiveTripEndResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<LiveTripEndResponse> call,
                                           @NonNull Response<LiveTripEndResponse> response) {
                        // Clear active trip data
                        prefManager.clearActiveTripData();

                        if (response.isSuccessful() && response.body() != null) {
                            LiveTripEndResponse endResponse = response.body();

                            Log.d(TAG, "Live trip ended: points=" + endResponse.getPointsEarned() +
                                    ", quality=" + endResponse.getQualityScore() +
                                    ", tier=" + endResponse.getDriverTier());

                            Toast.makeText(TripActivity.this,
                                    "🎉 Trip complete! +" + endResponse.getPointsEarned() + " points",
                                    Toast.LENGTH_SHORT).show();

                            // Open summary with server values
                            openTripSummaryWithServerData(
                                    durationMinutes,
                                    endResponse.getPointsEarned(),
                                    endResponse.getQualityScorePercent(),
                                    endResponse.getDriverTier(),
                                    endResponse.getDriverTotalPoints()
                            );
                        } else {
                            Log.w(TAG, "End trip API failed: " + response.code());
                            // Fallback to local calculation
                            openTripSummary(durationMinutes, false);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LiveTripEndResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "End trip request failed: " + t.getMessage());
                        // Clear active trip data
                        prefManager.clearActiveTripData();
                        // Fallback to local calculation
                        openTripSummary(durationMinutes, false);
                    }
                });
    }

    // Existing batch submission method (unchanged)
    private void submitTripToApi(int durationMinutes) {
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
            Toast.makeText(this, "Trip saved offline", Toast.LENGTH_LONG).show();
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
                            openTripSummary(durationMinutes, true);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TripResponse> call, @NonNull Throwable t) {
                        offlineManager.saveTripOffline(request);
                        openTripSummary(durationMinutes, true);
                    }
                });
    }

    // Existing method (unchanged) - for batch mode and fallback
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

    // NEW: Open summary with server-provided values
    private void openTripSummaryWithServerData(int durationMinutes, int pointsEarned,
                                               int qualityScore, String driverTier,
                                               int totalPoints) {
        Intent intent = new Intent(this, TripSummaryActivity.class);
        intent.putExtra(TripSummaryActivity.EXTRA_DURATION, durationMinutes);
        intent.putExtra(TripSummaryActivity.EXTRA_DISTANCE, totalDistance);
        intent.putExtra(TripSummaryActivity.EXTRA_GPS_POINTS, tripPoints.size());
        intent.putExtra("saved_offline", false);
        intent.putExtra("route_name", routeName);
        intent.putExtra("passenger_count", passengerCount);

        // NEW: Server-provided values
        intent.putExtra("is_live_mode", true);
        intent.putExtra("server_points_earned", pointsEarned);
        intent.putExtra("server_quality_score", qualityScore);
        intent.putExtra("server_driver_tier", driverTier);
        intent.putExtra("server_total_points", totalPoints);

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NetworkUtils.unregisterNetworkCallback(this);
        executor.shutdown();
        pulseHandler.removeCallbacks(pulseRunnable);
        arrivalHandler.removeCallbacksAndMessages(null);
        if (isTracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    // =========================================================================
    // D5: AUTO TRIP-END DETECTION
    // =========================================================================

    /**
     * Show arrival confirmation dialog with 30-second auto-confirm countdown.
     * Triggered when the driver is within 300m of the route destination.
     * "Keep Driving" re-arms the detector in case of route deviation.
     */
    private void showArrivalDialog() {
        if (!isTracking || isFinishing()) return;

        final int[] secondsLeft = {30};

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("\uD83C\uDFC1 You've Arrived!");
        builder.setMessage("You appear to have reached your destination.\n\nEnding trip in 30 seconds...");
        builder.setCancelable(false);
        builder.setPositiveButton("End Trip Now", (dialog, which) -> {
            arrivalHandler.removeCallbacks(arrivalCountdownRunnable);
            stopTrip();
        });
        builder.setNegativeButton("Keep Driving", (dialog, which) -> {
            arrivalHandler.removeCallbacks(arrivalCountdownRunnable);
            arrivalPromptShown = false; // Re-arm for further driving
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        arrivalCountdownRunnable = new Runnable() {
            @Override
            public void run() {
                secondsLeft[0]--;
                if (secondsLeft[0] <= 0) {
                    if (dialog.isShowing()) dialog.dismiss();
                    stopTrip();
                    return;
                }
                if (dialog.isShowing()) {
                    dialog.setMessage(
                            "You appear to have reached your destination.\n\nEnding trip in "
                                    + secondsLeft[0] + " seconds...");
                }
                arrivalHandler.postDelayed(this, 1000);
            }
        };
        arrivalHandler.postDelayed(arrivalCountdownRunnable, 1000);
    }
}