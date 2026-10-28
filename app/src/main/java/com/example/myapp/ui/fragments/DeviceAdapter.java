package com.example.myapp.ui.fragments;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.example.myapp.R;
import com.example.myapp.databinding.ItemDeviceBinding;
import com.example.myapp.domain.model.DeviceStatus;

/**
 * PERF FIX: Chuyển sang ListAdapter + DiffUtil.ItemCallback.
 * - Khi Room phát ra dữ liệu mới từ LiveData, chỉ các item có trạng thái
 *   thay đổi (pin, isActive) mới được bind lại — tránh vẽ lại toàn danh sách.
 */
public class DeviceAdapter extends ListAdapter<DeviceStatus, DeviceAdapter.DeviceViewHolder> {

    private static final DiffUtil.ItemCallback<DeviceStatus> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DeviceStatus>() {
                @Override
                public boolean areItemsTheSame(@NonNull DeviceStatus oldItem,
                                               @NonNull DeviceStatus newItem) {
                    // Tên trạm là định danh duy nhất
                    return oldItem.getName().equals(newItem.getName());
                }

                @Override
                public boolean areContentsTheSame(@NonNull DeviceStatus oldItem,
                                                  @NonNull DeviceStatus newItem) {
                    // Chỉ vẽ lại khi pin hoặc trạng thái kết nối thay đổi
                    return oldItem.getBattery() == newItem.getBattery()
                            && oldItem.isActive() == newItem.isActive()
                            && oldItem.getLatitude() == newItem.getLatitude()
                            && oldItem.getLongitude() == newItem.getLongitude();
                }
            };

    public DeviceAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDeviceBinding binding = ItemDeviceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DeviceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class DeviceViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        private final ItemDeviceBinding binding;

        public DeviceViewHolder(ItemDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(DeviceStatus device) {
            binding.tvDeviceName.setText(device.getName());

            int batteryPct = device.getBattery();
            binding.tvBatteryPercent.setText(batteryPct + "%");

            // ── Màu icon pin theo mức pin ──
            int batteryIconColor;
            int batteryBgColor;
            if (batteryPct >= 50) {
                batteryIconColor = ContextCompat.getColor(itemView.getContext(), R.color.agri_primary_green);
                batteryBgColor   = ContextCompat.getColor(itemView.getContext(), R.color.accent_green_light);
            } else if (batteryPct >= 20) {
                batteryIconColor = ContextCompat.getColor(itemView.getContext(), R.color.agri_tertiary_yellow);
                batteryBgColor   = ContextCompat.getColor(itemView.getContext(), R.color.accent_yellow_light);
            } else {
                batteryIconColor = ContextCompat.getColor(itemView.getContext(), R.color.agri_error_red);
                batteryBgColor   = ContextCompat.getColor(itemView.getContext(), R.color.accent_red_light);
            }
            binding.ivBattery.setImageTintList(
                    android.content.res.ColorStateList.valueOf(batteryIconColor));
            binding.tvBatteryPercent.setTextColor(batteryIconColor);

            // ── Chip trạng thái kết nối ──
            if (device.isActive()) {
                int activeGreen = ContextCompat.getColor(itemView.getContext(), R.color.agri_primary_green);
                int activeBg    = ContextCompat.getColor(itemView.getContext(), R.color.accent_green_light);

                binding.tvStatusText.setText("Đang hoạt động");
                binding.tvStatusText.setTextColor(activeGreen);
                binding.chipStatusWrapper.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(activeBg));
                binding.viewChipDot.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(activeGreen));
                binding.viewStatusDot.setBackgroundResource(R.drawable.ic_dot_indicator);
                binding.viewStatusDot.getBackground().setTint(activeGreen);
                binding.ivDeviceIcon.setImageTintList(
                        android.content.res.ColorStateList.valueOf(activeGreen));
                binding.cardIconBg.setCardBackgroundColor(activeBg);
            } else {
                int errorRed = ContextCompat.getColor(itemView.getContext(), R.color.agri_error_red);
                int errorBg  = ContextCompat.getColor(itemView.getContext(), R.color.accent_red_light);

                binding.tvStatusText.setText("Mất kết nối");
                binding.tvStatusText.setTextColor(errorRed);
                binding.chipStatusWrapper.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(errorBg));
                binding.viewChipDot.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(errorRed));
                binding.viewStatusDot.setBackgroundResource(R.drawable.ic_dot_indicator);
                binding.viewStatusDot.getBackground().setTint(errorRed);
                binding.ivDeviceIcon.setImageTintList(
                        android.content.res.ColorStateList.valueOf(errorRed));
                binding.cardIconBg.setCardBackgroundColor(errorBg);
            }
        }
    }
}