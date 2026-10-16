package com.sxplus.rider.data.repo

import com.sxplus.rider.data.local.TokenStore
import com.sxplus.rider.data.remote.IqubeApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Obtains and caches the p360 GraphQL bearer (CODPServiceToken) via the
 * iqube token-exchange: GET Vehicle/getnewcodptoken. Also captures the
 * authoritative app_user_id + vin for GraphQL variables.
 */
@Singleton
class CodpTokenManager @Inject constructor(
    private val iqubeApi: IqubeApi,
    private val tokens: TokenStore,
) {
    /** "Bearer <token>" for GraphQL, or null if none is available. */
    fun bearer(): String? = tokens.codpToken?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

    /** Force a fresh token exchange; returns the new bearer or null on failure. */
    suspend fun refresh(): String? {
        val uvId = tokens.userVehicleId?.toIntOrNull() ?: return null
        val appUserId = tokens.appUserId ?: tokens.userId ?: return null
        val vin = tokens.vin ?: return null
        return try {
            val res = iqubeApi.getCodpToken(uvId, appUserId, vin)
            val data = res.body()?.data
            data?.token?.let { tokens.codpToken = it }
            data?.appUserId?.let { tokens.appUserId = it }
            data?.vin?.let { tokens.vin = it }
            bearer()
        } catch (e: Exception) {
            bearer()  // fall back to whatever is cached (e.g. from vehicle list)
        }
    }

    /** Cached bearer if present, else attempt a refresh. */
    suspend fun ensureBearer(): String? = bearer() ?: refresh()
}
