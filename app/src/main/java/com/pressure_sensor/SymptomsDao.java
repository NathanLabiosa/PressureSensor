package com.pressure_sensor;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface SymptomsDao {
    @Insert
    long insertSymptomsLog(SymptomsLog log);

    @Query("SELECT * FROM symptoms_logs WHERE id = :id")
    SymptomsLog getLogById(long id);

}

