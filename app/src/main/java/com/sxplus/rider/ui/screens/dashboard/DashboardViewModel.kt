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
    private val themeController: com.sxplus.rider.ui.theme.ThemeController,
) : AndroidViewModel(app) {

    private val _ui = MutableStateFlow(DashboardUiState())
    val ui: StateFlow<DashboardUiState> = _ui.asStateFlow()

    /** Cloud vehicles cached so the settings-driven rebuild keeps them. */
    private var cloudVehicles: List<Vehicle> = emptyList()

    val bleState: StateFlow<BleState> = ble.state
    val bleLogs = ble.logs

    init {
        load()
        viewModelScope.launch {
            settings.settings.collect { s ->
                _ui.update { cur ->
                    // Rebuild the vehicle list from cloud (cached) + all directly-paired
                    // devices, and honour the persisted selection.
                    val local = s.pairedDevices.map { d ->
                        Vehicle("", "", d.name, d.name, null, d.mac)
                    }
                    val merged = (cloudVehicles + local).distinctBy { it.bleMac ?: it.key }
                    val vehicles = merged.ifEmpty { cur.vehicles }
                    val selected = vehicles.firstOrNull { it.key == s.selectedVehicleKey }
                        ?: cur.selected?.let { sel -> vehicles.firstOrNull { it.key == sel.key } }
                        ?: vehicles.firstOrNull()
                    cur.copy(
                        autoConnect = s.autoConnect,
                        autoActionOnApproach = s.autoActionOnApproach,
                        approachCommand = s.approachCommand,
                        vehicles = vehicles,
                        selected = selected,
                        error = if (vehicles.isNotEmpty()) null else cur.error,
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
                val s = settings.settings.first()

                // Cloud vehicles (if signed in) — cached for the reactive rebuild.
                cloudVehicles = when (val r = repo.vehicles()) {
                    is Outcome.Ok -> r.value
                    is Outcome.Err -> emptyList()
                }
                val local = s.pairedDevices.map { d -> Vehicle("", "", d.name, d.name, null, d.mac) }
                val vehicles = (cloudVehicles + local).distinctBy { it.bleMac ?: it.key }

                val selected = vehicles.firstOrNull { it.key == s.selectedVehicleKey }
                    ?: vehicles.firstOrNull { it.bleMac == s.pairedMac }
                    ?: vehicles.firstOrNull()

                _ui.update {
                    it.copy(
                        loading = false, user = user, vehicles = vehicles, selected = selected,
                        error = if (vehicles.isEmpty()) "No vehicle paired yet — tap Pair to scan." else null,
                    )
                }
                selected?.let { applyVehicleContext(it) }
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.message ?: "Could not load vehicles") }
            }
        }
    }

    /** Switch the active vehicle: persist, retarget cloud context, reconnect BLE. */
    fun selectVehicle(v: Vehicle) {
        val prevMac = _ui.value.selected?.bleMac
        _ui.update { it.copy(selected = v) }
        viewModelScope.launch {
            applyVehicleContext(v)
            if (!v.bleMac.isNullOrBlank() && v.bleMac != prevMac) {
                ble.disconnect()
                ble.connect(v.bleMac)
            }
        }
    }

    private suspend fun applyVehicleContext(v: Vehicle) {
        settings.setSelectedVehicleKey(v.key)
        v.bleMac?.let { settings.setPairedVehicle(it, v.displayName) }
        repo.setActiveVehicle(v)
        // Apply this vehicle's accent theme.
        val accents = settings.settings.first().vehicleAccents
        themeController.setAccent(accents[v.key] ?: 0)
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
