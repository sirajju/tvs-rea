package com.sxplus.rider.data.repo

import com.sxplus.rider.data.local.TokenStore
import com.sxplus.rider.data.remote.LoginRequest
import com.sxplus.rider.data.remote.TvsApi
import com.sxplus.rider.data.remote.VerifyOtpRequest
import com.sxplus.rider.domain.Outcome
import com.sxplus.rider.domain.UserProfile
import com.sxplus.rider.domain.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the real login → OTP → profile → vehicle-list flow.
 * Honours RuntimeFlags.mockEnabled (debug only) to run fully offline.
 */
@Singleton
class TvsRepository @Inject constructor(
    private val api: TvsApi,
    private val tokens: TokenStore,
    private val flags: RuntimeFlags,
) {
    /** Step 1: request an OTP for a phone number. */
    suspend fun requestOtp(mobile: String): Outcome<Unit> = withContext(Dispatchers.IO) {
        if (flags.mockEnabled) { delay(400); return@withContext Outcome.Ok(Unit) }
        try {
            val res = api.login(LoginRequest(mobileNumber = mobile))
            if (res.isSuccessful) Outcome.Ok(Unit)
            else Outcome.Err("Login failed (${res.code()})")
        } catch (e: Exception) {
            Outcome.Err(e.message ?: "Network error")
        }
    }

    /** Step 2: verify OTP, persist tokens + profile, return the user. */
    suspend fun verifyOtp(mobile: String, otp: String): Outcome<UserProfile> = withContext(Dispatchers.IO) {
        if (flags.mockEnabled) {
            delay(400)
            tokens.accessToken = "mock-access"
            tokens.userId = "MOCK-USER-1"
            tokens.userName = "Test Rider"
            tokens.mobile = mobile
            return@withContext Outcome.Ok(UserProfile("MOCK-USER-1", "Test Rider", mobile))
        }
        try {
            val res = api.verifyOtp(body = VerifyOtpRequest(mobileNumber = mobile, otp = otp))
            val body = res.body()
            val d = body?.data
            val ice = d?.iceUser?.firstOrNull()
            // Handle both the iceUser-wrapped shape and the flat Data-object shape.
            val accessToken = ice?.accessToken ?: d?.accessToken
            val refreshToken = ice?.refreshToken ?: d?.refreshToken
            val uid = ice?.userId ?: d?.userId
            val fullName = ice?.fullName ?: d?.fullName
            val mob = ice?.mobileNumber ?: d?.mobileNumber
            val email = ice?.email ?: d?.email
            if (!res.isSuccessful || accessToken.isNullOrBlank()) {
                return@withContext Outcome.Err(
                    body?.message?.takeIf { !res.isSuccessful } ?: "Could not sign in (${res.code()})"
                )
            }
            tokens.accessToken = accessToken
            tokens.refreshToken = refreshToken
            tokens.evAccessToken = d?.evUser?.firstOrNull()?.accessToken
            tokens.userId = uid?.toString() ?: ""
            tokens.userName = fullName?.trim()?.ifBlank { "Rider" } ?: "Rider"
            tokens.mobile = mob ?: mobile
            Outcome.Ok(
                UserProfile(
                    userId = tokens.userId ?: "",
                    name = tokens.userName ?: "Rider",
                    mobile = mob ?: mobile,
                    email = email,
                )
            )
        } catch (e: Exception) {
            Outcome.Err(e.message ?: "Network error")
        }
    }

    /** Step 3: fetch vehicles associated with the account. */
    suspend fun vehicles(): Outcome<List<Vehicle>> = withContext(Dispatchers.IO) {
        if (flags.mockEnabled) {
            delay(300)
            return@withContext Outcome.Ok(
                listOf(
                    Vehicle(
                        userVehicleId = "UV1", vin = "MD626EG56S1P31415",
                        model = "iQube ST", nickName = "My iQube",
                        registrationNumber = "KL00AA0000", bleMac = "48:23:35:69:B2:F6",
                    )
                )
            )
        }
        val token = tokens.accessToken ?: return@withContext Outcome.Err("Not logged in")
        val uid = tokens.userId ?: return@withContext Outcome.Err("Not logged in")
        try {
            val res = api.getVehicleList(uid)
            val data = res.body()?.data.orEmpty()
            val vehicles = data.map {
                Vehicle(
                    userVehicleId = it.userVehicleId ?: "",
                    vin = it.frameNo ?: "",
                    model = it.modelName ?: it.vehicleName ?: "Vehicle",
                    nickName = it.nickName,
                    registrationNumber = it.registrationNumber,
                    bleMac = it.macId,
                    imageUrl = it.imageUrl,
                )
            }
            // Capture the connected-vehicle context for the p360 GraphQL calls.
            data.firstOrNull()?.let { v ->
                tokens.vin = v.frameNo
                tokens.userVehicleId = v.userVehicleId
                tokens.vehicleTypeId = v.vehicleTypeId
                tokens.appUserId = tokens.appUserId ?: tokens.userId
                if (!v.codpServiceToken.isNullOrBlank()) tokens.codpToken = v.codpServiceToken
            }
            Outcome.Ok(vehicles)
        } catch (e: Exception) {
            Outcome.Err(e.message ?: "Network error")
        }
    }

    fun currentUser(): UserProfile? {
        val id = tokens.userId ?: return null
        if (!tokens.isLoggedIn) return null
        return UserProfile(id, tokens.userName ?: "Rider", tokens.mobile ?: "")
    }

    fun logout() = tokens.clear()
}
