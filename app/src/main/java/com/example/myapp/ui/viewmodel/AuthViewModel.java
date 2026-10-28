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

    // ───── ĐĂNG NHẬP ─────
    public void login(String email, String password) {
        // Backdoor cho Admin để kiểm thử offline
        if ("admin".equals(email) && "admin".equals(password)) {
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

        apiService.login(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    // Server trả về trường "token" chứa JWT
                    String token = data.has("token") ? data.get("token").getAsString() : null;
                    if (token != null) {
                        sessionManager.saveToken(token);
                        loginResult.postValue("OK");
                    } else {
                        errorMessage.postValue("Phản hồi không hợp lệ từ server");
                    }
                } else {
                    errorMessage.postValue("Sai tài khoản hoặc mật khẩu");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isLoading.postValue(false);
                android.util.Log.e("API_ERROR", "Lỗi mạng chi tiết (Login): ", t);
                errorMessage.postValue("Lỗi kết nối mạng: " + t.getMessage());
            }
        });
    }

    // ───── ĐĂNG KÝ ─────
    public void register(String email, String password, String deviceName) {
        isLoading.setValue(true);

        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("deviceName", deviceName);

        apiService.register(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading.postValue(false);
                if (response.isSuccessful()) {
                    registerResult.postValue("OK");
                } else {
                    errorMessage.postValue("Đăng ký thất bại. Email có thể đã tồn tại.");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isLoading.postValue(false);
                android.util.Log.e("API_ERROR", "Lỗi mạng chi tiết (Register): ", t);
                errorMessage.postValue("Lỗi kết nối mạng: " + t.getMessage());
            }
        });
    }

    // ───── QUÊN MẬT KHẨU ─────
    public void forgotPassword(String email) {
        isLoading.setValue(true);

        JsonObject body = new JsonObject();
        body.addProperty("email", email);

        apiService.forgotPassword(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading.postValue(false);
                if (response.isSuccessful()) {
                    forgotPasswordResult.postValue("OK");
                } else {
                    errorMessage.postValue("Không tìm thấy tài khoản với email này");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isLoading.postValue(false);
                android.util.Log.e("API_ERROR", "Lỗi mạng chi tiết (Forgot Password): ", t);
                errorMessage.postValue("Lỗi kết nối mạng: " + t.getMessage());
            }
        });
    }
}
