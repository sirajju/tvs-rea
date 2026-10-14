package com.sxplus.rider.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypted at-rest storage for session tokens + profile (Keystore-backed). */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = run {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "sxplus_secure",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var accessToken: String?
        get() = prefs.getString("access", null)
        set(v) = prefs.edit().putString("access", v).apply()

    var refreshToken: String?
        get() = prefs.getString("refresh", null)
        set(v) = prefs.edit().putString("refresh", v).apply()

    var evAccessToken: String?
        get() = prefs.getString("ev_access", null)
        set(v) = prefs.edit().putString("ev_access", v).apply()

    var userId: String?
        get() = prefs.getString("user_id", null)
        set(v) = prefs.edit().putString("user_id", v).apply()

    var userName: String?
        get() = prefs.getString("user_name", null)
        set(v) = prefs.edit().putString("user_name", v).apply()

    var mobile: String?
        get() = prefs.getString("mobile", null)
        set(v) = prefs.edit().putString("mobile", v).apply()

    val isLoggedIn: Boolean get() = !accessToken.isNullOrBlank() && !userId.isNullOrBlank()

    fun clear() = prefs.edit().clear().apply()
}
