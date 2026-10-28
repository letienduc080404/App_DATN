package com.example.myapp.data.remote;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AgriApiService {
    
    // Auth endpoints
    @POST("api/auth/login")
    Call<JsonObject> login(@Body JsonObject loginRequest);

    @POST("api/auth/register")
    Call<JsonObject> register(@Body JsonObject registerRequest);

    @POST("api/auth/forgot-password")
    Call<JsonObject> forgotPassword(@Body JsonObject forgotPasswordRequest);
}
