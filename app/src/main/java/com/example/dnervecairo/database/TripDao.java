package com.example.dnervecairo.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTrip(TripEntity trip);

    @Query("SELECT * FROM pending_trips WHERE isSynced = 0 ORDER BY createdAt ASC")
    List<TripEntity> getPendingTrips();

    @Query("SELECT COUNT(*) FROM pending_trips WHERE isSynced = 0")
    int getPendingTripsCount();

    @Query("UPDATE pending_trips SET isSynced = 1 WHERE localTripId = :tripId")
    void markAsSynced(String tripId);

    @Query("DELETE FROM pending_trips WHERE isSynced = 1")
    void deleteSyncedTrips();

    @Query("DELETE FROM pending_trips WHERE localTripId = :tripId")
    void deleteTrip(String tripId);
}