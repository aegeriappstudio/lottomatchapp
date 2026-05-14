package ch.lottomatchapp.app.data

import android.content.Context
import ch.lottomatchapp.app.ui.theme.ThemeVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistiert die vom Nutzer gewählte Theme-Variante.
 *
 * Nutzer können Tombola nur auswählen wenn Premium aktiv ist —
 * das Gating passiert im ViewModel über die isPremium-Flow,
 * damit ein Refund/Downgrade automatisch zurück auf Classic fällt.
 */
class ThemeRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _selected = MutableStateFlow(readVariant())
    val selected: StateFlow<ThemeVariant> = _selected.asStateFlow()

    private fun readVariant(): ThemeVariant {
        val raw = prefs.getString(KEY_VARIANT, null) ?: return ThemeVariant.Classic
        return runCatching { ThemeVariant.valueOf(raw) }.getOrDefault(ThemeVariant.Classic)
    }

    fun setVariant(variant: ThemeVariant) {
        if (_selected.value == variant) return
        _selected.value = variant
        prefs.edit().putString(KEY_VARIANT, variant.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "ch.lottomatchapp.app.theme"
        private const val KEY_VARIANT = "variant"

        @Volatile private var INSTANCE: ThemeRepository? = null

        fun getInstance(context: Context): ThemeRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeRepository(context).also { INSTANCE = it }
            }
    }
}
