package com.example.dnervecairo.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Helper for fetching road-following route polylines using the OSRM routing engine.
 * Uses OpenStreetMap data — completely free, no API key required.
 *
 * Replaces the Google Directions API calls in TripActivity.
 * API: https://router.project-osrm.org/route/v1/driving/{lon1},{lat1};{lon2},{lat2}?overview=full&geometries=polyline
 */
public class OsrmRouteHelper {

    // Utility class — prevent instantiation
    private OsrmRouteHelper() {}

    private static final String TAG = "OsrmRouteHelper";
    private static final String OSRM_BASE = "https://router.project-osrm.org/route/v1/driving/";

    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Callback for receiving the decoded road-following polyline.
     */
    public interface RouteCallback {
        /** Called on the main thread with the decoded list of road-following points. */
        void onRouteReady(List<LatLng> points);
        /** Called on the main thread if the request fails — fall back to straight lines. */
        void onRouteFailed();
    }

    /**
     * Fetches a road-following route between two points from OSRM.
     * Calls the callback on the main thread.
     *
     * @param origin      start coordinate
     * @param destination end coordinate
     * @param callback    receives the decoded polyline (or failure notification)
     */
    public static void fetchRoute(LatLng origin, LatLng destination, RouteCallback callback) {
        // OSRM expects lon,lat order (not lat,lon!)
        String url = OSRM_BASE
                + origin.longitude + "," + origin.latitude
                + ";"
                + destination.longitude + "," + destination.latitude
                + "?overview=full&geometries=polyline";

        executor.execute(() -> {
            try {
                Request request = new Request.Builder().url(url).build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        Log.w(TAG, "OSRM request failed: " + response.code());
                        mainHandler.post(callback::onRouteFailed);
                        return;
                    }

                    String json = response.body().string();
                    JSONObject root = new JSONObject(json);
                    String code = root.optString("code", "");
                    if (!"Ok".equals(code)) {
                        Log.w(TAG, "OSRM returned code: " + code);
                        mainHandler.post(callback::onRouteFailed);
                        return;
                    }

                    JSONArray routes = root.getJSONArray("routes");
                    if (routes.length() == 0) {
                        mainHandler.post(callback::onRouteFailed);
                        return;
                    }

                    String encoded = routes.getJSONObject(0).getString("geometry");
                    List<LatLng> points = decodePolyline(encoded);
                    Log.d(TAG, "OSRM route decoded: " + points.size() + " points");
                    mainHandler.post(() -> callback.onRouteReady(points));
                }
            } catch (IOException | org.json.JSONException e) {
                Log.e(TAG, "OSRM error: " + e.getMessage());
                mainHandler.post(callback::onRouteFailed);
            }
        });
    }

    /**
     * Decodes a Google-format encoded polyline string into a list of LatLng points.
     * OSRM uses the same encoding format as Google Maps (precision 5).
     */
    public static List<LatLng> decodePolyline(String encoded) {
        List<LatLng> points = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dLat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dLng;

            points.add(new LatLng(lat / 1e5, lng / 1e5));
        }
        return points;
    }
}
