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
        versionCode = 2
        versionName = "1.1.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
}