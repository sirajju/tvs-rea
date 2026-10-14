package com.sxplus.rider.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxplus.rider.ble.BleState
import com.sxplus.rider.ui.theme.Ash
import com.sxplus.rider.ui.theme.CarbonCard
import com.sxplus.rider.ui.theme.Danger
import com.sxplus.rider.ui.theme.Success

@Composable
fun DashboardScreen(
    onOpenFeature: (String) -> Unit,
    onOpenScan: () -> Unit,
    onLoggedOut: () -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val bleState by vm.bleState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.toast) {
        ui.toast?.let { snackbar.showSnackbar(it); vm.clearToast() }
    }
    // Auto-start proximity watcher if enabled.
    LaunchedEffect(ui.autoConnect, ui.selected?.bleMac) {
        if (ui.autoConnect && ui.selected?.bleMac != null) vm.startProximity()
    }
    // Already paired -> reconnect automatically when the bike is available.
    LaunchedEffect(ui.selected?.bleMac) {
        val mac = ui.selected?.bleMac
        if (mac != null && bleState != BleState.READY) vm.connect()
    }
    // Not paired yet -> show the scan list directly (one-shot, no loop).
    var autoScanned by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ui.loading, ui.vehicles.isEmpty()) {
        if (!ui.loading && ui.vehicles.isEmpty() && !autoScanned) {
            autoScanned = true
            onOpenScan()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            TopBar(
                userName = ui.user?.name ?: "Rider",
                onScan = onOpenScan,
                onLogout = { vm.logout(); onLoggedOut() },
            )
            Spacer(Modifier.height(16.dp))

            VehicleCard(
                name = ui.selected?.displayName ?: "No vehicle",
                reg = ui.selected?.registrationNumber,
                model = ui.selected?.model,
                bleState = bleState,
                onConnectToggle = {
                    val mac = ui.selected?.bleMac
                    when {
                        mac == null -> onOpenScan()                       // not paired -> show list
                        bleState == BleState.READY -> vm.disconnect()
                        else -> vm.connect()                              // paired -> reconnect
                    }
                },
            )

            if (ui.selected?.bleMac == null) {
                Spacer(Modifier.height(16.dp))
                com.sxplus.rider.ui.components.PrimaryButton(
                    text = "Scan & pair your vehicle",
                    onClick = onOpenScan,
                    icon = Icons.Default.BluetoothSearching,
                )
            }
            Spacer(Modifier.height(24.dp))

            KeylessControls(
                enabled = ui.selected?.bleMac != null,
                sending = ui.sendingCommand,
                onCommand = vm::sendCommand,
            )
            Spacer(Modifier.height(24.dp))

            ProximityCard(
                autoConnect = ui.autoConnect,
                autoAction = ui.autoActionOnApproach,
                approachCmdLabel = ui.approachCommand.label,
                onAutoConnect = vm::setAutoConnect,
                onAutoAction = vm::setAutoActionOnApproach,
            )
            Spacer(Modifier.height(24.dp))

            FeatureGrid(onOpenFeature)
            Spacer(Modifier.height(24.dp))

            DebugConsole(vm)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TopBar(userName: String, onScan: () -> Unit, onLogout: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Welcome back", color = Ash, style = MaterialTheme.typography.bodyMedium)
            Text(userName, fontSize = 24.sp, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground)
        }
        IconButton(onClick = onScan) {
            Icon(Icons.Default.BluetoothSearching, "Pair / scan", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onLogout) {
            Icon(Icons.Default.Logout, "Logout", tint = Ash)
        }
    }
}

@Composable
private fun VehicleCard(
    name: String,
    reg: String?,
    model: String?,
    bleState: BleState,
    onConnectToggle: () -> Unit,
) {
    val (badgeColor, badgeText) = when (bleState) {
        BleState.READY -> Success to "Connected"
        BleState.CONNECTING, BleState.DISCOVERING, BleState.SCANNING -> MaterialTheme.colorScheme.secondary to "Connecting…"
        BleState.FAILED -> Danger to "Failed"
        else -> Ash to "Disconnected"
    }
    Surface(color = CarbonCard, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.TwoWheeler, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(reg ?: model ?: "", color = Ash, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.sxplus.rider.ui.components.StatusDot(badgeColor)
                Spacer(Modifier.size(8.dp))
                Text(badgeText, color = badgeColor, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { onConnectToggle() },
                ) {
                    Row(
                        Modifier
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (bleState == BleState.READY) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                            null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            if (bleState == BleState.READY) "Disconnect" else "Connect",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProximityCard(
    autoConnect: Boolean,
    autoAction: Boolean,
    approachCmdLabel: String,
    onAutoConnect: (Boolean) -> Unit,
    onAutoAction: (Boolean) -> Unit,
) {
    Surface(color = CarbonCard, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Smart Proximity", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text("Auto-connect & act when you walk up to your bike",
                color = Ash, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            ToggleRow("Auto-connect nearby", autoConnect, onAutoConnect)
            Spacer(Modifier.height(10.dp))
            ToggleRow("Auto “$approachCmdLabel” on approach", autoAction, onAutoAction)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun DebugConsole(vm: DashboardViewModel) {
    val logs by vm.bleLogs.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(color = Color(0xFF0A0C0E), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            ) {
                Text("BLE Debug Console", color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (expanded) "Hide" else "Show (${logs.size})", color = Ash)
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    logs.takeLast(40).forEach { line ->
                        Text(
                            "${line.tag}: ${line.text}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = when {
                                line.tag.startsWith("TX") -> MaterialTheme.colorScheme.secondary
                                line.tag.startsWith("RX") -> Success
                                line.tag.startsWith("CMD") -> MaterialTheme.colorScheme.primary
                                else -> Ash
                            },
                        )
                    }
                }
            }
        }
    }
}
