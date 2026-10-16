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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.sxplus.rider.domain.SecondaryUser
import com.sxplus.rider.ui.components.FeatureScaffold
import com.sxplus.rider.ui.components.PrimaryButton
import com.sxplus.rider.ui.components.SectionCard
import com.sxplus.rider.ui.theme.Ash
import com.sxplus.rider.ui.theme.Danger
import com.sxplus.rider.ui.theme.Lime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(private val repo: FeatureRepository) : ViewModel() {
    val users = repo.secondaryUsers
    fun add(name: String, mobile: String, email: String) = repo.addSecondaryUser(name, mobile, email)
    fun remove(id: String) = repo.removeSecondaryUser(id)
}

@Composable
fun ShareVehicleScreen(onBack: () -> Unit, vm: ShareViewModel = hiltViewModel()) {
    val users by vm.users.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    FeatureScaffold(title = "Share Vehicle", onBack = onBack) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            SectionCard("Riders") {
                users.forEach { u -> UserRow(u) { vm.remove(u.appUserId) } }
            }
            SectionCard("Add secondary rider") {
                OutlinedTextField(name, { name = it }, label = { Text("Name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(mobile, { mobile = it.filter { c -> c.isDigit() } },
                    label = { Text("Mobile") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(email, { email = it }, label = { Text("Email") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Invite rider", onClick = {
                    vm.add(name, mobile, email); name = ""; mobile = ""; email = ""
                }, enabled = name.isNotBlank() && mobile.length >= 10, icon = Icons.Default.PersonAdd)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UserRow(u: SecondaryUser, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(u.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (u.isPrimary) {
                    Spacer(Modifier.height(0.dp))
                    Icon(Icons.Default.Star, "Primary", tint = Lime,
                        modifier = Modifier.padding(start = 6.dp).height(16.dp))
                }
            }
            Text(u.mobile + (if (u.email.isNotBlank() && u.email != "—") " · ${u.email}" else ""),
                color = Ash, style = MaterialTheme.typography.bodySmall)
        }
        if (!u.isPrimary) {
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Remove", tint = Danger) }
        } else {
            Text("Owner", color = Lime, style = MaterialTheme.typography.labelMedium)
        }
    }
}
