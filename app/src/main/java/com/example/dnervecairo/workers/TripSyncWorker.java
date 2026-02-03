package com.example.dnervecairo.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.requests.GpsPointRequest;
import com.example.dnervecairo.api.requests.TripSubmission;
import com.example.dnervecairo.database.AppDatabase;
import com.example.dnervecairo.database.TripEntity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import retrofit2.Response;

public class TripSyncWorker extends Worker {

    private static final String TAG = "TripSyncWorker";

    public TripSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting trip sync...");

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        List<TripEntity> pendingTrips = db.tripDao().getPendingTrips();

        if (pendingTrips.isEmpty()) {
            Log.d(TAG, "No pending trips to sync");
            return Result.success();
        }

        Log.d(TAG, "Found " + pendingTrips.size() + " pending trips");

        Gson gson = new Gson();
        int successCount = 0;

        for (TripEntity trip : pendingTrips) {
            try {
                // Convert GPS JSON string to list
                List<GpsPointRequest> gpsPoints = gson.fromJson(
                        trip.getGpsPointsJson(),
                        new TypeToken<List<GpsPointRequest>>(){}.getType()
                );

                TripSubmission submission = new TripSubmission(
                        trip.getDriverId(),
                        trip.getStartTime(),
                        trip.getEndTime(),
                        gpsPoints
                );

                Response<?> response = ApiClient.getInstance()
                        .getApiService()
                        .submitTrip(submission)
                        .execute();

                if (response.isSuccessful()) {
                    db.tripDao().markAsSynced(trip.getLocalTripId());
                    successCount++;
                    Log.d(TAG, "Synced trip: " + trip.getLocalTripId());
                } else {
                    Log.e(TAG, "Failed to sync trip: " + response.code());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error syncing trip: " + e.getMessage());
            }
        }

        // Clean up synced trips
        db.tripDao().deleteSyncedTrips();

        Log.d(TAG, "Sync complete. Synced " + successCount + " of " + pendingTrips.size() + " trips");

        return successCount > 0 ? Result.success() : Result.retry();
    }
}