package com.sxplus.rider.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sxplus.rider.ble.BleKeylessManager
import com.sxplus.rider.ble.BleState
import com.sxplus.rider.ble.KeylessCommand
import com.sxplus.rider.ble.KeylessResult
import com.sxplus.rider.data.local.SettingsStore
import com.sxplus.rider.data.repo.TvsRepository
import com.sxplus.rider.domain.Outcome
import com.sxplus.rider.domain.UserProfile
import com.sxplus.rider.domain.Vehicle
import com.sxplus.rider.proximity.ProximityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val user: UserProfile? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val selected: Vehicle? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val toast: String? = null,
    val autoConnect: Boolean = true,
    val autoActionOnApproach: Boolean = true,
    val approachCommand: KeylessCommand = KeylessCommand.FIND_VEHICLE,
    val sendingCommand: KeylessCommand? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    app: Application,
    private val repo: TvsRepository,
    private val ble: BleKeylessManager,
    private val settings: SettingsStore,
) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(DashboardUiState())
    val ui: StateFlow<DashboardUiState> = _ui.asStateFlow()

    val bleState: StateFlow<BleState> = ble.state
    val bleLogs = ble.logs

    init {
        load()
        viewModelScope.launch {
            settings.settings.collect { s ->
                _ui.update {
                    it.copy(
                        autoConnect = s.autoConnect,
                        autoActionOnApproach = s.autoActionOnApproach,
                        approachCommand = s.approachCommand,
                    )
                }
            }
        }
    }

    fun load() {
        _ui.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
            val user = repo.currentUser()
            val paired = settings.settings.first()

            // Cloud vehicles (if signed in); empty otherwise.
            val cloud = when (val r = repo.vehicles()) {
                is Outcome.Ok -> r.value
                is Outcome.Err -> emptyList()
            }
            // Locally-paired vehicle from the BLE scanner (direct mode, no sign-in).
            val local = paired.pairedMac?.let { mac ->
                Vehicle(
                    userVehicleId = "local",
                    vin = "",
                    model = paired.pairedName ?: "My Vehicle",
                    nickName = paired.pairedName,
                    registrationNumber = null,
                    bleMac = mac,
                )
            }

            val vehicles = if (cloud.isNotEmpty()) cloud else listOfNotNull(local)
            val selected = vehicles.firstOrNull { it.bleMac == paired.pairedMac } ?: vehicles.firstOrNull()
            _ui.update {
                it.copy(
                    loading = false,
                    user = user,
                    vehicles = vehicles,
                    selected = selected,
                    error = if (vehicles.isEmpty()) "No vehicle paired yet — tap Pair to scan." else null,
                )
            }
            selected?.bleMac?.let { mac -> settings.setPairedVehicle(mac, selected.displayName) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Could not load vehicles") }
            }
        }
    }

    fun selectVehicle(v: Vehicle) {
        _ui.update { it.copy(selected = v) }
        viewModelScope.launch { v.bleMac?.let { settings.setPairedVehicle(it, v.displayName) } }
    }

    fun connect() {
        val mac = _ui.value.selected?.bleMac
        if (mac.isNullOrBlank()) { toast("No BLE MAC for this vehicle"); return }
        ble.connect(mac)
    }

    fun disconnect() = ble.disconnect()

    fun sendCommand(cmd: KeylessCommand) {
        if (ble.state.value != BleState.READY) {
            // auto-connect first, then the user can retry; keeps UX snappy
            connect()
            toast("Connecting… tap again once connected")
            return
        }
        _ui.update { it.copy(sendingCommand = cmd) }
        viewModelScope.launch {
            val result = ble.sendKeylessCommand(cmd)
            val msg = when (result) {
                is KeylessResult.Success -> "${cmd.label} sent ✓"
                is KeylessResult.ChallengeTimeout -> result.message
                is KeylessResult.NotReady -> result.message
                is KeylessResult.Error -> "Error: ${result.message}"
            }
            _ui.update { it.copy(sendingCommand = null, toast = msg) }
        }
    }

    fun setAutoConnect(v: Boolean) = viewModelScope.launch {
        settings.setAutoConnect(v)
        runCatching {
            if (v) ProximityService.start(getApplication()) else ProximityService.stop(getApplication())
        }
    }

    fun setAutoActionOnApproach(v: Boolean) = viewModelScope.launch { settings.setAutoActionOnApproach(v) }
    fun setApproachCommand(c: KeylessCommand) = viewModelScope.launch { settings.setApproachCommand(c) }

    fun startProximity() {
        runCatching { ProximityService.start(getApplication()) }
    }

    fun logout() {
        ble.disconnect()
        ProximityService.stop(getApplication())
        repo.logout()
    }

    fun toast(msg: String) = _ui.update { it.copy(toast = msg) }
    fun clearToast() = _ui.update { it.copy(toast = null) }
}
