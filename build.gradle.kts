plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.7" apply false
    alias(libs.plugins.kotlin.compose) apply false
}