package com.sxplus.rider

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Make any hard crash diagnosable:  adb logcat -s SXP_CRASH
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("SXP_CRASH", "Uncaught on ${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }
}
