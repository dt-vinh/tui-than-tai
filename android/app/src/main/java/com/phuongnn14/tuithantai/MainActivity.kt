package com.phuongnn14.tuithantai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.phuongnn14.tuithantai.ui.LuckyWalletApp
import com.phuongnn14.tuithantai.ui.LuckyWalletViewModel
import com.phuongnn14.tuithantai.ui.theme.TuiThanTaiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LuckyWalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen phải được gọi TRƯỚC super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TuiThanTaiTheme {
                LuckyWalletApp(viewModel = viewModel)
            }
        }
    }
}
