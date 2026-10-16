package com.sxplus.rider.ui.screens.features

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sxplus.rider.data.repo.FeatureRepository
import com.sxplus.rider.domain.BatteryPack
import com.sxplus.rider.domain.ChargingStatus
import com.sxplus.rider.ui.components.FeatureScaffold
import com.sxplus.rider.ui.components.InfoRow
import com.sxplus.rider.ui.components.SectionCard
import com.sxplus.rider.ui.theme.Ash
import com.sxplus.rider.ui.theme.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChargingViewModel @Inject constructor(private val repo: FeatureRepository) : ViewModel() {
    private val _s = MutableStateFlow<ChargingStatus?>(null)
    val s = _s.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) { _s.value = repo.charging(); _loading.value = false; delay(4000) }
        }
    }
}

@Composable
fun ChargingScreen(onBack: () -> Unit, vm: ChargingViewModel = hiltViewModel()) {
    val s by vm.s.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    FeatureScaffold(title = "Charging", onBack = onBack) { pad ->
        val status = s
        when {
            loading && status == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            status == null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No battery data. Sign in and connect the vehicle.", color = Ash, textAlign = TextAlign.Center)
            }
            else -> Column(
                Modifier.padding(pad).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                SectionCard("Charger") {
                    InfoRow(Icons.Default.Power, "Connected", if (status.chargerConnected) "Yes" else "No",
                        valueColor = if (status.chargerConnected) Success else null)
                    InfoRow(Icons.Default.BatteryChargingFull, "Type", status.chargerType)
                    InfoRow(null, "Location", if (status.onVehicle) "On vehicle" else "Off vehicle")
                }
                status.packs.forEach { PackCard(it) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PackCard(p: BatteryPack) {
    SectionCard(p.label) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${p.soc}%", fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall, color = Success)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { p.soc / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Success,
                )
                Spacer(Modifier.height(6.dp))
                Text(p.chargingStatus, color = Ash, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        InfoRow(null, "Time to full", "${p.timeToFullMin} min")
    }
}
