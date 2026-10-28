package com.example.myapp.data.remote;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ThingsBoardApi {
    
    // API lấy dữ liệu Telemetry (timeseries) từ ThingsBoard
    @GET("api/plugins/telemetry/{entityType}/{entityId}/values/timeseries")
    Call<JsonObject> getLatestTelemetry(
            @Path("entityType") String entityType,
            @Path("entityId") String entityId,
            @Query("keys") String keys
    );
}
