package com.sxplus.rider.ble

import java.util.Calendar

/**
 * Instrument-cluster push-control protocol (separate from keyless).
 *
 * Frames are [0x5A/0x5B][opcode][payload…][0xFF], encrypted by XOR-ing the
 * middle bytes with 0xEA (NOT the keyless 0xEB) while preserving bytes 0,1 and
 * the trailing 0xFF — verified from U399EncryptDecryptDataUtils.encryptData.
 *
 * Note: this matches the U-series (iQube) cluster. N360/NTorq uses AES instead,
 * so these frames are model-specific.
 */
object ClusterProtocol {
    private const val XOR_KEY = 0xEA
    private const val START_VEHICLE = 0x5A.toByte()
    private const val DATA_ID_VEHICLE_CONTROL = 0xF1.toByte()
    private const val POSTFIX = 0xFF.toByte()

    /** XOR envelope: preserve [0],[1] and last byte; XOR the middle with 0xEA. */
    fun encrypt(raw: ByteArray): ByteArray {
        if (raw.size < 3) return raw
        val out = raw.copyOf()
        for (i in 2 until raw.size - 1) out[i] = (raw[i].toInt() xor XOR_KEY).toByte()
        out[raw.size - 1] = POSTFIX
        return out
    }

    /**
     * Vehicle-control frame carrying screen brightness + ride mode (+ date sync).
     * Layout (pre-encrypt): [5A F1][illum][rideMode][0 0 0 0 0][day][mon][yr][0 0 1 0 0 0 0][FF]
     */
    fun vehicleControlFrame(illumination: Int, rideMode: Int): ByteArray {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR) % 100
        val raw = byteArrayOf(
            START_VEHICLE, DATA_ID_VEHICLE_CONTROL,
            illumination.toByte(), rideMode.toByte(),
            0, 0, 0, 0, 0,
            day.toByte(), month.toByte(), year.toByte(),
            0, 0, 1, 0, 0, 0, 0,
            POSTFIX,
        )
        return encrypt(raw)
    }

    const val MIN_BRIGHTNESS = 1
    const val MAX_BRIGHTNESS = 10
}
