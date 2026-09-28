plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
}

// Firebase-Plugins nur aktivieren wenn google-services.json vorhanden ist.
// So lässt sich das Projekt auch ohne Firebase-Setup bauen; sobald du
// die Datei in app/ ablegst, ziehen Plugin und Crashlytics-Mapping
// beim nächsten Build automatisch an. Die Plugin-IDs kommen aus dem
// Version-Catalog; auf dem Classpath liegen sie über das Root-Build-Script.
val hasFirebaseConfig = project.file("google-services.json").exists()
if (hasFirebaseConfig) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

android {
    namespace = "ch.lottomatchapp.app"
    // Compose 1.12 / core 1.19 / lifecycle 2.11 verlangen compileSdk 37.
    // targetSdk bleibt bewusst auf 36 (kein neues Runtime-Verhalten).
    compileSdk = 37

    defaultConfig {
        applicationId = "ch.lottomatchapp.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 18
        versionName = "2.1"
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
    implementation(platform(libs.androidx.compose.bom))

    // ---- Compose & AndroidX ----
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    // ---- CameraX (einheitliche Versionen über das Bundle) ----
    implementation(libs.bundles.androidx.camera)

    // ---- ML Kit: On-device Text Recognition (Latin) ----
    // Wichtig: KEIN play-services-* Pendant zusätzlich mischen.
    implementation(libs.mlkit.text.recognition)

    // ---- Konfetti animation ----
    implementation(libs.konfetti.compose)

    // ---- Glance (Home-Screen Widget) ----
    // GlanceTheme kommt aus androidx.glance (Core); glance-material3 wird nicht benötigt.
    implementation(libs.androidx.glance.appwidget)

    // ---- Google Play Billing (In-App Purchases) ----
    implementation(libs.billing.ktx)

    // ---- Firebase (Crashlytics + Analytics) ----
    // Die Dependencies werden immer eingezogen; das eigentliche Aktivieren
    // passiert per Plugin oben (nur wenn google-services.json existiert).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    // ---- Room Database ----
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ---- Gson for JSON serialization (legacy, used for Room migration) ----
    implementation(libs.gson)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // ---- Testing ----
    testImplementation(libs.junit)
}
