package com.sxplus.rider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sxplus.rider.data.local.SettingsStore
import com.sxplus.rider.data.local.TokenStore
import com.sxplus.rider.ui.AppNav
import com.sxplus.rider.ui.BluetoothEnableGate
import com.sxplus.rider.ui.PermissionGate
import com.sxplus.rider.ui.theme.SxpTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Land on the dashboard if signed in OR a bike was already paired directly.
        val pairedMac = runBlocking { settingsStore.settings.first().pairedMac }
        val startOnHome = tokenStore.isLoggedIn || !pairedMac.isNullOrBlank()
        setContent {
            SxpTheme {
                PermissionGate {
                    BluetoothEnableGate {
                        AppNav(startLoggedIn = startOnHome)
                    }
                }
            }
        }
    }
}
