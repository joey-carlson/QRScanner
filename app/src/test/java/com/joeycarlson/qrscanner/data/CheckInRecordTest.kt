package com.joeycarlson.qrscanner.data

import com.joeycarlson.qrscanner.TestLogger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for CheckInRecord data class
 * Tests data model validation, timestamps, and serialization
 */
class CheckInRecordTest {
    
    @get:Rule
    val testLogger = TestLogger()
    
    @Test
    fun `CheckInRecord creates with valid data`() {
        val record = CheckInRecord(
            kitId = "KIT-123",
            type = "CHECKIN",
            value = "Kit KIT-123 checked in"
        )
        
        assertEquals("KIT-123", record.kitId)
        assertEquals("CHECKIN", record.type)
        assertEquals("Kit KIT-123 checked in", record.value)
        assertTrue("Timestamp should not be empty", record.timestamp.isNotEmpty())
    }
    
    @Test
    fun `CheckInRecord generates valid timestamp`() {
        val record = CheckInRecord(
            kitId = "KIT-123",
            type = "CHECKIN",
            value = "Test"
        )
        
        assertTrue("Timestamp should be ISO-8601 format", record.timestamp.contains("T"))
        assertTrue("Timestamp should contain Z timezone", record.timestamp.endsWith("Z"))
    }
    
    @Test
    fun `CheckInRecord supports empty kitId`() {
        val record = CheckInRecord(
            kitId = "",
            type = "CHECKIN",
            value = "Test"
        )
        
        assertEquals("", record.kitId)
        assertNotNull(record.timestamp)
    }
    
    @Test
    fun `CheckInRecord equality comparison works correctly`() {
        val timestamp = "2024-10-13T12:00:00Z"
        val record1 = CheckInRecord(
            kitId = "KIT-123",
            type = "CHECKIN",
            value = "Test",
            timestamp = timestamp
        )
        val record2 = CheckInRecord(
            kitId = "KIT-123",
            type = "CHECKIN",
            value = "Test",
            timestamp = timestamp
        )
        
        assertEquals(record1, record2)
        assertEquals(record1.hashCode(), record2.hashCode())
    }
    
    @Test
    fun `CheckInRecord different timestamps create different records`() {
        val record1 = CheckInRecord(
            kitId = "KIT-123",
            type = "CHECKIN",
            value = "Test",
            timestamp = "2024-10-13T12:00:00Z"
        )
        val record2 = CheckInRecord(
            kitId = "KIT-123",
            type = "CHECKIN",
            value = "Test",
            timestamp = "2024-10-13T13:00:00Z"
        )
        
        assertNotEquals(record1, record2)
    }
    
    @Test
    fun `CheckInRecord copy creates independent instance`() {
        val original = CheckInRecord(
            kitId = "KIT-123",
            type = "CHECKIN",
            value = "Original"
        )
        val copy = original.copy(value = "Modified")
        
        assertEquals("KIT-123", copy.kitId)
        assertEquals("Modified", copy.value)
        assertEquals(original.timestamp, copy.timestamp)
        assertNotEquals(original.value, copy.value)
    }
    
    @Test
    fun `CheckInRecord handles special characters in kitId`() {
        val specialKitIds = listOf(
            "KIT-2024-001",
            "PILOT_KIT_123",
            "KIT.BUNDLE.456",
            "KIT@789"
        )
        
        specialKitIds.forEach { kitId ->
            val record = CheckInRecord(
                kitId = kitId,
                type = "CHECKIN",
                value = "Test"
            )
            assertEquals(kitId, record.kitId)
        }
    }
    
    @Test
    fun `CheckInRecord toString includes all fields`() {
        val record = CheckInRecord(
            kitId = "KIT-123",
            type = "CHECKIN",
            value = "Test value"
        )
        val string = record.toString()
        
        assertTrue(string.contains("KIT-123"))
        assertTrue(string.contains("CHECKIN"))
        assertTrue(string.contains("Test value"))
    }
}
