package com.example.myapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.MainActivity;
import com.example.myapp.databinding.ActivityLoginBinding;
import com.example.myapp.ui.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        // Nút Đăng nhập
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            // Validate đầu vào
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

            viewModel.login(email, password);
        });

        // Cấu hình Server URL
        binding.btnServerConfig.setOnClickListener(v -> showServerConfigDialog());

        // Chuyển sang Quên mật khẩu
        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        // Chuyển sang Đăng ký
        binding.tvGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
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
            binding.btnLogin.setEnabled(!isLoading);
            binding.btnLogin.setText(isLoading ? "Đang xử lý..." : "ĐĂNG NHẬP");
        });

        // Đăng nhập thành công
        viewModel.getLoginResult().observe(this, result -> {
            if ("OK".equals(result)) {
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
