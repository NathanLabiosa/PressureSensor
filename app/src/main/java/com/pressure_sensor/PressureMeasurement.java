package com.pressure_sensor;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pressure_measurements")
public class PressureMeasurement {
    @PrimaryKey(autoGenerate = true)
    public int id;

    // Use System.currentTimeMillis() for the timestamp
    public long timestamp;

    public double pressure;
}
