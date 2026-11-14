plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cinematycoonbid.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cinematycoonbid.game"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "1.0.7"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("com.google.zxing:core:3.5.3")
}
