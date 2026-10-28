package com.example.myapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.R;
import com.example.myapp.databinding.FragmentDashboardBinding;
import com.example.myapp.databinding.ItemSensorCardBinding;
import com.example.myapp.domain.model.SensorData;
import com.example.myapp.ui.viewmodel.DashboardViewModel;
import com.google.android.material.snackbar.Snackbar;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        setupCardStyles();
        observeViewModel();

        // Không cần gọi fetchData() nữa vì dữ liệu tự động cập nhật từ Room
        // viewModel.fetchData();

        // Xử lý sự kiện đăng xuất
        binding.btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.myapp.MainActivity) {
                ((com.example.myapp.MainActivity) getActivity()).performLogout();
            }
        });
    }

    private void setupCardStyles() {
        // Thiết lập Label, Icon và Màu sắc cao cấp (Premium)
        setupCard(binding.cardHumidity, "Độ ẩm đất", "%", R.drawable.ic_humidity, R.color.agri_secondary_blue, R.color.accent_blue_light);
        setupCard(binding.cardPh, "Độ pH", "pH", R.drawable.ic_ph, R.color.agri_tertiary_yellow, R.color.accent_yellow_light);
        setupCard(binding.cardWaterLevel, "Mực nước", "cm", R.drawable.ic_humidity, R.color.agri_secondary_blue, R.color.accent_blue_light);
        setupCard(binding.cardTemp, "Nhiệt độ", "°C", R.drawable.ic_temp, R.color.agri_error_red, R.color.accent_red_light);
        setupCard(binding.cardN, "Nitơ (N)", "mg/kg", R.drawable.ic_nutrient, R.color.agri_primary_green, R.color.accent_green_light);
        setupCard(binding.cardP, "Photpho (P)", "mg/kg", R.drawable.ic_nutrient, R.color.agri_primary_green, R.color.accent_green_light);
        setupCard(binding.cardK, "Kali (K)", "mg/kg", R.drawable.ic_nutrient, R.color.agri_primary_green, R.color.accent_green_light);
        setupCard(binding.cardEc, "Độ mặn EC", "mS/cm", R.drawable.ic_dot_indicator, R.color.agri_teal, R.color.accent_teal_light);
    }

    private void setupCard(ItemSensorCardBinding cardBinding, String label, String unit, int iconRes, int colorRes, int bgColorRes) {
        cardBinding.tvLabel.setText(label);
        cardBinding.tvUnit.setText(unit);
        cardBinding.ivSensorIcon.setImageResource(iconRes);
        
        // Hiệu ứng màu nhấn hiện đại — dùng drawable hình tròn bg_sensor_icon_circle
        cardBinding.viewIndicator.setBackgroundResource(R.drawable.bg_sensor_icon_circle);
        cardBinding.viewIndicator.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), bgColorRes));
        cardBinding.ivSensorIcon.setImageTintList(ContextCompat.getColorStateList(requireContext(), colorRes));
    }

    private void observeViewModel() {
        // Lắng nghe trạng thái loading
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.scrollView.setVisibility(View.GONE);
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.scrollView.setVisibility(View.VISIBLE);
            }
        });

        // Lắng nghe dữ liệu cảm biến
        viewModel.getSensorData().observe(getViewLifecycleOwner(), this::updateUI);

        // Lắng nghe lỗi (kết nối mạng, API...)
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.agri_tertiary_yellow))
                        .setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                        .show();
            }
        });
    }

    private void updateUI(SensorData data) {
        if (data == null) return;

        binding.cardHumidity.tvValue.setText(String.valueOf(data.getHumidity()));
        binding.cardPh.tvValue.setText(String.valueOf(data.getPhValue()));
        binding.cardWaterLevel.tvValue.setText(String.valueOf(data.getWaterLevel()));
        binding.cardTemp.tvValue.setText(String.valueOf(data.getTemperature()));
        binding.cardN.tvValue.setText(String.valueOf(data.getNitrogen()));
        binding.cardP.tvValue.setText(String.valueOf(data.getPhosphorus()));
        binding.cardK.tvValue.setText(String.valueOf(data.getPotassium()));
        binding.cardEc.tvValue.setText(String.valueOf(data.getEcValue()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}