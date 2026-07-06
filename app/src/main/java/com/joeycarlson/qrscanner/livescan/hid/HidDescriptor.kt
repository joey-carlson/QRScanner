package com.joeycarlson.qrscanner.livescan.hid

/**
 * Standard USB HID keyboard report descriptor.
 *
 * This is the byte sequence that describes the format of HID reports this device sends.
 * The host OS reads this once at pairing time to understand how to interpret our reports.
 *
 * Describes a standard 8-byte keyboard report:
 *   Byte 0: Modifier keys bitmask (Left Ctrl, Left Shift, Left Alt, Left GUI, Right Ctrl, Right Shift, Right Alt, Right GUI)
 *   Byte 1: Reserved (always 0x00)
 *   Bytes 2–7: Up to 6 simultaneous key codes (0x00 = no key in that slot)
 *
 * Reference: USB HID Usage Tables 1.4, Section 10 (Keyboard/Keypad Page)
 */
object HidDescriptor {

    /**
     * The HID report descriptor byte array. Passed to [android.bluetooth.BluetoothHidDevice]
     * via [android.bluetooth.BluetoothHidDevice.AppSdpSettings].
     */
    val KEYBOARD_DESCRIPTOR: ByteArray = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),  // Usage Page: Generic Desktop Controls
        0x09.toByte(), 0x06.toByte(),  // Usage: Keyboard

        0xA1.toByte(), 0x01.toByte(),  // Collection: Application

        // Modifier keys — 8 bits, one per modifier key
        0x05.toByte(), 0x07.toByte(),  //   Usage Page: Key Codes
        0x19.toByte(), 0xE0.toByte(),  //   Usage Minimum: Left Ctrl (0xE0)
        0x29.toByte(), 0xE7.toByte(),  //   Usage Maximum: Right GUI (0xE7)
        0x15.toByte(), 0x00.toByte(),  //   Logical Minimum: 0
        0x25.toByte(), 0x01.toByte(),  //   Logical Maximum: 1
        0x75.toByte(), 0x01.toByte(),  //   Report Size: 1 bit
        0x95.toByte(), 0x08.toByte(),  //   Report Count: 8 (one bit per modifier)
        0x81.toByte(), 0x02.toByte(),  //   Input: Data, Variable, Absolute (modifier byte)

        // Reserved byte
        0x95.toByte(), 0x01.toByte(),  //   Report Count: 1
        0x75.toByte(), 0x08.toByte(),  //   Report Size: 8 bits
        0x81.toByte(), 0x03.toByte(),  //   Input: Constant (reserved)

        // Key code array — 6 slots, each 8 bits, for simultaneous key presses
        0x95.toByte(), 0x06.toByte(),  //   Report Count: 6
        0x75.toByte(), 0x08.toByte(),  //   Report Size: 8 bits
        0x15.toByte(), 0x00.toByte(),  //   Logical Minimum: 0
        0x25.toByte(), 0x65.toByte(),  //   Logical Maximum: 101
        0x05.toByte(), 0x07.toByte(),  //   Usage Page: Key Codes
        0x19.toByte(), 0x00.toByte(),  //   Usage Minimum: Reserved (0x00)
        0x29.toByte(), 0x65.toByte(),  //   Usage Maximum: Keyboard Application (0x65)
        0x81.toByte(), 0x00.toByte(),  //   Input: Data, Array (key array)

        0xC0.toByte()                  // End Collection
    )

    /** Size of each keyboard HID report in bytes. */
    const val REPORT_SIZE = 8

    /** Index of the modifier byte in the report. */
    const val MODIFIER_INDEX = 0

    /** Index of the reserved byte in the report. */
    const val RESERVED_INDEX = 1

    /** Index of the first key slot in the report. */
    const val KEY_START_INDEX = 2

    /** Returns an empty (all-zeros) keyboard report — represents "no keys pressed". */
    fun emptyReport(): ByteArray = ByteArray(REPORT_SIZE)

    /**
     * Builds a single-key report.
     *
     * @param modifier  Modifier byte (e.g., [HidKeyCode.MOD_LEFT_SHIFT])
     * @param keyCode   HID usage code for the key (e.g., [HidKeyCode.KEY_A])
     */
    fun keyReport(modifier: Byte, keyCode: Byte): ByteArray {
        val report = ByteArray(REPORT_SIZE)
        report[MODIFIER_INDEX] = modifier
        report[RESERVED_INDEX] = 0x00
        report[KEY_START_INDEX] = keyCode
        return report
    }
}
