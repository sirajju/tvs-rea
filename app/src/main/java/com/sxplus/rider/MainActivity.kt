package com.sxplus.rider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sxplus.rider.data.local.SettingsStore
import com.sxplus.rider.data.local.SettingsStore.ThemeMode
import com.sxplus.rider.data.local.TokenStore
import com.sxplus.rider.ui.AppNav
import com.sxplus.rider.ui.BluetoothEnableGate
import com.sxplus.rider.ui.PermissionGate
import com.sxplus.rider.ui.theme.SxpTheme
import com.sxplus.rider.ui.theme.ThemeController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var themeController: ThemeController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = runCatching { runBlocking { settingsStore.settings.first() } }.getOrNull()
        val startOnHome = tokenStore.isLoggedIn || !settings?.pairedMac.isNullOrBlank()
        // Seed theme from persisted settings + the selected vehicle's accent.
        settings?.let { s ->
            themeController.setMode(s.themeMode)
            themeController.setAccent(s.vehicleAccents[s.selectedVehicleKey] ?: 0)
        }
        setContent {
            val mode by themeController.mode.collectAsState()
            val accentIdx by themeController.accent.collectAsState()
            val dark = when (mode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            SxpTheme(darkTheme = dark, accent = com.sxplus.rider.ui.theme.ACCENTS.getOrNull(accentIdx)) {
                PermissionGate {
                    BluetoothEnableGate {
                        AppNav(startLoggedIn = startOnHome)
                    }
                }
            }
        }
    }
}
