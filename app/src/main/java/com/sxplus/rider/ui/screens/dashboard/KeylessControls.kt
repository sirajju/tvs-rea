package com.sxplus.rider.ui.screens.dashboard

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.sxplus.rider.ble.KeylessCommand
import com.sxplus.rider.ui.theme.CarbonCard
import com.sxplus.rider.ui.theme.Danger
import com.sxplus.rider.ui.theme.Lime
import com.sxplus.rider.ui.theme.Sky
import com.sxplus.rider.ui.theme.Success

private data class CmdSpec(val cmd: KeylessCommand, val icon: ImageVector, val tint: Color)

private val specs = listOf(
    CmdSpec(KeylessCommand.FIND_VEHICLE, Icons.Default.NotificationsActive, Lime),
    CmdSpec(KeylessCommand.UNLOCK, Icons.Default.LockOpen, Success),
    CmdSpec(KeylessCommand.LOCK, Icons.Default.Lock, Danger),
    CmdSpec(KeylessCommand.OPEN_TRUNK, Icons.Outlined.Inventory2, Sky),
)

@Composable
fun KeylessControls(
    enabled: Boolean,
    sending: KeylessCommand?,
    onCommand: (KeylessCommand) -> Unit,
) {
    Column {
        Text(
            "Keyless Control",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            specs.take(2).forEach { spec ->
                KeylessTile(Modifier.weight(1f), spec, enabled, sending, onCommand)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            specs.drop(2).forEach { spec ->
                KeylessTile(Modifier.weight(1f), spec, enabled, sending, onCommand)
            }
        }
    }
}

@Composable
private fun KeylessTile(
    modifier: Modifier,
    spec: CmdSpec,
    enabled: Boolean,
    sending: KeylessCommand?,
    onCommand: (KeylessCommand) -> Unit,
) {
    val isSending = sending == spec.cmd
    val bg by animateColorAsState(
        if (enabled) CarbonCard else CarbonCard.copy(alpha = 0.5f), label = "bg"
    )
    Surface(
        modifier = modifier
            .aspectRatio(1.35f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled && sending == null) { onCommand(spec.cmd) },
        color = bg,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    Modifier.size(34.dp), color = spec.tint, strokeWidth = 3.dp
                )
            } else {
                Icon(
                    spec.icon, null,
                    tint = if (enabled) spec.tint else spec.tint.copy(alpha = 0.4f),
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                spec.cmd.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
