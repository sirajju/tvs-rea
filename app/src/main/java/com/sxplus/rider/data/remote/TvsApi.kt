package com.sxplus.rider.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * ICE REST (base https://tvsconnectapi.tvsmotor.com/api/).
 * Auth headers (accessToken/UserId/…) are added by AuthInterceptor.
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
    suspend fun getVehicleList(@Query("UserId") userId: String): Response<VehicleListResponse>

    @GET("Travel/GetTravelList")
    suspend fun getRideList(
        @Query("UserVehicleId") userVehicleId: String,
        @Query("UserId") userId: String,
        @Query("VehicleTypeId") vehicleTypeId: String,
        @Query("PageNumber") pageNumber: String = "1",
        @Query("isRideType") isRideType: Boolean = true,
    ): Response<RideListResponse>
}

/** EV / iqube REST (base https://tvsiqubeapi.tvsmotor.com/api/). */
interface IqubeApi {
    @GET("Vehicle/getnewcodptoken")
    suspend fun getCodpToken(
        @Query(value = "userVehicleId", encoded = true) userVehicleId: Int,
        @Query(value = "appUserId", encoded = true) appUserId: String,
        @Query(value = "frameNumber", encoded = true) frameNumber: String,
    ): Response<CodpTokenResponse>
}

/** p360 GraphQL gateway (base https://p360.tvsmotor.com/). Bearer = CODPServiceToken. */
interface GraphQlApi {
    @POST("gapi")
    suspend fun telemetry(
        @Header("Authorization") bearer: String,
        @Body body: GraphQlRequest,
    ): Response<TelemetryEnvelope>

    @POST("gapi")
    suspend fun batteries(
        @Header("Authorization") bearer: String,
        @Body body: GraphQlRequest,
    ): Response<BatteriesEnvelope>
}
