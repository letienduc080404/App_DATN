package com.example.myapp.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.myapp.data.repository.AgriRepositoryImpl;
import com.example.myapp.domain.model.AlertMessage;
import com.example.myapp.domain.repository.AgriRepository;
import java.util.List;

public class AlertViewModel extends AndroidViewModel {

    private final LiveData<List<AlertMessage>> alerts;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public AlertViewModel(@NonNull Application application) {
        super(application);
        AgriRepository repository = AgriRepositoryImpl.getInstance(application);
        this.alerts = repository.getAllAlerts();
    }

    public LiveData<List<AlertMessage>> getAlerts() {
        return alerts;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
}