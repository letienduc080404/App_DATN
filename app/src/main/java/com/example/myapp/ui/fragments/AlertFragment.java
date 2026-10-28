package com.example.myapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapp.databinding.FragmentAlertBinding;
import com.example.myapp.ui.viewmodel.AlertViewModel;

public class AlertFragment extends Fragment {

    private FragmentAlertBinding binding;
    private AlertViewModel viewModel;
    private AlertAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAlertBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Thiết lập RecyclerView
        adapter = new AlertAdapter();
        binding.rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAlerts.setAdapter(adapter);

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(AlertViewModel.class);

        observeViewModel();

        // Dữ liệu sẽ tự động cập nhật từ Room
        // viewModel.fetchAlerts();
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.rvAlerts.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });

        viewModel.getAlerts().observe(getViewLifecycleOwner(), alerts -> {
            if (alerts != null) {
                // PERF: ListAdapter.submitList() kích hoạt DiffUtil trên background thread
                adapter.submitList(alerts);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}