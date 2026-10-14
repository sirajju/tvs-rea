package com.sxplus.rider.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Real TVS Connect endpoints used by the core login + vehicle flow.
 * Base URL = BuildConfig.BASE_URL_ICE (https://tvsconnectapi.tvsmotor.com/api/).
 */
interface TvsApi {

    @POST("v3/UserLogin/Loginv3")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("v3/userlogin/verifyloginotp")
    suspend fun verifyOtp(
        @Header("Version") version: String = "8.12.3",
        @Body body: VerifyOtpRequest,
    ): Response<VerifyOtpResponse>

    @GET("Vehicle/GetVahicleList")
    suspend fun getVehicleList(
        @Header("accessToken") accessToken: String,
        @Query("UserId") userId: String,
    ): Response<VehicleListResponse>
}
