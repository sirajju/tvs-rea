package com.sxplus.rider.data.repo

import com.sxplus.rider.data.local.TokenStore
import com.sxplus.rider.data.remote.GraphQlApi
import com.sxplus.rider.data.remote.GraphQlRequest
import com.sxplus.rider.data.remote.TvsApi
import com.sxplus.rider.domain.BatteryPack
import com.sxplus.rider.domain.ChargingStatus
import com.sxplus.rider.domain.CrashConfig
import com.sxplus.rider.domain.EmergencyContact
import com.sxplus.rider.domain.Fence
import com.sxplus.rider.domain.FenceType
import com.sxplus.rider.domain.Ride
import com.sxplus.rider.domain.RideAggregate
import com.sxplus.rider.domain.SecondaryUser
import com.sxplus.rider.domain.Telemetry
import com.sxplus.rider.domain.VehicleSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.max
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connected/companion feature data.
 *
 * Telemetry and ride history now call the REAL backend (p360 GraphQL +
 * ICE REST) using the session's CODP token / access token. When there is no
 * session (direct-BLE mode) or mock mode is on, reads return null/sample so
 * the UI degrades honestly instead of showing fake live data.
 */
@Singleton
class FeatureRepository @Inject constructor(
    private val graphQlApi: GraphQlApi,
    private val tvsApi: TvsApi,
    private val codp: CodpTokenManager,
    private val tokens: TokenStore,
    private val flags: RuntimeFlags,
) {
    // ---- Live telemetry (GraphQL GetTvsmLatestDeviceData) --------------------
    private val telemetryQuery =
        "query GetTvsmLatestDeviceData(\$vin: String!) { getTvsmLatestDeviceData(vin: \$vin) { " +
            "response { speed soc odometer latitude longitude address ignition immobilizer " +
            "charging_status co2Saved is_crash is_fall is_sos is_tow " +
            "ev_range { dte_eco dte_power } tyre_pressure { front rear } } status statusMessage } }"

    /** Null when there is no live session or the call fails. */
    suspend fun telemetry(): Telemetry? = withContext(Dispatchers.IO) {
        if (flags.mockEnabled) return@withContext sampleTelemetry()
        val vin = tokens.vin ?: return@withContext null
        val bearer = codp.ensureBearer() ?: return@withContext null
        try {
            val res = graphQlApi.telemetry(
                bearer, GraphQlRequest(telemetryQuery, mapOf("vin" to vin), "GetTvsmLatestDeviceData"),
            )
            val r = res.body()?.data?.result?.response ?: return@withContext null
            Telemetry(
                speedKmph = (r.speed ?: 0.0).toInt(),
                soc = (r.soc ?: 0.0).toInt(),
                odometerKm = (r.odometer ?: 0.0).toInt(),
                rangeEcoKm = (r.evRange?.eco ?: 0.0).toInt(),
                rangePowerKm = (r.evRange?.power ?: 0.0).toInt(),
                latitude = r.latitude ?: 0.0,
                longitude = r.longitude ?: 0.0,
                address = r.address ?: "",
                ignitionOn = (r.ignition ?: 0) == 1,
                immobilized = (r.immobilizer ?: 0) == 1,
                chargingStatus = r.chargingStatus ?: "—",
                tyreFrontKpa = (r.tyrePressure?.front ?: 0.0).toInt(),
                tyreRearKpa = (r.tyrePressure?.rear ?: 0.0).toInt(),
                co2SavedKg = r.co2Saved ?: 0.0,
                isCrash = (r.isCrash ?: 0) == 1,
                isFall = (r.isFall ?: 0) == 1,
                isSos = (r.isSos ?: 0) == 1,
                isTow = (r.isTow ?: 0) == 1,
                updatedAtMs = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            null
        }
    }

    // ---- Rides (REST Travel/GetTravelList) -----------------------------------
    private val _rides = MutableStateFlow<List<Ride>>(emptyList())
    val rides: StateFlow<List<Ride>> = _rides.asStateFlow()

    private val _activeRide = MutableStateFlow<Ride?>(null)
    val activeRide: StateFlow<Ride?> = _activeRide.asStateFlow()

    /** Loads the real ride list for the active vehicle. */
    suspend fun loadRides() = withContext(Dispatchers.IO) {
        if (flags.mockEnabled) { _rides.value = sampleRides(); return@withContext }
        val uid = tokens.userId ?: return@withContext
        val uvId = tokens.userVehicleId ?: return@withContext
        val typeId = tokens.vehicleTypeId ?: "101"
        try {
            val res = tvsApi.getRideList(uvId, uid, typeId)
            val items = res.body()?.data?.results.orEmpty()
            _rides.value = items.map {
                val durMs = it.rideTimeMs ?: it.totalTimeMs ?: 0L
                Ride(
                    id = it.travelId ?: UUID.randomUUID().toString(),
                    startedAtMs = it.startMs ?: 0L,
                    distanceKm = (it.distanceKm ?: 0.0),
                    durationMin = (durMs / 60000L).toInt(),
                    avgSpeedKmph = it.avgSpeed ?: 0,
                    maxSpeedKmph = (it.topSpeed ?: 0.0).toInt(),
                    energyWh = 0,
                    startAddress = "",
                    endAddress = "",
                    co2SavedKg = 0.0,
                    synced = it.synced ?: true,
                )
            }
        } catch (e: Exception) {
            // keep whatever was loaded
        }
    }

    fun aggregate(): RideAggregate {
        val rs = _rides.value
        return RideAggregate(
            totalRides = rs.size,
            totalDistanceKm = rs.sumOf { it.distanceKm },
            totalTimeMin = rs.sumOf { it.durationMin },
            totalCo2Kg = rs.sumOf { it.co2SavedKg },
        )
    }

    fun startRide() {
        if (_activeRide.value != null) return
        _activeRide.value = Ride(
            id = "active", startedAtMs = System.currentTimeMillis(),
            distanceKm = 0.0, durationMin = 0, avgSpeedKmph = 0, maxSpeedKmph = 0,
            energyWh = 0, startAddress = "Current location", endAddress = "",
            co2SavedKg = 0.0, synced = false,
        )
    }

    fun stopRide() {
        val a = _activeRide.value ?: return
        val dur = max(1, ((System.currentTimeMillis() - a.startedAtMs) / 60000).toInt())
        val finished = a.copy(id = UUID.randomUUID().toString(), durationMin = dur, synced = false)
        _rides.update { listOf(finished) + it }
        _activeRide.value = null
    }

    suspend fun syncRides(): Int {
        val unsynced = _rides.value.count { !it.synced }
        loadRides()
        return unsynced
    }

    // ---- Crash alert (in-session; GraphQL/REST write wiring = next cycle) ----
    private val _crashConfig = MutableStateFlow(CrashConfig())
    val crashConfig: StateFlow<CrashConfig> = _crashConfig.asStateFlow()

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    fun updateCrashConfig(block: (CrashConfig) -> CrashConfig) = _crashConfig.update(block)
    fun addContact(name: String, mobile: String) {
        if (name.isBlank() || mobile.isBlank()) return
        _contacts.update { it + EmergencyContact(UUID.randomUUID().toString(), name.trim(), mobile.trim()) }
    }
    fun removeContact(id: String) = _contacts.update { list -> list.filterNot { it.id == id } }

    // ---- Geo / Time fence ----------------------------------------------------
    private val _fences = MutableStateFlow<List<Fence>>(emptyList())
    val fences: StateFlow<List<Fence>> = _fences.asStateFlow()
    private val _overspeedLimit = MutableStateFlow(60)
    val overspeedLimit: StateFlow<Int> = _overspeedLimit.asStateFlow()
    fun setOverspeedLimit(v: Int) { _overspeedLimit.value = v.coerceIn(20, 120) }
    fun addFence(fence: Fence) = _fences.update { it + fence.copy(id = UUID.randomUUID().toString()) }
    fun removeFence(id: String) = _fences.update { list -> list.filterNot { it.id == id } }
    fun toggleFence(id: String) = _fences.update { list ->
        list.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
    }

    // ---- Share vehicle -------------------------------------------------------
    private val _secondaryUsers = MutableStateFlow(
        listOf(SecondaryUser("me", "You", "—", "", isPrimary = true))
    )
    val secondaryUsers: StateFlow<List<SecondaryUser>> = _secondaryUsers.asStateFlow()
    fun addSecondaryUser(name: String, mobile: String, email: String) {
        if (name.isBlank() || mobile.isBlank()) return
        _secondaryUsers.update {
            it + SecondaryUser(UUID.randomUUID().toString(), name.trim(), mobile.trim(), email.trim())
        }
    }
    fun removeSecondaryUser(appUserId: String) =
        _secondaryUsers.update { list -> list.filterNot { it.appUserId == appUserId && !it.isPrimary } }

    // ---- Vehicle settings ----------------------------------------------------
    private val _vehicleSettings = MutableStateFlow(VehicleSettings())
    val vehicleSettings: StateFlow<VehicleSettings> = _vehicleSettings.asStateFlow()
    fun updateVehicleSettings(block: (VehicleSettings) -> VehicleSettings) = _vehicleSettings.update(block)

    // ---- Charging (GraphQL GetBatteriesLatestData) --------------------------
    private val batteriesQuery =
        "query GetBatteriesLatestData(\$vin: String!) { getBatteriesLatestData(vin: \$vin) { status " +
            "response { bat_a_soc bat_b_soc bat_a_charging_status bat_b_charging_status " +
            "time_to_charge_on_vehicle eco_range pow_range } } }"

    suspend fun charging(): ChargingStatus? = withContext(Dispatchers.IO) {
        if (flags.mockEnabled) return@withContext sampleCharging()
        val vin = tokens.vin ?: return@withContext null
        val bearer = codp.ensureBearer() ?: return@withContext null
        try {
            val res = graphQlApi.batteries(
                bearer, GraphQlRequest(batteriesQuery, mapOf("vin" to vin), "GetBatteriesLatestData"),
            )
            val r = res.body()?.data?.result?.response ?: return@withContext null
            ChargingStatus(
                onVehicle = true,
                chargerConnected = (r.batACharging?.contains("charg", true) == true) ||
                    (r.batBCharging?.contains("charg", true) == true),
                chargerType = "—",
                packs = listOf(
                    BatteryPack("BAT-A", "Battery A", (r.batASoc ?: 0.0).toInt(),
                        r.batACharging ?: "—", 0, r.timeToCharge ?: 0),
                    BatteryPack("BAT-B", "Battery B", (r.batBSoc ?: 0.0).toInt(),
                        r.batBCharging ?: "—", 0, r.timeToCharge ?: 0),
                ),
            )
        } catch (e: Exception) {
            null
        }
    }

    // ---- Sample data (mock mode only) ---------------------------------------
    private fun sampleTelemetry() = Telemetry(
        speedKmph = Random.nextInt(0, 60), soc = Random.nextInt(60, 90), odometerKm = 4821,
        rangeEcoKm = 95, rangePowerKm = 72, latitude = 10.0159, longitude = 76.3419,
        address = "Kakkanad, Kochi", ignitionOn = true, chargingStatus = "Idle",
        tyreFrontKpa = 220, tyreRearKpa = 225, co2SavedKg = 128.4, updatedAtMs = System.currentTimeMillis(),
    )

    private fun sampleCharging() = ChargingStatus(
        onVehicle = true, chargerConnected = true, chargerType = "Home",
        packs = listOf(
            BatteryPack("BAT-A", "Battery A", 82, "Charging", 29, 35),
            BatteryPack("BAT-B", "Battery B", 78, "Charging", 30, 41),
        ),
    )

    private fun sampleRides(): List<Ride> {
        val now = System.currentTimeMillis(); val day = 86_400_000L
        return listOf(
            Ride("r1", now - day, 12.4, 31, 24, 58, 272, "Kakkanad", "Fort Kochi", 1.4),
            Ride("r2", now - 2 * day, 7.8, 19, 25, 49, 171, "Home", "Office", 0.9),
        )
    }
}
