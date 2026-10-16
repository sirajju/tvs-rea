package com.sxplus.rider.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxplus.rider.ui.theme.CarbonCard

private data class Feature(val key: String, val label: String, val icon: ImageVector)

private val features = listOf(
    Feature("find", "Find Vehicle", Icons.Default.MyLocation),
    Feature("telemetry", "Live Telemetry", Icons.Default.Insights),
    Feature("crash", "Crash Alert", Icons.Default.Sos),
    Feature("geofence", "Geo / Time Fence", Icons.Default.Map),
    Feature("rides", "Ride History", Icons.Default.Route),
    Feature("charging", "Charging", Icons.Default.BatteryChargingFull),
    Feature("share", "Share Vehicle", Icons.Default.Groups),
    Feature("cluster", "Cluster Control", Icons.Default.BrightnessHigh),
    Feature("settings", "Vehicle Settings", Icons.Default.Tune),
    Feature("appearance", "Appearance", Icons.Default.Palette),
    Feature("profile", "Profile", Icons.Default.Person),
)

@Composable
fun FeatureGrid(onOpen: (String) -> Unit) {
    Column {
        Text("Features", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(12.dp))
        features.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { f ->
                    Surface(
                        color = CarbonCard,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onOpen(f.key) },
                    ) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(f.icon, null, tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(f.label, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
                // pad incomplete rows for alignment
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
