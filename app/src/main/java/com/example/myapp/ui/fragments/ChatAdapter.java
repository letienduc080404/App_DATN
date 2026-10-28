package com.example.myapp.ui.fragments;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.databinding.ItemChatAiBinding;
import com.example.myapp.databinding.ItemChatUserBinding;
import com.example.myapp.domain.model.ChatMessage;

/**
 * PERF FIX: Chuyển sang ListAdapter + DiffUtil.
 *
 * Chat có điểm đặc biệt: Tin nhắn CHỈ được thêm vào (append-only),
 * không bao giờ sửa/xóa — DiffUtil sẽ nhận ra điều này và chỉ
 * notify insert cho item cuối, không vẽ lại toàn bộ list.
 *
 * QUAN TRỌNG: ChatAdapter dùng MultiViewType → KHÔNG thể dùng ListAdapter
 * trực tiếp vì ListAdapter chỉ hỗ trợ 1 ViewHolder type trong signature.
 * Giải pháp: Vẫn dùng RecyclerView.Adapter nhưng thay notifyDataSetChanged()
 * bằng DiffUtil.calculateDiff() chạy đúng cách để tính diff thủ công.
 * Cách này tương đương ListAdapter nhưng hỗ trợ được MultiViewType.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI   = 2;

    private java.util.List<ChatMessage> messages = new java.util.ArrayList<>();

    /**
     * PERF: Thay notifyDataSetChanged() bằng DiffUtil manual để tính diff
     * chỉ cho item mới thêm vào (append-only pattern của Chat).
     */
    public void setMessages(java.util.List<ChatMessage> newMessages) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return messages.size(); }
            @Override public int getNewListSize() { return newMessages.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                // Chat append-only: nếu cùng index và cùng sender thì là cùng item
                ChatMessage oldMsg = messages.get(oldPos);
                ChatMessage newMsg = newMessages.get(newPos);
                return oldMsg.isFromUser() == newMsg.isFromUser()
                        && oldMsg.getText().equals(newMsg.getText());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return messages.get(oldPos).getText()
                        .equals(newMessages.get(newPos).getText());
            }
        });

        this.messages = newMessages;
        result.dispatchUpdatesTo(this); // Chỉ notify đúng các vị trí thay đổi
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isFromUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            ItemChatUserBinding b = ItemChatUserBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new UserViewHolder(b);
        } else {
            ItemChatAiBinding b = ItemChatAiBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new AiViewHolder(b);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(message);
        } else if (holder instanceof AiViewHolder) {
            ((AiViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    // PERF: setHasStableIds giúp RecyclerView tái sử dụng ViewHolder hiệu quả hơn
    // khi DiffUtil phát hiện item không thay đổi vị trí.
    @Override
    public long getItemId(int position) {
        ChatMessage msg = messages.get(position);
        return (long) msg.getText().hashCode() * 31 + (msg.isFromUser() ? 1 : 0);
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatUserBinding binding;
        UserViewHolder(ItemChatUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        void bind(ChatMessage message) { binding.tvMessage.setText(message.getText()); }
    }

    static class AiViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatAiBinding binding;
        AiViewHolder(ItemChatAiBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        void bind(ChatMessage message) {
            // Hiển thị “AI đang suy nghĩ...” khi là tin nhắn tạm chờ Gemini trả về
            if ("__THINKING__".equals(message.getText())) {
                binding.tvMessage.setText("… AI đang suy nghĩ");
                binding.tvMessage.setAlpha(0.6f);
            } else {
                binding.tvMessage.setText(message.getText());
                binding.tvMessage.setAlpha(1.0f);
            }
        }
    }
}