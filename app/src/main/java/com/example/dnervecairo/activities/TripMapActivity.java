package com.example.dnervecairo.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dnervecairo.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TripMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_TRIP_ID = "trip_id";
    public static final String EXTRA_GPS_POINTS = "gps_points";
    public static final String EXTRA_DURATION = "duration";
    public static final String EXTRA_DISTANCE = "distance";
    public static final String EXTRA_POINTS_EARNED = "points_earned";
    public static final String EXTRA_QUALITY = "quality";
    public static final String EXTRA_DATE = "date";
    public static final String EXTRA_ROUTE_NAME = "route_name";
    public static final String EXTRA_PASSENGERS = "passengers";

    private GoogleMap mMap;
    private String gpsPointsJson;
    private List<LatLng> routePoints = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Views
    private TextView tvTripId, tvDuration, tvDistance, tvPoints, tvQuality;
    private TextView tvRouteName, tvPassengers, tvGpsPoints;
    private MaterialCardView cardInfo, cardRoute;
    private View layoutStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_map);

        initViews();
        setupToolbar();
        prepareEntranceAnimations();
        loadTripData();

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void initViews() {
        tvTripId = findViewById(R.id.tv_trip_id);
        tvDuration = findViewById(R.id.tv_duration);
        tvDistance = findViewById(R.id.tv_distance);
        tvPoints = findViewById(R.id.tv_points);
        tvQuality = findViewById(R.id.tv_quality);
        tvRouteName = findViewById(R.id.tv_route_name);
        tvPassengers = findViewById(R.id.tv_passengers);
        tvGpsPoints = findViewById(R.id.tv_gps_points);
        cardInfo = findViewById(R.id.card_info);
        cardRoute = findViewById(R.id.card_route);
        layoutStats = findViewById(R.id.layout_stats);
    }

    private void prepareEntranceAnimations() {
        if (cardRoute != null) {
            cardRoute.setAlpha(0f);
            cardRoute.setTranslationY(-30f);
        }
        if (cardInfo != null) {
            cardInfo.setAlpha(0f);
            cardInfo.setTranslationY(50f);
        }
    }

    private void playEntranceAnimations() {
        // Route card slides down
        if (cardRoute != null) {
            cardRoute.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }

        // Info card slides up
        handler.postDelayed(() -> {
            if (cardInfo != null) {
                cardInfo.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            }
        }, 200);

        // Animate stat values
        handler.postDelayed(this::animateStatValues, 500);
    }

    private void animateStatValues() {
        View[] statViews = {
            findViewById(R.id.stat_duration),
            findViewById(R.id.stat_distance),
            findViewById(R.id.stat_points),
            findViewById(R.id.stat_quality)
        };

        for (int i = 0; i < statViews.length; i++) {
            View stat = statViews[i];
            if (stat != null) {
                stat.setScaleX(0.8f);
                stat.setScaleY(0.8f);
                stat.setAlpha(0.5f);
                
                final int delay = i * 80;
                handler.postDelayed(() -> {
                    stat.animate()
                        .scaleX(1f).scaleY(1f)
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
                }, delay);
            }
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            animateClick(v);
            handler.postDelayed(this::finish, 100);
        });
    }

    private void loadTripData() {
        String tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        gpsPointsJson = getIntent().getStringExtra(EXTRA_GPS_POINTS);
        double duration = getIntent().getDoubleExtra(EXTRA_DURATION, 0);
        double distance = getIntent().getDoubleExtra(EXTRA_DISTANCE, 0);
        int pointsEarned = getIntent().getIntExtra(EXTRA_POINTS_EARNED, 0);
        double quality = getIntent().getDoubleExtra(EXTRA_QUALITY, 0);
        String date = getIntent().getStringExtra(EXTRA_DATE);
        String routeName = getIntent().getStringExtra(EXTRA_ROUTE_NAME);
        int passengers = getIntent().getIntExtra(EXTRA_PASSENGERS, 0);

        // Display trip info
        if (tvTripId != null) {
            tvTripId.setText(date != null ? date : tripId);
        }
        
        if (tvDuration != null) {
            tvDuration.setText(String.format(Locale.US, "%.0f min", duration));
        }
        
        if (tvDistance != null) {
            tvDistance.setText(String.format(Locale.US, "%.2f km", distance));
        }
        
        if (tvPoints != null) {
            tvPoints.setText(String.format(Locale.US, "+%d", pointsEarned));
        }
        
        if (tvQuality != null) {
            int qualityPercent = (int) (quality * 100);
            tvQuality.setText(String.format(Locale.US, "%d%%", qualityPercent));
            
            // Color based on quality
            int qualityColor;
            if (qualityPercent >= 80) {
                qualityColor = getResources().getColor(R.color.success, null);
            } else if (qualityPercent >= 60) {
                qualityColor = getResources().getColor(R.color.warning, null);
            } else {
                qualityColor = getResources().getColor(R.color.error, null);
            }
            tvQuality.setTextColor(qualityColor);
        }

        // Route name
        if (tvRouteName != null) {
            if (routeName != null && !routeName.isEmpty()) {
                tvRouteName.setText(routeName);
                tvRouteName.setVisibility(View.VISIBLE);
            } else {
                tvRouteName.setVisibility(View.GONE);
            }
        }

        // Passengers
        if (tvPassengers != null) {
            if (passengers > 0) {
                tvPassengers.setText(String.format(Locale.US, "%d passengers", passengers));
                tvPassengers.setVisibility(View.VISIBLE);
            } else {
                tvPassengers.setVisibility(View.GONE);
            }
        }

        // Parse GPS points
        parseGpsPoints();

        // GPS points count
        if (tvGpsPoints != null) {
            tvGpsPoints.setText(String.format(Locale.US, "%d GPS points recorded", routePoints.size()));
        }
    }

    private void parseGpsPoints() {
        if (gpsPointsJson == null || gpsPointsJson.isEmpty()) {
            return;
        }

        try {
            JSONArray jsonArray = new JSONArray(gpsPointsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject point = jsonArray.getJSONObject(i);
                double lat = point.getDouble("latitude");
                double lon = point.getDouble("longitude");
                routePoints.add(new LatLng(lat, lon));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Configure map UI
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Try to apply custom style (optional)
        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        } catch (Exception e) {
            // Style not found, use default
        }

        if (routePoints.isEmpty()) {
            // Default to Cairo if no points
            LatLng cairo = new LatLng(30.0444, 31.2357);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cairo, 12));
            showToast("No GPS data available for this trip");
            playEntranceAnimations();
            return;
        }

        // Draw route polyline with gradient effect
        drawRoute();

        // Add markers
        addMarkers();

        // Zoom to fit route
        zoomToFitRoute();

        // Play animations after map is ready
        playEntranceAnimations();
    }

    private void drawRoute() {
        if (routePoints.size() < 2) return;

        // Main route line
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(routePoints)
                .width(12)
                .color(Color.parseColor("#1E88E5"))
                .geodesic(true)
                .startCap(new RoundCap())
                .endCap(new RoundCap());
        mMap.addPolyline(polylineOptions);

        // Border/shadow line (drawn first, underneath)
        PolylineOptions borderOptions = new PolylineOptions()
                .addAll(routePoints)
                .width(16)
                .color(Color.parseColor("#0D47A1"))
                .geodesic(true)
                .startCap(new RoundCap())
                .endCap(new RoundCap())
                .zIndex(-1);
        mMap.addPolyline(borderOptions);
    }

    private void addMarkers() {
        if (routePoints.isEmpty()) return;

        // Start marker (green)
        LatLng startPoint = routePoints.get(0);
        mMap.addMarker(new MarkerOptions()
                .position(startPoint)
                .title("Trip Start")
                .snippet("Beginning of route")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // End marker (red)
        LatLng endPoint = routePoints.get(routePoints.size() - 1);
        mMap.addMarker(new MarkerOptions()
                .position(endPoint)
                .title("Trip End")
                .snippet("End of route")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    private void zoomToFitRoute() {
        if (routePoints.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng point : routePoints) {
            boundsBuilder.include(point);
        }

        try {
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 120; // pixels
            
            // Animate camera with delay for smooth transition
            handler.postDelayed(() -> {
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, padding),
                    1000, // duration ms
                    null
                );
            }, 300);
        } catch (Exception e) {
            // If bounds fail, zoom to first point
            LatLng startPoint = routePoints.get(0);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 15));
        }
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
