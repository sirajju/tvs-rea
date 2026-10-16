package com.sxplus.rider.ride

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.sxplus.rider.R
import com.sxplus.rider.ble.BleKeylessManager
import com.sxplus.rider.ble.BleState
import com.sxplus.rider.data.local.SettingsStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives auto ride recording:
 *   connected + ignition-on  -> record (sample every 5s)
 *   disconnected             -> pause + start a 5-min grace window
 *   reconnect within 5 min   -> resume the same ride
 *   no reconnect for 5 min   -> finalize + SAVE the ride, then stop
 *
 * The 5-min-disconnect save rule is deliberate: the OEM app relied on a
 * flaky ignition-off packet and could lose rides; this does not.
 */
@AndroidEntryPoint
class RideRecorderService : LifecycleService() {

    @Inject lateinit var recorder: RideRecorder
    @Inject lateinit var ble: BleKeylessManager
    @Inject lateinit var settings: SettingsStore
    @Inject lateinit var features: com.sxplus.rider.data.repo.FeatureRepository

    private var lastOverspeedAlertMs = 0L

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, notification("Ride recorder active"))
        loop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun loop() {
        lifecycleScope.launch {
            var disconnectedSinceMs = 0L
            var wasReady = false
            while (isActive) {
                val s = runCatching { settings.settings.first() }.getOrNull()
                val enabled = s?.autoRecordRides ?: true
                val ready = ble.state.value == BleState.READY

                // Falling edge: just disconnected -> remember where the bike is parked.
                if (wasReady && !ready) saveParkedLocation(s?.pairedName ?: "Vehicle")
                wasReady = ready

                if (enabled && ready) {
                    disconnectedSinceMs = 0L
                    runCatching { recorder.tick() }
                    recorder.active.value?.let { ar ->
                        updateNotification(
                            if (ar.points == 0) "Waiting for ignition…"
                            else "Recording · ${ar.distanceKm} km · ${ar.lastSpeedKmph} km/h"
                        )
                        maybeOverspeedAlert(ar.lastSpeedKmph, s?.overspeedAlerts ?: true)
                    }
                } else if (recorder.isRecording()) {
                    // Connected ride is now disconnected — start / check the 5-min grace.
                    if (disconnectedSinceMs == 0L) {
                        disconnectedSinceMs = System.currentTimeMillis()
                        recorder.pause()
                        updateNotification("Disconnected — saving in 5 min if not reconnected")
                    } else if (System.currentTimeMillis() - disconnectedSinceMs >= GRACE_MS) {
                        val saved = runCatching { recorder.finalizeAndSave() }.getOrNull()
                        updateNotification(if (saved != null) "Ride saved · ${saved.distanceKm} km" else "Ride discarded (too short)")
                        disconnectedSinceMs = 0L
                    }
                }
                delay(TICK_MS)
            }
        }
    }

    /** Overspeed + active time-fence alert (debounced to once/minute). */
    private fun maybeOverspeedAlert(speed: Int, enabled: Boolean) {
        if (!enabled) return
        val limit = features.overspeedLimit.value
        val timeFenceLimit = com.sxplus.rider.ride.activeTimeFenceLimit(features.fences.value)
        val effectiveLimit = minOf(limit, timeFenceLimit ?: Int.MAX_VALUE)
        if (speed > effectiveLimit && System.currentTimeMillis() - lastOverspeedAlertMs > 60_000L) {
            lastOverspeedAlertMs = System.currentTimeMillis()
            com.sxplus.rider.util.Notifier.alert(
                this, 44, "Overspeed", "Riding at $speed km/h (limit $effectiveLimit)"
            )
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private suspend fun saveParkedLocation(vehicle: String) {
        runCatching {
            val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val loc = listOf(
                android.location.LocationManager.GPS_PROVIDER,
                android.location.LocationManager.NETWORK_PROVIDER,
            ).mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
            if (loc != null) {
                settings.setLastParked(loc.latitude, loc.longitude, System.currentTimeMillis(), vehicle)
            }
        }
    }

    private fun notification(text: String): Notification {
        val channelId = ensureChannel()
        val pi = packageManager.getLaunchIntentForPackage(packageName)?.let {
            android.app.PendingIntent.getActivity(
                this, 2, it,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SmartXonnect+ Ride")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_motorcycle)
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, notification(text))
    }

    private fun ensureChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Ride recording", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
        return CHANNEL_ID
    }

    companion object {
        private const val CHANNEL_ID = "ride_recorder"
        private const val NOTIF_ID = 43
        private const val TICK_MS = 5_000L
        private const val GRACE_MS = 5 * 60_000L

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, RideRecorderService::class.java))

        fun stop(context: Context) =
            context.stopService(Intent(context, RideRecorderService::class.java))
    }
}
