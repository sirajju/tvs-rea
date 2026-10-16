package com.sxplus.rider.domain

/** Models for the connected/companion features. Shapes mirror the real
 *  TVS GraphQL (p360) and REST operations noted on each repository method. */

// ---- Live telemetry (GraphQL GetTvsmLatestDeviceData) -----------------------
data class Telemetry(
    val speedKmph: Int = 0,
    val soc: Int = 0,                 // state of charge %
    val odometerKm: Int = 0,
    val rangeEcoKm: Int = 0,
    val rangePowerKm: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val ignitionOn: Boolean = false,
    val immobilized: Boolean = false,
    val chargingStatus: String = "Idle",
    val tyreFrontKpa: Int = 0,
    val tyreRearKpa: Int = 0,
    val co2SavedKg: Double = 0.0,
    val isCrash: Boolean = false,
    val isFall: Boolean = false,
    val isSos: Boolean = false,
    val isTow: Boolean = false,
    val updatedAtMs: Long = 0,
)

// ---- Ride history & management (REST Travel/GetTravelList) ------------------
data class Ride(
    val id: String,
    val startedAtMs: Long,
    val distanceKm: Double,
    val durationMin: Int,
    val avgSpeedKmph: Int,
    val maxSpeedKmph: Int,
    val energyWh: Int,
    val startAddress: String,
    val endAddress: String,
    val co2SavedKg: Double,
    val synced: Boolean = true,
)

data class RideAggregate(
    val totalRides: Int,
    val totalDistanceKm: Double,
    val totalTimeMin: Int,
    val totalCo2Kg: Double,
)

// ---- Crash alert (GraphQL addEmergencyContacts + SetVehicleSettings CRASH) --
data class EmergencyContact(
    val id: String,
    val name: String,
    val mobile: String,
    val countryCode: String = "+91",
    val email: String? = null,
)

data class CrashConfig(
    val enabled: Boolean = true,
    val fallEnabled: Boolean = true,
    val sensitivity: Int = 70,        // confidence threshold 0..100
    val countdownSec: Int = 180,
)

// ---- Geo / Time fence (GraphQL setTimeFence / setOverspeedThreshold) --------
data class Fence(
    val id: String,
    val name: String,
    val type: FenceType,
    val fromTime: String = "",        // "HH:mm"
    val toTime: String = "",
    val weekdays: Set<Int> = emptySet(),  // 1=Mon..7=Sun
    val overspeedLimit: Int = 60,
    val radiusM: Int = 200,
    val enabled: Boolean = true,
    val pushEnabled: Boolean = true,
    val smsEnabled: Boolean = false,
)

enum class FenceType { GEO, TIME }

// ---- Share vehicle (GraphQL addSecondaryUser / removeUserVehicleAssociation)
data class SecondaryUser(
    val appUserId: String,
    val name: String,
    val mobile: String,
    val email: String,
    val isPrimary: Boolean = false,
)

// ---- Vehicle settings (GraphQL SetVehicleSettings: IMMOB/THEFT/TLBATT/…) ----
data class VehicleSettings(
    val immobilizer: Boolean = false,
    val theftMode: Boolean = false,
    val autoLock: Boolean = true,
    val lowBatteryAlertPct: Int = 15,
    val heartbeatMinutes: Int = 10,
    val headlampAssist: Boolean = true,
)

// ---- Charging (GraphQL getHomechargerLatestData / GetBatteriesLatestData) ---
data class BatteryPack(
    val uid: String,
    val label: String,
    val soc: Int,
    val chargingStatus: String,       // "Charging" | "Full" | "Idle" | "Discharging"
    val temperatureC: Int,
    val timeToFullMin: Int,
)

data class ChargingStatus(
    val onVehicle: Boolean,
    val packs: List<BatteryPack>,
    val chargerConnected: Boolean,
    val chargerType: String,          // "Home" | "Portable" | "None"
)
