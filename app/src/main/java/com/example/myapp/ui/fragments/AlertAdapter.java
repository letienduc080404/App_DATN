package com.example.myapp.ui.fragments;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.example.myapp.R;
import com.example.myapp.databinding.ItemAlertBinding;
import com.example.myapp.domain.model.AlertMessage;

/**
 * PERF FIX: Chuyển từ RecyclerView.Adapter + notifyDataSetChanged()
 * sang ListAdapter + DiffUtil.ItemCallback.
 *
 * Lợi ích:
 * - DiffUtil tính diff trên background thread (AsyncListDiffer nội bộ).
 * - Chỉ những item THỰC SỰ thay đổi mới được vẽ lại, tránh flash/stutter.
 * - notifyDataSetChanged() bị loại bỏ hoàn toàn.
 */
public class AlertAdapter extends ListAdapter<AlertMessage, AlertAdapter.AlertViewHolder> {

    // ── DiffUtil Callback: Định nghĩa "hai item có phải là cùng một item không?"
    // và "nội dung của chúng có giống nhau không?"
    private static final DiffUtil.ItemCallback<AlertMessage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AlertMessage>() {
                @Override
                public boolean areItemsTheSame(@NonNull AlertMessage oldItem,
                                               @NonNull AlertMessage newItem) {
                    // Dùng title + timestamp làm khoá nhận dạng duy nhất
                    return oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getTimestamp().equals(newItem.getTimestamp());
                }

                @Override
                public boolean areContentsTheSame(@NonNull AlertMessage oldItem,
                                                  @NonNull AlertMessage newItem) {
                    // So sánh toàn bộ nội dung để quyết định có vẽ lại item không
                    return oldItem.getContent().equals(newItem.getContent())
                            && oldItem.getPriority() == newItem.getPriority();
                }
            };

    public AlertAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlertBinding binding = ItemAlertBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AlertViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        holder.bind(getItem(position)); // ListAdapter dùng getItem() thay vì list.get()
    }

    static class AlertViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        private final ItemAlertBinding binding;

        public AlertViewHolder(ItemAlertBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(AlertMessage alert) {
            binding.tvAlertTitle.setText(alert.getTitle());
            binding.tvAlertContent.setText(alert.getContent());
            binding.tvTimestamp.setText(alert.getTimestamp());

            // ── Xác định bộ màu theo mức ưu tiên ──
            int iconColor, bgColor, labelText;
            switch (alert.getPriority()) {
                case DANGER:
                    iconColor = ContextCompat.getColor(itemView.getContext(), R.color.agri_error_red);
                    bgColor   = ContextCompat.getColor(itemView.getContext(), R.color.accent_red_light);
                    labelText = R.string.priority_danger;
                    break;
                case WARNING:
                    iconColor = ContextCompat.getColor(itemView.getContext(), R.color.agri_tertiary_yellow);
                    bgColor   = ContextCompat.getColor(itemView.getContext(), R.color.accent_yellow_light);
                    labelText = R.string.priority_warning;
                    break;
                case INFO:
                default:
                    iconColor = ContextCompat.getColor(itemView.getContext(), R.color.agri_secondary_blue);
                    bgColor   = ContextCompat.getColor(itemView.getContext(), R.color.accent_blue_light);
                    labelText = R.string.priority_info;
                    break;
            }

            android.content.res.ColorStateList iconTintList =
                    android.content.res.ColorStateList.valueOf(iconColor);
            android.content.res.ColorStateList bgTintList =
                    android.content.res.ColorStateList.valueOf(bgColor);

            binding.cardAlertIcon.setCardBackgroundColor(bgTintList);
            binding.ivAlertIcon.setImageTintList(iconTintList);
            binding.cardPriorityBadge.setCardBackgroundColor(bgTintList);
            binding.tvPriorityLabel.setTextColor(iconColor);
            binding.tvPriorityLabel.setText(labelText);
            binding.btnDetail.setBackgroundTintList(bgTintList);
            binding.btnDetail.setTextColor(iconColor);
            binding.viewPriorityIndicator.setBackgroundColor(iconColor);
        }
    }
}