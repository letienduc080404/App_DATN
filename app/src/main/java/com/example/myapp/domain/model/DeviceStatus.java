package com.example.myapp.domain.model;

/**
 * Domain model chứa trạng thái trạm thiết bị, bao gồm tọa độ GPS cho bản đồ.
 */
public class DeviceStatus {
    private String name;
    private int battery;
    private boolean isActive;
    private double latitude;
    private double longitude;

    public DeviceStatus(String name, int battery, boolean isActive, double latitude, double longitude) {
        this.name = name;
        this.battery = battery;
        this.isActive = isActive;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters
    public String getName() { return name; }
    public int getBattery() { return battery; }
    public boolean isActive() { return isActive; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}