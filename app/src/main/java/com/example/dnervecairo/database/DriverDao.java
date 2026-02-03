package com.example.dnervecairo.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface DriverDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDriver(CachedDriverEntity driver);

    @Query("SELECT * FROM cached_driver WHERE driverId = :driverId LIMIT 1")
    CachedDriverEntity getDriver(String driverId);

    @Query("DELETE FROM cached_driver")
    void clearCache();
}