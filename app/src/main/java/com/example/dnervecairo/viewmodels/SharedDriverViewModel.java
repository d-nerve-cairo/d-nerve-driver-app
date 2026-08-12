package com.example.dnervecairo.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.dnervecairo.api.ApiClient;
import com.example.dnervecairo.api.responses.DriverResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shared ViewModel scoped to MainActivity.
 * Provides a single cached DriverResponse to Home, Profile, and Rewards fragments.
 * Eliminates redundant getDriver() API calls across tabs.
 */
public class SharedDriverViewModel extends AndroidViewModel {

    private static final String TAG = "SharedDriverViewModel";
    private static final long STALENESS_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutes

    private final MutableLiveData<DriverResponse> driverData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private long lastFetchTimestamp = 0;
    private String currentDriverId = null;
    private boolean isFetching = false;

    public SharedDriverViewModel(@NonNull Application application) {
        super(application);
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Returns LiveData for observing driver data changes.
     * All fragments should observe this.
     */
    public LiveData<DriverResponse> getDriverData() {
        return driverData;
    }

    /**
     * Returns LiveData for loading state.
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Returns LiveData for error messages.
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Load driver data with smart caching.
     * Only fetches from network if:
     * - Data has never been loaded
     * - Data is older than 5 minutes
     * - Driver ID changed
     *
     * @param driverId The driver's unique ID
     */
    public void loadDriverData(String driverId) {
        if (driverId == null || driverId.isEmpty()) {
            Log.w(TAG, "loadDriverData called with null/empty driverId");
            return;
        }

        // If data is fresh and for the same driver, skip the fetch
        if (driverData.getValue() != null
                && driverId.equals(currentDriverId)
                && !isStale()) {
            Log.d(TAG, "Data is fresh (age: " + getDataAgeSeconds() + "s), skipping fetch");
            return;
        }

        fetchFromNetwork(driverId);
    }

    /**
     * Force a fresh fetch from network, ignoring cache.
     * Use for: SwipeRefresh, returning from EditProfile, after trip completion.
     *
     * @param driverId The driver's unique ID
     */
    public void forceRefresh(String driverId) {
        if (driverId == null || driverId.isEmpty()) return;
        Log.d(TAG, "Force refresh requested");
        lastFetchTimestamp = 0; // Invalidate cache
        fetchFromNetwork(driverId);
    }

    /**
     * Invalidate cached data without fetching.
     * Next call to loadDriverData() will trigger a network fetch.
     */
    public void invalidateCache() {
        lastFetchTimestamp = 0;
        Log.d(TAG, "Cache invalidated");
    }

    // =========================================================================
    // PRIVATE
    // =========================================================================

    private boolean isStale() {
        return (System.currentTimeMillis() - lastFetchTimestamp) > STALENESS_THRESHOLD_MS;
    }

    private long getDataAgeSeconds() {
        return (System.currentTimeMillis() - lastFetchTimestamp) / 1000;
    }

    private void fetchFromNetwork(String driverId) {
        // Prevent duplicate in-flight requests
        if (isFetching) {
            Log.d(TAG, "Already fetching, skipping duplicate request");
            return;
        }

        isFetching = true;
        currentDriverId = driverId;
        isLoading.setValue(true);

        Log.d(TAG, "Fetching driver data from network for: " + driverId);

        ApiClient.getInstance().getApiService()
                .getDriver(driverId)
                .enqueue(new Callback<DriverResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DriverResponse> call,
                                           @NonNull Response<DriverResponse> response) {
                        isFetching = false;
                        isLoading.setValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            lastFetchTimestamp = System.currentTimeMillis();
                            driverData.setValue(response.body());
                            Log.d(TAG, "Driver data fetched successfully");
                        } else {
                            errorMessage.setValue("Failed to load driver data");
                            Log.e(TAG, "API error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DriverResponse> call, @NonNull Throwable t) {
                        isFetching = false;
                        isLoading.setValue(false);
                        errorMessage.setValue("Network error");
                        Log.e(TAG, "Network error: " + t.getMessage());
                    }
                });
    }
}
