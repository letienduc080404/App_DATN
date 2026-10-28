package com.example.myapp.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.myapp.data.local.AppDatabase;
import com.example.myapp.data.local.dao.AgriDao;
import com.example.myapp.data.local.entities.AlertEntity;
import com.example.myapp.data.local.entities.SensorDataEntity;
import com.example.myapp.data.remote.ApiClient;
import com.example.myapp.data.remote.ThingsBoardApi;
import com.example.myapp.domain.model.AlertMessage;
import com.example.myapp.domain.model.DeviceStatus;
import com.example.myapp.domain.model.SensorData;
import com.example.myapp.domain.repository.AgriRepository;
import com.example.myapp.domain.repository.DataCallback;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Triển khai Single Source of Truth: ThingsBoard API -> Room -> UI.
 */
public class AgriRepositoryImpl implements AgriRepository {

    private static final String TAG = "AgriRepositoryImpl";
    private static volatile AgriRepositoryImpl instance;
    
    private final Context context;
    private final ExecutorService executorService;
    private final AgriDao agriDao;
    private final com.example.myapp.data.local.SessionManager sessionManager;
    private final MutableLiveData<String> errorEvent = new MutableLiveData<>();

    private static final String ENTITY_TYPE = "DEVICE";

    private AgriRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(2);
        this.agriDao = AppDatabase.getDatabase(context).agriDao();
        this.sessionManager = com.example.myapp.data.local.SessionManager.getInstance(context);
    }
    
    public static AgriRepositoryImpl getInstance(Context context) {
        if (instance == null) {
            synchronized (AgriRepositoryImpl.class) {
                if (instance == null) {
                    instance = new AgriRepositoryImpl(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private ThingsBoardApi getThingsBoardApi() {
        return ApiClient.getClient(context).create(ThingsBoardApi.class);
    }

    /**
     * Lấy danh sách thiết bị thuộc Tenant từ ThingsBoard và lưu vào Room.
     */
    @Override
    public void syncTenantDevices(Runnable onComplete) {
        String token = sessionManager.getToken();
        if (token == null || "mock_admin_token".equals(token)) {
            Log.d(TAG, "Đang dùng tài khoản Mock Admin hoặc chưa có token, bỏ qua đồng bộ ThingsBoard.");
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        getThingsBoardApi().getTenantDevices(50, 0).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    executorService.execute(() -> {
                        try {
                            JsonObject body = response.body();
                            if (body.has("data")) {
                                JsonArray dataArr = body.getAsJsonArray("data");
                                List<com.example.myapp.data.local.entities.DeviceEntity> entities = new ArrayList<>();
                                
                                for (int i = 0; i < dataArr.size(); i++) {
                                    JsonObject devObj = dataArr.get(i).getAsJsonObject();
                                    String devName = devObj.has("name") ? devObj.get("name").getAsString() : "Thiết bị " + (i + 1);
                                    String devId = devObj.getAsJsonObject("id").get("id").getAsString();

                                    // Nếu chưa có active device thì chọn thiết bị đầu tiên
                                    if (i == 0 && (sessionManager.getActiveDeviceId() == null || sessionManager.getActiveDeviceId().isEmpty())) {
                                        sessionManager.setActiveDeviceId(devId);
                                        sessionManager.setActiveDeviceName(devName);
                                        Log.d(TAG, "Đã chọn thiết bị mặc định: " + devName + " (" + devId + ")");
                                    }

                                    // Tọa độ giả định trên bản đồ
                                    double lat = 10.0451 + (i * 0.0015);
                                    double lng = 105.7468 + (i * 0.0012);

                                    entities.add(new com.example.myapp.data.local.entities.DeviceEntity(
                                            devName, 90, true, lat, lng
                                    ));
                                }

                                if (!entities.isEmpty()) {
                                    agriDao.deleteAllDevices();
                                    for (com.example.myapp.data.local.entities.DeviceEntity d : entities) {
                                        agriDao.insertDevice(d);
                                    }
                                    Log.d(TAG, "Đã đồng bộ " + entities.size() + " thiết bị của Tenant vào Room.");
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi lưu danh sách thiết bị vào Room", e);
                        }

                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
                } else {
                    Log.e(TAG, "Lỗi lấy danh sách thiết bị: " + response.code());
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Lỗi kết nối khi lấy thiết bị: " + t.getMessage());
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    /**
     * Đồng bộ dữ liệu (Đồng bộ - Blocking). Dùng cho WorkManager.
     */
    public boolean syncDataSync() {
        String token = sessionManager.getToken();
        if (token == null || "mock_admin_token".equals(token)) {
            return false;
        }

        String deviceId = sessionManager.getActiveDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            try {
                Response<JsonObject> devRes = getThingsBoardApi().getTenantDevices(10, 0).execute();
                if (devRes.isSuccessful() && devRes.body() != null && devRes.body().has("data")) {
                    JsonArray arr = devRes.body().getAsJsonArray("data");
                    if (arr.size() > 0) {
                        JsonObject first = arr.get(0).getAsJsonObject();
                        deviceId = first.getAsJsonObject("id").get("id").getAsString();
                        sessionManager.setActiveDeviceId(deviceId);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "syncDataSync: Lỗi lấy deviceId", e);
            }
        }

        if (deviceId == null || deviceId.isEmpty()) {
            return false;
        }

        String keys = "temp,humi,ph,n,p,k,water,ec";
        Call<JsonObject> call = getThingsBoardApi().getLatestTelemetry(ENTITY_TYPE, deviceId, keys);
        try {
            Response<JsonObject> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                saveThingsBoardDataToRoom(response.body());
                return true;
            } else {
                Log.e(TAG, "syncDataSync: API trả về lỗi HTTP " + response.code());
            }
        } catch (java.net.SocketTimeoutException e) {
            Log.w(TAG, "syncDataSync: Timeout trên mạng yếu", e);
        } catch (IOException e) {
            Log.e(TAG, "syncDataSync: Lỗi kết nối mạng", e);
        }
        return false;
    }
    
    @Override
    public LiveData<String> getErrorEvent() {
        return errorEvent;
    }

    /**
     * Gọi API lấy dữ liệu Telemetry từ ThingsBoard và lưu vào Room.
     * UI Fragment sẽ tự động cập nhật nhờ LiveData quan sát từ Room.
     */
    @Override
    public void syncDataFromThingsBoard() {
        String token = sessionManager.getToken();
        if (token == null || "mock_admin_token".equals(token)) {
            Log.d(TAG, "Đang dùng tài khoản Mock Admin, tạo dữ liệu mock ngẫu nhiên.");
            injectMockAdminData();
            return;
        }

        String deviceId = sessionManager.getActiveDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            Log.d(TAG, "Chưa có activeDeviceId, tiến hành đồng bộ danh sách thiết bị trước...");
            syncTenantDevices(this::syncDataFromThingsBoard);
            return;
        }

        String keys = "temp,humi,ph,n,p,k,water,ec"; // Các key cần lấy
        
        Call<JsonObject> call = getThingsBoardApi().getLatestTelemetry(ENTITY_TYPE, deviceId, keys);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    saveThingsBoardDataToRoom(data);
                } else {
                    Log.e(TAG, "Lỗi API ThingsBoard: " + response.code());
                    errorEvent.postValue("Lỗi API (" + response.code() + "). Đang hiển thị dữ liệu lưu tạm");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Lỗi kết nối tới ThingsBoard: " + t.getMessage());
                errorEvent.postValue("Mất kết nối mạng. Đang hiển thị dữ liệu lưu tạm");
            }
        });
    }

    private void saveThingsBoardDataToRoom(JsonObject json) {
        executorService.execute(() -> {
            try {
                // Parse dữ liệu Timeseries của ThingsBoard (thường trả về mảng cho mỗi key)
                double temp = extractValue(json, "temp", 0.0);
                double humi = extractValue(json, "humi", 0.0);
                double ph = extractValue(json, "ph", 7.0);
                double n = extractValue(json, "n", 0.0);
                double p = extractValue(json, "p", 0.0);
                double k = extractValue(json, "k", 0.0);
                double water = extractValue(json, "water", 0.0);
                double ec = extractValue(json, "ec", 0.0);

                SensorDataEntity entity = new SensorDataEntity(
                        System.currentTimeMillis(),
                        temp, humi, ph, n, p, k, water, ec
                );
                
                // Insert thẳng vào DB. LiveData trên UI sẽ tự trigger update.
                agriDao.insertSensorData(entity);
                Log.d(TAG, "Đã đồng bộ dữ liệu cảm biến thật từ ThingsBoard vào Room: Temp=" + temp + ", Humi=" + humi);

                // Tự động kiểm tra ngưỡng bất thường để tạo Alert Message
                checkAndGenerateAlerts(temp, humi, ph, water, ec);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi parse dữ liệu ThingsBoard", e);
            }
        });
    }

    private void checkAndGenerateAlerts(double temp, double humi, double ph, double water, double ec) {
        String currentTimeStr = new java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()
        ).format(new java.util.Date(System.currentTimeMillis()));

        if (water > 0 && water < 10.0) {
            agriDao.insertAlert(new AlertEntity(
                    "Mực nước ruộng thấp nguy hiểm",
                    "CẢNH BÁO: Mực nước ruộng hiện tại là " + water + " cm (dưới 10cm). Hãy bật máy bơm!",
                    "DANGER",
                    currentTimeStr
            ));
        }
        if (temp > 35.0) {
            agriDao.insertAlert(new AlertEntity(
                    "Nhiệt độ môi trường quá cao",
                    "CẢNH BÁO: Nhiệt độ hiện tại lên tới " + temp + " °C. Chú ý tưới làm mát cây trồng.",
                    "WARNING",
                    currentTimeStr
            ));
        }
        if (ph > 0 && (ph < 5.5 || ph > 8.0)) {
            agriDao.insertAlert(new AlertEntity(
                    "Độ pH đất bất thường",
                    "CẢNH BÁO: Độ pH đất đo được là " + ph + ". Cần cải tạo đất thích hợp.",
                    "WARNING",
                    currentTimeStr
            ));
        }
    }

    private double extractValue(JsonObject json, String key, double defaultValue) {
        if (json.has(key)) {
            JsonArray arr = json.getAsJsonArray(key);
            if (arr != null && arr.size() > 0) {
                JsonObject obj = arr.get(0).getAsJsonObject();
                if (obj.has("value")) {
                    try {
                        return Double.parseDouble(obj.get("value").getAsString());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                }
            }
        }
        return defaultValue;
    }

    @Override
    public void getDashboardData(DataCallback<SensorData> callback) {
        // Chuyển sang dùng LiveData quan sát từ Room (Single Source of Truth)
        // Lưu ý: Fragment sẽ cần cập nhật để observe LiveData mới
    }

    /**
     * Phương thức mới trả về LiveData quan sát trực tiếp từ Database.
     */
    public LiveData<SensorData> getLatestSensorData() {
        return Transformations.map(agriDao.getLatestSensorData(), entity -> {
            if (entity == null) return null;
            return new SensorData(
                    entity.temperature, entity.humidity, entity.phValue,
                    entity.nitrogen, entity.phosphorus, entity.potassium,
                    entity.waterLevel, entity.ecValue
            );
        });
    }

    @Override
    public void getAlerts(DataCallback<List<AlertMessage>> callback) {
        // Cũ: Trả về mock qua callback
    }

    /**
     * Phương thức mới trả về LiveData danh sách cảnh báo từ Database.
     */
    public LiveData<List<AlertMessage>> getAllAlerts() {
        return Transformations.map(agriDao.getAllAlerts(), entities -> {
            List<AlertMessage> messages = new ArrayList<>();
            for (AlertEntity entity : entities) {
                messages.add(new AlertMessage(
                        entity.title, entity.content,
                        AlertMessage.Priority.valueOf(entity.priority),
                        entity.timestamp
                ));
            }
            return messages;
        });
    }

    /**
     * Phương thức mới trả về LiveData danh sách thiết bị kèm tọa độ GPS từ Database.
     */
    public LiveData<List<DeviceStatus>> getAllDevices() {
        return Transformations.map(agriDao.getAllDevices(), entities -> {
            List<DeviceStatus> devices = new ArrayList<>();
            for (com.example.myapp.data.local.entities.DeviceEntity entity : entities) {
                devices.add(new DeviceStatus(
                        entity.name, entity.battery, entity.isActive,
                        entity.latitude, entity.longitude
                ));
            }
            return devices;
        });
    }

    @Override
    public void getDevices(DataCallback<List<DeviceStatus>> callback) {
        // Phương thức callback cũ — không còn sử dụng, giữ lại để tuân thủ interface
    }

    @Override
    public void injectMockAdminData() {
        executorService.execute(() -> {
            // ── Bước 1: Xóa toàn bộ dữ liệu cũ để tránh dữ liệu rác tích lũy ──
            agriDao.deleteAllSensorData();
            agriDao.deleteAllAlerts();
            agriDao.deleteAllDevices();
            Log.d(TAG, "Đã xóa dữ liệu cũ, chuẩn bị bơm Mock Data mới...");

            String currentTimeStr = new java.text.SimpleDateFormat(
                    "dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()
            ).format(new java.util.Date(System.currentTimeMillis()));

            // ── Bước 2: Tạo Dữ liệu Cảm biến (Dashboard & AI Chat) ──
            // Chỉ số kích hoạt cảnh báo: nước thấp 8cm, nhiệt cao 35.2°C, pH thấp 5.5
            SensorDataEntity sensorData = new SensorDataEntity(
                    System.currentTimeMillis(),
                    35.2,  // Nhiệt độ (°C) — cảnh báo cao
                    50.0,  // Độ ẩm (%)
                    5.5,   // pH — hơi axit
                    80.0,  // Nitơ (mg/kg)
                    40.0,  // Photpho (mg/kg)
                    50.0,  // Kali (mg/kg)
                    8.0,   // Mực nước (cm) — rất thấp, kích hoạt cảnh báo
                    1.8    // EC (mS/cm)
            );
            agriDao.insertSensorData(sensorData);
            Log.d(TAG, "Đã insert SensorData.");

            // ── Bước 3: Tạo Dữ liệu Thiết bị (Map) với tọa độ thực ĐBSCL ──
            com.example.myapp.data.local.entities.DeviceEntity device1 =
                    new com.example.myapp.data.local.entities.DeviceEntity(
                            "Trạm Lúa A - Trung Tâm", 95, true, 10.0451, 105.7468);
            com.example.myapp.data.local.entities.DeviceEntity device2 =
                    new com.example.myapp.data.local.entities.DeviceEntity(
                            "Trạm Lúa B - Bờ Bao", 42, true, 10.0465, 105.7480);
            com.example.myapp.data.local.entities.DeviceEntity device3 =
                    new com.example.myapp.data.local.entities.DeviceEntity(
                            "Trạm Bơm Tự Động 1", 10, false, 10.0440, 105.7450);
            com.example.myapp.data.local.entities.DeviceEntity device4 =
                    new com.example.myapp.data.local.entities.DeviceEntity(
                            "Trạm Đo Đất NPK", 78, true, 10.0470, 105.7445);
            agriDao.insertDevice(device1);
            agriDao.insertDevice(device2);
            agriDao.insertDevice(device3);
            agriDao.insertDevice(device4);
            Log.d(TAG, "Đã insert 4 DeviceEntity.");

            // ── Bước 4: Tạo Dữ liệu Cảnh báo (Alerts Fragment) — 3 mức độ ──
            AlertEntity alertDanger = new AlertEntity(
                    "Mực nước ruộng A nguy hiểm",
                    "CẢNH BÁO: Mực nước ruộng A xuống dưới 10cm. Yêu cầu bật trạm bơm ngay!",
                    "DANGER",
                    currentTimeStr
            );
            AlertEntity alertWarning = new AlertEntity(
                    "Pin trạm bơm sắp hết",
                    "Trạm Bơm Tự Động 1 sắp hết pin (10%) và đã mất kết nối.",
                    "WARNING",
                    currentTimeStr
            );
            AlertEntity alertInfo = new AlertEntity(
                    "Bón phân định kỳ thành công",
                    "Đã bón phân NPK định kỳ thành công lúc 08:00 sáng nay.",
                    "INFO",
                    currentTimeStr
            );
            agriDao.insertAlert(alertDanger);
            agriDao.insertAlert(alertWarning);
            agriDao.insertAlert(alertInfo);

            Log.d(TAG, "Đã inject toàn bộ Mock Data phong phú cho Admin thành công!");
        });
    }
}