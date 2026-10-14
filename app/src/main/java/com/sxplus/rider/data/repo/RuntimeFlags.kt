package com.sxplus.rider.data.repo

import com.sxplus.rider.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/** Session-level runtime switches (dev conveniences). */
@Singleton
class RuntimeFlags @Inject constructor() {
    /** When true, repositories return canned data instead of hitting the network. */
    @Volatile var mockEnabled: Boolean = false
        set(value) { field = value && BuildConfig.ALLOW_MOCK }
}
