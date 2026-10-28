package com.example.myapp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapp.data.local.entities.AlertEntity;
import com.example.myapp.data.local.entities.DeviceEntity;
import com.example.myapp.data.local.entities.SensorDataEntity;

import java.util.List;

@Dao
public interface AgriDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSensorData(SensorDataEntity data);

    @Query("SELECT * FROM sensor_data ORDER BY timestamp DESC LIMIT 1")
    LiveData<SensorDataEntity> getLatestSensorData();

    @Query("SELECT * FROM sensor_data ORDER BY timestamp DESC")
    LiveData<List<SensorDataEntity>> getAllSensorDataHistory();

    @Query("DELETE FROM sensor_data")
    void deleteAllSensorData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAlert(AlertEntity alert);

    @Query("SELECT * FROM alerts ORDER BY id DESC")
    LiveData<List<AlertEntity>> getAllAlerts();

    @Query("DELETE FROM alerts")
    void deleteAllAlerts();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDevice(DeviceEntity device);

    @Query("SELECT * FROM devices")
    LiveData<List<DeviceEntity>> getAllDevices();

    @Query("DELETE FROM devices")
    void deleteAllDevices();
}