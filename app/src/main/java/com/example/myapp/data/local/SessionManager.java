package com.example.myapp.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SessionManager {

    private static final String PREF_NAME = "agri_secure_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_ACTIVE_DEVICE_ID = "active_device_id";
    private static final String KEY_ACTIVE_DEVICE_NAME = "active_device_name";
    public static final String DEFAULT_SERVER_URL = "http://10.0.2.2:9090/";
    
    private static volatile SessionManager instance;
    private final SharedPreferences sharedPreferences;

    private SessionManager(Context context) {
        SharedPreferences prefs;
        try {
            MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context.getApplicationContext(),
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Log chi tiết lỗi ra Logcat thay vì printStackTrace() (không hiển thị trên Logcat API 37)
            Log.e("SessionManager", "Khởi tạo EncryptedSharedPreferences thất bại, dùng fallback.", e);
            // Fallback to normal SharedPreferences if encryption fails
            prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        this.sharedPreferences = prefs;
    }

    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }

    public void saveToken(String token) {
        sharedPreferences.edit()
                .putString(KEY_TOKEN, token)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static boolean isEmulator() {
        return android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.BRAND.startsWith("generic")
                || android.os.Build.DEVICE.startsWith("generic")
                || android.os.Build.PRODUCT.contains("sdk_google")
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu");
    }

    public static String getDefaultServerUrl() {
        if (isEmulator()) {
            return "http://10.0.2.2:9090/";
        } else {
            // Điện thoại thật khi cắm cáp USB (đã forward qua adb reverse)
            return "http://127.0.0.1:9090/";
        }
    }

    public void setServerUrl(String serverUrl) {
        if (serverUrl != null && !serverUrl.endsWith("/")) {
            serverUrl += "/";
        }
        sharedPreferences.edit()
                .putString(KEY_SERVER_URL, serverUrl)
                .apply();
    }

    public String getServerUrl() {
        return sharedPreferences.getString(KEY_SERVER_URL, getDefaultServerUrl());
    }

    public void setActiveDeviceId(String deviceId) {
        sharedPreferences.edit()
                .putString(KEY_ACTIVE_DEVICE_ID, deviceId)
                .apply();
    }

    public String getActiveDeviceId() {
        return sharedPreferences.getString(KEY_ACTIVE_DEVICE_ID, null);
    }

    public void setActiveDeviceName(String deviceName) {
        sharedPreferences.edit()
                .putString(KEY_ACTIVE_DEVICE_NAME, deviceName)
                .apply();
    }

    public String getActiveDeviceName() {
        return sharedPreferences.getString(KEY_ACTIVE_DEVICE_NAME, null);
    }

    public void clearSession() {
        String savedServerUrl = getServerUrl();
        sharedPreferences.edit().clear().apply();
        // Giữ lại server URL đã cấu hình
        if (savedServerUrl != null) {
            setServerUrl(savedServerUrl);
        }
    }
}
