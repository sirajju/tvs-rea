package com.sxplus.rider.util

/** Hex helpers shared across the BLE/keyless layer. */
object Hex {
    fun decode(s: String): ByteArray {
        val clean = s.replace(":", "").replace(" ", "").trim()
        require(clean.length % 2 == 0) { "Invalid hex length: $s" }
        return ByteArray(clean.length / 2) { i ->
            ((Character.digit(clean[i * 2], 16) shl 4) + Character.digit(clean[i * 2 + 1], 16)).toByte()
        }
    }

    fun encode(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    fun ByteArray.toHex(): String = encode(this)
}
