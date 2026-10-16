package com.sxplus.rider.ui.screens.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxplus.rider.data.repo.FeatureRepository
import com.sxplus.rider.domain.Fence
import com.sxplus.rider.domain.FenceType
import com.sxplus.rider.ui.components.FeatureScaffold
import com.sxplus.rider.ui.components.PrimaryButton
import com.sxplus.rider.ui.components.SectionCard
import com.sxplus.rider.ui.theme.Ash
import com.sxplus.rider.ui.theme.Danger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FenceViewModel @Inject constructor(private val repo: FeatureRepository) : ViewModel() {
    val fences = repo.fences
    val overspeed = repo.overspeedLimit
    fun setOverspeed(v: Int) = repo.setOverspeedLimit(v)
    fun add(name: String, type: FenceType) =
        repo.addFence(Fence(id = "", name = name, type = type))
    fun remove(id: String) = repo.removeFence(id)
    fun toggle(id: String) = repo.toggleFence(id)
}

@Composable
fun FenceScreen(onBack: () -> Unit, vm: FenceViewModel = hiltViewModel()) {
    val fences by vm.fences.collectAsStateWithLifecycle()
    val overspeed by vm.overspeed.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(FenceType.GEO) }

    FeatureScaffold(title = "Geo / Time Fence", onBack = onBack) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            SectionCard("Overspeed alert") {
                Text("Limit: $overspeed km/h", color = MaterialTheme.colorScheme.onSurface)
                Slider(
                    value = overspeed.toFloat(),
                    onValueChange = { vm.setOverspeed(it.toInt()) },
                    valueRange = 20f..120f,
                )
            }
            SectionCard("Fences") {
                fences.forEach { f -> FenceRow(f, onToggle = { vm.toggle(f.id) }, onRemove = { vm.remove(f.id) }) }
            }
            SectionCard("Add fence") {
                OutlinedTextField(name, { name = it }, label = { Text("Fence name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = type == FenceType.GEO, onClick = { type = FenceType.GEO },
                        label = { Text("Geo") })
                    androidx.compose.material3.FilterChip(
                        selected = type == FenceType.TIME, onClick = { type = FenceType.TIME },
                        label = { Text("Time") })
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Add fence", onClick = { vm.add(name, type); name = "" },
                    enabled = name.isNotBlank(), icon = Icons.Default.Add)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FenceRow(f: Fence, onToggle: () -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (f.type == FenceType.TIME) Icons.Default.Schedule else Icons.Default.Map, null,
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(0.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(f.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (f.type == FenceType.TIME) "${f.fromTime}–${f.toTime} · ${f.overspeedLimit} km/h"
                else "Radius ${f.radiusM} m",
                color = Ash, style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = f.enabled, onCheckedChange = { onToggle() })
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Remove", tint = Danger) }
    }
}
