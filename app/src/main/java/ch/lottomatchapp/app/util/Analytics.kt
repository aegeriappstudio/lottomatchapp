package ch.lottomatchapp.app.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

private const val TAG = "Analytics"

/**
 * Dünner Wrapper um Firebase Analytics und Crashlytics.
 *
 * Funktioniert nur wenn `app/google-services.json` vorhanden ist und das
 * Google-Services-Plugin beim Build aktiv war. Ohne das fällt jeder Aufruf
 * still durch — die App läuft trotzdem normal weiter.
 *
 * `init` muss aus der Application bzw. der Launch-Activity einmal aufgerufen
 * werden.
 */
object Analytics {

    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null

    fun init(context: Context) {
        if (analytics != null) return
        try {
            FirebaseApp.initializeApp(context.applicationContext) ?: return
            analytics = FirebaseAnalytics.getInstance(context.applicationContext)
            crashlytics = FirebaseCrashlytics.getInstance()
        } catch (e: Throwable) {
            // google-services.json fehlt oder ist kaputt — Firebase bleibt off.
            Log.w(TAG, "Firebase not configured — analytics + crashlytics disabled.", e)
        }
    }

    // ---- Analytics ----

    fun logEvent(name: String, params: Bundle.() -> Unit = {}) {
        analytics?.logEvent(name, Bundle().apply(params))
    }

    fun setUserProperty(name: String, value: String?) {
        analytics?.setUserProperty(name, value)
    }

    // ---- Crashlytics ----

    /** Non-fatal Fehler loggen (App stürzt nicht ab). */
    fun recordError(throwable: Throwable, message: String? = null) {
        val cl = crashlytics ?: return
        message?.let { cl.log(it) }
        cl.recordException(throwable)
    }

    /** Breadcrumb für künftige Crash-Reports. */
    fun log(message: String) {
        crashlytics?.log(message)
    }

    // ---- App-spezifische Events ----
    // Praktische Wrapper damit die Event-Namen an einer Stelle gepflegt sind.

    fun cardAdded(cardCount: Int) = logEvent("card_added") {
        putInt("card_count", cardCount)
    }

    fun cardScanned() = logEvent("card_scanned")

    fun roundCompleted(drawnCount: Int, winnersCount: Int) = logEvent("round_completed") {
        putInt("drawn_count", drawnCount)
        putInt("winners_count", winnersCount)
    }

    fun paywallShown(trigger: String) = logEvent("paywall_shown") {
        putString("trigger", trigger)
    }

    fun premiumPurchased() = logEvent("premium_purchased")

    fun premiumRestored() = logEvent("premium_restored")
}
