package com.pressure_sensor;



import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.pressure_sensor.SensorDataDao;
import com.pressure_sensor.SensorData;
import androidx.room.TypeConverters;

@Database(entities = {SensorData.class}, version = 1)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    // Define your DAOs here
    public abstract SensorDataDao sensorDataDao();
}


