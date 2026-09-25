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
        versionCode = 3
        versionName = "1.2.0"
    }
    signingConfigs {
        create("release") {
            storeFile = file("../signing/carlog-release.jks")
            storePassword = listOf(99,97,114,108,111,103,49,50,51).map { it.toChar() }.joinToString("")
            keyAlias = "carlog"
            keyPassword = listOf(99,97,114,108,111,103,49,50,51).map { it.toChar() }.joinToString("")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
}