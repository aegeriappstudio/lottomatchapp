# Signed Release-AAB bauen

Das ist eine zusammengefasste Anleitung für den Upload nach Google Play.

## Voraussetzungen

- Dein bestehender **Release-Keystore** (`*.jks`) — der gleiche, mit dem du v1.11 unterschrieben hast. Play verlangt fortlaufend denselben Signing-Key.
- Keystore-Passwort + Alias + Alias-Passwort
- `app/google-services.json` liegt vor (Firebase-Config); sonst ist der Build trotzdem OK, aber Analytics + Crashlytics sind off.

## Option A: Android Studio UI (empfohlen, einfach)

1. **Build** → **Generate Signed App Bundle / APK…**
2. **Android App Bundle** wählen → Next
3. Keystore-Pfad, Passwort, Alias eingeben → Next
4. Build-Type: **release** → Finish
5. AAB landet in `app/release/app-release.aab`

## Option B: CLI

Voraussetzung dafür: Signing-Config in der Gradle-Konfiguration (haben wir aktuell **nicht** drin gelassen, weil das Keystore-Passwörter im File braucht).

Wenn du das willst, lege `keystore.properties` im Projekt-Root an (ist bereits in `.gitignore`, Stand der nächsten Sitzung):

```properties
storeFile=C:/Users/oacada/keystores/lottomatch-release.jks
storePassword=…
keyAlias=lottomatch
keyPassword=…
```

Und in `app/build.gradle.kts` vor `defaultConfig` einfügen:

```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = java.util.Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

android {
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    // ...

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            // ...
        }
    }
}
```

Dann:

```
gradlew bundleRelease
```

Die AAB landet in `app/build/outputs/bundle/release/app-release.aab`.

## Was beim Bauen automatisch passiert

- **R8 Minification + Resource Shrinking** — APK wird ~40% kleiner (debug 48 MB → release 28 MB).
- **Crashlytics-Mapping-Upload**: das Plugin lädt das ProGuard-Mapping automatisch zu Firebase hoch, damit Crash-Reports lesbar sind.
- **Native-Libs**: nur `arm64-v8a` und `x86_64` (durch `abiFilters`), alle 16‑KB-page-aligned.

## Upload zu Google Play

1. **Play Console** → deine App → **Production** (oder erst **Internal testing** wenn du auf Tester-Track willst)
2. **Neue Version erstellen** → AAB hochladen
3. **Release notes** ausfüllen (pro Sprache; Vorlage siehe weiter unten)
4. **Review** → **Save** → **Rollout**

### Release-Notes-Vorlage (für versionCode 12 / 2.0)

**Deutsch (max 500 Zeichen)**
```
Version 2.0 — grosses Update:
• Komplett überarbeitete Oberfläche mit 1–90-Tippgitter, Heatmap, Swipe-Aktionen
• Neu: Premium-Upgrade für bis zu 30 Karten und erweiterte Statistik
• Homescreen-Widget mit der besten Karte
• 15 Sprachen statt 2
• Optionale anonyme Absturz- und Nutzungsanalyse (Opt-in, jederzeit abschaltbar)
• 16-KB-Pages: bereit für Android 15+
```

**Englisch**
```
Version 2.0 — major update:
• Redesigned interface with 1–90 tap grid, heatmap, swipe actions
• New: Premium upgrade for up to 30 cards and advanced statistics
• Home-screen widget showing your best card
• 15 languages (was 2)
• Optional anonymous crash + usage analytics (opt-in, can be turned off anytime)
• 16-KB-page support for Android 15+
```

## Häufige Build-Probleme

- **„No matching client found for package name"** beim Build → `google-services.json` enthält den falschen Package-Namen. Im Firebase Console korrigieren (siehe frühere Sitzung) und Datei neu runterladen.
- **„Crashlytics: mapping file already uploaded"** → harmlos, ignorieren.
- **`assembleDebug` failed mit „input file does not exist"** nach CLI-Eingriff → in Android Studio einmal **Build → Clean Project**, dann normal weitermachen.
- **R8 Crash zur Laufzeit** (NPE / NoClassDefFound) in einer von uns nicht abgedeckten Stelle → in `proguard-rules.pro` eine `-keep class …`-Regel adden.
