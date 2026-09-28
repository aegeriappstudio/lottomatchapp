# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep line numbers for readable Crashlytics stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Gson-serialised model classes ----
# Card states + round summaries are written/read as JSON via Gson
# (see StatisticsRepository). Gson reflects on field names, so minify
# would rename them and JSON would be unreadable after upgrade.
-keep class ch.lottomatchapp.app.stats.CardState { *; }
-keep class ch.lottomatchapp.app.stats.RoundStatistics { *; }
-keep class ch.lottomatchapp.app.stats.DailySummary { *; }
-keep class ch.lottomatchapp.app.stats.LocalDateAdapter { *; }

# Generic TypeToken usage in Gson.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# ---- Enums persisted via name() / valueOf() ----
# ConsentStatus is stored to SharedPreferences as its enum name.
-keepclassmembers enum ch.lottomatchapp.app.data.ConsentStatus {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
# ThemeVariant wird per name() in den SharedPreferences gespeichert und via valueOf() gelesen (ThemeRepository).
-keepclassmembers enum ch.lottomatchapp.app.ui.theme.ThemeVariant {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Konfetti / Glance / Room / Firebase / ML Kit / Play Billing all ship
# their own consumer-proguard-rules, no extra rules needed here.
