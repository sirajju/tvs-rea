package com.sxplus.rider.ble

/** High-level connection lifecycle for the paired vehicle. */
enum class BleState {
    IDLE,
    SCANNING,
    CONNECTING,
    DISCOVERING,
    READY,          // services discovered, notify enabled — can send commands
    DISCONNECTED,
    FAILED
}

/** Result of a keyless command attempt. */
sealed interface KeylessResult {
    data object Success : KeylessResult
    data class ChallengeTimeout(val message: String) : KeylessResult
    data class NotReady(val message: String) : KeylessResult
    data class Error(val message: String) : KeylessResult
}

/** A log line surfaced to the debug console in the UI. */
data class BleLogLine(val ts: Long, val tag: String, val text: String)
