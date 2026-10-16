package com.sxplus.rider.ui.screens.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sxplus.rider.data.local.RideStore
import com.sxplus.rider.data.local.SettingsStore
import com.sxplus.rider.data.repo.FeatureRepository
import com.sxplus.rider.domain.ActiveRide
import com.sxplus.rider.domain.Ride
import com.sxplus.rider.ride.RideRecorder
import com.sxplus.rider.ui.components.FeatureScaffold
import com.sxplus.rider.ui.components.SettingToggle
import com.sxplus.rider.ui.components.StatTile
import com.sxplus.rider.ui.theme.Ash
import com.sxplus.rider.ui.theme.CarbonCard
import com.sxplus.rider.ui.theme.Danger
import com.sxplus.rider.ui.theme.Lime
import com.sxplus.rider.ui.theme.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RideViewModel @Inject constructor(
    private val repo: FeatureRepository,
    rideStore: RideStore,
    recorder: RideRecorder,
    private val settings: SettingsStore,
) : ViewModel() {
    val cloudRides = repo.rides
    val recordedRides = rideStore.rides
    val active = recorder.active
    val autoRecord = settings.settings.map { it.autoRecordRides }

    init { viewModelScope.launch { repo.loadRides() } }

    fun setAutoRecord(v: Boolean) = viewModelScope.launch { settings.setAutoRecordRides(v) }
    fun sync(onDone: (Int) -> Unit) = viewModelScope.launch { onDone(repo.syncRides()) }
}

@Composable
fun RideHistoryScreen(onBack: () -> Unit, vm: RideViewModel = hiltViewModel()) {
    val cloud by vm.cloudRides.collectAsStateWithLifecycle(emptyList())
    val recorded by vm.recordedRides.collectAsStateWithLifecycle(emptyList())
    val active by vm.active.collectAsStateWithLifecycle()
    val autoRecord by vm.autoRecord.collectAsStateWithLifecycle(true)
    var lastSync by rememberSaveable { mutableStateOf("") }

    val all = (recorded + cloud).sortedByDescending { it.startedAtMs }

    FeatureScaffold(
        title = "Ride History",
        onBack = onBack,
        actions = {
            IconButton(onClick = { vm.sync { n -> lastSync = "Synced $n ride(s)" } }) {
                Icon(Icons.Default.CloudSync, "Sync", tint = MaterialTheme.colorScheme.primary)
            }
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Rides", all.size.toString(), Modifier.weight(1f))
                    StatTile("Distance", "${all.sumOf { it.distanceKm }.toInt()} km", Modifier.weight(1f), accent = Lime)
                    StatTile("CO₂", "${all.sumOf { it.co2SavedKg }.toInt()} kg", Modifier.weight(1f), accent = Success)
                }
            }
            active?.let { item { ActiveRideCard(it) } }
            item {
                Surface(color = CarbonCard, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        SettingToggle(
                            "Auto-record rides", autoRecord, { vm.setAutoRecord(it) },
                            subtitle = "Starts on connect + ignition; saves after 5 min disconnected",
                        )
                        if (lastSync.isNotEmpty()) Text(lastSync, color = Success,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(all, key = { it.id }) { RideRow(it) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ActiveRideCard(a: ActiveRide) {
    Surface(color = CarbonCard, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FiberManualRecord, null,
                tint = if (a.paused) Ash else Danger, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (a.paused) "Ride paused (disconnected)" else "Recording ride…",
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("${a.distanceKm} km · ${a.points} points · ${a.lastSpeedKmph} km/h",
                    color = Ash, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RideRow(ride: Ride) {
    var expanded by rememberSaveable(ride.id) { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("EEE, dd MMM · HH:mm", Locale.getDefault()) }
    Surface(
        color = CarbonCard, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Route, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${ride.distanceKm} km · ${ride.durationMin} min",
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(fmt.format(Date(ride.startedAtMs)), color = Ash, style = MaterialTheme.typography.bodySmall)
                }
                if (!ride.synced) Text("●", color = Lime)
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                DetailLine("Avg speed", "${ride.avgSpeedKmph} km/h")
                DetailLine("Top speed", "${ride.maxSpeedKmph} km/h")
                DetailLine("Energy", "${ride.energyWh} Wh")
                DetailLine("CO₂ saved", "${ride.co2SavedKg} kg")
                DetailLine("Synced", if (ride.synced) "Yes" else "Local only")
            }
        }
    }
}

@Composable
private fun DetailLine(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(k, Modifier.weight(1f), color = Ash, style = MaterialTheme.typography.bodySmall)
        Text(v, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
    }
}
