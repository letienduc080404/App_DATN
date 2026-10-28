package com.example.myapp.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapp.R;
import com.example.myapp.databinding.FragmentDeviceMapBinding;
import com.example.myapp.domain.model.DeviceStatus;
import com.example.myapp.ui.viewmodel.DeviceMapViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "DeviceMapFragment";

    private FragmentDeviceMapBinding binding;
    private DeviceMapViewModel viewModel;
    private DeviceAdapter adapter;
    private GoogleMap mMap;

    // PERF FIX: Cache Marker theo tên thiết bị để cập nhật tại chỗ
    // thay vì clear() + addMarker() toàn bộ mỗi lần LiveData phát ra.
    private final Map<String, Marker> markerCache = new HashMap<>();

    private static final double BASE_LAT = 10.0451;
    private static final double BASE_LNG = 105.7468;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo RecyclerView — adapter dùng ListAdapter, không cần setDeviceList()
        adapter = new DeviceAdapter();
        binding.rvDevices.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDevices.setAdapter(adapter);

        // Khởi tạo ViewModel TRƯỚC khi khởi tạo bản đồ
        viewModel = new ViewModelProvider(this).get(DeviceMapViewModel.class);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Không tìm thấy SupportMapFragment với ID map_container");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Bật My Location button chỉ khi đã có quyền — tránh SecurityException API 37
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException khi bật My Location dù đã kiểm tra quyền", e);
            }
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(BASE_LAT, BASE_LNG), 15f));

        // observe() chỉ bắt đầu sau khi mMap đã sẵn sàng
        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding == null) return;
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.rvDevices.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        });

        viewModel.getDevices().observe(getViewLifecycleOwner(), devices -> {
            if (binding == null) return;
            if (devices != null) {
                // PERF: ListAdapter.submitList() — DiffUtil tự chạy trên background thread
                adapter.submitList(devices);
                updateMapMarkersIncremental(devices);
            }
        });
    }

    /**
     * PERF FIX: Thay thế mMap.clear() + addMarker() toàn bộ bằng chiến lược incremental:
     * - Nếu marker đã tồn tại (theo tên thiết bị): cập nhật position và icon tại chỗ.
     * - Nếu là thiết bị mới: chỉ addMarker() cho thiết bị đó.
     * - Nếu thiết bị đã bị xóa khỏi danh sách: xóa marker tương ứng.
     * Kết quả: Không có frame nào bị bỏ qua (dropped frame) khi bản đồ cập nhật.
     */
    private void updateMapMarkersIncremental(List<DeviceStatus> devices) {
        if (mMap == null) {
            Log.w(TAG, "updateMapMarkersIncremental() bỏ qua — mMap chưa sẵn sàng");
            return;
        }

        // Thu thập tên các thiết bị trong danh sách mới
        java.util.Set<String> activeNames = new java.util.HashSet<>();

        boolean shouldAnimateCamera = markerCache.isEmpty(); // Chỉ animate lần đầu

        for (DeviceStatus device : devices) {
            if (device.getLatitude() == 0.0 && device.getLongitude() == 0.0) {
                Log.w(TAG, "Bỏ qua device không có tọa độ: " + device.getName());
                continue;
            }

            activeNames.add(device.getName());
            LatLng position = new LatLng(device.getLatitude(), device.getLongitude());
            float markerColor = device.isActive()
                    ? BitmapDescriptorFactory.HUE_GREEN
                    : BitmapDescriptorFactory.HUE_RED;
            String snippet = "Pin: " + device.getBattery() + "% — "
                    + (device.isActive() ? "Hoạt động tốt" : "Đang mất kết nối");

            Marker existingMarker = markerCache.get(device.getName());
            if (existingMarker != null) {
                // PERF: Cập nhật tại chỗ — không tạo Marker mới
                existingMarker.setPosition(position);
                existingMarker.setIcon(BitmapDescriptorFactory.defaultMarker(markerColor));
                existingMarker.setSnippet(snippet);
            } else {
                // Thiết bị mới — chỉ addMarker() cho thiết bị này
                Marker newMarker = mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(device.getName())
                        .snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
                if (newMarker != null) {
                    markerCache.put(device.getName(), newMarker);
                }
            }
        }

        // Xóa marker của các thiết bị đã không còn trong danh sách
        java.util.Iterator<Map.Entry<String, Marker>> iterator =
                markerCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Marker> entry = iterator.next();
            if (!activeNames.contains(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }

        // Animate camera về marker đầu tiên chỉ khi lần đầu tải (không gây giật khi update)
        if (shouldAnimateCamera && !devices.isEmpty()) {
            for (DeviceStatus device : devices) {
                if (device.getLatitude() != 0.0 || device.getLongitude() != 0.0) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(device.getLatitude(), device.getLongitude()), 15f));
                    break;
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Xóa cache để giải phóng tham chiếu Marker khi Fragment bị hủy
        markerCache.clear();
        mMap = null;
        binding = null;
    }
}