plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.armeasure.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.armeasure.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Optional single-ABI build for a much smaller APK (arm64-v8a only):
        //   ./gradlew :app:assembleRelease -Parm64Only
        // Default build stays universal (all ABIs).
        if (project.hasProperty("arm64Only")) {
            ndk {
                abiFilters.add("arm64-v8a")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // RecyclerView (measurement history list)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Gson (measurement history JSON serialization)
    implementation("com.google.code.gson:gson:2.10.1")

    // ARCore
    implementation("com.google.ar:core:1.41.0")

    // AR Sceneform (maintained fork for easier AR rendering)
    implementation("io.github.sceneview:arsceneview:2.0.3")

    // CameraX (for camera frame processing)
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // OpenCV for Android (edge detection)
    implementation("org.opencv:opencv:4.9.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
