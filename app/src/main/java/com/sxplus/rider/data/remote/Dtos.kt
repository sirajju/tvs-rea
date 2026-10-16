package com.sxplus.rider.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---- Login (POST v3/UserLogin/Loginv3) --------------------------------------
@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "MobileNumber") val mobileNumber: String,
    @Json(name = "otpLength") val otpLength: Int = 6,
    @Json(name = "IsSuppressOTP") val isSuppressOtp: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "Status") val status: Int? = null,
    @Json(name = "Message") val message: String? = null,
    @Json(name = "ICEUserId") val iceUserId: String? = null,
    @Json(name = "EVUserId") val evUserId: String? = null,
)

// ---- Verify OTP (POST v3/userlogin/verifyloginotp) --------------------------
@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(
    @Json(name = "MobileNumber") val mobileNumber: String,
    @Json(name = "OTP") val otp: String,
)

@JsonClass(generateAdapter = true)
data class VerifyOtpResponse(
    @Json(name = "Status") val status: Int? = null,
    @Json(name = "Message") val message: String? = null,
    @Json(name = "AccessToken") val accessToken: String? = null,
    @Json(name = "RefreshToken") val refreshToken: String? = null,
    @Json(name = "EVAccessToken") val evAccessToken: String? = null,
    @Json(name = "EVRefreshToken") val evRefreshToken: String? = null,
    @Json(name = "Data") val data: List<UserRecord>? = null,
)

@JsonClass(generateAdapter = true)
data class UserRecord(
    @Json(name = "UserId") val userId: String? = null,
    @Json(name = "FirstName") val firstName: String? = null,
    @Json(name = "LastName") val lastName: String? = null,
    @Json(name = "EmailId") val email: String? = null,
    @Json(name = "MobileNumber") val mobileNumber: String? = null,
)

// ---- Vehicle list (GET Vehicle/GetVahicleList?UserId=) ----------------------
@JsonClass(generateAdapter = true)
data class VehicleListResponse(
    @Json(name = "StatusCode") val statusCode: Int? = null,
    @Json(name = "Message") val message: String? = null,
    @Json(name = "Data") val data: List<VehicleRecord>? = null,
)

/** Mirrors VehicleData — FRAME_NO is the VIN, macId is the BLE MAC. */
@JsonClass(generateAdapter = true)
data class VehicleRecord(
    @Json(name = "UserVehicleId") val userVehicleId: String? = null,
    @Json(name = "FRAME_NO") val frameNo: String? = null,
    @Json(name = "VehicleModelName") val modelName: String? = null,
    @Json(name = "VehicleName") val vehicleName: String? = null,
    @Json(name = "NickName") val nickName: String? = null,
    @Json(name = "REG_NO") val registrationNumber: String? = null,
    @Json(name = "macId") val macId: String? = null,
    @Json(name = "VehicleTypeId") val vehicleTypeId: String? = null,
    @Json(name = "ImageUrl") val imageUrl: String? = null,
    @Json(name = "CODPServiceToken") val codpServiceToken: String? = null,
    @Json(name = "odometerReading") val odometerReading: Double? = null,
)

// ---- CODP token exchange (GET Vehicle/getnewcodptoken on iqube base) --------
@JsonClass(generateAdapter = true)
data class CodpTokenResponse(
    @Json(name = "StatusCode") val statusCode: Int? = null,
    @Json(name = "Data") val data: CodpTokenData? = null,
)

@JsonClass(generateAdapter = true)
data class CodpTokenData(
    @Json(name = "token") val token: String? = null,
    @Json(name = "vin") val vin: String? = null,
    @Json(name = "app_user_id") val appUserId: String? = null,
)

// ---- Ride list (GET Travel/GetTravelList) -----------------------------------
@JsonClass(generateAdapter = true)
data class RideListResponse(
    @Json(name = "StatusCode") val statusCode: Int? = null,
    @Json(name = "Data") val data: RideListData? = null,
)

@JsonClass(generateAdapter = true)
data class RideListData(
    @Json(name = "CurrentPage") val currentPage: Int? = null,
    @Json(name = "NextPage") val nextPage: Int? = null,
    @Json(name = "Results") val results: List<RideItem>? = null,
)

@JsonClass(generateAdapter = true)
data class RideItem(
    @Json(name = "travelId") val travelId: String? = null,
    @Json(name = "travelName") val travelName: String? = null,
    @Json(name = "travelStartDateTime") val startMs: Long? = null,
    @Json(name = "travelEndDateTime") val endMs: Long? = null,
    @Json(name = "totalTravelledDistance") val distanceKm: Double? = null,
    @Json(name = "rideTime") val rideTimeMs: Long? = null,
    @Json(name = "totalTime") val totalTimeMs: Long? = null,
    @Json(name = "averageSpeed") val avgSpeed: Int? = null,
    @Json(name = "topSpeed") val topSpeed: Double? = null,
    @Json(name = "isRideSynced") val synced: Boolean? = null,
    @Json(name = "wayPoints") val wayPoints: String? = null,
)
