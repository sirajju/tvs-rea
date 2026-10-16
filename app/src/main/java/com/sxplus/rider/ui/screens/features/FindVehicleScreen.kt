package com.sxplus.rider.ui.screens.features

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxplus.rider.data.local.SettingsStore
import com.sxplus.rider.ui.components.FeatureScaffold
import com.sxplus.rider.ui.components.InfoRow
import com.sxplus.rider.ui.components.PrimaryButton
import com.sxplus.rider.ui.components.SectionCard
import com.sxplus.rider.ui.theme.Ash
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class FindVehicleViewModel @Inject constructor(settings: SettingsStore) : ViewModel() {
    val parked = settings.settings.map { it.lastParked }
}

@Composable
fun FindVehicleScreen(onBack: () -> Unit, vm: FindVehicleViewModel = hiltViewModel()) {
    val parked by vm.parked.collectAsStateWithLifecycle(null)
    val ctx = LocalContext.current

    FeatureScaffold(title = "Find my vehicle", onBack = onBack) { pad ->
        val p = parked
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOff, null, tint = Ash, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No parked location yet. It's saved automatically when you disconnect from the bike.",
                        color = Ash, textAlign = TextAlign.Center)
                }
            }
        } else {
            val ago = ((System.currentTimeMillis() - p.timeMs) / 60000L).toInt()
            val fmt = remember0 { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
            Column(
                Modifier.padding(pad).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                SectionCard("Last parked") {
                    InfoRow(Icons.Default.Map, "Vehicle", p.vehicle)
                    InfoRow(null, "When", "${fmt.format(Date(p.timeMs))} · ${ago} min ago")
                    InfoRow(null, "Coordinates", "%.5f, %.5f".format(p.lat, p.lng))
                }
                PrimaryButton("Navigate to my bike", icon = Icons.Default.Navigation, onClick = {
                    val uri = Uri.parse("google.navigation:q=${p.lat},${p.lng}")
                    runCatching {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                })
                PrimaryButton("Open in Maps", icon = Icons.Default.Map, onClick = {
                    val uri = Uri.parse("geo:${p.lat},${p.lng}?q=${p.lat},${p.lng}(My bike)")
                    runCatching {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                })
            }
        }
    }
}

@Composable
private fun <T> remember0(calc: () -> T): T = androidx.compose.runtime.remember(calculation = calc)
