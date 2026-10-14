package com.sxplus.rider.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** A device seen during a scan. */
data class ScannedDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val bonded: Boolean,
    val looksLikeVehicle: Boolean,
) {
    val label: String get() = name?.takeIf { it.isNotBlank() } ?: "Unknown device"
}

/**
 * Active BLE scanner for the "pick my bike" screen (like the old
 * BluetoothDeviceListActivity). Emits a live, de-duplicated list sorted by
 * signal strength. Devices whose name/UUID look like a TVS cluster are flagged.
 */
@Singleton
@SuppressLint("MissingPermission")
class BleScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val adapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private val found = linkedMapOf<String, ScannedDevice>()
    private val _devices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devices: StateFlow<List<ScannedDevice>> = _devices.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val vehicleHints = listOf("TVS", "SMART", "XONNECT", "TVSBT", "IQUBE", "NTORQ", "RAIDER", "APACHE", "RONIN")

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = add(result)
        override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach(::add)
        override fun onScanFailed(errorCode: Int) { _scanning.value = false }
    }

    private fun add(result: ScanResult) {
        val dev = result.device
        val name = result.scanRecord?.deviceName ?: runCatching { dev.name }.getOrNull()
        val uuids = result.scanRecord?.serviceUuids?.map { it.uuid.toString().uppercase() } ?: emptyList()
        val hintByName = name?.uppercase()?.let { n -> vehicleHints.any { n.contains(it) } } ?: false
        val hintByUuid = uuids.any { it.startsWith("00005352") || it.startsWith("00005354") || it.contains("5456534D") }
        found[dev.address] = ScannedDevice(
            address = dev.address,
            name = name,
            rssi = result.rssi,
            bonded = runCatching { dev.bondState == android.bluetooth.BluetoothDevice.BOND_BONDED }.getOrDefault(false),
            looksLikeVehicle = hintByName || hintByUuid,
        )
        _devices.update {
            found.values.sortedWith(
                compareByDescending<ScannedDevice> { it.looksLikeVehicle }.thenByDescending { it.rssi }
            )
        }
    }

    fun isBluetoothOn(): Boolean = adapter?.isEnabled == true

    fun start() {
        val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner ?: return
        found.clear()
        _devices.value = emptyList()
        _scanning.value = true
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(null, settings, callback)
    }

    fun stop() {
        _scanning.value = false
        runCatching { adapter?.bluetoothLeScanner?.stopScan(callback) }
    }
}
