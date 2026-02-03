package com.example.dnervecairo.utils;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.dnervecairo.api.requests.TripSubmission;
import com.example.dnervecairo.api.responses.DriverResponse;
import com.example.dnervecairo.database.AppDatabase;
import com.example.dnervecairo.database.CachedDriverEntity;
import com.example.dnervecairo.database.TripEntity;
import com.example.dnervecairo.workers.TripSyncWorker;
import com.google.gson.Gson;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OfflineManager {

    private static final String TAG = "OfflineManager";
    private static final String SYNC_WORK_NAME = "trip_sync_work";

    private final Context context;
    private final AppDatabase database;
    private final ExecutorService executor;
    private final Gson gson;

    public OfflineManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.gson = new Gson();
    }

    /**
     * Save trip locally for later sync
     */
    public void saveTripOffline(TripSubmission submission) {
        executor.execute(() -> {
            String localTripId = "local_" + UUID.randomUUID().toString();

            TripEntity entity = new TripEntity(
                    localTripId,
                    submission.getDriverId(),
                    submission.getRouteId(),
                    submission.getStartTime(),
                    submission.getEndTime(),
                    gson.toJson(submission.getGpsPoints()),
                    submission.getGpsPoints().size(),
                    false,
                    System.currentTimeMillis()
            );

            database.tripDao().insertTrip(entity);
            Log.d(TAG, "Trip saved offline: " + localTripId);

            // Schedule sync when network is available
            scheduleTripSync();
        });
    }

    /**
     * Cache driver data for offline access
     */
    public void cacheDriverData(DriverResponse driver) {
        executor.execute(() -> {
            CachedDriverEntity entity = new CachedDriverEntity(
                    driver.getDriverId(),
                    driver.getName(),
                    driver.getPhone(),
                    driver.getVehicleType(),
                    driver.getLicensePlate(),
                    driver.getTotalPoints(),
                    driver.getTier(),
                    driver.getTripsCompleted(),
                    driver.getQualityAvg(),
                    driver.getCurrentStreak(),
                    System.currentTimeMillis()
            );

            database.driverDao().insertDriver(entity);
            Log.d(TAG, "Driver data cached: " + driver.getDriverId());
        });
    }

    /**
     * Get cached driver data
     */
    public void getCachedDriver(String driverId, CacheCallback<CachedDriverEntity> callback) {
        executor.execute(() -> {
            CachedDriverEntity driver = database.driverDao().getDriver(driverId);
            callback.onResult(driver);
        });
    }

    /**
     * Get count of pending trips
     */
    public void getPendingTripsCount(CacheCallback<Integer> callback) {
        executor.execute(() -> {
            int count = database.tripDao().getPendingTripsCount();
            callback.onResult(count);
        });
    }

    /**
     * Schedule background sync when network is available
     */
    public void scheduleTripSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(TripSyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.REPLACE, syncRequest);

        Log.d(TAG, "Trip sync scheduled");
    }

    /**
     * Trigger immediate sync
     */
    public void syncNow() {
        if (NetworkUtils.isNetworkAvailable(context)) {
            OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(TripSyncWorker.class)
                    .build();

            WorkManager.getInstance(context).enqueue(syncRequest);
            Log.d(TAG, "Immediate sync triggered");
        }
    }

    public interface CacheCallback<T> {
        void onResult(T result);
    }
}