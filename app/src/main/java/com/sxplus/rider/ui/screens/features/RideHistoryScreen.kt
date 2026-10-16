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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.sxplus.rider.data.repo.FeatureRepository
import com.sxplus.rider.domain.Ride
import com.sxplus.rider.ui.components.FeatureScaffold
import com.sxplus.rider.ui.components.StatTile
import com.sxplus.rider.ui.theme.Ash
import com.sxplus.rider.ui.theme.CarbonCard
import com.sxplus.rider.ui.theme.Lime
import com.sxplus.rider.ui.theme.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RideViewModel @Inject constructor(val repo: FeatureRepository) : ViewModel() {
    val rides = repo.rides
    val activeRide = repo.activeRide
    init { viewModelScope.launch { repo.loadRides() } }
    fun agg() = repo.aggregate()
    fun start() = repo.startRide()
    fun stop() = repo.stopRide()
    fun sync(onDone: (Int) -> Unit) = viewModelScope.launch { onDone(repo.syncRides()) }
}

@Composable
fun RideHistoryScreen(onBack: () -> Unit, vm: RideViewModel = hiltViewModel()) {
    val rides by vm.rides.collectAsStateWithLifecycle()
    val active by vm.activeRide.collectAsStateWithLifecycle()
    val agg = vm.agg()
    var lastSync by rememberSaveable { mutableStateOf("") }

    FeatureScaffold(
        title = "Ride History",
        onBack = onBack,
        actions = {
            androidx.compose.material3.IconButton(onClick = { vm.sync { n -> lastSync = "Synced $n ride(s)" } }) {
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
                    StatTile("Rides", agg.totalRides.toString(), Modifier.weight(1f))
                    StatTile("Distance", "${agg.totalDistanceKm.toInt()} km", Modifier.weight(1f), accent = Lime)
                    StatTile("CO₂ saved", "${agg.totalCo2Kg.toInt()} kg", Modifier.weight(1f), accent = Success)
                }
            }
            item {
                Surface(color = CarbonCard, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (active != null) "Ride in progress…" else "Track a ride",
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(if (active != null) "Recording live" else "Start to record distance & route",
                                color = Ash, style = MaterialTheme.typography.bodySmall)
                            if (lastSync.isNotEmpty()) Text(lastSync, color = Success, style = MaterialTheme.typography.bodySmall)
                        }
                        androidx.compose.material3.FilledTonalButton(
                            onClick = { if (active != null) vm.stop() else vm.start() },
                        ) {
                            Icon(if (active != null) Icons.Default.Stop else Icons.Default.PlayArrow, null,
                                Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(if (active != null) "Stop" else "Start")
                        }
                    }
                }
            }
            items(rides, key = { it.id }) { RideRow(it) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun RideRow(ride: Ride) {
    var expanded by rememberSaveable { mutableStateOf(false) }
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
                DetailLine("Route", "${ride.startAddress} → ${ride.endAddress}")
                DetailLine("Avg speed", "${ride.avgSpeedKmph} km/h")
                DetailLine("Top speed", "${ride.maxSpeedKmph} km/h")
                DetailLine("Energy", "${ride.energyWh} Wh")
                DetailLine("CO₂ saved", "${ride.co2SavedKg} kg")
                DetailLine("Synced", if (ride.synced) "Yes" else "Pending")
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

