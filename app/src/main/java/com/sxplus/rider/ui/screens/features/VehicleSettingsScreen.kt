package com.sxplus.rider.ui.screens.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxplus.rider.data.repo.FeatureRepository
import com.sxplus.rider.domain.VehicleSettings
import com.sxplus.rider.ui.components.FeatureScaffold
import com.sxplus.rider.ui.components.SectionCard
import com.sxplus.rider.ui.components.SettingToggle
import com.sxplus.rider.ui.theme.Ash
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VehicleSettingsViewModel @Inject constructor(private val repo: FeatureRepository) : ViewModel() {
    val settings = repo.vehicleSettings
    fun update(block: (VehicleSettings) -> VehicleSettings) = repo.updateVehicleSettings(block)
}

@Composable
fun VehicleSettingsScreen(onBack: () -> Unit, vm: VehicleSettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    FeatureScaffold(title = "Vehicle Settings", onBack = onBack) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            SectionCard("Security") {
                SettingToggle("Immobilizer", s.immobilizer,
                    { v -> vm.update { it.copy(immobilizer = v) } },
                    subtitle = "Block the motor remotely")
                SettingToggle("Theft mode", s.theftMode,
                    { v -> vm.update { it.copy(theftMode = v) } },
                    subtitle = "Alert on unexpected movement")
                SettingToggle("Auto-lock on leave", s.autoLock,
                    { v -> vm.update { it.copy(autoLock = v) } })
            }
            SectionCard("Alerts") {
                Text("Low-battery alert: ${s.lowBatteryAlertPct}%", color = MaterialTheme.colorScheme.onSurface)
                Slider(
                    value = s.lowBatteryAlertPct.toFloat(),
                    onValueChange = { v -> vm.update { it.copy(lowBatteryAlertPct = v.toInt()) } },
                    valueRange = 5f..40f,
                )
                Text("Heartbeat report: every ${s.heartbeatMinutes} min", color = Ash,
                    style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = s.heartbeatMinutes.toFloat(),
                    onValueChange = { v -> vm.update { it.copy(heartbeatMinutes = v.toInt()) } },
                    valueRange = 1f..30f,
                )
            }
            SectionCard("Comfort") {
                SettingToggle("Headlamp assist", s.headlampAssist,
                    { v -> vm.update { it.copy(headlampAssist = v) } })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
