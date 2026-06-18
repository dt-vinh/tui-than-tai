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
        val splashScreen = installSplashScreen()
        val splashStartMs = System.currentTimeMillis()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Giữ splash screen cho đến khi ViewModel load settings xong
        // → user thấy icon app ngay lập tức thay vì màn hình trắng
        splashScreen.setKeepOnScreenCondition {
            !viewModel.isAppReady.value &&
                System.currentTimeMillis() - splashStartMs < MAX_SPLASH_WAIT_MS
        }

        setContent {
            TuiThanTaiTheme {
                LuckyWalletApp(viewModel = viewModel)
            }
        }
    }

    private companion object {
        const val MAX_SPLASH_WAIT_MS = 700L
    }
}
