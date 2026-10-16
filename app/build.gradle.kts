import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// ---- Load secrets (NOT committed). See secrets.properties.example ----
val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties().apply {
    if (secretsFile.exists()) secretsFile.inputStream().use { load(it) }
}
fun secret(key: String, default: String = ""): String =
    (secrets.getProperty(key) ?: System.getenv(key) ?: default)

android {
    namespace = "com.sxplus.rider"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sxplus.rider"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }

        // ---- Keyless / BLE secrets surfaced to code via BuildConfig ----
        buildConfigField("String", "AES_KEY_HEX", "\"${secret("AES_KEY_HEX")}\"")
        buildConfigField("int", "XOR_KEY_BYTE", secret("XOR_KEY_BYTE", "235")) // 0xEB
        buildConfigField("String", "SEED_FIND_VEHICLE", "\"${secret("SEED_FIND_VEHICLE")}\"")
        buildConfigField("String", "SEED_LOCK", "\"${secret("SEED_LOCK")}\"")
        buildConfigField("String", "SEED_UNLOCK", "\"${secret("SEED_UNLOCK")}\"")
        buildConfigField("String", "SEED_OPEN_TRUNK", "\"${secret("SEED_OPEN_TRUNK")}\"")
        buildConfigField("String", "UUID_WRITE", "\"${secret("UUID_WRITE", "00005352-0000-1000-8000-00805f9b34fb")}\"")
        buildConfigField("String", "UUID_NOTIFY", "\"${secret("UUID_NOTIFY", "00005354-0000-1000-8000-00805f9b34fb")}\"")
        buildConfigField("String", "BOOTSTRAP_FRAMES", "\"${secret("BOOTSTRAP_FRAMES")}\"") // comma-separated hex
        buildConfigField("String", "USER_ID", "\"${secret("USER_ID", "0")}\"")
        // ---- Cloud ----
        buildConfigField("String", "BASE_URL_ICE", "\"${secret("BASE_URL_ICE", "https://tvsconnectapi.tvsmotor.com/api/")}\"")
        buildConfigField("String", "BASE_URL_EV", "\"${secret("BASE_URL_EV", "https://tvsiqubeapi.tvsmotor.com/api/")}\"")
        buildConfigField("String", "GRAPHQL_URL", "\"${secret("GRAPHQL_URL", "https://p360.tvsmotor.com/gapi")}\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("boolean", "ALLOW_MOCK", "true")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "ALLOW_MOCK", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)

    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
    implementation(libs.accompanist.permissions)
    implementation(libs.kotlinx.coroutines.android)
}
