package ch.lottomatchapp.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.lottomatchapp.app.billing.BillingManager
import ch.lottomatchapp.app.billing.LocalBillingManager
import ch.lottomatchapp.app.data.ConsentRepository
import ch.lottomatchapp.app.data.ConsentStatus
import ch.lottomatchapp.app.data.PremiumRepository
import ch.lottomatchapp.app.ui.screens.LottoAppScreen
import ch.lottomatchapp.app.ui.theme.LottomatchTheme
import ch.lottomatchapp.app.util.Analytics

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Edge-to-edge mit transparenten System-Bars; Scrim wird automatisch
        // gewählt je nach Helligkeit des Hintergrunds (Theme-aware).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        Analytics.init(applicationContext)
        // Tracking nur einschalten wenn bereits zugestimmt wurde (sonst bleibt
        // es per Manifest-Default off). ConsentSheet erscheint später aus der UI.
        val consentRepo = ConsentRepository.getInstance(applicationContext)
        Analytics.applyConsent(consentRepo.status.value == ConsentStatus.Granted)

        val premiumRepo = PremiumRepository.getInstance(applicationContext)
        billingManager = BillingManager(this, premiumRepo)
        billingManager.connect()

        setContent {
            CompositionLocalProvider(LocalBillingManager provides billingManager) {
                LottomatchApp()
            }
        }
    }

    @Composable
    private fun LottomatchApp(viewModel: LottoViewModel = viewModel()) {
        // Theme-Variante kommt aus dem ViewModel; gated über Premium so dass
        // nicht-Premium-Nutzer auch dann Classic sehen, wenn ein Premium-Theme gespeichert war.
        val themeVariant by viewModel.themeVariant.collectAsStateWithLifecycle()
        LottomatchTheme(themeVariant = themeVariant) {
            LottoAppScreen(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        // Käufe nach App-Resume re-validieren (z.B. nach externer Erstattung)
        if (::billingManager.isInitialized) {
            billingManager.queryPurchases()
        }
    }

    override fun onDestroy() {
        if (::billingManager.isInitialized) {
            billingManager.disconnect()
        }
        super.onDestroy()
    }
}
