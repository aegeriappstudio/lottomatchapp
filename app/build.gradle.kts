plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
}

// Firebase-Plugins nur aktivieren wenn google-services.json vorhanden ist.
// So lässt sich das Projekt auch ohne Firebase-Setup bauen; sobald du
// die Datei in app/ ablegst, ziehen Plugin und Crashlytics-Mapping
// beim nächsten Build automatisch an.
val hasFirebaseConfig = project.file("google-services.json").exists()
if (hasFirebaseConfig) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "ch.lottomatchapp.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "ch.lottomatchapp.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 16
        versionName = "2.0"
        // multiDexEnabled = true // nur nötig, falls du wirklich >64k Methoden brauchst

        // Nur 64-Bit-ABIs ausliefern.
        // - armeabi-v7a / x86 sind weltweit <5% (in DACH praktisch 0%)
        // - Wichtig für 16-KB-Page-Size: ML Kit 16.0.1 liefert die
        //   32-Bit-.so-Dateien noch mit 4-KB-LOAD-Alignment, die 64-Bit-
        //   Varianten sind bereits 16 KB. Durch das Wegfiltern erfüllt
        //   das AAB die Play-Anforderung ab 1. Nov 2025.
        ndk {
            abiFilters += setOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Native-Debug-Symbole (ML Kit, CameraX) ins AAB einpacken,
            // damit Play / Crashlytics native Crashes deobfuscaten können.
            // SYMBOL_TABLE = klein (Funktionsnamen), FULL = Voll-Symbole.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
        // Debug: keep defaults (no minify, debuggable, etc.)
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
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // ---- Google Play Billing (In-App Purchases) ----
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    // ---- Firebase (Crashlytics + Analytics) ----
    // Die Dependencies werden immer eingezogen; das eigentliche Aktivieren
    // passiert per Plugin oben (nur wenn google-services.json existiert).
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

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
