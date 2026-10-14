package com.sxplus.rider.ui.screens.scan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxplus.rider.ble.ScannedDevice
import com.sxplus.rider.ui.theme.Ash
import com.sxplus.rider.ui.theme.CarbonCard
import com.sxplus.rider.ui.theme.Success
import kotlinx.coroutines.delay

private const val SCAN_WINDOW_MS = 20_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onPaired: () -> Unit,
    vm: ScanViewModel = hiltViewModel(),
) {
    val devices by vm.devices.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    var showAll by remember { mutableStateOf(false) }
    var scanNonce by remember { mutableIntStateOf(0) }

    // Scan in bounded windows so it isn't "unstoppable". Rescan bumps scanNonce.
    LaunchedEffect(scanNonce) {
        vm.startScan()
        delay(SCAN_WINDOW_MS)
        vm.stopScan()
    }
    DisposableEffect(Unit) { onDispose { vm.stopScan() } }

    val vehicles = devices.filter { it.looksLikeVehicle || it.bonded }
    val shown = if (showAll) devices else vehicles
    val hiddenCount = devices.size - vehicles.size

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Find my vehicle") },
                navigationIcon = {
                    IconButton(onClick = { vm.stopScan(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (scanning) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.size(12.dp))
                    }
                    IconButton(onClick = { scanNonce++ }) {
                        Icon(Icons.Default.Refresh, "Rescan", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            Text(
                when {
                    scanning -> "Scanning for your vehicle…"
                    shown.isEmpty() -> "Nothing found. Tap rescan, and keep the bike's key on."
                    else -> "Tap your vehicle to connect"
                },
                color = Ash, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = showAll,
                    onClick = { showAll = !showAll },
                    label = { Text(if (showAll) "Showing all devices" else "Vehicles only") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    ),
                )
                if (!showAll && hiddenCount > 0) {
                    Spacer(Modifier.size(10.dp))
                    Text("$hiddenCount other hidden", color = Ash,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(10.dp))

            if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (scanning) "Looking…" else "No devices", color = Ash)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(shown, key = { it.address }) { d ->
                        DeviceRow(d) { vm.pair(d, onPaired) }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(d: ScannedDevice, onClick: () -> Unit) {
    Surface(
        color = CarbonCard,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.TwoWheeler, null,
                tint = if (d.looksLikeVehicle) MaterialTheme.colorScheme.primary else Ash,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.label, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (d.looksLikeVehicle) {
                        Spacer(Modifier.size(6.dp))
                        Icon(Icons.Default.Verified, "Likely vehicle",
                            tint = Success, modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    d.address + (if (d.bonded) " · paired" else ""),
                    color = Ash,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
            Text("${d.rssi} dBm", color = Ash, style = MaterialTheme.typography.labelMedium)
        }
    }
}
