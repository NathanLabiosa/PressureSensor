package com.pressure_sensor;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PressureMeasurementDao {
    @Insert
    void insertMeasurement(PressureMeasurement measurement);

    // Query to get all measurements from the last 10 minutes
    @Query("SELECT * FROM pressure_measurements WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    List<PressureMeasurement> getMeasurementsSince(long startTime);

    @Query("SELECT * FROM pressure_measurements WHERE pressure > :threshold")
    List<PressureMeasurement> getMeasurementsAbove(double threshold);

    // New method to directly get the maximum pressure measured since a specific time
    @Query("SELECT MAX(pressure) FROM pressure_measurements WHERE timestamp >= :startTime")
    Double getMaxPressureSince(long startTime);

}
