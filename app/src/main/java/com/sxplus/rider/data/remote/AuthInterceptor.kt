package com.sxplus.rider.data.remote

import com.sxplus.rider.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds the standard TVS Connect auth headers to every ICE REST request,
 * matching RestClient.provideCacheInterceptor + the app-version interceptor.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokens: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val uid = tokens.userId ?: "0"
        val req = chain.request().newBuilder()
            .header("Accept", "*/*")
            .header("appVersion-android", "8.12.3")
            .header("UserId", uid)
            .header("ICEUserId", uid)
            .header("EVUserId", uid)
            .header("countryId", "1")
            .header("Region", "")
            .header("lang", "en-US")
            .apply {
                tokens.accessToken?.let { header("accessToken", it) }
            }
            .build()
        return chain.proceed(req)
    }
}

/**
 * Adds iqube login headers (AccessToken/RefreshToken/userid) used by the
 * EV base URL, including the getnewcodptoken token-exchange endpoint.
 */
@Singleton
class IqubeAuthInterceptor @Inject constructor(
    private val tokens: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
            .header("Accept", "*/*")
            .header("appVersion-android", "8.12.3")
            .header("userid", tokens.userId ?: "0")
            .apply {
                tokens.accessToken?.let { header("AccessToken", it) }
                tokens.refreshToken?.let { header("RefreshToken", it) }
            }
            .build()
        return chain.proceed(req)
    }
}
