plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}

android {
    namespace = "ch.lottomatchapp.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ch.lottomatchapp.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 11
        versionName = "1.11"
        // multiDexEnabled = true // nur nötig, falls du wirklich >64k Methoden brauchst
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ---- Compose BOM (steuert Versionen der Compose-Artefakte zentral) ----
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // ---- Compose & AndroidX ----
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")


    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-tooling-preview")

    // ---- CameraX (einheitliche Versionen!) ----
    val cameraxVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ---- ML Kit: On-device Text Recognition (Latin) ----
    // Wichtig: KEIN play-services-* Pendant zusätzlich mischen.
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // ---- Konfetti animation ----
    implementation("nl.dionsegijn:konfetti-compose:2.0.4")

    // ---- Glance (Home-Screen Widget) ----
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // ---- Room Database ----
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // ---- Gson for JSON serialization (legacy, used for Room migration) ----
    implementation("com.google.code.gson:gson:2.10.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // ---- Testing ----
    testImplementation("junit:junit:4.13.2")
}
