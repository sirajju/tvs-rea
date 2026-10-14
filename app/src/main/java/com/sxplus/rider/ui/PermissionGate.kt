package com.sxplus.rider.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.sxplus.rider.ui.components.PrimaryButton
import com.sxplus.rider.ui.components.ScreenScaffold

/**
 * Requests the runtime permissions the keyless + proximity features need.
 * Renders [content] once all required permissions are granted.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val perms = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val state = rememberMultiplePermissionsState(perms)

    if (state.allPermissionsGranted) {
        content()
    } else {
        ScreenScaffold { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("Permissions needed", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Bluetooth and location are required to detect, auto-connect and send keyless commands to your vehicle. Notifications power the proximity watcher.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Grant permissions", onClick = { state.launchMultiplePermissionRequest() })
            }
        }
    }
}
