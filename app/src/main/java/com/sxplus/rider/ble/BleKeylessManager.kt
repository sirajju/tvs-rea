package com.sxplus.rider.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import com.sxplus.rider.BuildConfig
import com.sxplus.rider.util.Hex.toHex
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single-vehicle BLE GATT manager implementing the keyless 2-step handshake.
 *
 * Connect -> discover -> locate WRITE + NOTIFY chars (by UUID, any service) ->
 * enable CCCD -> (optional bootstrap) -> on command: send 0x40, await 0xF2
 * challenge, AES-CTR, send 0x50. Everything device-specific comes from
 * KeylessProtocol (which reads secrets from BuildConfig).
 */
@Singleton
@SuppressLint("MissingPermission")
class BleKeylessManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val writeUuid = UUID.fromString(BuildConfig.UUID_WRITE)
    private val notifyUuid = UUID.fromString(BuildConfig.UUID_NOTIFY)

    private val adapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private var gatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null

    @Volatile private var challengeWaiter: CompletableDeferred<ByteArray>? = null

    private val _state = MutableStateFlow(BleState.IDLE)
    val state: StateFlow<BleState> = _state.asStateFlow()

    private val _deviceAddress = MutableStateFlow<String?>(null)
    val deviceAddress: StateFlow<String?> = _deviceAddress.asStateFlow()

    private val _logs = MutableStateFlow<List<BleLogLine>>(emptyList())
    val logs: StateFlow<List<BleLogLine>> = _logs.asStateFlow()

    private fun log(tag: String, text: String) {
        _logs.update { (it + BleLogLine(System.currentTimeMillis(), tag, text)).takeLast(200) }
    }

    fun isConnected(): Boolean = _state.value == BleState.READY

    // --- Connection ---------------------------------------------------------
    fun connect(macAddress: String) {
        val a = adapter ?: run { log("BLE", "No Bluetooth adapter"); _state.value = BleState.FAILED; return }
        if (gatt != null && _deviceAddress.value == macAddress &&
            _state.value in listOf(BleState.READY, BleState.CONNECTING, BleState.DISCOVERING)
        ) return
        disconnect()
        val device = try { a.getRemoteDevice(macAddress) } catch (e: Exception) {
            log("BLE", "Bad MAC: ${e.message}"); _state.value = BleState.FAILED; return
        }
        _deviceAddress.value = macAddress
        _state.value = BleState.CONNECTING
        log("BLE", "Connecting to $macAddress")
        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            device.connectGatt(context, false, callback, BluetoothDevice_TRANSPORT_LE)
        else device.connectGatt(context, false, callback)
    }

    fun disconnect() {
        challengeWaiter?.cancel()
        challengeWaiter = null
        gatt?.let {
            try { it.disconnect() } catch (_: Exception) {}
            try { it.close() } catch (_: Exception) {}
        }
        gatt = null
        writeChar = null
        notifyChar = null
        if (_state.value != BleState.IDLE) _state.value = BleState.DISCONNECTED
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    log("BLE", "Connected (status=$status), discovering services…")
                    _state.value = BleState.DISCOVERING
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    log("BLE", "Disconnected (status=$status)")
                    _state.value = BleState.DISCONNECTED
                    challengeWaiter?.cancel()
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log("BLE", "Service discovery failed: $status"); _state.value = BleState.FAILED; return
            }
            // Find write + notify characteristics across all services by UUID.
            for (svc in g.services) {
                svc.getCharacteristic(writeUuid)?.let { writeChar = it }
                svc.getCharacteristic(notifyUuid)?.let { notifyChar = it }
            }
            val w = writeChar; val n = notifyChar
            if (w == null || n == null) {
                log("BLE", "Required characteristics not found (write=$w notify=$n)")
                _state.value = BleState.FAILED
                return
            }
            // Enable notifications (CCCD).
            g.setCharacteristicNotification(n, true)
            val cccd = n.getDescriptor(cccdUuid)
            if (cccd != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    g.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    g.writeDescriptor(cccd)
                }
            } else {
                // No CCCD — treat as ready anyway.
                markReady()
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
            if (d.uuid == cccdUuid) {
                log("BLE", "CCCD write status=$status")
                markReady()
            }
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) log("TX", "write failed status=$status")
        }

        // API 33+
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
            handleNotification(value)
        }

        // Legacy <33
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            handleNotification(c.value ?: ByteArray(0))
        }
    }

    private fun markReady() {
        _state.value = BleState.READY
        log("BLE", "Ready — notifications enabled")
        sendBootstrap()
    }

    private fun handleNotification(raw: ByteArray) {
        log("RX raw", raw.toHex())
        val dec = KeylessProtocol.xorFrame(raw)
        log("RX dec", dec.toHex())
        if (KeylessProtocol.isChallenge(dec)) {
            val challenge = KeylessProtocol.extractChallenge(dec)
            log("RX", "CHALLENGE ${challenge.toHex()}")
            challengeWaiter?.takeIf { !it.isCompleted }?.complete(challenge)
        }
    }

    // --- Writes -------------------------------------------------------------
    private fun writeRaw(frame: ByteArray) {
        val g = gatt ?: return
        val w = writeChar ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(w, frame, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        } else {
            @Suppress("DEPRECATION")
            w.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            w.value = frame
            @Suppress("DEPRECATION")
            g.writeCharacteristic(w)
        }
        log("TX", frame.toHex())
    }

    private fun sendBootstrap() {
        val frames = KeylessProtocol.bootstrapFrames
        if (frames.isEmpty()) return
        log("BLE", "Sending ${frames.size} bootstrap frames")
        frames.forEach { writeRaw(it) }
    }

    // --- Keyless command (the 2-step handshake) -----------------------------
    /**
     * Executes one keyless command end-to-end. Suspends until the bike's
     * challenge arrives (or times out), then sends the AES-CTR response.
     */
    suspend fun sendKeylessCommand(
        cmd: KeylessCommand,
        challengeTimeoutMs: Long = 8_000L
    ): KeylessResult {
        if (!KeylessProtocol.isConfigured())
            return KeylessResult.NotReady("Keyless secrets not configured (see secrets.properties)")
        if (_state.value != BleState.READY)
            return KeylessResult.NotReady("Vehicle not connected")

        return try {
            val seed = KeylessProtocol.seedFor(cmd)
            val start = KeylessProtocol.randomStart()
            val length = KeylessProtocol.randomLength(start)
            val rawReq = KeylessProtocol.buildRawFrame(KeylessProtocol.PKT_INITIATE_REQUEST, seed, start, length)

            // arm challenge waiter BEFORE writing the trigger
            val waiter = CompletableDeferred<ByteArray>()
            challengeWaiter = waiter

            log("CMD", "${cmd.label}: 0x40 INITIATE (start=$start len=$length)")
            writeRaw(KeylessProtocol.xorFrame(rawReq))

            val challenge = withTimeoutOrNull(challengeTimeoutMs) { waiter.await() }
                ?: return KeylessResult.ChallengeTimeout("No challenge within ${challengeTimeoutMs}ms — keep the bike awake and retry")

            val encrypted = KeylessProtocol.aesCtrEncrypt(challenge)
            val aStart = KeylessProtocol.randomStart()
            val aLen = KeylessProtocol.randomLength(aStart)
            val rawResp = KeylessProtocol.buildRawFrame(KeylessProtocol.PKT_SEND_RESPONSE, encrypted, aStart, aLen)
            log("CMD", "0x50 RESPONSE (start=$aStart len=$aLen)")
            writeRaw(KeylessProtocol.xorFrame(rawResp))

            KeylessResult.Success
        } catch (e: Exception) {
            KeylessResult.Error(e.message ?: "Unknown BLE error")
        } finally {
            challengeWaiter = null
        }
    }

    companion object {
        // android.bluetooth.BluetoothDevice.TRANSPORT_LE without importing the whole class name twice
        private const val BluetoothDevice_TRANSPORT_LE = 2
    }
}
