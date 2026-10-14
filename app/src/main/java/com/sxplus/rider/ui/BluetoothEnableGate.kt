package com.sxplus.rider.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sxplus.rider.ui.components.PrimaryButton
import com.sxplus.rider.ui.components.ScreenScaffold

private fun isBtOn(context: Context): Boolean {
    val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    return adapter?.isEnabled == true
}

/**
 * Ensures Bluetooth is ON before showing [content]. If it is off, prompts the
 * user with the system enable dialog (ACTION_REQUEST_ENABLE) on open.
 */
@Composable
fun BluetoothEnableGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(isBtOn(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { enabled = isBtOn(context) }

    // Auto-prompt once when we arrive and BT is off.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!enabled) launcher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

    if (enabled) {
        content()
    } else {
        ScreenScaffold { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.BluetoothDisabled, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("Bluetooth is off", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(10.dp))
                Text("SmartXonnect+ needs Bluetooth to detect and connect to your vehicle.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Turn on Bluetooth",
                    onClick = { launcher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) })
            }
        }
    }
}
