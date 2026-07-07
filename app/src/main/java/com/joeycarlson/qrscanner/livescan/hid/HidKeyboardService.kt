package com.joeycarlson.qrscanner.livescan.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * Manages the Bluetooth HID keyboard connection lifecycle and key report transmission.
 *
 * This service:
 * 1. Registers the app as a Bluetooth HID keyboard with [BluetoothHidDevice]
 * 2. Advertises so the host computer can pair with it
 * 3. Sends HID key-down/key-up reports for each character in [typeString]
 *
 * Requires [android.permission.BLUETOOTH_CONNECT] on Android 12+ (API 31+).
 * Call [start] to begin advertising, [stop] to clean up.
 *
 * [BluetoothHidDevice] API requires Android 9 (API 28).
 */
@RequiresApi(Build.VERSION_CODES.P)
class HidKeyboardService(private val context: Context) {

    companion object {
        private const val TAG = "HidKeyboardService"

        // SDP service name visible to the host when pairing
        private const val SDP_NAME = "Pilot Scanner"
        private const val SDP_DESCRIPTION = "QR/Barcode Scanner Keyboard"
        private const val SDP_PROVIDER = "joecrls"

        // Delay between key-down and key-up reports (ms). Increase if characters drop.
        private const val DEFAULT_KEY_DELAY_MS = 8L

        // How long to wait for the BT HID Device profile service to bind before declaring
        // the device incompatible. 3 seconds is generous — compatible devices bind in <200ms.
        private const val PROFILE_BIND_TIMEOUT_MS = 3000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<HidConnectionState>(HidConnectionState.Idle)

    /** Observe the current BT HID connection state. */
    val connectionState: StateFlow<HidConnectionState> = _connectionState.asStateFlow()

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null

    // -------------------------------------------------------------------------
    // BluetoothHidDevice.Callback — handles profile events from the BT stack
    // -------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private val hidCallback = object : BluetoothHidDevice.Callback() {

        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged: registered=$registered device=$pluggedDevice")
            if (registered) {
                _connectionState.value = HidConnectionState.Advertising
                Log.i(TAG, "HID app registered — advertising as keyboard")
            } else {
                _connectionState.value = HidConnectionState.Idle
                hidDevice = null
                connectedHost = null
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            val deviceName = device.name ?: device.address
            Log.d(TAG, "onConnectionStateChanged: $deviceName state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device
                    _connectionState.value = HidConnectionState.Connected(deviceName)
                    Log.i(TAG, "HID connected to $deviceName")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedHost = null
                    // Auto re-advertise so user doesn't need to re-enter Live Scan
                    if (_connectionState.value !is HidConnectionState.Idle) {
                        _connectionState.value = HidConnectionState.Advertising
                        Log.i(TAG, "HID disconnected from $deviceName — re-advertising")
                    }
                }
                else -> Log.d(TAG, "HID state change ignored: state=$state")
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            // Respond with an empty report to satisfy host polling
            hidDevice?.replyReport(device, type, id, HidDescriptor.emptyReport())
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            // Handle LED output reports (Num Lock, Caps Lock, Scroll Lock) — ignored for now
            hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            Log.i(TAG, "Virtual cable unplug from ${device.name}")
            connectedHost = null
            _connectionState.value = HidConnectionState.Advertising
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Initialises the Bluetooth HID device profile proxy and registers this app as a keyboard.
     * Must be called before [typeString].
     *
     * Transitions: Idle → Registering → Advertising (via [hidCallback.onAppStatusChanged])
     *
     * If the device's Bluetooth stack doesn't include the HID Device service (common on Motorola
     * and other OEM builds), [onServiceConnected] will never fire. We detect this with a 3-second
     * timeout and transition to [HidConnectionState.UnsupportedDevice] instead of hanging forever.
     */
    @SuppressLint("MissingPermission")
    fun start() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = HidConnectionState.Error(
                "Bluetooth is disabled. Enable Bluetooth and try again."
            )
            return
        }

        _connectionState.value = HidConnectionState.Registering

        val executor: Executor = Executor { command -> scope.launch { command.run() } }

        // Track whether onServiceConnected fires within the timeout window.
        var serviceConnected = false

        val profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile != BluetoothProfile.HID_DEVICE) return
                serviceConnected = true
                hidDevice = proxy as BluetoothHidDevice

                val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                    SDP_NAME,
                    SDP_DESCRIPTION,
                    SDP_PROVIDER,
                    BluetoothHidDevice.SUBCLASS1_KEYBOARD,
                    HidDescriptor.KEYBOARD_DESCRIPTOR
                )

                val registered = hidDevice?.registerApp(sdpSettings, null, null, executor, hidCallback)
                if (registered != true) {
                    Log.e(TAG, "registerApp returned false — HID profile service present but rejected registration")
                    _connectionState.value = HidConnectionState.UnsupportedDevice()
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                Log.w(TAG, "HID profile service disconnected")
                hidDevice = null
                connectedHost = null
                if (_connectionState.value !is HidConnectionState.Idle) {
                    _connectionState.value = HidConnectionState.Error("Bluetooth service disconnected.")
                }
            }
        }

        val success = adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        if (!success) {
            // getProfileProxy returned false immediately — profile definitely not available.
            Log.e(TAG, "getProfileProxy returned false for HID_DEVICE — profile unavailable on this device")
            _connectionState.value = HidConnectionState.UnsupportedDevice()
            return
        }

        // Start a 3-second watchdog: if onServiceConnected hasn't fired, the OEM Bluetooth stack
        // has the HID Device service missing (common on Motorola devices — logcat shows
        // "getProfileProxy(),bluetooth service not start"). Transition to UnsupportedDevice
        // instead of hanging on "Registering" indefinitely.
        scope.launch {
            delay(PROFILE_BIND_TIMEOUT_MS)
            if (!serviceConnected && _connectionState.value is HidConnectionState.Registering) {
                Log.w(TAG, "HID Device profile service did not bind within ${PROFILE_BIND_TIMEOUT_MS}ms " +
                    "— OEM Bluetooth stack likely missing HID Device support " +
                    "(${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL})")
                _connectionState.value = HidConnectionState.UnsupportedDevice()
            }
        }
    }

    /**
     * Unregisters the HID app and closes the profile proxy.
     * Safe to call multiple times. Transitions to [HidConnectionState.Idle].
     */
    @SuppressLint("MissingPermission")
    fun stop() {
        try {
            hidDevice?.let { device ->
                device.unregisterApp()
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                bluetoothManager?.adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, device)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during stop: ${e.message}")
        } finally {
            hidDevice = null
            connectedHost = null
            _connectionState.value = HidConnectionState.Idle
        }
    }

    /** Cancels the coroutine scope. Call when the owning ViewModel or component is destroyed. */
    fun destroy() {
        stop()
        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Typing
    // -------------------------------------------------------------------------

    /**
     * Types [text] as HID key-down/key-up report pairs on the connected host.
     *
     * - Characters with no HID mapping are silently skipped.
     * - Each key is sent as a key-down report followed by [charDelayMs], then a key-up report.
     * - After the text, the [suffix] character is sent (Enter by default).
     *
     * Must be called from a coroutine (suspend function).
     * Does nothing if not in [HidConnectionState.Connected] state.
     *
     * @param text         The string to type (e.g., a scanned barcode value).
     * @param suffix       Character to send after [text]. Use '\n' for Enter, '\t' for Tab, null for none.
     * @param charDelayMs  Milliseconds between key-down and key-up. Increase to 15–20ms if chars drop.
     */
    @SuppressLint("MissingPermission")
    suspend fun typeString(
        text: String,
        suffix: Char? = '\n',
        charDelayMs: Long = DEFAULT_KEY_DELAY_MS
    ) {
        val host = connectedHost
        val hid = hidDevice

        if (host == null || hid == null || !(_connectionState.value is HidConnectionState.Connected)) {
            Log.w(TAG, "typeString() called but not connected — dropping: $text")
            return
        }

        val fullText = if (suffix != null) text + suffix else text

        for (char in fullText) {
            val keyEvent = KeyMapper.map(char)
            if (keyEvent == null) {
                Log.v(TAG, "Skipping unmappable char: '${char}' (0x${char.code.toString(16)})")
                continue
            }

            val keyDownReport = HidDescriptor.keyReport(keyEvent.modifier, keyEvent.keyCode)
            val keyUpReport = HidDescriptor.emptyReport()

            val sentDown = hid.sendReport(host, 0, keyDownReport)
            delay(charDelayMs)
            val sentUp = hid.sendReport(host, 0, keyUpReport)
            delay(charDelayMs)

            if (!sentDown || !sentUp) {
                Log.w(TAG, "sendReport failed for char '$char' — host may have disconnected")
                break
            }
        }
    }

    /**
     * Convenience wrapper: launches [typeString] in the service's IO scope.
     * Use this from non-suspend callers (e.g., ViewModel callbacks).
     */
    fun typeStringAsync(
        text: String,
        suffix: Char? = '\n',
        charDelayMs: Long = DEFAULT_KEY_DELAY_MS
    ) {
        scope.launch {
            typeString(text, suffix, charDelayMs)
        }
    }
}
