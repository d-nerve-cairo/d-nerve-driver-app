package com.example.dnervecairo.activities;

import android.graphics.Color;
import android.os.Bundle;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.appbar.MaterialToolbar;

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

    private GoogleMap mMap;
    private String gpsPointsJson;
    private List<LatLng> routePoints = new ArrayList<>();

    private TextView tvTripId, tvDuration, tvDistance, tvPoints, tvQuality;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_map);

        initViews();
        setupToolbar();
        loadTripData();

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void initViews() {
        tvTripId = findViewById(R.id.tv_trip_id);
        tvDuration = findViewById(R.id.tv_duration);
        tvDistance = findViewById(R.id.tv_distance);
        tvPoints = findViewById(R.id.tv_points);
        tvQuality = findViewById(R.id.tv_quality);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadTripData() {
        String tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        gpsPointsJson = getIntent().getStringExtra(EXTRA_GPS_POINTS);
        double duration = getIntent().getDoubleExtra(EXTRA_DURATION, 0);
        double distance = getIntent().getDoubleExtra(EXTRA_DISTANCE, 0);
        int pointsEarned = getIntent().getIntExtra(EXTRA_POINTS_EARNED, 0);
        double quality = getIntent().getDoubleExtra(EXTRA_QUALITY, 0);
        String date = getIntent().getStringExtra(EXTRA_DATE);

        // Display trip info
        tvTripId.setText(date != null ? date : tripId);
        tvDuration.setText(String.format(Locale.US, "%.0f min", duration));
        tvDistance.setText(String.format(Locale.US, "%.2f km", distance));
        tvPoints.setText(String.format(Locale.US, "+%d pts", pointsEarned));
        tvQuality.setText(String.format(Locale.US, "%.0f%%", quality * 100));

        // Parse GPS points
        parseGpsPoints();
    }

    private void parseGpsPoints() {
        if (gpsPointsJson == null || gpsPointsJson.isEmpty()) {
            Toast.makeText(this, "No GPS data available", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Error parsing GPS data", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (routePoints.isEmpty()) {
            // Default to Cairo if no points
            LatLng cairo = new LatLng(30.0444, 31.2357);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cairo, 12));
            return;
        }

        // Draw route polyline
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(routePoints)
                .width(8)
                .color(Color.parseColor("#1E88E5"))
                .geodesic(true);
        mMap.addPolyline(polylineOptions);

        // Add start marker (green)
        LatLng startPoint = routePoints.get(0);
        mMap.addMarker(new MarkerOptions()
                .position(startPoint)
                .title("Start")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Add end marker (red)
        LatLng endPoint = routePoints.get(routePoints.size() - 1);
        mMap.addMarker(new MarkerOptions()
                .position(endPoint)
                .title("End")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Zoom to fit route
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng point : routePoints) {
            boundsBuilder.include(point);
        }

        try {
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 100; // pixels
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } catch (Exception e) {
            // If bounds fail, zoom to first point
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 15));
        }
    }
}