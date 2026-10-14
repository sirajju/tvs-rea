package com.sxplus.rider.ui.screens.scaffold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sxplus.rider.ui.theme.Ash

private val descriptions = mapOf(
    "telemetry" to "Live GPS, speed, SOC/dual-battery, odometer, EV range, tyre pressure — streamed from GetTvsmLatestDeviceData + the p360 live-tracking subscription.",
    "crash" to "Fall & crash detection (accelerometer + activity-recognition fusion), 180s cancel countdown, emergency-contact alerts and nearby-hospital lookup.",
    "geofence" to "Geo-fence, scheduled time-fence with weekday + per-channel alerts, and overspeed threshold — via the GraphQL setTimeFence / setOverspeedThreshold mutations.",
    "rides" to "Ride history, routes, analytics and trip stats from the Travel/GetTravelList endpoints.",
    "charging" to "Home & portable charger status, battery health, and remote charge control for EV models.",
    "share" to "Add/remove secondary riders and transfer primary ownership (addSecondaryUser / updatePrimaryUser).",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureScaffoldScreen(featureKey: String, title: String, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Construction, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Coming in the next phase", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(12.dp))
            Text(
                descriptions[featureKey] ?: "This feature screen is scaffolded and wired into navigation; the data layer lands in the next build phase.",
                color = Ash, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
