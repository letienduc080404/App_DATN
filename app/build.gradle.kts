plugins {
    alias(libs.plugins.android.application)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.myapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 26  // Nâng lên API 26 (Android O) — bắt buộc cho Guava MethodHandle (dùng bởi google-genai SDK)
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // GEMINI_API_KEY được secrets-gradle-plugin tự động inject vào BuildConfig
        // từ local.properties — KHÔNG khai báo buildConfigField thủ công ở đây
        // vì sẽ gây xung đột và trả về empty string
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true  // Bắt buộc để tạo BuildConfig.GEMINI_API_KEY
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

// Cấu hình tường minh cho secrets-gradle-plugin v2.0.1
// Đảm bảo GEMINI_API_KEY từ local.properties được inject vào BuildConfig
secrets {
    // File chứa các key bí mật (không commit lên git)
    propertiesFileName = "local.properties"
    // File chứa các giá trị mặc định (commit lên git, giá trị placeholder)
    defaultPropertiesFileName = "local.defaults.properties"
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Security Crypto for EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // WorkManager
    val workVersion = "2.9.0"
    implementation("androidx.work:work-runtime:$workVersion")
    
    // MQTT Paho Client
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    
    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Google Gemini AI SDK cho Android (hỗ trợ Java thông qua các lớp .java.* trong generativeai)
    // QUAN TRỌNG: 0.9.0 là phiên bản MỚI NHẤT VÀ CUỐI CÙNG của artifact này (released Jul 2024).
    // Google đã ngừng phát triển artifact này — KHÔNG có phiên bản 1.0.0 trên Maven.
    // Lỗi MissingFieldException được xử lý bằng cách bắt exception thủ công trong onFailure().
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    
    // Guava ListenableFuture — cần thiết cho GenerativeModelFutures đối với Java
    implementation("com.google.guava:guava:33.3.0-android")
}