package com.sxplus.rider.ui.screens.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sxplus.rider.data.local.SettingsStore
import com.sxplus.rider.data.local.SettingsStore.ThemeMode
import com.sxplus.rider.ui.components.FeatureScaffold
import com.sxplus.rider.ui.components.SectionCard
import com.sxplus.rider.ui.theme.ACCENTS
import com.sxplus.rider.ui.theme.Ash
import com.sxplus.rider.ui.theme.ThemeController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val theme: ThemeController,
) : ViewModel() {
    val mode = settings.settings.map { it.themeMode }
    val accent = theme.accent

    fun setMode(m: ThemeMode) = viewModelScope.launch { settings.setThemeMode(m); theme.setMode(m) }
    fun setAccent(i: Int) = viewModelScope.launch {
        val key = settings.settings.first().selectedVehicleKey ?: "global"
        settings.setVehicleAccent(key, i)
        theme.setAccent(i)
    }
}

@Composable
fun AppearanceScreen(onBack: () -> Unit, vm: AppearanceViewModel = hiltViewModel()) {
    val mode by vm.mode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    val accent by vm.accent.collectAsStateWithLifecycle()

    FeatureScaffold(title = "Appearance", onBack = onBack) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            SectionCard("Theme") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { vm.setMode(m) },
                            label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
            }
            SectionCard("Accent (this vehicle)") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ACCENTS.forEachIndexed { i, color ->
                        val selected = i == accent
                        Spacer(
                            Modifier
                                .size(36.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { vm.setAccent(i) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Each vehicle can have its own accent colour.",
                    color = Ash, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
