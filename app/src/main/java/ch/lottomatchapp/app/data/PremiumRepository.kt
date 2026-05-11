package ch.lottomatchapp.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistierter Premium-Flag. Wird vom BillingManager nach Käufen / Restore gesetzt.
 * UI/ViewModel lesen den StateFlow.
 *
 * Hinweis: rein lokaler Cache. Source of truth ist Google Play (queryPurchasesAsync
 * läuft bei jedem App-Start). Lokaler Bypass über editieren der SharedPreferences
 * wäre möglich aber zu Aufwand verhältnismäßig — Play Billing-Setup mit Echtzeit-
 * Validierung wäre der nächste Schritt wenn die Pirat-Quote relevant wird.
 */
class PremiumRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isPremium = MutableStateFlow(prefs.getBoolean(KEY_PREMIUM, false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun setPremium(value: Boolean) {
        if (_isPremium.value == value) return
        _isPremium.value = value
        prefs.edit().putBoolean(KEY_PREMIUM, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "ch.lottomatchapp.app.premium"
        private const val KEY_PREMIUM = "is_premium"

        @Volatile private var INSTANCE: PremiumRepository? = null

        fun getInstance(context: Context): PremiumRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PremiumRepository(context).also { INSTANCE = it }
            }
    }
}
