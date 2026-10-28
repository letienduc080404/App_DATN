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
    private final android.os.Handler refreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable refreshRunnable;
    private boolean isAutoRefreshing = false;

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

    /**
     * Đồng bộ dữ liệu cảm biến mới nhất từ ThingsBoard Server
     */
    public void refreshData() {
        isLoading.postValue(true);
        repository.syncDataFromThingsBoard();
        // Tắt loading sau 1s
        refreshHandler.postDelayed(() -> isLoading.postValue(false), 1000);
    }

    /**
     * Bắt đầu tự động làm mới dữ liệu theo chu kỳ (mặc định mỗi 5 giây)
     */
    public void startAutoRefresh(long intervalMillis) {
        if (isAutoRefreshing) return;
        isAutoRefreshing = true;

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAutoRefreshing) {
                    repository.syncDataFromThingsBoard();
                    refreshHandler.postDelayed(this, intervalMillis);
                }
            }
        };

        // Chạy ngay lần đầu và lên lịch các lần tiếp theo
        refreshRunnable.run();
    }

    public void stopAutoRefresh() {
        isAutoRefreshing = false;
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopAutoRefresh();
    }
}