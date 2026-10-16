package com.sxplus.rider.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sxplus.rider.ble.KeylessCommand
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "sxplus_settings")

/** User preferences: paired vehicle, proximity behaviour, auto-actions. */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PAIRED_MAC = stringPreferencesKey("paired_mac")
        val PAIRED_NAME = stringPreferencesKey("paired_name")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val AUTO_ACTION_ON_APPROACH = booleanPreferencesKey("auto_action_on_approach")
        val APPROACH_COMMAND = stringPreferencesKey("approach_command")
        val RSSI_ENTER = intPreferencesKey("rssi_enter")
        val RSSI_EXIT = intPreferencesKey("rssi_exit")
        val AUTO_LOCK_ON_LEAVE = booleanPreferencesKey("auto_lock_on_leave")
        val AUTO_RECORD_RIDES = booleanPreferencesKey("auto_record_rides")
        val SELECTED_VEHICLE_KEY = stringPreferencesKey("selected_vehicle_key")
        val PAIRED_DEVICES = stringPreferencesKey("paired_devices_csv")
        val LAST_PARKED = stringPreferencesKey("last_parked")        // "lat|lng|ts|name"
        val THEME_MODE = stringPreferencesKey("theme_mode")          // SYSTEM|LIGHT|DARK
        val VEHICLE_ACCENTS = stringPreferencesKey("vehicle_accents") // "key|idx;..."
        val OVERSPEED_ALERTS = booleanPreferencesKey("overspeed_alerts")
    }

    enum class ThemeMode { SYSTEM, LIGHT, DARK }
    data class ParkedLocation(val lat: Double, val lng: Double, val timeMs: Long, val vehicle: String)

    data class Settings(
        val pairedMac: String? = null,
        val pairedName: String? = null,
        val autoConnect: Boolean = true,
        val autoActionOnApproach: Boolean = true,
        val approachCommand: KeylessCommand = KeylessCommand.FIND_VEHICLE,
        val rssiEnter: Int = -65,   // closer than this => "arrived"
        val rssiExit: Int = -85,    // farther than this => "left"
        val autoLockOnLeave: Boolean = false,
        val autoRecordRides: Boolean = true,
        val selectedVehicleKey: String? = null,
        val pairedDevices: List<PairedDevice> = emptyList(),
        val lastParked: ParkedLocation? = null,
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val vehicleAccents: Map<String, Int> = emptyMap(),
        val overspeedAlerts: Boolean = true,
    )

    data class PairedDevice(val mac: String, val name: String)

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            pairedMac = p[Keys.PAIRED_MAC],
            pairedName = p[Keys.PAIRED_NAME],
            autoConnect = p[Keys.AUTO_CONNECT] ?: true,
            autoActionOnApproach = p[Keys.AUTO_ACTION_ON_APPROACH] ?: true,
            approachCommand = p[Keys.APPROACH_COMMAND]?.let { runCatching { KeylessCommand.valueOf(it) }.getOrNull() }
                ?: KeylessCommand.FIND_VEHICLE,
            rssiEnter = p[Keys.RSSI_ENTER] ?: -65,
            rssiExit = p[Keys.RSSI_EXIT] ?: -85,
            autoLockOnLeave = p[Keys.AUTO_LOCK_ON_LEAVE] ?: false,
            autoRecordRides = p[Keys.AUTO_RECORD_RIDES] ?: true,
            selectedVehicleKey = p[Keys.SELECTED_VEHICLE_KEY],
            pairedDevices = parsePaired(p[Keys.PAIRED_DEVICES]),
            lastParked = parseParked(p[Keys.LAST_PARKED]),
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME_MODE] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            vehicleAccents = parseAccents(p[Keys.VEHICLE_ACCENTS]),
            overspeedAlerts = p[Keys.OVERSPEED_ALERTS] ?: true,
        )
    }

    private fun parseParked(s: String?): ParkedLocation? {
        val p = s?.split("|") ?: return null
        return runCatching {
            ParkedLocation(p[0].toDouble(), p[1].toDouble(), p[2].toLong(), p.getOrElse(3) { "Vehicle" })
        }.getOrNull()
    }

    private fun parseAccents(csv: String?): Map<String, Int> =
        csv?.split(";")?.mapNotNull {
            val parts = it.split("|")
            if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
        }?.toMap() ?: emptyMap()

    private fun parsePaired(csv: String?): List<PairedDevice> =
        csv?.split(";")?.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 2 && parts[0].isNotBlank()) PairedDevice(parts[0], parts[1]) else null
        } ?: emptyList()

    suspend fun setPairedVehicle(mac: String, name: String?) = context.dataStore.edit {
        it[Keys.PAIRED_MAC] = mac
        if (name != null) it[Keys.PAIRED_NAME] = name
    }

    suspend fun setAutoConnect(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_CONNECT] = v }
    suspend fun setAutoActionOnApproach(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_ACTION_ON_APPROACH] = v }
    suspend fun setApproachCommand(c: KeylessCommand) = context.dataStore.edit { it[Keys.APPROACH_COMMAND] = c.name }
    suspend fun setRssiEnter(v: Int) = context.dataStore.edit { it[Keys.RSSI_ENTER] = v }
    suspend fun setRssiExit(v: Int) = context.dataStore.edit { it[Keys.RSSI_EXIT] = v }
    suspend fun setAutoLockOnLeave(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_LOCK_ON_LEAVE] = v }
    suspend fun setAutoRecordRides(v: Boolean) = context.dataStore.edit { it[Keys.AUTO_RECORD_RIDES] = v }

    suspend fun setSelectedVehicleKey(key: String) = context.dataStore.edit { it[Keys.SELECTED_VEHICLE_KEY] = key }

    suspend fun setLastParked(lat: Double, lng: Double, timeMs: Long, vehicle: String) =
        context.dataStore.edit { it[Keys.LAST_PARKED] = "$lat|$lng|$timeMs|$vehicle" }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setOverspeedAlerts(v: Boolean) = context.dataStore.edit { it[Keys.OVERSPEED_ALERTS] = v }

    suspend fun setVehicleAccent(key: String, idx: Int) = context.dataStore.edit { p ->
        val m = parseAccents(p[Keys.VEHICLE_ACCENTS]).toMutableMap()
        m[key] = idx
        p[Keys.VEHICLE_ACCENTS] = m.entries.joinToString(";") { "${it.key}|${it.value}" }
    }

    /** Remember a directly-paired BLE device (for multi-vehicle in no-login mode). */
    suspend fun addPairedDevice(mac: String, name: String?) = context.dataStore.edit { p ->
        val current = parsePaired(p[Keys.PAIRED_DEVICES]).filterNot { it.mac.equals(mac, true) }
        val updated = current + PairedDevice(mac, name ?: "Vehicle")
        p[Keys.PAIRED_DEVICES] = updated.joinToString(";") { "${it.mac}|${it.name}" }
    }
}
