plugins {
    id("com.android.application")
}
android {
    namespace = "com.mehdi.carlog"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.mehdi.carlog"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
}
