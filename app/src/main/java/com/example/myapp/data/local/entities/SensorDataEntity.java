package com.example.myapp.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sensor_data")
public class SensorDataEntity {
    @PrimaryKey
    public long timestamp;
    
    public double temperature;
    public double humidity;
    public double phValue;
    public double nitrogen;
    public double phosphorus;
    public double potassium;
    public double waterLevel;
    public double ecValue;

    public SensorDataEntity(long timestamp, double temperature, double humidity, double phValue, 
                            double nitrogen, double phosphorus, double potassium, 
                            double waterLevel, double ecValue) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.humidity = humidity;
        this.phValue = phValue;
        this.nitrogen = nitrogen;
        this.phosphorus = phosphorus;
        this.potassium = potassium;
        this.waterLevel = waterLevel;
        this.ecValue = ecValue;
    }
}