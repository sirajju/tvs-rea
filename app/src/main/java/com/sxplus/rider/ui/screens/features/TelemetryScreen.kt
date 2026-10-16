package com.sxplus.rider.ui.screens.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.ElectricMoped
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sxplus.rider.data.repo.FeatureRepository
import com.sxplus.rider.domain.Telemetry
import com.sxplus.rider.ui.components.FeatureScaffold
import com.sxplus.rider.ui.components.InfoRow
import com.sxplus.rider.ui.components.SectionCard
import com.sxplus.rider.ui.components.StatTile
import com.sxplus.rider.ui.theme.Ash
import com.sxplus.rider.ui.theme.Danger
import com.sxplus.rider.ui.theme.Lime
import com.sxplus.rider.ui.theme.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TelemetryViewModel @Inject constructor(private val repo: FeatureRepository) : ViewModel() {
    private val _t = MutableStateFlow<Telemetry?>(null)
    val t = _t.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _t.value = repo.telemetry()
                _loading.value = false
                delay(3000)
            }
        }
    }
}

@Composable
fun TelemetryScreen(onBack: () -> Unit, vm: TelemetryViewModel = hiltViewModel()) {
    val t by vm.t.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    FeatureScaffold(title = "Live Telemetry", onBack = onBack) { pad ->
        val snapshot = t
        when {
            loading && snapshot == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            snapshot == null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No live data. Sign in and make sure the vehicle is connected to its telematics network.",
                    color = Ash, textAlign = TextAlign.Center,
                )
            }
            else -> TelemetryBody(Modifier.padding(pad), snapshot)
        }
    }
}

@Composable
private fun TelemetryBody(modifier: Modifier, t: Telemetry) {
    Column(
        modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        if (t.isCrash || t.isFall || t.isSos || t.isTow) {
            SectionCard {
                Text(
                    buildString {
                        if (t.isCrash) append("CRASH ")
                        if (t.isFall) append("FALL ")
                        if (t.isSos) append("SOS ")
                        if (t.isTow) append("TOW ")
                        append("alert active")
                    },
                    color = Danger, style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Speed", "${t.speedKmph} km/h", Modifier.weight(1f), accent = Lime)
            StatTile("Charge", "${t.soc}%", Modifier.weight(1f), accent = Success)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Range (Eco)", "${t.rangeEcoKm} km", Modifier.weight(1f))
            StatTile("Odometer", "${t.odometerKm} km", Modifier.weight(1f))
        }
        SectionCard("Status") {
            InfoRow(Icons.Default.ElectricMoped, "Ignition", if (t.ignitionOn) "ON" else "OFF",
                valueColor = if (t.ignitionOn) Success else null)
            InfoRow(Icons.Default.Bolt, "Charging", t.chargingStatus)
            InfoRow(Icons.Default.Speed, "Range (Power)", "${t.rangePowerKm} km")
            InfoRow(Icons.Default.TireRepair, "Tyre F / R", "${t.tyreFrontKpa} / ${t.tyreRearKpa} kPa")
            InfoRow(Icons.Default.Co2, "CO₂ saved", "${t.co2SavedKg} kg")
        }
        SectionCard("Location") {
            InfoRow(Icons.Default.LocationOn, "Address", t.address.ifBlank { "—" })
            InfoRow(null, "Coordinates", "%.4f, %.4f".format(t.latitude, t.longitude))
        }
        Spacer(Modifier.height(24.dp))
    }
}
