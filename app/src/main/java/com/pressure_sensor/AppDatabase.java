package com.pressure_sensor;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {PressureMeasurement.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PressureMeasurementDao pressureMeasurementDao();

    // Singleton instance to prevent having multiple instances of the database opened at the same time.
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "pressure_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
