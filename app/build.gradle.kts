plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// CI passes -PversionName=1.2.3 -PversionCode=10203 (derived from the git
// tag, see .github/workflows/release.yml). Local/dev builds fall back to
// these defaults so the project still builds fine from Android Studio.
val ciVersionName = (project.findProperty("versionName") as String?) ?: "0.1.0"
val ciVersionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1

// Optional release signing, supplied by CI via env vars when the keystore
// secrets are configured in the repo. If absent (local dev, or before
// secrets are set up), we fall back to the debug key so the build never fails.
val hasCiSigning = System.getenv("BQDIPTV_KEYSTORE_PATH") != null

android {
    namespace = "com.bqdiptv.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bqdiptv.tv"
        minSdk = 21
        targetSdk = 34
        versionCode = ciVersionCode
        versionName = ciVersionName

        // Used by the in-app updater to know where to look for releases.
        // CHANGE THIS to your GitHub "owner/repo" before publishing.
        buildConfigField("String", "GITHUB_REPO", "\"DaretsE/bqip\"")
    }

    if (hasCiSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(System.getenv("BQDIPTV_KEYSTORE_PATH")!!)
                storePassword = System.getenv("BQDIPTV_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("BQDIPTV_KEY_ALIAS")
                keyPassword = System.getenv("BQDIPTV_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasCiSigning) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.tv:tv-foundation:1.0.0-alpha11")

    // Media playback
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Persisted settings (playlist url, epg url, favourites)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Background update checks
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Networking for M3U/XMLTV/GitHub API/APK download
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Local HTTP server used for phone-based first-run setup (QR flow)
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // QR code generation for the TV-side setup screen
    implementation("com.google.zxing:core:3.5.3")

    implementation("androidx.leanback:leanback:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
