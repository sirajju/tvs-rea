package com.sxplus.rider.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sxplus.rider.ble.BleKeylessManager
import com.sxplus.rider.ble.BleState
import com.sxplus.rider.ble.KeylessCommand
import com.sxplus.rider.data.local.SettingsStore
import com.sxplus.rider.util.Notifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires a keyless command from outside the app — home-screen widget, launcher
 * shortcut, Tasker, or `adb shell am broadcast`. Connects if needed, then sends.
 *
 *   adb shell am broadcast -a com.sxplus.rider.action.FIND_VEHICLE com.sxplus.rider
 */
@AndroidEntryPoint
class KeylessActionReceiver : BroadcastReceiver() {

    @Inject lateinit var ble: BleKeylessManager
    @Inject lateinit var settings: SettingsStore

    override fun onReceive(context: Context, intent: Intent) {
        val cmd = when (intent.action) {
            ACTION_FIND -> KeylessCommand.FIND_VEHICLE
            ACTION_LOCK -> KeylessCommand.LOCK
            ACTION_UNLOCK -> KeylessCommand.UNLOCK
            ACTION_TRUNK -> KeylessCommand.OPEN_TRUNK
            else -> return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (ble.state.value != BleState.READY) {
                    val mac = settings.settings.first().pairedMac
                    if (mac != null) { ble.connect(mac); awaitReady(15_000) }
                }
                if (ble.state.value == BleState.READY) {
                    ble.sendKeylessCommand(cmd)
                    Notifier.alert(context, 46, "SmartXonnect+", "${cmd.label} sent")
                } else {
                    Notifier.alert(context, 46, "SmartXonnect+", "Couldn't reach the vehicle for ${cmd.label}")
                }
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    private suspend fun awaitReady(timeoutMs: Long) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (ble.state.value == BleState.READY) return
            delay(300)
        }
    }

    companion object {
        const val ACTION_FIND = "com.sxplus.rider.action.FIND_VEHICLE"
        const val ACTION_LOCK = "com.sxplus.rider.action.LOCK"
        const val ACTION_UNLOCK = "com.sxplus.rider.action.UNLOCK"
        const val ACTION_TRUNK = "com.sxplus.rider.action.OPEN_TRUNK"
    }
}
