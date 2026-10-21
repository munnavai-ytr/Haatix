package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.SettingsManager
import com.example.service.GestureService
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    HOME,
    GUIDE,
    PRIVACY
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsManager = SettingsManager.getInstance(this)

        setContent {
            MyApplicationTheme {
                val isServiceRunning by GestureService.isServiceRunning.collectAsState()
                val settings by settingsManager.settings.collectAsState()

                // Keep screen on in MainActivity if enabled and running
                LaunchedEffect(isServiceRunning, settings.keepScreenOn) {
                    if (isServiceRunning && settings.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkNavyBackground
                ) {
                    var currentScreen by remember { mutableStateOf(Screen.HOME) }

                    when (currentScreen) {
                        Screen.HOME -> {
                            HomeScreen(
                                onNavigateToGuide = { currentScreen = Screen.GUIDE },
                                onNavigateToPrivacy = { currentScreen = Screen.PRIVACY }
                            )
                        }
                        Screen.GUIDE -> {
                            GuideScreen(
                                onBackClick = { currentScreen = Screen.HOME }
                            )
                        }
                        Screen.PRIVACY -> {
                            PrivacyScreen(
                                onBackClick = { currentScreen = Screen.HOME }
                            )
                        }
                    }
                }
            }
        }
    }
}
