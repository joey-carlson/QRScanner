package com.joeycarlson.qrscanner.livescan

import com.joeycarlson.qrscanner.livescan.hid.HidKeyCode
import com.joeycarlson.qrscanner.livescan.hid.KeyMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [KeyMapper].
 *
 * Verifies that ASCII characters map to the correct HID key codes and modifiers,
 * and that unmappable characters are handled gracefully.
 */
class KeyMapperTest {

    // -------------------------------------------------------------------------
    // Gist test — covers core functionality end-to-end
    // -------------------------------------------------------------------------

    @Test
    fun `gist - mapString produces correct key events for typical barcode value`() {
        // Arrange
        val barcode = "KIT-123"

        // Act
        val events = KeyMapper.mapString(barcode)

        // Assert — all 7 chars must be mappable and produce correct modifiers
        assertEquals(7, events.size)
        // 'K' = uppercase → shift
        assertEquals(HidKeyCode.MOD_LEFT_SHIFT, events[0].modifier)
        assertEquals(HidKeyCode.KEY_K, events[0].keyCode)
        // 'I' = uppercase → shift
        assertEquals(HidKeyCode.MOD_LEFT_SHIFT, events[1].modifier)
        // 'T' = uppercase → shift
        assertEquals(HidKeyCode.MOD_LEFT_SHIFT, events[2].modifier)
        // '-' = unshifted
        assertEquals(HidKeyCode.MOD_NONE, events[3].modifier)
        assertEquals(HidKeyCode.KEY_MINUS, events[3].keyCode)
        // '1' = unshifted digit
        assertEquals(HidKeyCode.MOD_NONE, events[4].modifier)
        assertEquals(HidKeyCode.KEY_1, events[4].keyCode)
    }

    // -------------------------------------------------------------------------
    // Letters
    // -------------------------------------------------------------------------

    @Test
    fun `lowercase a maps to KEY_A with no modifier`() {
        val event = KeyMapper.map('a')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_A, event!!.keyCode)
        assertEquals(HidKeyCode.MOD_NONE, event.modifier)
    }

    @Test
    fun `uppercase A maps to KEY_A with Left Shift`() {
        val event = KeyMapper.map('A')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_A, event!!.keyCode)
        assertEquals(HidKeyCode.MOD_LEFT_SHIFT, event.modifier)
    }

    @Test
    fun `lowercase z maps to KEY_Z with no modifier`() {
        val event = KeyMapper.map('z')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_Z, event!!.keyCode)
        assertEquals(HidKeyCode.MOD_NONE, event.modifier)
    }

    @Test
    fun `uppercase Z maps to KEY_Z with Left Shift`() {
        val event = KeyMapper.map('Z')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_Z, event!!.keyCode)
        assertEquals(HidKeyCode.MOD_LEFT_SHIFT, event.modifier)
    }

    @Test
    fun `all 26 lowercase letters are mappable`() {
        ('a'..'z').forEach { char ->
            assertNotNull("Expected '$char' to be mappable", KeyMapper.map(char))
        }
    }

    @Test
    fun `all 26 uppercase letters are mappable`() {
        ('A'..'Z').forEach { char ->
            assertNotNull("Expected '$char' to be mappable", KeyMapper.map(char))
        }
    }

    @Test
    fun `uppercase and lowercase of same letter share same key code`() {
        ('a'..'z').forEach { lower ->
            val upper = lower.uppercaseChar()
            val lowerEvent = KeyMapper.map(lower)
            val upperEvent = KeyMapper.map(upper)
            assertNotNull(lowerEvent)
            assertNotNull(upperEvent)
            assertEquals(
                "Expected same keyCode for '$lower' and '$upper'",
                lowerEvent!!.keyCode,
                upperEvent!!.keyCode
            )
        }
    }

    // -------------------------------------------------------------------------
    // Digits
    // -------------------------------------------------------------------------

    @Test
    fun `digit 0 maps to KEY_0 with no modifier`() {
        val event = KeyMapper.map('0')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_0, event!!.keyCode)
        assertEquals(HidKeyCode.MOD_NONE, event.modifier)
    }

    @Test
    fun `all digits 0-9 are mappable with no modifier`() {
        ('0'..'9').forEach { digit ->
            val event = KeyMapper.map(digit)
            assertNotNull("Expected '$digit' to be mappable", event)
            assertEquals("Expected digit '$digit' to have no modifier", HidKeyCode.MOD_NONE, event!!.modifier)
        }
    }

    @Test
    fun `exclamation mark maps with Left Shift modifier`() {
        val event = KeyMapper.map('!')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_1, event!!.keyCode)
        assertEquals(HidKeyCode.MOD_LEFT_SHIFT, event.modifier)
    }

    // -------------------------------------------------------------------------
    // Control characters
    // -------------------------------------------------------------------------

    @Test
    fun `newline maps to KEY_ENTER`() {
        val event = KeyMapper.map('\n')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_ENTER, event!!.keyCode)
        assertEquals(HidKeyCode.MOD_NONE, event.modifier)
    }

    @Test
    fun `tab maps to KEY_TAB`() {
        val event = KeyMapper.map('\t')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_TAB, event!!.keyCode)
        assertEquals(HidKeyCode.MOD_NONE, event.modifier)
    }

    @Test
    fun `space maps to KEY_SPACE`() {
        val event = KeyMapper.map(' ')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_SPACE, event!!.keyCode)
    }

    // -------------------------------------------------------------------------
    // Punctuation
    // -------------------------------------------------------------------------

    @Test
    fun `hyphen maps to KEY_MINUS with no modifier`() {
        val event = KeyMapper.map('-')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_MINUS, event!!.keyCode)
        assertEquals(HidKeyCode.MOD_NONE, event.modifier)
    }

    @Test
    fun `underscore maps to KEY_MINUS with Left Shift`() {
        val event = KeyMapper.map('_')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_MINUS, event!!.keyCode)
        assertEquals(HidKeyCode.MOD_LEFT_SHIFT, event.modifier)
    }

    @Test
    fun `period maps to KEY_PERIOD with no modifier`() {
        val event = KeyMapper.map('.')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_PERIOD, event!!.keyCode)
        assertEquals(HidKeyCode.MOD_NONE, event.modifier)
    }

    @Test
    fun `colon maps to KEY_SEMICOLON with Left Shift`() {
        val event = KeyMapper.map(':')
        assertNotNull(event)
        assertEquals(HidKeyCode.KEY_SEMICOLON, event!!.keyCode)
        assertEquals(HidKeyCode.MOD_LEFT_SHIFT, event.modifier)
    }

    // -------------------------------------------------------------------------
    // Unmappable characters
    // -------------------------------------------------------------------------

    @Test
    fun `null character is not mappable`() {
        assertNull(KeyMapper.map('\u0000'))
    }

    @Test
    fun `non-ASCII unicode is not mappable`() {
        assertNull(KeyMapper.map('é'))
        assertNull(KeyMapper.map('中'))
        assertNull(KeyMapper.map('€'))
    }

    @Test
    fun `mapString skips unmappable characters`() {
        // 'é' has no HID mapping — should be silently skipped
        val events = KeyMapper.mapString("héllo")
        assertEquals(4, events.size)  // h, l, l, o — 'é' dropped
    }

    @Test
    fun `unmappableCount returns correct count`() {
        assertEquals(0, KeyMapper.unmappableCount("Hello-123"))
        assertEquals(1, KeyMapper.unmappableCount("héllo"))
        assertEquals(2, KeyMapper.unmappableCount("€uro中"))  // € and 中
    }

    // -------------------------------------------------------------------------
    // mapString
    // -------------------------------------------------------------------------

    @Test
    fun `mapString on empty string returns empty list`() {
        assertTrue(KeyMapper.mapString("").isEmpty())
    }

    @Test
    fun `mapString preserves character order`() {
        val events = KeyMapper.mapString("abc")
        assertEquals(3, events.size)
        assertEquals(HidKeyCode.KEY_A, events[0].keyCode)
        assertEquals(HidKeyCode.KEY_B, events[1].keyCode)
        assertEquals(HidKeyCode.KEY_C, events[2].keyCode)
    }
}
