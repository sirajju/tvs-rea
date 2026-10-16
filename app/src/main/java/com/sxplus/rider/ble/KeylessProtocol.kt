package com.sxplus.rider.ble

import com.sxplus.rider.BuildConfig
import com.sxplus.rider.util.Hex
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor
import kotlin.math.min

/**
 * Pure (stateless) implementation of the TVS SmartXonnect keyless-entry protocol.
 *
 * Direct Kotlin port of the reference Python PoC. All device-specific material
 * (AES key, command seeds, XOR byte, UUIDs, bootstrap frames) is injected from
 * BuildConfig (populated from secrets.properties) — nothing is hardcoded here.
 *
 *  OUTGOING frame (phone -> bike), 20 bytes, before XOR:
 *      [9A F1][pktType][start][len][00][seed_slice...][padding][FF]
 *  XOR pass: byte0,byte1 preserved; bytes[2..len-2] ^= XOR_KEY; last byte = FF.
 *
 *  Protocol: CCCD -> 0x40 INITIATE_REQUEST -> bike 0xF2 CHALLENGE ->
 *            AES/CTR(challenge) -> 0x50 SEND_RESPONSE -> bike acts.
 */
object KeylessProtocol {

    const val PKT_INITIATE_REQUEST = 0x40
    const val PKT_SEND_RESPONSE = 0x50
    const val CHALLENGE_FRAME_ID = 0xF2
    const val AWAITING_RESPONSE_FLAG = 0x27
    private const val POSTFIX = 0xFF.toByte()
    private val HEADER = byteArrayOf(0x9A.toByte(), 0xF1.toByte())

    // Stable slice (v2 default) — far more reliable than a random slice.
    const val FIXED_NONCE = 1
    const val FIXED_LENGTH = 11

    private val aesKey: ByteArray by lazy { Hex.decode(BuildConfig.AES_KEY_HEX) }
    private val xorKey: Int get() = BuildConfig.XOR_KEY_BYTE and 0xFF

    /** Seeds keyed by command. */
    fun seedFor(cmd: KeylessCommand): ByteArray = when (cmd) {
        KeylessCommand.FIND_VEHICLE -> Hex.decode(BuildConfig.SEED_FIND_VEHICLE)
        KeylessCommand.LOCK -> Hex.decode(BuildConfig.SEED_LOCK)
        KeylessCommand.UNLOCK -> Hex.decode(BuildConfig.SEED_UNLOCK)
        KeylessCommand.OPEN_TRUNK -> Hex.decode(BuildConfig.SEED_OPEN_TRUNK)
    }

    /** Parsed legacy bootstrap preamble frames (comma-separated hex in config). */
    val bootstrapFrames: List<ByteArray> by lazy {
        BuildConfig.BOOTSTRAP_FRAMES
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { Hex.decode(it) }
    }

    /**
     * [0x5B,0x22][userId big-endian][zeros][FF] — byteArrayForFistConnection.
     * This is the wake frame the bike needs before it will answer with a challenge.
     * [userId] is resolved at runtime (login session first, secret fallback).
     */
    fun buildUserIdFrame(userId: Long): ByteArray {
        val uid = if (userId <= 0) byteArrayOf(0) else {
            var v = userId
            val tmp = ArrayDeque<Byte>()
            while (v > 0) { tmp.addFirst((v and 0xFF).toByte()); v = v shr 8 }
            tmp.toByteArray()
        }
        val frame = ByteArray(20)
        frame[0] = 0x5B
        frame[1] = 0x22
        uid.copyInto(frame, destinationOffset = 2, endIndex = min(uid.size, 17))
        frame[19] = POSTFIX
        return frame
    }

    /** [0x5B,0x4A][signal][battery]...[FF] — returnInitialMobilePacket. */
    fun buildInitialDataPacket(signal: Int = 0, battery: Int = 0, networkType: Int = 0): ByteArray {
        val f = ByteArray(20)
        f[0] = 0x5B
        f[1] = 0x4A
        f[2] = signal.toByte()
        f[3] = battery.toByte()
        f[7] = 0x01
        f[8] = 0x02
        f[17] = networkType.toByte()
        f[19] = POSTFIX
        return f
    }

    /** Full wake/bootstrap sequence to send (raw) right before the 0x40 trigger. */
    fun wakeSequence(userId: Long): List<ByteArray> =
        bootstrapFrames + buildUserIdFrame(userId) + buildInitialDataPacket()

    // --- XOR layer (symmetric: encrypts and decrypts) -----------------------
    fun xorFrame(raw: ByteArray): ByteArray {
        if (raw.size < 3) return raw
        val out = ByteArray(raw.size)
        out[0] = raw[0]
        out[1] = raw[1]
        for (i in 2 until raw.size - 1) out[i] = (raw[i].toInt() xor xorKey).toByte()
        out[raw.size - 1] = POSTFIX
        return out
    }

    // --- Frame builder ------------------------------------------------------
    /** Mirrors BluetoothUtil.getLength(): max valid seed slice length for a start. */
    fun lengthMax(start: Int): Int = min(16 - start, 12)

    /** Mirrors BluetoothUtil.getRandomNumber(min, max). */
    private fun randomNumber(minInclusive: Int, maxExclusive: Int): Int {
        if (maxExclusive <= minInclusive) return minInclusive
        return floor(Math.random() * (maxExclusive - minInclusive) + minInclusive).toInt()
    }

    fun randomStart(): Int = randomNumber(1, 15)               // [1..14]
    fun randomLength(start: Int): Int = randomNumber(1, lengthMax(start))

    /**
     * Build a 20-byte frame (before XOR). Byte order verified against the
     * decompiled smali (BluetoothUtil.byteArrayForUnLock) and the v2 PoC:
     *
     *   [0,1] = 9A F1 (header)
     *   [2]   = nonce   (random 1..14)
     *   [3]   = length
     *   [4]   = 0x00
     *   [5]   = cmdByte (0x40 INITIATE / 0x50 RESPONSE)   <-- NOT at [2]
     *   [6..] = seed[nonce : nonce+length]
     *   pad zeros, [19] = 0xFF
     */
    fun buildRawFrame(cmdByte: Int, data: ByteArray, nonce: Int, length: Int): ByteArray {
        val n = nonce.coerceIn(1, 14)
        val len = length.coerceIn(1, lengthMax(n))
        val end = (n + len).coerceAtMost(data.size)
        val slice = if (n < data.size) data.copyOfRange(n, end) else ByteArray(0)
        val frame = ByteArray(20)
        frame[0] = HEADER[0]
        frame[1] = HEADER[1]
        frame[2] = n.toByte()
        frame[3] = len.toByte()
        frame[4] = 0x00
        frame[5] = cmdByte.toByte()
        slice.copyInto(frame, destinationOffset = 6, endIndex = min(slice.size, 13))
        frame[19] = POSTFIX
        return frame
    }

    // --- AES-CTR cipher -----------------------------------------------------
    /** AES/CTR/NoPadding with IV = challenge; encrypts the challenge itself. */
    fun aesCtrEncrypt(challenge: ByteArray): ByteArray {
        val iv = ByteArray(16).also { challenge.copyInto(it, endIndex = min(16, challenge.size)) }
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(iv)
    }

    // --- Challenge extraction ----------------------------------------------
    /** True if a decrypted notification frame carries a keyless challenge. */
    fun isChallenge(dec: ByteArray): Boolean {
        val byIdentifier = dec.size >= 18 && (dec[1].toInt() and 0xFF) == CHALLENGE_FRAME_ID
        val byAwaiting = dec.size >= 19 && (dec[18].toInt() and 0xFF) == AWAITING_RESPONSE_FLAG
        return byIdentifier || byAwaiting
    }

    /** The 16-byte challenge payload: decrypted bytes [2..18). */
    fun extractChallenge(dec: ByteArray): ByteArray = dec.copyOfRange(2, 18)

    /** Validate that required secrets are present (not the example zero-fill). */
    fun isConfigured(): Boolean =
        BuildConfig.AES_KEY_HEX.length == 32 &&
            BuildConfig.AES_KEY_HEX.any { it != '0' } &&
            BuildConfig.SEED_FIND_VEHICLE.any { it != '0' }
}
