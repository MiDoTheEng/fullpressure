plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.midotheeng.fullpressure"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.midotheeng.fullpressure"
        minSdk = 28
        targetSdk = 35
        versionCode = 8
        versionName = "1.7"
    }

    signingConfigs {
        create("fp") {
            storeFile = file("fullpressure.keystore")
            storePassword = "fpress1234"
            keyAlias = "fullpressure"
            keyPassword = "fpress1234"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fp")
        }
        debug {
            signingConfig = signingConfigs.getByName("fp")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
}
