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

        // Chuyển về Đăng nhập
        binding.tvGoLogin.setOnClickListener(v -> finish());
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
