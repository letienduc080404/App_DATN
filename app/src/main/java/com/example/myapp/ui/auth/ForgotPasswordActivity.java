package com.example.myapp.ui.auth;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.databinding.ActivityForgotPasswordBinding;
import com.example.myapp.ui.viewmodel.AuthViewModel;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        // Nút quay lại
        binding.btnBack.setOnClickListener(v -> finish());

        // Nút Gửi mã xác nhận
        binding.btnSendCode.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                binding.tilEmail.setError("Vui lòng nhập email");
                return;
            }
            binding.tilEmail.setError(null);

            viewModel.forgotPassword(email);
        });

        // Quay về đăng nhập
        binding.tvGoLogin.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        // Loading state
        viewModel.isLoading().observe(this, isLoading -> {
            binding.btnSendCode.setEnabled(!isLoading);
            binding.btnSendCode.setText(isLoading ? "Đang gửi..." : "GỬI MÃ XÁC NHẬN");
        });

        // Gửi thành công
        viewModel.getForgotPasswordResult().observe(this, result -> {
            if ("OK".equals(result)) {
                Toast.makeText(this,
                        "Mã xác nhận đã được gửi đến email của bạn!",
                        Toast.LENGTH_LONG).show();
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
