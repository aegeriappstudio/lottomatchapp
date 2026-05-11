package ch.lottomatchapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ch.lottomatchapp.app.ui.screens.LottoAppScreen
import ch.lottomatchapp.app.ui.theme.LottomatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            // Das Theme kümmert sich bereits um Hell/Dunkel-Modus
            LottomatchTheme {
                LottoAppScreen()
            }
        }
    }
}
