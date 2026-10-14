package com.sxplus.rider.proximity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.sxplus.rider.R
import com.sxplus.rider.ble.BleKeylessManager
import com.sxplus.rider.ble.BleState
import com.sxplus.rider.data.local.SettingsStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that continuously (low-power) scans for the paired
 * vehicle. Uses RSSI hysteresis to detect "arrived near bike":
 *   - when smoothed RSSI rises above rssiEnter -> auto-connect + fire approach action once
 *   - when it falls below rssiExit -> mark left (optionally auto-lock)
 */
@AndroidEntryPoint
class ProximityService : LifecycleService() {

    @Inject lateinit var ble: BleKeylessManager
    @Inject lateinit var settings: SettingsStore

    private var scanner: BluetoothLeScanner? = null
    private var smoothedRssi: Double = -100.0
    private var isNear = false
    private var lastActionAt = 0L

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground(NOTIF_ID, buildNotification("Watching for your vehicle…"))
            startScan()
        } catch (e: Exception) {
            // e.g. FGS start restrictions / missing permission — fail quietly instead of crashing the app.
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (!hasScanPermission()) { stopSelf(); return }
        val mgr = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        scanner = mgr?.adapter?.bluetoothLeScanner ?: run { stopSelf(); return }
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        scanner?.startScan(null, scanSettings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            lifecycleScope.launch { handleScan(result) }
        }
    }

    private suspend fun handleScan(result: ScanResult) {
        val cfg = settings.settings.first()
        val target = cfg.pairedMac ?: return
        if (!result.device.address.equals(target, ignoreCase = true)) return

        // Exponential smoothing of RSSI to reduce jitter.
        smoothedRssi = if (smoothedRssi <= -100.0) result.rssi.toDouble()
        else 0.6 * smoothedRssi + 0.4 * result.rssi

        updateNotification("Vehicle nearby · ${smoothedRssi.toInt()} dBm")

        val enteringNear = !isNear && smoothedRssi >= cfg.rssiEnter
        val leavingNear = isNear && smoothedRssi <= cfg.rssiExit

        if (enteringNear) {
            isNear = true
            onArrivedNearVehicle(cfg)
        } else if (leavingNear) {
            isNear = false
            onLeftVehicle(cfg)
        }
    }

    private suspend fun onArrivedNearVehicle(cfg: SettingsStore.Settings) {
        val mac = cfg.pairedMac ?: return
        if (cfg.autoConnect && !ble.isConnected()) ble.connect(mac)

        if (cfg.autoActionOnApproach && System.currentTimeMillis() - lastActionAt > ACTION_DEBOUNCE_MS) {
            lastActionAt = System.currentTimeMillis()
            // Wait briefly for the connection to become READY, then fire.
            val ready = awaitReady(timeoutMs = 6_000)
            if (ready) {
                ble.sendKeylessCommand(cfg.approachCommand)
                updateNotification("Arrived · ${cfg.approachCommand.label} sent")
            }
        }
    }

    private suspend fun onLeftVehicle(cfg: SettingsStore.Settings) {
        if (cfg.autoLockOnLeave && ble.isConnected()) {
            ble.sendKeylessCommand(com.sxplus.rider.ble.KeylessCommand.LOCK)
            updateNotification("Left vehicle · auto-locked")
        }
    }

    private suspend fun awaitReady(timeoutMs: Long): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (ble.state.value == BleState.READY) return true
            val s = ble.state.firstOrNull()
            if (s == BleState.FAILED) return false
            kotlinx.coroutines.delay(250)
        }
        return ble.state.value == BleState.READY
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        try { scanner?.stopScan(scanCallback) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    // --- Notification -------------------------------------------------------
    private fun buildNotification(text: String): Notification {
        val channelId = ensureChannel()
        val pi = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SmartXonnect+")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_motorcycle)
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun ensureChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Vehicle proximity", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
        return CHANNEL_ID
    }

    private fun hasScanPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Manifest.permission.BLUETOOTH_SCAN else Manifest.permission.ACCESS_FINE_LOCATION
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val CHANNEL_ID = "proximity"
        private const val NOTIF_ID = 42
        private const val ACTION_DEBOUNCE_MS = 30_000L

        fun start(context: Context) {
            val i = Intent(context, ProximityService::class.java)
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProximityService::class.java))
        }
    }
}
