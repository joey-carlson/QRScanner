package com.joeycarlson.qrscanner.livescan.hid

/**
 * Represents the Bluetooth HID keyboard connection lifecycle.
 *
 * State transitions:
 *   Idle → Registering (user enters Live Scan, BT is on)
 *   Registering → Advertising (app registered with BT stack successfully)
 *   Advertising → Connected (host device connected)
 *   Connected → Advertising (host disconnected, auto re-advertising)
 *   Any → Error (unrecoverable failure)
 *   Any → Idle (user exits Live Scan or BT disabled)
 */
sealed class HidConnectionState {

    /** Initial state. BT HID service not yet started. */
    object Idle : HidConnectionState()

    /** Registering app as a HID device with the Android Bluetooth stack. */
    object Registering : HidConnectionState()

    /**
     * App registered. Advertising as a Bluetooth keyboard.
     * Waiting for the host computer to initiate pairing or reconnect.
     */
    object Advertising : HidConnectionState()

    /**
     * Connected to a host device. HID key reports can now be sent.
     *
     * @param deviceName Display name of the connected host (e.g., "MacBook Air").
     */
    data class Connected(val deviceName: String) : HidConnectionState()

    /**
     * Unrecoverable error. Displayed to user with guidance.
     *
     * @param message Human-readable error description.
     */
    data class Error(val message: String) : HidConnectionState()

    /** True only when a host is connected and ready to receive key reports. */
    val isReadyToType: Boolean get() = this is Connected
}
