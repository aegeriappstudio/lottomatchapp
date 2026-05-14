# Play Console — Checkliste für Release 2.0

Alles was du in [play.google.com/console](https://play.google.com/console) erledigen musst, in der Reihenfolge die am wenigsten Hin-und-Her verursacht.

## 1. In-App-Product „lottomatch_premium" anlegen

Pflicht, sonst funktioniert der Kauf-Flow in der App nicht.

1. Play Console → deine App → links **„Monetization"** → **„Products"** → **„In-app products"**
2. **„Create product"**
3. Product details:
   - **Product ID**: `lottomatch_premium` (exakt so — steht hardcoded in [BillingManager.kt:28](app/src/main/java/ch/lottomatchapp/app/billing/BillingManager.kt:28))
   - **Name**: „Lottomatch Premium"
   - **Description**: „Up to 30 cards and 1–90 frequency heatmap. One-time purchase, no subscription."
4. **Pricing** → Preis setzen — Vorschlag CHF 4.90 (€4.99 / $4.99) als Anker. Play berechnet die Lokalpreise automatisch.
5. **Save** → **Activate**

Bis das Product „active" ist (kann 1–2 Stunden dauern), liefert die App in der Paywall keinen Preis (Button bleibt auf „Verbinde mit Play Store…").

## 2. License-Tester einrichten

Damit du den Kauf-Flow testen kannst, ohne echtes Geld zu zahlen.

1. Play Console → ganz links **„Setup"** → **„License testing"**
2. Bei „License testers": deine Google-Konto-E-Mail eintragen (die Mail, mit der das Test-Gerät bei Google Play eingeloggt ist)
3. **„License response"** auf `RESPOND_NORMALLY` lassen
4. Save

Tester sehen IAPs als **„Test card, always approves"** im Play-Dialog — kein echter Abzug.

## 3. Data-Safety-Formular ausfüllen

Pflicht in Play seit 2022.

1. Play Console → **„App content"** → **„Data safety"** → **„Manage"**
2. **„Data collection and security"**:
   - „Does your app collect or share any of the required user data types?" → **Yes**
3. **Data types** auswählen — ankreuzen was deine App über Firebase einsammelt:
   - Unter **„App activity"**:
     - **„App interactions"** ✓ (Analytics-Events wie card_added, paywall_shown)
   - Unter **„App info and performance"**:
     - **„Crash logs"** ✓ (Crashlytics)
     - **„Diagnostics"** ✓ (Firebase Performance, falls aktiv)
4. Pro Daten-Typ:
   - „Is this data collected, shared, or both?" → **Collected** (Google sammelt; du als Entwickler siehst aggregierte Daten)
   - „Is this data processed ephemerally?" → **No** (Firebase speichert bis 14 Monate)
   - „Is data collection required or optional?" → **Optional** (wegen unserem Consent-Flow)
   - „What is this data used for?" → **Analytics** + **App functionality** (für Crash logs)
5. **„Data security"**:
   - „Is data encrypted in transit?" → **Yes** (Firebase nutzt HTTPS)
   - „Can users request data deletion?" → **Yes** (Privacy-Settings + E-Mail an aegeriappstudio@outlook.com)
6. **Save** → submit für Review.

Es kann sein, dass Play 24h für die Review braucht.

## 4. Privacy-Policy-URL setzen

Beim Data-Safety-Schritt verlangt Play eine öffentliche Privacy-Policy-URL.

1. Play Console → **„App content"** → **„Privacy Policy"**
2. URL: `https://github.com/aegeriappstudio/lottomatchapp/wiki/Datenschutz-Lottomatch-App` (oder ein eigener Host)
3. **Save**

Wichtig: erst die [neue Datenschutzerklärung](privacy-policy-de.md) ins Wiki einpflegen, dann diese URL bestätigen — Play crawlt die Seite.

## 5. App-Inhaltliche Pflichtangaben

- **„Target audience and content"** → „13 and over" (nicht „Children" — sonst extra Auflagen)
- **„Ads"** → „No, my app does not contain ads" (du hast keine Ads)
- **„Government apps"** → No
- **„Financial features"** → No (Lottomatch ist kein Wett-/Glücksspiel-Tool, sondern ein Helfer — wichtig in der Beschreibung klar zu machen!)
- **„Health"** → No
- **„News"** → No

## 6. Listing-Texte in 15 Sprachen

Aus [docs/play-store-listing.md](play-store-listing.md) pro Sprache:
- **Title** (max 30)
- **Short Description** (max 80)
- **Full Description** (max 4000)

Vorgehen:
1. Play Console → **„Store presence"** → **„Main store listing"**
2. Oben links Sprache wählen → **„Add translation"** für jede neue Locale
3. Title, Short, Full einfügen → **Save**
4. Weiter mit der nächsten Sprache

Falls du nicht alle 15 sofort machen willst: **EN-US und DE-DE reichen für den Launch**, der Rest fällt auf EN zurück. Die App-Innenoberfläche ist trotzdem in allen 15 Sprachen verfügbar.

## 7. Grafiken

Falls du nicht alles neu machst, behalte was du für 1.11 hattest. Was Play 2025+ verlangt:
- **App icon**: 512×512 PNG (haben wir wahrscheinlich schon)
- **Feature graphic**: 1024×500 PNG (sichtbar im Play-Listing oben)
- **Phone screenshots**: mind. 2, max 8, mind. 320 px breit
- **Tablet screenshots** (optional, aber empfohlen für 7" und 10")

Tipp: aus dem aktuellen Emulator kannst du 4–5 frische Screenshots ziehen, die das neue Design zeigen (Tape, Number Pad, Win-Reveal, Heatmap, Premium-Sheet). Lokalisierte Screenshots sind nice-to-have, aber EN/DE reicht.

## 8. AAB hochladen

1. Lokal: Signed AAB bauen wie in [docs/release-build.md](release-build.md) beschrieben
2. Play Console → links **„Release"** → **Internal testing** (für den ersten Run!) → **„Create new release"**
3. AAB-Datei hochladen
4. **Release notes** einfügen (siehe Vorlage in `release-build.md`)
5. **„Review release"** → **„Start rollout to Internal testing"**

## 9. Auf Tester-Gerät installieren und durchspielen

1. Internal-Testing-Tester sind dieselben Mail-Adressen wie deine License-Tester
2. Im Internal-Testing-Tab den **Opt-in-Link** kopieren → auf dem Test-Gerät öffnen → „Become a tester"
3. Play Store öffnen → Lottomatch updated als 2.0 → installieren
4. Checkliste durchspielen:
   - [ ] App startet ohne Crash
   - [ ] Consent-Sheet erscheint, beide Buttons funktionieren
   - [ ] Karten hinzufügen bis Limit (3) → Tap auf + öffnet Paywall
   - [ ] Premium kaufen mit Test-Card → Limit erhöht auf 30, Heatmap sichtbar
   - [ ] Scan einer realen Lotto-Karte funktioniert
   - [ ] Statistik → Heatmap rendert
   - [ ] Widget auf Home-Screen platzieren → Daten erscheinen
   - [ ] Sprache am Gerät auf z.B. Französisch wechseln → App ist FR
   - [ ] Logcat: keine Firebase-Init-Fehler

## 10. Rollout

Wenn Internal-Test rund läuft:
1. Play Console → **„Promote release"** → **Closed testing** (kleinere Gruppe) oder direkt **Production**
2. Bei Production: **Staged rollout** (z.B. 20% → 50% → 100% über mehrere Tage)

## Common Rejection Reasons (avoid these)

- **Gambling content review**: Lottomatch hat „Lotto" im Namen, kann automatische Reviews auslösen. In der Description **explizit** klarstellen: „This app is a helper to track your existing bingo/lotto game cards. It does not offer betting or gambling functionality."
- **Privacy policy mismatch**: Data-Safety-Daten und die Datenschutz-Wiki-Seite müssen konsistent sein. Wenn du Analytics als „collected" deklarierst, muss das auch in der Privacy Policy stehen — das ist jetzt der Fall.
- **Missing 16-KB-Compliance**: erledigt, siehe [Schritt 4-Commit](release-build.md).
- **Untranslated mandatory strings**: alle 15 Sprachen sind in der App, im Listing reichen EN+DE für Launch.

## Was ich nicht für dich machen kann

- **Firebase Console → google-services.json** (du hast das schon)
- **Play Console** — bedarf deines Logins; alle Eintragungen sind manuell
- **Echte Screenshots** — können wir gemeinsam machen, aber du musst sie aus dem Gerät ziehen
- **Echter Keystore-Zugriff** — der bleibt bei dir

Sag Bescheid wenn etwas davon stockt.
