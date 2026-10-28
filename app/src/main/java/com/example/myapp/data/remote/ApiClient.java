package com.example.myapp.data.remote;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapp.data.local.SessionManager;

import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static volatile Retrofit retrofit = null;
    
    // Sự kiện Auth Error (SingleLiveEvent) để UI có thể observe và chỉ kích hoạt một lần duy nhất khi có lỗi 401 thật
    private static final SingleLiveEvent<Boolean> authErrorEvent = new SingleLiveEvent<>();

    public static LiveData<Boolean> getAuthErrorEvent() {
        return authErrorEvent;
    }

    public static void resetAuthErrorEvent() {
        authErrorEvent.reset();
    }

    public static synchronized void resetClient() {
        retrofit = null;
    }

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    SessionManager sessionManager = SessionManager.getInstance(context);
                    String baseUrl = sessionManager.getServerUrl();

                    // 1. Logic Auth Interceptor
                    Interceptor authInterceptor = chain -> {
                        Request originalRequest = chain.request();
                        String path = originalRequest.url().encodedPath();

                        // Bỏ qua xác thực cho các endpoint liên quan đến auth
                        if (path.contains("login") || path.contains("register") || path.contains("forgot-password") || path.contains("noauth")) {
                            return chain.proceed(originalRequest);
                        }

                        // Đính kèm token
                        String token = sessionManager.getToken();
                        Request.Builder requestBuilder = originalRequest.newBuilder();
                        if (token != null && !token.isEmpty() && !"mock_admin_token".equals(token)) {
                            requestBuilder.header("X-Authorization", "Bearer " + token);
                        }

                        Request newRequest = requestBuilder.build();
                        Response response = chain.proceed(newRequest);

                        // Bắt lỗi 401 Unauthorized (chỉ kích hoạt khi token thật bị hết hạn)
                        if (response.code() == 401) {
                            String curToken = sessionManager.getToken();
                            if (curToken != null && !curToken.isEmpty() && !"mock_admin_token".equals(curToken)) {
                                sessionManager.clearSession();
                                authErrorEvent.postValue(true); // Phát sự kiện đăng xuất
                            }
                        }

                        return response;
                    };

                    // 2. Logic Logging Interceptor
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(loggingInterceptor)
                            .addInterceptor(authInterceptor)
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)  // Tăng lên 20s cho mạng yếu đồng ruộng
                            .writeTimeout(15, TimeUnit.SECONDS) // Chống ANR khi upload dữ liệu
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit;
    }
}
