package com.example.myapp.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapp.databinding.FragmentChatAiLocalBinding;
import com.example.myapp.ui.viewmodel.ChatViewModel;

import java.util.ArrayList;

public class ChatAILocalFragment extends Fragment {

    private FragmentChatAiLocalBinding binding;
    private ChatViewModel viewModel;
    private ChatAdapter adapter;

    // Launcher nhận diện giọng nói (Speech-to-Text)
    private final ActivityResultLauncher<Intent> speechRecognizerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData()
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        if (!spokenText.trim().isEmpty()) {
                            binding.etMessage.setText(spokenText);
                            // Tự động gửi câu hỏi hoặc lệnh giọng nói tới AI / ViewModel
                            viewModel.sendMessage(spokenText);
                            binding.etMessage.setText("");
                        }
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatAiLocalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Thiết lập RecyclerView
        adapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        // Hiển thị tin nhắn mới nhất ở dưới cùng
        layoutManager.setStackFromEnd(true);
        binding.rvChat.setLayoutManager(layoutManager);
        binding.rvChat.setAdapter(adapter);

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        observeViewModel();

        // Xử lý gửi tin nhắn
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString();
            if (!text.trim().isEmpty()) {
                viewModel.sendMessage(text);
                binding.etMessage.setText("");
            }
        });

        // Xử lý xóa lịch sử trò chuyện
        binding.btnClearChat.setOnClickListener(v -> viewModel.clearHistory());

        // Xử lý nút Micro (Speech-to-Text & Voice Commands)
        binding.btnVoice.setOnClickListener(v -> startVoiceInput());
    }

    /**
     * Kích hoạt nhận diện giọng nói Tiếng Việt (Speech-to-Text)
     */
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Bác hãy nói câu hỏi hoặc câu lệnh (ví dụ: 'Nhiệt độ ruộng bao nhiêu?' hoặc 'Bật máy bơm')...");

        try {
            speechRecognizerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Thiết bị của bác chưa hỗ trợ nhận diện giọng nói.", Toast.LENGTH_SHORT).show();
        }
    }

    private void observeViewModel() {
        // VERIFIED: getViewLifecycleOwner() đúng chuẩn — Observer tự hủy khi View bị destroy,
        // không bị nhân bản khi chuyển tab (Replace Fragment).
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                // ChatAdapter.setMessages() đã dùng DiffUtil.calculateDiff() nội bộ
                adapter.setMessages(messages);
                // Cuộn xuống tin nhắn mới nhất
                if (!messages.isEmpty()) {
                    binding.rvChat.smoothScrollToPosition(messages.size() - 1);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}