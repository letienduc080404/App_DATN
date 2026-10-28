package com.example.myapp.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.myapp.data.repository.AgriRepositoryImpl;
import com.example.myapp.domain.model.DeviceStatus;
import com.example.myapp.domain.repository.AgriRepository;

import java.util.List;

public class DeviceMapViewModel extends AndroidViewModel {

    private final AgriRepository repository;
    private final LiveData<List<DeviceStatus>> devices;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public DeviceMapViewModel(@NonNull Application application) {
        super(application);
        this.repository = AgriRepositoryImpl.getInstance(application);
        // Observe trực tiếp từ Repository (Single Source of Truth)
        this.devices = repository.getAllDevices();
    }

    public LiveData<List<DeviceStatus>> getDevices() {
        return devices;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    // Không cần fetchDevices() chủ động vì Room sẽ tự đẩy dữ liệu
}