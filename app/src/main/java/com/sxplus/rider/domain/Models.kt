package com.sxplus.rider.domain

/** Clean UI-facing models (decoupled from network DTOs). */
data class UserProfile(
    val userId: String,
    val name: String,
    val mobile: String,
    val email: String? = null,
)

data class Vehicle(
    val userVehicleId: String,
    val vin: String,
    val model: String,
    val nickName: String?,
    val registrationNumber: String?,
    val bleMac: String?,
    val imageUrl: String? = null,
    val vehicleTypeId: String? = null,
    val codpToken: String? = null,
) {
    val displayName: String get() = nickName?.takeIf { it.isNotBlank() } ?: model
    /** Stable key for selection/persistence (cloud id, else local BLE mac). */
    val key: String get() = userVehicleId.ifBlank { "local:${bleMac.orEmpty()}" }
}

/** Generic outcome wrapper for repository calls. */
sealed interface Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>
    data class Err(val message: String) : Outcome<Nothing>
}
