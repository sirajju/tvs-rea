package com.sxplus.rider.ui.screens.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sxplus.rider.ble.BleKeylessManager
import com.sxplus.rider.ble.BleState
import com.sxplus.rider.ble.ClusterProtocol
import com.sxplus.rider.ble.KeylessResult
import com.sxplus.rider.ui.components.FeatureScaffold
import com.sxplus.rider.ui.components.PrimaryButton
import com.sxplus.rider.ui.components.SectionCard
import com.sxplus.rider.ui.theme.Ash
import com.sxplus.rider.ui.theme.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val RIDE_MODES = listOf("Eco", "City", "Power")

@HiltViewModel
class ClusterViewModel @Inject constructor(private val ble: BleKeylessManager) : ViewModel() {
    val bleState = ble.state
    private val _toast = MutableStateFlow<String?>(null)
    val toast = _toast.asStateFlow()

    fun apply(brightness: Int, rideMode: Int) = viewModelScope.launch {
        _toast.value = when (val r = ble.sendVehicleControl(brightness, rideMode)) {
            is KeylessResult.Success -> "Sent to cluster ✓"
            is KeylessResult.NotReady -> r.message
            is KeylessResult.Error -> "Error: ${r.message}"
            is KeylessResult.ChallengeTimeout -> r.message
        }
    }
    fun clearToast() { _toast.value = null }
}

@Composable
fun ClusterControlScreen(onBack: () -> Unit, vm: ClusterViewModel = hiltViewModel()) {
    val state by vm.bleState.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    var brightness by remember { mutableIntStateOf(5) }
    var rideMode by remember { mutableIntStateOf(1) }
    val connected = state == BleState.READY

    FeatureScaffold(title = "Cluster Control", onBack = onBack) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            if (!connected) {
                SectionCard {
                    Text("Vehicle not connected. Connect over Bluetooth first (dashboard → Connect).",
                        color = Ash)
                }
            }
            SectionCard("Screen brightness") {
                Row {
                    androidx.compose.material3.Icon(Icons.Default.BrightnessHigh, null, tint = Ash)
                    Spacer(Modifier.height(0.dp))
                    Text("  Level: $brightness / ${ClusterProtocol.MAX_BRIGHTNESS}",
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
                Slider(
                    value = brightness.toFloat(),
                    onValueChange = { brightness = it.toInt() },
                    valueRange = ClusterProtocol.MIN_BRIGHTNESS.toFloat()..ClusterProtocol.MAX_BRIGHTNESS.toFloat(),
                    enabled = connected,
                )
            }
            SectionCard("Ride mode") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RIDE_MODES.forEachIndexed { i, label ->
                        FilterChip(
                            selected = rideMode == i,
                            onClick = { rideMode = i },
                            label = { Text(label) },
                            enabled = connected,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Mode indices map to your bike's modes; watch the cluster to confirm.",
                    color = Ash, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = "Apply to cluster",
                onClick = { vm.apply(brightness, rideMode) },
                enabled = connected,
                icon = Icons.Default.Send,
            )
            toast?.let { Text(it, color = Success) }
        }
    }
}
