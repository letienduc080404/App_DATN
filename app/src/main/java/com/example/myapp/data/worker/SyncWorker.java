package com.example.myapp.data.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapp.data.repository.AgriRepositoryImpl;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    private final AgriRepositoryImpl repository;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        repository = AgriRepositoryImpl.getInstance(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Bắt đầu tiến trình đồng bộ ngầm...");

        try {
            boolean success = repository.syncDataSync();
            if (success) {
                Log.d(TAG, "Đồng bộ ngầm thành công!");
                return Result.success();
            } else {
                // syncDataSync() trả false khi API lỗi hoặc timeout — đã được log bên trong
                Log.w(TAG, "syncDataSync() trả về false (lỗi mạng hoặc API), sẽ thử lại sau.");
                return Result.retry();
            }
        } catch (Exception e) {
            // syncDataSync() bắt IOException nội bộ và trả boolean,
            // nhánh này chỉ xử lý RuntimeException không lường trước.
            Log.e(TAG, "Lỗi không xác định khi đồng bộ ngầm", e);
            return Result.retry();
        }
    }
}
