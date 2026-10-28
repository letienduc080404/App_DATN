package com.example.myapp.data.remote;

import com.google.gson.JsonObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AgriApiService {
    
    // Auth endpoints
    @POST("api/auth/login")
    Call<JsonObject> login(@Body JsonObject loginRequest);

    // Quên mật khẩu - ThingsBoard Endpoint
    @POST("api/noauth/resetPasswordByEmail")
    Call<Void> resetPasswordByEmail(@Body JsonObject forgotPasswordRequest);

    // Tạo Tenant (Dành cho Admin)
    @POST("api/tenant")
    Call<JsonObject> createTenant(@Header("X-Authorization") String authHeader, @Body JsonObject request);

    // Tạo User cho Tenant (Dành cho Admin)
    @POST("api/user?sendActivationMail=false")
    Call<JsonObject> createUser(@Header("X-Authorization") String authHeader, @Body JsonObject request);

    // Lấy Link kích hoạt User
    @GET("api/user/{userId}/activationLink")
    Call<ResponseBody> getActivationLink(@Header("X-Authorization") String authHeader, @Path("userId") String userId);

    // Kích hoạt tài khoản và đặt mật khẩu
    @POST("api/noauth/activate")
    Call<JsonObject> activateUser(@Body JsonObject request);

    // Tạo thiết bị mặc định cho Tenant
    @POST("api/device")
    Call<JsonObject> createDevice(@Header("X-Authorization") String authHeader, @Body JsonObject request);
}
