package com.example.myapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.databinding.ActivityRegisterBinding;
import com.example.myapp.ui.viewmodel.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        // Nút quay lại
        binding.btnBack.setOnClickListener(v -> finish());

        // Nút Đăng ký
        binding.btnRegister.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
            String deviceName = binding.etDeviceName.getText().toString().trim();

            // Validate
            if (email.isEmpty()) {
                binding.tilEmail.setError("Vui lòng nhập email");
                return;
            }
            binding.tilEmail.setError(null);

            if (password.isEmpty()) {
                binding.tilPassword.setError("Vui lòng nhập mật khẩu");
                return;
            }
            binding.tilPassword.setError(null);

            if (!password.equals(confirmPassword)) {
                binding.tilConfirmPassword.setError("Mật khẩu không khớp");
                return;
            }
            binding.tilConfirmPassword.setError(null);

            if (deviceName.isEmpty()) {
                binding.tilDeviceName.setError("Vui lòng nhập tên trạm đo");
                return;
            }
            binding.tilDeviceName.setError(null);

            viewModel.register(email, password, deviceName);
        });

        // Cấu hình máy chủ
        binding.btnServerConfig.setOnClickListener(v -> showServerConfigDialog());

        // Chuyển về Đăng nhập
        binding.tvGoLogin.setOnClickListener(v -> finish());
    }

    private void showServerConfigDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(viewModel.getServerUrl());
        input.setSingleLine(true);
        input.setPadding(40, 30, 40, 30);

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(48, 16, 48, 16);

        android.widget.TextView tvHint = new android.widget.TextView(this);
        tvHint.setText("Chọn nhanh hoặc nhập IP/URL máy chủ ThingsBoard (port 9090):");
        tvHint.setPadding(0, 0, 0, 16);
        container.addView(tvHint);
        container.addView(input);

        // Nút chọn nhanh
        android.widget.Button btnUsb = new android.widget.Button(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnUsb.setText("Dùng Cáp USB (127.0.0.1:9090)");
        btnUsb.setOnClickListener(b -> input.setText("http://127.0.0.1:9090/"));
        container.addView(btnUsb);

        android.widget.Button btnLan = new android.widget.Button(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnLan.setText("Dùng Máy Thật Wi-Fi (10.251.50.94:9090)");
        btnLan.setOnClickListener(b -> input.setText("http://10.251.50.94:9090/"));
        container.addView(btnLan);

        android.widget.Button btnEmulator = new android.widget.Button(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnEmulator.setText("Dùng Máy Ảo Emulator (10.0.2.2:9090)");
        btnEmulator.setOnClickListener(b -> input.setText("http://10.0.2.2:9090/"));
        container.addView(btnEmulator);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cấu hình Máy Chủ ThingsBoard")
                .setView(container)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        viewModel.setServerUrl(url);
                        Toast.makeText(this, "Đã lưu Server URL: " + viewModel.getServerUrl(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void observeViewModel() {
        // Loading state
        viewModel.isLoading().observe(this, isLoading -> {
            binding.btnRegister.setEnabled(!isLoading);
            binding.btnRegister.setText(isLoading ? "Đang xử lý..." : "TẠO TÀI KHOẢN");
        });

        // Đăng ký thành công
        viewModel.getRegisterResult().observe(this, result -> {
            if ("OK".equals(result)) {
                Toast.makeText(this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show();
                // Quay về LoginActivity
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        // Lỗi
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
