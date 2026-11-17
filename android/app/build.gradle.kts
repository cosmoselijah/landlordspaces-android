plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing comes from environment variables (set by CI from repository secrets).
// Without them the release build is signed with the debug key so it can still be installed for testing.
val ksPath: String? = System.getenv("LS_KEYSTORE")

android {
    namespace = "com.landlordspaces.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.landlordspaces.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "1.7.0"
    }

    signingConfigs {
        create("release") {
            if (ksPath != null) {
                storeFile = file(ksPath)
                storePassword = System.getenv("LS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LS_KEY_ALIAS")
                keyPassword = System.getenv("LS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (ksPath != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.work:work-runtime:2.9.1")
}
