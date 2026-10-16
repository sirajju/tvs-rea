package com.sxplus.rider.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.sxplus.rider.domain.Ride
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.rideDataStore by preferencesDataStore(name = "sxplus_rides")

/** Durable, local store of auto-recorded rides (survives app kill). */
@Singleton
class RideStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("recorded_rides_json")
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter<List<Ride>>(
        Types.newParameterizedType(List::class.java, Ride::class.java)
    )

    val rides: Flow<List<Ride>> = context.rideDataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun add(ride: Ride) {
        context.rideDataStore.edit { prefs ->
            val current = prefs[key]?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: emptyList()
            prefs[key] = adapter.toJson(listOf(ride) + current)
        }
    }

    suspend fun markSynced(id: String) {
        context.rideDataStore.edit { prefs ->
            val current = prefs[key]?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: return@edit
            prefs[key] = adapter.toJson(current.map { if (it.id == id) it.copy(synced = true) else it })
        }
    }
}
