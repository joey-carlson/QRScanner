package com.joeycarlson.qrscanner.data

import com.joeycarlson.qrscanner.data.ScanHistoryItem.ActivityType
import com.joeycarlson.qrscanner.data.ScanHistoryItem.ScanType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the [ScanHistoryItem] model's pure logic: the `getShortValue`
 * truncation boundary. Plain JVM — no Android framework classes.
 */
class ScanHistoryItemTest {

    private fun item(value: String) = ScanHistoryItem(
        value = value,
        scanType = ScanType.BARCODE,
        activityType = ActivityType.CHECKOUT
    )

    @Test
    fun `getShortValue returns the value unchanged when at or under the limit`() {
        val exactly30 = "a".repeat(30)

        // Boundary: length == maxLength is NOT truncated (only length > maxLength is).
        assertEquals(exactly30, item(exactly30).getShortValue(30))
        assertEquals("short", item("short").getShortValue(30))
    }

    @Test
    fun `getShortValue truncates and appends ellipsis when over the limit`() {
        val long = "a".repeat(31)

        val result = item(long).getShortValue(30)

        assertEquals("a".repeat(30) + "...", result)
    }

    @Test
    fun `getShortValue honors a custom max length`() {
        assertEquals("abc...", item("abcdef").getShortValue(3))
    }
}
