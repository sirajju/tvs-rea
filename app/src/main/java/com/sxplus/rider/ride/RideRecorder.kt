package com.sxplus.rider.ride

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.sxplus.rider.data.local.RideStore
import com.sxplus.rider.data.repo.FeatureRepository
import com.sxplus.rider.domain.ActiveRide
import dagger.hilt.android.qualifiers.ApplicationContext
import com.sxplus.rider.domain.Ride
import com.sxplus.rider.domain.Waypoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auto ride recorder. Starts when the vehicle is connected + ignition on,
 * accumulates distance/speed/waypoints from cloud telemetry, and is finalized
 * by the service when the vehicle has been disconnected for 5 minutes.
 *
 * Distance uses the odometer delta when available (whole-km from telemetry),
 * else GPS Haversine over the recorded waypoints.
 */
@Singleton
class RideRecorder @Inject constructor(
    private val repo: FeatureRepository,
    private val rideStore: RideStore,
    @ApplicationContext private val context: Context,
) {
    private val _active = MutableStateFlow<ActiveRide?>(null)
    val active = _active.asStateFlow()

    private var startTime = 0L
    private var startOdo = -1
    private var lastOdo = -1
    private var maxSpeed = 0
    private var sumSpeed = 0L
    private var count = 0
    private var paused = false
    private var gpsMeters = 0.0
    private var lastLat = Double.NaN
    private var lastLng = Double.NaN
    private val waypoints = mutableListOf<Waypoint>()

    fun isRecording() = _active.value != null

    /**
     * One sampling tick while connected. Uses cloud telemetry when available
     * (IoT/EV: odometer + ignition), else falls back to phone GPS (direct-BLE
     * mode) so rides still record. Starts a ride when ignition is on, or
     * immediately in direct mode (ignition can't be read there).
     */
    suspend fun tick() {
        val t = repo.telemetry()
        val lat: Double; val lng: Double; val speed: Int; val odo: Int; val ignitionOn: Boolean
        if (t != null) {
            lat = t.latitude; lng = t.longitude; speed = t.speedKmph
            odo = t.odometerKm; ignitionOn = t.ignitionOn
        } else {
            val loc = phoneLocation()
            lat = loc?.latitude ?: 0.0
            lng = loc?.longitude ?: 0.0
            speed = ((loc?.speed ?: 0f) * 3.6f).toInt()   // m/s -> km/h
            odo = -1                                       // no vehicle odometer in direct mode
            ignitionOn = true                              // record while connected
        }

        if (!isRecording()) {
            if (!ignitionOn) return
            begin(odo)
        }
        if (startOdo < 0 && odo > 0) { startOdo = odo; lastOdo = odo }
        if (odo > 0) lastOdo = odo
        if (lat != 0.0 && lng != 0.0) {
            if (!lastLat.isNaN()) gpsMeters += haversineMeters(lastLat, lastLng, lat, lng)
            lastLat = lat; lastLng = lng
        }
        maxSpeed = max(maxSpeed, speed)
        sumSpeed += speed
        count++
        waypoints.add(Waypoint(System.currentTimeMillis(), lat, lng, speed, odo.coerceAtLeast(0)))
        if (paused) paused = false
        _active.value = ActiveRide(startTime, distanceKm(), waypoints.size, speed, paused)
    }

    @SuppressLint("MissingPermission")
    private fun phoneLocation(): Location? = runCatching {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
    }.getOrNull()

    fun pause() {
        if (isRecording()) { paused = true; _active.update { it?.copy(paused = true) } }
    }

    /** Finalize + persist the ride (if it covered real distance). Returns the saved ride. */
    suspend fun finalizeAndSave(): Ride? {
        if (!isRecording()) return null
        val dist = distanceKm()
        val durMin = max(1, ((System.currentTimeMillis() - startTime) / 60_000L).toInt())
        val ride = Ride(
            id = UUID.randomUUID().toString(),
            startedAtMs = startTime,
            distanceKm = round1(dist),
            durationMin = durMin,
            avgSpeedKmph = if (count > 0) (sumSpeed / count).toInt() else 0,
            maxSpeedKmph = maxSpeed,
            energyWh = (dist * 22).toInt(),
            startAddress = "",
            endAddress = "",
            co2SavedKg = round1(dist * 0.11),
            synced = false,
        )
        val saved = dist >= 0.1
        if (saved) rideStore.add(ride)
        reset()
        return if (saved) ride else null
    }

    private fun begin(odo: Int) {
        startTime = System.currentTimeMillis()
        startOdo = if (odo > 0) odo else -1
        lastOdo = startOdo
        maxSpeed = 0; sumSpeed = 0; count = 0; paused = false
        gpsMeters = 0.0; lastLat = Double.NaN; lastLng = Double.NaN
        waypoints.clear()
        _active.value = ActiveRide(startTime, 0.0, 0, 0, false)
    }

    private fun reset() {
        _active.value = null
        waypoints.clear()
        startOdo = -1; lastOdo = -1; maxSpeed = 0; sumSpeed = 0; count = 0
        gpsMeters = 0.0; lastLat = Double.NaN; lastLng = Double.NaN; paused = false
    }

    private fun distanceKm(): Double {
        val odoDelta = if (startOdo in 0..lastOdo) (lastOdo - startOdo) else 0
        return if (odoDelta > 0) odoDelta.toDouble() else gpsMeters / 1000.0
    }

    private fun round1(v: Double) = (v * 10).toInt() / 10.0

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
