package com.joeycarlson.qrscanner.livescan.hid

/**
 * Maps printable ASCII characters to HID keyboard report bytes (keyCode + modifier).
 *
 * Covers the full printable ASCII range (0x20–0x7E) plus Enter and Tab control characters,
 * using a standard US QWERTY keyboard layout.
 *
 * Characters outside this set are unmappable and will be skipped during typing.
 */
object KeyMapper {

    /** Holds the two bytes needed to type one character via HID. */
    data class KeyEvent(val keyCode: Byte, val modifier: Byte)

    private val NONE = HidKeyCode.MOD_NONE
    private val SHIFT = HidKeyCode.MOD_LEFT_SHIFT

    /**
     * Lookup table: ASCII character → (keyCode, modifier).
     * Only printable ASCII (space through ~) plus \n (Enter) and \t (Tab) are mapped.
     */
    private val charMap: Map<Char, KeyEvent> = buildMap {
        // Control characters
        put('\n', KeyEvent(HidKeyCode.KEY_ENTER, NONE))
        put('\t', KeyEvent(HidKeyCode.KEY_TAB, NONE))
        put(' ',  KeyEvent(HidKeyCode.KEY_SPACE, NONE))

        // Digits — unshifted
        put('1', KeyEvent(HidKeyCode.KEY_1, NONE))
        put('2', KeyEvent(HidKeyCode.KEY_2, NONE))
        put('3', KeyEvent(HidKeyCode.KEY_3, NONE))
        put('4', KeyEvent(HidKeyCode.KEY_4, NONE))
        put('5', KeyEvent(HidKeyCode.KEY_5, NONE))
        put('6', KeyEvent(HidKeyCode.KEY_6, NONE))
        put('7', KeyEvent(HidKeyCode.KEY_7, NONE))
        put('8', KeyEvent(HidKeyCode.KEY_8, NONE))
        put('9', KeyEvent(HidKeyCode.KEY_9, NONE))
        put('0', KeyEvent(HidKeyCode.KEY_0, NONE))

        // Digits — shifted symbols (US QWERTY)
        put('!', KeyEvent(HidKeyCode.KEY_1, SHIFT))
        put('@', KeyEvent(HidKeyCode.KEY_2, SHIFT))
        put('#', KeyEvent(HidKeyCode.KEY_3, SHIFT))
        put('$', KeyEvent(HidKeyCode.KEY_4, SHIFT))
        put('%', KeyEvent(HidKeyCode.KEY_5, SHIFT))
        put('^', KeyEvent(HidKeyCode.KEY_6, SHIFT))
        put('&', KeyEvent(HidKeyCode.KEY_7, SHIFT))
        put('*', KeyEvent(HidKeyCode.KEY_8, SHIFT))
        put('(', KeyEvent(HidKeyCode.KEY_9, SHIFT))
        put(')', KeyEvent(HidKeyCode.KEY_0, SHIFT))

        // Lowercase letters
        put('a', KeyEvent(HidKeyCode.KEY_A, NONE))
        put('b', KeyEvent(HidKeyCode.KEY_B, NONE))
        put('c', KeyEvent(HidKeyCode.KEY_C, NONE))
        put('d', KeyEvent(HidKeyCode.KEY_D, NONE))
        put('e', KeyEvent(HidKeyCode.KEY_E, NONE))
        put('f', KeyEvent(HidKeyCode.KEY_F, NONE))
        put('g', KeyEvent(HidKeyCode.KEY_G, NONE))
        put('h', KeyEvent(HidKeyCode.KEY_H, NONE))
        put('i', KeyEvent(HidKeyCode.KEY_I, NONE))
        put('j', KeyEvent(HidKeyCode.KEY_J, NONE))
        put('k', KeyEvent(HidKeyCode.KEY_K, NONE))
        put('l', KeyEvent(HidKeyCode.KEY_L, NONE))
        put('m', KeyEvent(HidKeyCode.KEY_M, NONE))
        put('n', KeyEvent(HidKeyCode.KEY_N, NONE))
        put('o', KeyEvent(HidKeyCode.KEY_O, NONE))
        put('p', KeyEvent(HidKeyCode.KEY_P, NONE))
        put('q', KeyEvent(HidKeyCode.KEY_Q, NONE))
        put('r', KeyEvent(HidKeyCode.KEY_R, NONE))
        put('s', KeyEvent(HidKeyCode.KEY_S, NONE))
        put('t', KeyEvent(HidKeyCode.KEY_T, NONE))
        put('u', KeyEvent(HidKeyCode.KEY_U, NONE))
        put('v', KeyEvent(HidKeyCode.KEY_V, NONE))
        put('w', KeyEvent(HidKeyCode.KEY_W, NONE))
        put('x', KeyEvent(HidKeyCode.KEY_X, NONE))
        put('y', KeyEvent(HidKeyCode.KEY_Y, NONE))
        put('z', KeyEvent(HidKeyCode.KEY_Z, NONE))

        // Uppercase letters (same key code, Left Shift modifier)
        put('A', KeyEvent(HidKeyCode.KEY_A, SHIFT))
        put('B', KeyEvent(HidKeyCode.KEY_B, SHIFT))
        put('C', KeyEvent(HidKeyCode.KEY_C, SHIFT))
        put('D', KeyEvent(HidKeyCode.KEY_D, SHIFT))
        put('E', KeyEvent(HidKeyCode.KEY_E, SHIFT))
        put('F', KeyEvent(HidKeyCode.KEY_F, SHIFT))
        put('G', KeyEvent(HidKeyCode.KEY_G, SHIFT))
        put('H', KeyEvent(HidKeyCode.KEY_H, SHIFT))
        put('I', KeyEvent(HidKeyCode.KEY_I, SHIFT))
        put('J', KeyEvent(HidKeyCode.KEY_J, SHIFT))
        put('K', KeyEvent(HidKeyCode.KEY_K, SHIFT))
        put('L', KeyEvent(HidKeyCode.KEY_L, SHIFT))
        put('M', KeyEvent(HidKeyCode.KEY_M, SHIFT))
        put('N', KeyEvent(HidKeyCode.KEY_N, SHIFT))
        put('O', KeyEvent(HidKeyCode.KEY_O, SHIFT))
        put('P', KeyEvent(HidKeyCode.KEY_P, SHIFT))
        put('Q', KeyEvent(HidKeyCode.KEY_Q, SHIFT))
        put('R', KeyEvent(HidKeyCode.KEY_R, SHIFT))
        put('S', KeyEvent(HidKeyCode.KEY_S, SHIFT))
        put('T', KeyEvent(HidKeyCode.KEY_T, SHIFT))
        put('U', KeyEvent(HidKeyCode.KEY_U, SHIFT))
        put('V', KeyEvent(HidKeyCode.KEY_V, SHIFT))
        put('W', KeyEvent(HidKeyCode.KEY_W, SHIFT))
        put('X', KeyEvent(HidKeyCode.KEY_X, SHIFT))
        put('Y', KeyEvent(HidKeyCode.KEY_Y, SHIFT))
        put('Z', KeyEvent(HidKeyCode.KEY_Z, SHIFT))

        // Punctuation — unshifted
        put('-', KeyEvent(HidKeyCode.KEY_MINUS, NONE))
        put('=', KeyEvent(HidKeyCode.KEY_EQUAL, NONE))
        put('[', KeyEvent(HidKeyCode.KEY_LEFT_BRACKET, NONE))
        put(']', KeyEvent(HidKeyCode.KEY_RIGHT_BRACKET, NONE))
        put('\\',KeyEvent(HidKeyCode.KEY_BACKSLASH, NONE))
        put(';', KeyEvent(HidKeyCode.KEY_SEMICOLON, NONE))
        put('\'',KeyEvent(HidKeyCode.KEY_QUOTE, NONE))
        put('`', KeyEvent(HidKeyCode.KEY_GRAVE, NONE))
        put(',', KeyEvent(HidKeyCode.KEY_COMMA, NONE))
        put('.', KeyEvent(HidKeyCode.KEY_PERIOD, NONE))
        put('/', KeyEvent(HidKeyCode.KEY_SLASH, NONE))

        // Punctuation — shifted
        put('_', KeyEvent(HidKeyCode.KEY_MINUS, SHIFT))
        put('+', KeyEvent(HidKeyCode.KEY_EQUAL, SHIFT))
        put('{', KeyEvent(HidKeyCode.KEY_LEFT_BRACKET, SHIFT))
        put('}', KeyEvent(HidKeyCode.KEY_RIGHT_BRACKET, SHIFT))
        put('|', KeyEvent(HidKeyCode.KEY_BACKSLASH, SHIFT))
        put(':', KeyEvent(HidKeyCode.KEY_SEMICOLON, SHIFT))
        put('"', KeyEvent(HidKeyCode.KEY_QUOTE, SHIFT))
        put('~', KeyEvent(HidKeyCode.KEY_GRAVE, SHIFT))
        put('<', KeyEvent(HidKeyCode.KEY_COMMA, SHIFT))
        put('>', KeyEvent(HidKeyCode.KEY_PERIOD, SHIFT))
        put('?', KeyEvent(HidKeyCode.KEY_SLASH, SHIFT))
    }

    /**
     * Maps [char] to a [KeyEvent], or null if the character cannot be typed via HID.
     * Unmappable characters (non-printable, non-ASCII, etc.) should be silently skipped.
     */
    fun map(char: Char): KeyEvent? = charMap[char]

    /**
     * Maps each character in [text] to a list of [KeyEvent]s.
     * Characters that have no HID mapping are omitted (not null entries).
     *
     * @param text The string to type.
     * @return Ordered list of key events, one per mappable character.
     */
    fun mapString(text: String): List<KeyEvent> = text.mapNotNull { charMap[it] }

    /** True if [char] can be typed via HID keyboard reports. */
    fun isMappable(char: Char): Boolean = charMap.containsKey(char)

    /** Count of characters in [text] that cannot be typed via HID. */
    fun unmappableCount(text: String): Int = text.count { !isMappable(it) }
}
