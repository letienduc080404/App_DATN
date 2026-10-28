package com.example.myapp.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.myapp.data.repository.AgriRepositoryImpl;
import com.example.myapp.domain.model.SensorData;
import com.example.myapp.domain.repository.AgriRepository;

public class DashboardViewModel extends AndroidViewModel {

    private final AgriRepository repository;
    private final LiveData<SensorData> sensorData;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);


    public DashboardViewModel(@NonNull Application application) {
        super(application);
        // Dùng Singleton Repository
        this.repository = AgriRepositoryImpl.getInstance(application);
        // Observe trực tiếp từ Repository (Single Source of Truth)
        this.sensorData = repository.getLatestSensorData();
    }

    public LiveData<SensorData> getSensorData() {
        return sensorData;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return repository.getErrorEvent();
    }

    // Không cần hàm fetchData() chủ động vì Room sẽ tự đẩy dữ liệu khi MQTT cập nhật
}