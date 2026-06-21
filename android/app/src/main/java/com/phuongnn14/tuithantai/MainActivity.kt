package com.phuongnn14.tuithantai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.phuongnn14.tuithantai.ui.LuckyWalletApp
import com.phuongnn14.tuithantai.ui.LuckyWalletViewModel
import com.phuongnn14.tuithantai.ui.theme.TuiThanTaiTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen phải được gọi TRƯỚC super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TuiThanTaiTheme {
                var showApp by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    showApp = true
                }

                if (showApp) {
                    val appViewModel: LuckyWalletViewModel = viewModel()
                    LuckyWalletApp(viewModel = appViewModel)
                } else {
                    FastLaunchShell()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun FastLaunchShell() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF020716)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = null,
            modifier = Modifier.size(132.dp)
        )
    }
}
