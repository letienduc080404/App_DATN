package com.example.myapp.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "devices")
public class DeviceEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public int battery;
    public boolean isActive;
    public double latitude;
    public double longitude;

    public DeviceEntity(String name, int battery, boolean isActive, double latitude, double longitude) {
        this.name = name;
        this.battery = battery;
        this.isActive = isActive;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}