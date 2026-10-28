package com.example.myapp.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.myapp.domain.model.AlertMessage;
import com.example.myapp.domain.model.DeviceStatus;
import com.example.myapp.domain.model.SensorData;
import java.util.List;

/**
 * Interface định nghĩa các phương thức giao tiếp với dữ liệu.
 * Đã cập nhật hỗ trợ LiveData cho Single Source of Truth.
 */
public interface AgriRepository {
    // Luồng dữ liệu tức thời từ Database
    LiveData<SensorData> getLatestSensorData();
    LiveData<List<AlertMessage>> getAllAlerts();
    LiveData<List<DeviceStatus>> getAllDevices();
    LiveData<String> getErrorEvent();
    
    // Các thao tác khác dùng callback
    void getDashboardData(DataCallback<SensorData> callback);
    void getAlerts(DataCallback<List<AlertMessage>> callback);
    void getDevices(DataCallback<List<DeviceStatus>> callback);
    void injectMockAdminData();
}