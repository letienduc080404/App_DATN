package com.example.myapp.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapp.data.local.SessionManager;
import com.example.myapp.data.remote.AgriApiService;
import com.example.myapp.data.remote.ApiClient;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {

    private final AgriApiService apiService;
    private final SessionManager sessionManager;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> loginResult = new MutableLiveData<>();
    private final MutableLiveData<String> registerResult = new MutableLiveData<>();
    private final MutableLiveData<String> forgotPasswordResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        sessionManager = SessionManager.getInstance(application);
        apiService = ApiClient.getClient(application).create(AgriApiService.class);
    }

    // ───── Getters cho UI observe ─────
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<String> getLoginResult() { return loginResult; }
    public LiveData<String> getRegisterResult() { return registerResult; }
    public LiveData<String> getForgotPasswordResult() { return forgotPasswordResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public String getServerUrl() {
        return sessionManager.getServerUrl();
    }

    public void setServerUrl(String url) {
        sessionManager.setServerUrl(url);
        ApiClient.resetClient();
    }

    private AgriApiService getApiService() {
        return ApiClient.getClient(getApplication()).create(AgriApiService.class);
    }

    // ───── ĐĂNG NHẬP ─────
    public void login(String email, String password) {
        ApiClient.resetAuthErrorEvent();

        // Backdoor cho Mock Admin nếu người dùng muốn kiểm thử offline
        if ("mockadmin".equals(email) && "admin".equals(password)) {
            sessionManager.saveToken("mock_admin_token");
            com.example.myapp.data.repository.AgriRepositoryImpl.getInstance(getApplication())
                    .injectMockAdminData();
            loginResult.postValue("OK");
            return;
        }

        isLoading.setValue(true);

        JsonObject body = new JsonObject();
        body.addProperty("username", email);
        body.addProperty("password", password);

        getApiService().login(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    // ThingsBoard trả về trường "token" chứa JWT
                    String token = data.has("token") ? data.get("token").getAsString() : null;
                    if (token != null) {
                        ApiClient.resetAuthErrorEvent();
                        sessionManager.saveToken(token);
                        
                        // Đồng bộ thiết bị và dữ liệu cảm biến đầu tiên ngay sau khi đăng nhập
                        com.example.myapp.data.repository.AgriRepositoryImpl repo = 
                                com.example.myapp.data.repository.AgriRepositoryImpl.getInstance(getApplication());
                        repo.syncTenantDevices(repo::syncDataFromThingsBoard);

                        loginResult.postValue("OK");
                    } else {
                        errorMessage.postValue("Phản hồi không hợp lệ từ server");
                    }
                } else {
                    errorMessage.postValue("Sai tài khoản hoặc mật khẩu (Mã lỗi " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isLoading.postValue(false);
                android.util.Log.e("API_ERROR", "Lỗi mạng chi tiết (Login): ", t);
                errorMessage.postValue("Lỗi kết nối tới Server: " + t.getMessage());
            }
        });
    }

    // ───── ĐĂNG KÝ TENANT MỚI TRÊN THINGSBOARD ─────
    public void register(String email, String password, String deviceName) {
        isLoading.setValue(true);
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AgriApiService api = getApiService();

                // 1. Đăng nhập quyền SysAdmin ThingsBoard
                JsonObject sysLogin = new JsonObject();
                sysLogin.addProperty("username", "sysadmin@thingsboard.org");
                sysLogin.addProperty("password", "sysadmin");

                Response<JsonObject> sysRes = api.login(sysLogin).execute();
                if (!sysRes.isSuccessful() || sysRes.body() == null || !sysRes.body().has("token")) {
                    postError("Không thể xác thực quyền quản trị hệ thống để tạo Tenant.");
                    return;
                }
                String sysToken = "Bearer " + sysRes.body().get("token").getAsString();

                // 2. Tạo Tenant mới (Nông trại)
                JsonObject tenantReq = new JsonObject();
                String farmTitle = (deviceName != null && !deviceName.trim().isEmpty()) ? 
                        "Nông trại " + deviceName.trim() : "Nông trại " + email;
                tenantReq.addProperty("title", farmTitle);
                tenantReq.addProperty("email", email);

                Response<JsonObject> tenantRes = api.createTenant(sysToken, tenantReq).execute();
                if (!tenantRes.isSuccessful() || tenantRes.body() == null) {
                    postError("Tạo nông trại thất bại (" + tenantRes.code() + "). Email hoặc tên có thể đã tồn tại.");
                    return;
                }
                String tenantId = tenantRes.body().getAsJsonObject("id").get("id").getAsString();

                // 3. Tạo tài khoản Tenant Admin
                JsonObject userReq = new JsonObject();
                JsonObject tIdObj = new JsonObject();
                tIdObj.addProperty("entityType", "TENANT");
                tIdObj.addProperty("id", tenantId);
                userReq.add("tenantId", tIdObj);
                userReq.addProperty("email", email);
                userReq.addProperty("authority", "TENANT_ADMIN");
                userReq.addProperty("firstName", "Chủ");
                userReq.addProperty("lastName", "Trang trại");

                Response<JsonObject> userRes = api.createUser(sysToken, userReq).execute();
                if (!userRes.isSuccessful() || userRes.body() == null) {
                    postError("Tạo tài khoản thất bại. Email này có thể đã được đăng ký trên hệ thống.");
                    return;
                }
                String userId = userRes.body().getAsJsonObject("id").get("id").getAsString();

                // 4. Lấy link kích hoạt và trích xuất token
                Response<okhttp3.ResponseBody> linkRes = api.getActivationLink(sysToken, userId).execute();
                if (!linkRes.isSuccessful() || linkRes.body() == null) {
                    postError("Không thể lấy mã kích hoạt tài khoản.");
                    return;
                }
                String link = linkRes.body().string();
                String activateToken = null;
                if (link.contains("activateToken=")) {
                    int idx = link.indexOf("activateToken=");
                    activateToken = link.substring(idx + "activateToken=".length()).trim();
                    if (activateToken.contains("&")) {
                        activateToken = activateToken.substring(0, activateToken.indexOf("&"));
                    }
                }
                if (activateToken == null || activateToken.isEmpty()) {
                    postError("Không tìm thấy mã kích hoạt tài khoản.");
                    return;
                }

                // 5. Kích hoạt tài khoản và đặt mật khẩu người dùng đã nhập
                JsonObject actReq = new JsonObject();
                actReq.addProperty("activateToken", activateToken);
                actReq.addProperty("password", password);

                Response<JsonObject> actRes = api.activateUser(actReq).execute();
                if (!actRes.isSuccessful() || actRes.body() == null) {
                    postError("Kích hoạt tài khoản thất bại (" + actRes.code() + ").");
                    return;
                }
                String userToken = "Bearer " + actRes.body().get("token").getAsString();

                // 6. Tạo thiết bị đo mặc định cho Tenant
                if (deviceName != null && !deviceName.trim().isEmpty()) {
                    JsonObject devReq = new JsonObject();
                    devReq.addProperty("name", deviceName.trim());
                    devReq.addProperty("type", "default");
                    devReq.addProperty("label", "Trạm quan trắc nông nghiệp");
                    try {
                        api.createDevice(userToken, devReq).execute();
                    } catch (Exception ignored) {}
                }

                // Hoàn tất thành công
                isLoading.postValue(false);
                registerResult.postValue("OK");

            } catch (Exception e) {
                isLoading.postValue(false);
                android.util.Log.e("REGISTER_ERROR", "Lỗi tạo tài khoản: ", e);
                errorMessage.postValue("Lỗi kết nối khi đăng ký: " + e.getMessage());
            }
        });
    }

    private void postError(String message) {
        isLoading.postValue(false);
        errorMessage.postValue(message);
    }

    // ───── QUÊN MẬT KHẨU (THINGSBOARD REST API) ─────
    public void forgotPassword(String email) {
        isLoading.setValue(true);

        JsonObject body = new JsonObject();
        body.addProperty("email", email);

        getApiService().resetPasswordByEmail(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLoading.postValue(false);
                if (response.isSuccessful()) {
                    forgotPasswordResult.postValue("OK");
                } else if (response.code() == 404) {
                    errorMessage.postValue("Không tìm thấy tài khoản với email này trên hệ thống!");
                } else {
                    errorMessage.postValue("Lỗi gửi email (" + response.code() + "). Hãy kiểm tra cấu hình Mail Server trên ThingsBoard.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLoading.postValue(false);
                android.util.Log.e("API_ERROR", "Lỗi mạng chi tiết (Forgot Password): ", t);
                errorMessage.postValue("Lỗi kết nối tới Server: " + t.getMessage());
            }
        });
    }
}
