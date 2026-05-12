package ch.lottomatchapp.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConsentStatus { Undefined, Granted, Denied }

/**
 * Persistiert die Nutzerentscheidung zu Analytics/Crashlytics.
 * - Undefined: ConsentSheet beim nächsten App-Start zeigen.
 * - Granted: Analytics + Crashlytics werden zur Laufzeit aktiviert.
 * - Denied: bleibt aus (Manifest-Default).
 */
class ConsentRepository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(readStatus())
    val status: StateFlow<ConsentStatus> = _status.asStateFlow()

    private fun readStatus(): ConsentStatus {
        val raw = prefs.getString(KEY_STATUS, null) ?: return ConsentStatus.Undefined
        return runCatching { ConsentStatus.valueOf(raw) }.getOrDefault(ConsentStatus.Undefined)
    }

    fun setStatus(status: ConsentStatus) {
        if (_status.value == status) return
        _status.value = status
        prefs.edit().putString(KEY_STATUS, status.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "ch.lottomatchapp.app.consent"
        private const val KEY_STATUS = "status"

        @Volatile private var INSTANCE: ConsentRepository? = null

        fun getInstance(context: Context): ConsentRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConsentRepository(context).also { INSTANCE = it }
            }
    }
}
