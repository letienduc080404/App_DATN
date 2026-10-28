package com.example.myapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.myapp.data.local.AppDatabase;
import com.example.myapp.data.local.SessionManager;
import com.example.myapp.data.remote.ApiClient;
import com.example.myapp.databinding.ActivityMainBinding;
import com.example.myapp.ui.auth.LoginActivity;
import com.example.myapp.ui.fragments.AlertFragment;
import com.example.myapp.ui.fragments.ChatAILocalFragment;
import com.example.myapp.ui.fragments.DashboardFragment;
import com.example.myapp.ui.fragments.DeviceMapFragment;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.example.myapp.data.worker.SyncWorker;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kiểm tra phiên đăng nhập trước khi hiển thị UI
        sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        // Khởi tạo ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Hiển thị Fragment mặc định (Dashboard) khi lần đầu chạy app
        if (savedInstanceState == null) {
            replaceFragment(new DashboardFragment());
        }

        // Lên lịch đồng bộ ngầm định kỳ (15 phút/lần)
        setupBackgroundSync();

        // Thiết lập sự kiện chọn item trên BottomNavigationView
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                replaceFragment(new DashboardFragment());
                return true;
            } else if (itemId == R.id.nav_devices) {
                replaceFragment(new DeviceMapFragment());
                return true;
            } else if (itemId == R.id.nav_ai_chat) {
                replaceFragment(new ChatAILocalFragment());
                return true;
            } else if (itemId == R.id.nav_alerts) {
                replaceFragment(new AlertFragment());
                return true;
            }
            return false;
        });

        // Observe sự kiện Token hết hạn (401) từ ApiClient
        ApiClient.getAuthErrorEvent().observe(this, isAuthError -> {
            if (isAuthError != null && isAuthError) {
                performLogout();
            }
        });
    }

    /**
     * Hàm dùng để thay thế Fragment hiện tại trong container
     */
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    /**
     * Lên lịch đồng bộ dữ liệu ngầm bằng WorkManager.
     * Chỉ chạy khi có mạng, chu kỳ 15 phút.
     */
    private void setupBackgroundSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        // Sử dụng KEEP để không ghi đè nếu work đã được đặt lịch
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "AgriSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }

    /**
     * Đăng xuất: Xóa session, xóa cache Room, chuyển về LoginActivity.
     * Gọi được từ Fragment thông qua getActivity().
     */
    public void performLogout() {
        // 1. Xóa token / session
        sessionManager.clearSession();

        // 2. Xóa toàn bộ cache Room Database trên background thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase.getDatabase(getApplicationContext()).clearAllTables();
        });

        // 3. Hủy WorkManager sync
        WorkManager.getInstance(this).cancelUniqueWork("AgriSyncWork");

        // 4. Chuyển về LoginActivity
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}