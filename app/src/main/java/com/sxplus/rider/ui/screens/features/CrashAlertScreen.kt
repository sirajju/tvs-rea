package com.sxplus.rider.ui.screens.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import com.sxplus.rider.domain.CrashConfig
import com.sxplus.rider.domain.EmergencyContact
import com.sxplus.rider.ui.components.FeatureScaffold
import com.sxplus.rider.ui.components.PrimaryButton
import com.sxplus.rider.ui.components.SectionCard
import com.sxplus.rider.ui.components.SettingToggle
import com.sxplus.rider.ui.theme.Ash
import com.sxplus.rider.ui.theme.Danger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CrashViewModel @Inject constructor(private val repo: FeatureRepository) : ViewModel() {
    val config = repo.crashConfig
    val contacts = repo.contacts
    fun update(block: (CrashConfig) -> CrashConfig) = repo.updateCrashConfig(block)
    fun add(name: String, mobile: String) = repo.addContact(name, mobile)
    fun remove(id: String) = repo.removeContact(id)
}

@Composable
fun CrashAlertScreen(onBack: () -> Unit, vm: CrashViewModel = hiltViewModel()) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }

    FeatureScaffold(title = "Crash Alert", onBack = onBack) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            SectionCard("Detection") {
                SettingToggle("Crash detection", cfg.enabled, { v -> vm.update { it.copy(enabled = v) } })
                SettingToggle("Fall detection", cfg.fallEnabled, { v -> vm.update { it.copy(fallEnabled = v) } })
                Spacer(Modifier.height(8.dp))
                Text("Sensitivity: ${cfg.sensitivity}%", color = MaterialTheme.colorScheme.onSurface)
                Slider(
                    value = cfg.sensitivity.toFloat(),
                    onValueChange = { v -> vm.update { it.copy(sensitivity = v.toInt()) } },
                    valueRange = 30f..100f,
                )
                Text("Countdown before alerting: ${cfg.countdownSec}s", color = Ash,
                    style = MaterialTheme.typography.bodySmall)
            }
            SectionCard("Emergency contacts") {
                contacts.forEach { c -> ContactRow(c) { vm.remove(c.id) } }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(mobile, { mobile = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Mobile") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Add contact", onClick = {
                    vm.add(name, mobile); name = ""; mobile = ""
                }, enabled = name.isNotBlank() && mobile.length >= 10, icon = Icons.Default.PersonAdd)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContactRow(c: EmergencyContact, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(c.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text("${c.countryCode} ${c.mobile}", color = Ash, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Remove", tint = Danger) }
    }
}
