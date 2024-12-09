package com.pressure_sensor;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;
import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;



@Entity
public class SensorData {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String value;
    public Date time;
    public String color;
}
