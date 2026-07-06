package com.joeycarlson.qrscanner.livescan.hid

/**
 * HID keyboard usage codes per the USB HID Usage Tables specification (Section 10).
 * These are the values placed in bytes 2-7 of the 8-byte keyboard HID report.
 *
 * Reference: https://usb.org/sites/default/files/hut1_4.pdf (Table 12: Keyboard/Keypad Page)
 */
object HidKeyCode {
    // Modifier bit masks (Byte 0 of the HID report)
    const val MOD_NONE: Byte = 0x00.toByte()
    const val MOD_LEFT_SHIFT: Byte = 0x02.toByte()

    // No key / key up
    const val KEY_NONE: Byte = 0x00.toByte()

    // Letters a–z (usage codes 0x04–0x1D)
    const val KEY_A: Byte = 0x04.toByte()
    const val KEY_B: Byte = 0x05.toByte()
    const val KEY_C: Byte = 0x06.toByte()
    const val KEY_D: Byte = 0x07.toByte()
    const val KEY_E: Byte = 0x08.toByte()
    const val KEY_F: Byte = 0x09.toByte()
    const val KEY_G: Byte = 0x0A.toByte()
    const val KEY_H: Byte = 0x0B.toByte()
    const val KEY_I: Byte = 0x0C.toByte()
    const val KEY_J: Byte = 0x0D.toByte()
    const val KEY_K: Byte = 0x0E.toByte()
    const val KEY_L: Byte = 0x0F.toByte()
    const val KEY_M: Byte = 0x10.toByte()
    const val KEY_N: Byte = 0x11.toByte()
    const val KEY_O: Byte = 0x12.toByte()
    const val KEY_P: Byte = 0x13.toByte()
    const val KEY_Q: Byte = 0x14.toByte()
    const val KEY_R: Byte = 0x15.toByte()
    const val KEY_S: Byte = 0x16.toByte()
    const val KEY_T: Byte = 0x17.toByte()
    const val KEY_U: Byte = 0x18.toByte()
    const val KEY_V: Byte = 0x19.toByte()
    const val KEY_W: Byte = 0x1A.toByte()
    const val KEY_X: Byte = 0x1B.toByte()
    const val KEY_Y: Byte = 0x1C.toByte()
    const val KEY_Z: Byte = 0x1D.toByte()

    // Digits 1–9, 0
    const val KEY_1: Byte = 0x1E.toByte()
    const val KEY_2: Byte = 0x1F.toByte()
    const val KEY_3: Byte = 0x20.toByte()
    const val KEY_4: Byte = 0x21.toByte()
    const val KEY_5: Byte = 0x22.toByte()
    const val KEY_6: Byte = 0x23.toByte()
    const val KEY_7: Byte = 0x24.toByte()
    const val KEY_8: Byte = 0x25.toByte()
    const val KEY_9: Byte = 0x26.toByte()
    const val KEY_0: Byte = 0x27.toByte()

    // Control keys
    const val KEY_ENTER: Byte = 0x28.toByte()
    const val KEY_ESCAPE: Byte = 0x29.toByte()
    const val KEY_BACKSPACE: Byte = 0x2A.toByte()
    const val KEY_TAB: Byte = 0x2B.toByte()
    const val KEY_SPACE: Byte = 0x2C.toByte()

    // Punctuation (unshifted positions on US QWERTY)
    const val KEY_MINUS: Byte = 0x2D.toByte()          // - / _
    const val KEY_EQUAL: Byte = 0x2E.toByte()           // = / +
    const val KEY_LEFT_BRACKET: Byte = 0x2F.toByte()   // [ / {
    const val KEY_RIGHT_BRACKET: Byte = 0x30.toByte()  // ] / }
    const val KEY_BACKSLASH: Byte = 0x31.toByte()      // \ / |
    const val KEY_SEMICOLON: Byte = 0x33.toByte()      // ; / :
    const val KEY_QUOTE: Byte = 0x34.toByte()           // ' / "
    const val KEY_GRAVE: Byte = 0x35.toByte()           // ` / ~
    const val KEY_COMMA: Byte = 0x36.toByte()           // , / <
    const val KEY_PERIOD: Byte = 0x37.toByte()          // . / >
    const val KEY_SLASH: Byte = 0x38.toByte()           // / / ?
}
