package com.sxplus.rider.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxplus.rider.ble.BleKeylessManager
import com.sxplus.rider.ble.BleScanner
import com.sxplus.rider.ble.ScannedDevice
import com.sxplus.rider.data.local.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanner: BleScanner,
    private val ble: BleKeylessManager,
    private val settings: SettingsStore,
) : ViewModel() {

    val devices = scanner.devices
    val scanning = scanner.scanning

    fun isBluetoothOn() = scanner.isBluetoothOn()

    fun startScan() = scanner.start()
    fun stopScan() = scanner.stop()

    /** Remember this device as the paired vehicle and connect to it. */
    fun pair(device: ScannedDevice, onPaired: () -> Unit) {
        scanner.stop()
        viewModelScope.launch {
            settings.setPairedVehicle(device.address, device.name)
            ble.connect(device.address)
            onPaired()
        }
    }

    override fun onCleared() {
        scanner.stop()
        super.onCleared()
    }
}
