plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing comes from ~/.gradle/gradle.properties or environment variables.
// Never put these values in this repository.
fun secret(name: String): String? =
    (findProperty(name) as String?) ?: System.getenv(name)

android {
    namespace = "com.reyespascal.picturehelper"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "com.reyespascal.picturehelper"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val store = secret("PH_STORE_FILE")
            if (store != null) {
                storeFile = file(store)
                storePassword = secret("PH_STORE_PASSWORD")
                keyAlias = secret("PH_KEY_ALIAS")
                keyPassword = secret("PH_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (secret("PH_STORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    // Keep video clips uncompressed inside the APK so the player can read them directly.
    androidResources { noCompress += listOf("mp4") }

    // No abiFilters and no ABI splits: one universal APK.
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.tv.material)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui.compose)
    implementation(libs.datastore.preferences)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
