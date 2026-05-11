package ch.lottomatchapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ch.lottomatchapp.app.billing.BillingManager
import ch.lottomatchapp.app.billing.LocalBillingManager
import ch.lottomatchapp.app.data.PremiumRepository
import ch.lottomatchapp.app.ui.screens.LottoAppScreen
import ch.lottomatchapp.app.ui.theme.LottomatchTheme

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val premiumRepo = PremiumRepository.getInstance(applicationContext)
        billingManager = BillingManager(this, premiumRepo)
        billingManager.connect()

        setContent {
            CompositionLocalProvider(LocalBillingManager provides billingManager) {
                LottomatchTheme {
                    LottoAppScreen()
                }
            }
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
