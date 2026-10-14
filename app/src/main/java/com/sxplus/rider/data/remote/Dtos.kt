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
    @Json(name = "Status") val status: Int? = null,
    @Json(name = "Data") val data: List<VehicleRecord>? = null,
)

@JsonClass(generateAdapter = true)
data class VehicleRecord(
    @Json(name = "UserVehicleId") val userVehicleId: String? = null,
    @Json(name = "VIN") val vin: String? = null,
    @Json(name = "ModelName") val modelName: String? = null,
    @Json(name = "VehicleNickName") val nickName: String? = null,
    @Json(name = "RegistrationNumber") val registrationNumber: String? = null,
    @Json(name = "BleMacId") val bleMacId: String? = null,
    @Json(name = "VehicleImage") val imageUrl: String? = null,
)
