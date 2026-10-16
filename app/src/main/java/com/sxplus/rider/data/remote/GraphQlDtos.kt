package com.sxplus.rider.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Generic GraphQL HTTP request body: POST /gapi { query, variables, operationName }. */
@JsonClass(generateAdapter = true)
data class GraphQlRequest(
    val query: String,
    val variables: Map<String, Any?>,
    val operationName: String? = null,
)

// ---- GetTvsmLatestDeviceData ------------------------------------------------
@JsonClass(generateAdapter = true)
data class TelemetryEnvelope(val data: TelemetryData?)

@JsonClass(generateAdapter = true)
data class TelemetryData(
    @Json(name = "getTvsmLatestDeviceData") val result: TelemetryResult?,
)

@JsonClass(generateAdapter = true)
data class TelemetryResult(
    val status: Int? = null,
    val statusMessage: String? = null,
    val response: TelemetryResponse? = null,
)

@JsonClass(generateAdapter = true)
data class TelemetryResponse(
    val speed: Double? = null,
    val soc: Double? = null,
    val odometer: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val ignition: Int? = null,
    val immobilizer: Int? = null,
    @Json(name = "charging_status") val chargingStatus: String? = null,
    val co2Saved: Double? = null,
    @Json(name = "is_crash") val isCrash: Int? = null,
    @Json(name = "is_fall") val isFall: Int? = null,
    @Json(name = "is_sos") val isSos: Int? = null,
    @Json(name = "is_tow") val isTow: Int? = null,
    @Json(name = "ev_range") val evRange: EvRange? = null,
    @Json(name = "tyre_pressure") val tyrePressure: TyrePressure? = null,
)

@JsonClass(generateAdapter = true)
data class EvRange(
    @Json(name = "dte_eco") val eco: Double? = null,
    @Json(name = "dte_power") val power: Double? = null,
)

@JsonClass(generateAdapter = true)
data class TyrePressure(
    val front: Double? = null,
    val rear: Double? = null,
)

// ---- GetBatteriesLatestData -------------------------------------------------
@JsonClass(generateAdapter = true)
data class BatteriesEnvelope(val data: BatteriesData?)

@JsonClass(generateAdapter = true)
data class BatteriesData(
    @Json(name = "getBatteriesLatestData") val result: BatteriesResult?,
)

@JsonClass(generateAdapter = true)
data class BatteriesResult(
    val status: Int? = null,
    val response: BatteriesResponse? = null,
)

@JsonClass(generateAdapter = true)
data class BatteriesResponse(
    @Json(name = "bat_a_soc") val batASoc: Double? = null,
    @Json(name = "bat_b_soc") val batBSoc: Double? = null,
    @Json(name = "bat_a_charging_status") val batACharging: String? = null,
    @Json(name = "bat_b_charging_status") val batBCharging: String? = null,
    @Json(name = "time_to_charge_on_vehicle") val timeToCharge: Int? = null,
    @Json(name = "eco_range") val ecoRange: Double? = null,
    @Json(name = "pow_range") val powRange: Double? = null,
)
