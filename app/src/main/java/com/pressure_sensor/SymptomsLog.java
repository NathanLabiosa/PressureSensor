package com.pressure_sensor;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "symptoms_logs")
public class SymptomsLog {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public long timestamp;     // e.g., System.currentTimeMillis()
    public int painLevel;      // 1..5
    public String otherSymptoms; // The user’s typed text, if any
}
