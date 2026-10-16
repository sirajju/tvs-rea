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
    }

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
    )

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
        )
    }

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
}
