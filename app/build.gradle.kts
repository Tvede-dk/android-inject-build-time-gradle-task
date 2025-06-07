plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("BuildTimePlugin")
}

android {
    namespace = "com.example.update_build_date_plugin"
    compileSdk = 35
    defaultConfig {
       minSdk = 21
       targetSdk = 35
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}
