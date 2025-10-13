package com.joeycarlson.qrscanner.util

import com.joeycarlson.qrscanner.TestLogger
import com.joeycarlson.qrscanner.util.BarcodeValidator.BarcodeFormat
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for BarcodeValidator
 * Tests security validations, format detection, and format-specific validations
 */
class BarcodeValidatorTest {
    
    @get:Rule
    val testLogger = TestLogger()
    
    // ========== Security Validation Tests ==========
    
    @Test
    fun `validateBarcodeData rejects empty string`() {
        val result = BarcodeValidator.validateBarcodeData("")
        
        assertFalse("Empty data should be invalid", result.isValid)
        assertEquals("Empty barcode data", result.errorMessage)
        assertEquals(BarcodeFormat.UNKNOWN, result.format)
    }
    
    @Test
    fun `validateBarcodeData rejects whitespace only`() {
        val result = BarcodeValidator.validateBarcodeData("   ")
        
        assertFalse("Whitespace-only data should be invalid", result.isValid)
        assertEquals(BarcodeFormat.UNKNOWN, result.format)
    }
    
    @Test
    fun `validateBarcodeData rejects data exceeding 200 characters`() {
        val longData = "A".repeat(201)
        val result = BarcodeValidator.validateBarcodeData(longData)
        
        assertFalse("Data over 200 chars should be invalid", result.isValid)
        assertEquals("Barcode data too long", result.errorMessage)
    }
    
    @Test
    fun `validateBarcodeData accepts data at 200 character limit`() {
        val maxData = "A".repeat(200)
        val result = BarcodeValidator.validateBarcodeData(maxData)
        
        // Should pass length check (even if format validation might fail)
        assertNotEquals("Barcode data too long", result.errorMessage)
    }
    
    @Test
    fun `validateBarcodeData detects script injection attempts`() {
        val injectionAttempts = listOf(
            "<script>alert('xss')</script>",
            "javascript:void(0)",
            "vbscript:msgbox",
            "onload=alert(1)",
            "onerror=alert(1)"
        )
        
        injectionAttempts.forEach { injection ->
            val result = BarcodeValidator.validateBarcodeData(injection)
            assertFalse("Should reject injection: $injection", result.isValid)
            assertEquals("Suspicious content detected", result.errorMessage)
        }
    }
    
    @Test
    fun `validateBarcodeData detects SQL injection attempts`() {
        val sqlInjections = listOf(
            "'; DROP TABLE users--",
            "1' OR '1'='1",
            "admin'--",
            "' UNION SELECT * FROM users--",
            "'; DELETE FROM data--"
        )
        
        sqlInjections.forEach { injection ->
            val result = BarcodeValidator.validateBarcodeData(injection)
            assertFalse("Should reject SQL injection: $injection", result.isValid)
            assertEquals("Suspicious content detected", result.errorMessage)
        }
    }
    
    @Test
    fun `validateBarcodeData detects template injection attempts`() {
        val templateInjections = listOf(
            "{{constructor.constructor('alert(1)')()}}",
            "\${system('ls')}",
            "<%= system('ls') %>",
            "<?php echo shell_exec('ls'); ?>"
        )
        
        templateInjections.forEach { injection ->
            val result = BarcodeValidator.validateBarcodeData(injection)
            assertFalse("Should reject template injection: $injection", result.isValid)
            assertEquals("Suspicious content detected", result.errorMessage)
        }
    }
    
    // ========== Format Detection Tests ==========
    
    @Test
    fun `detectBarcodeFormat identifies UPC-A correctly`() {
        val upcA = "123456789012"
        val result = BarcodeValidator.validateBarcodeData(upcA)
        
        assertEquals(BarcodeFormat.UPC_A, result.format)
        assertTrue(result.isValid)
    }
    
    @Test
    fun `detectBarcodeFormat identifies UPC-E correctly`() {
        val upcE = "01234567"
        val result = BarcodeValidator.validateBarcodeData(upcE)
        
        assertEquals(BarcodeFormat.UPC_E, result.format)
        assertTrue(result.isValid)
    }
    
    @Test
    fun `detectBarcodeFormat identifies EAN-13 correctly`() {
        val ean13 = "1234567890123"
        val result = BarcodeValidator.validateBarcodeData(ean13)
        
        assertEquals(BarcodeFormat.EAN_13, result.format)
        assertTrue(result.isValid)
    }
    
    @Test
    fun `detectBarcodeFormat identifies EAN-8 correctly`() {
        val ean8 = "12345678"
        val result = BarcodeValidator.validateBarcodeData(ean8)
        
        assertEquals(BarcodeFormat.EAN_8, result.format)
        assertTrue(result.isValid)
    }
    
    @Test
    fun `detectBarcodeFormat identifies Code 39 correctly`() {
        val code39 = "ABC-123"
        val result = BarcodeValidator.validateBarcodeData(code39)
        
        assertEquals(BarcodeFormat.CODE_39, result.format)
        assertTrue(result.isValid)
    }
    
    @Test
    fun `detectBarcodeFormat identifies Code 128 correctly`() {
        val code128 = "ABC123xyz"
        val result = BarcodeValidator.validateBarcodeData(code128)
        
        assertEquals(BarcodeFormat.CODE_128, result.format)
        assertTrue(result.isValid)
    }
    
    @Test
    fun `detectBarcodeFormat identifies QR Code with URL correctly`() {
        val qrCode = "https://example.com/path"
        val result = BarcodeValidator.validateBarcodeData(qrCode)
        
        assertEquals(BarcodeFormat.QR_CODE, result.format)
        assertTrue(result.isValid)
    }
    
    @Test
    fun `detectBarcodeFormat identifies QR Code with long content correctly`() {
        val longQrContent = "A".repeat(51)
        val result = BarcodeValidator.validateBarcodeData(longQrContent)
        
        assertEquals(BarcodeFormat.QR_CODE, result.format)
    }
    
    // ========== Format-Specific Validation Tests ==========
    
    @Test
    fun `validateFormat rejects UPC-A with incorrect length`() {
        val invalidUpcA = listOf("12345", "12345678901", "1234567890123")
        
        invalidUpcA.forEach { code ->
            val result = BarcodeValidator.validateBarcodeData(code)
            assertNotEquals("Should not be UPC-A: $code", BarcodeFormat.UPC_A, result.format)
        }
    }
    
    @Test
    fun `validateFormat rejects UPC-A with non-digits`() {
        val invalidUpcA = "12345678901A"
        val result = BarcodeValidator.validateBarcodeData(invalidUpcA)
        
        assertNotEquals(BarcodeFormat.UPC_A, result.format)
    }
    
    @Test
    fun `validateFormat rejects UPC-E not starting with zero`() {
        val notUpcE = "12345678"
        val result = BarcodeValidator.validateBarcodeData(notUpcE)
        
        assertEquals(BarcodeFormat.EAN_8, result.format)
        assertNotEquals(BarcodeFormat.UPC_E, result.format)
    }
    
    @Test
    fun `validateFormat rejects Code 39 with lowercase letters`() {
        val invalidCode39 = "abc-123"
        val result = BarcodeValidator.validateBarcodeData(invalidCode39)
        
        assertNotEquals(BarcodeFormat.CODE_39, result.format)
    }
    
    @Test
    fun `validateFormat rejects Code 93 exceeding 47 characters`() {
        val longCode93 = "A".repeat(48)
        val result = BarcodeValidator.validateBarcodeData(longCode93)
        
        // Should be detected as QR_CODE due to length
        assertEquals(BarcodeFormat.QR_CODE, result.format)
    }
    
    @Test
    fun `validateFormat accepts valid Code 128 with mixed case`() {
        val code128 = "AbC123xyz"
        val result = BarcodeValidator.validateBarcodeData(code128)
        
        assertEquals(BarcodeFormat.CODE_128, result.format)
        assertTrue(result.isValid)
    }
    
    @Test
    fun `validateFormat rejects QR Code with unsafe characters`() {
        val unsafeQr = "http://example.com/<script>"
        val result = BarcodeValidator.validateBarcodeData(unsafeQr)
        
        // Should be caught by security validation
        assertFalse(result.isValid)
        assertEquals("Suspicious content detected", result.errorMessage)
    }
    
    // ========== Edge Cases and Special Characters ==========
    
    @Test
    fun `validateBarcodeData trims whitespace`() {
        val dataWithWhitespace = "  123456789012  "
        val result = BarcodeValidator.validateBarcodeData(dataWithWhitespace)
        
        assertEquals("123456789012", result.sanitizedData)
        assertEquals(BarcodeFormat.UPC_A, result.format)
    }
    
    @Test
    fun `validateBarcodeData handles valid special characters in Code 39`() {
        val specialChars = "ABC-123 $+%"
        val result = BarcodeValidator.validateBarcodeData(specialChars)
        
        assertEquals(BarcodeFormat.CODE_39, result.format)
        assertTrue(result.isValid)
    }
    
    @Test
    fun `validateBarcodeData handles URLs in QR codes`() {
        val urls = listOf(
            "http://example.com",
            "https://example.com/path?param=value",
            "ftp://files.example.com",
            "mailto:test@example.com"
        )
        
        urls.forEach { url ->
            val result = BarcodeValidator.validateBarcodeData(url)
            assertEquals("Should be QR_CODE: $url", BarcodeFormat.QR_CODE, result.format)
            assertTrue("Should be valid: $url", result.isValid)
        }
    }
    
    @Test
    fun `validateBarcodeData returns sanitized data`() {
        val data = "  123456789012  "
        val result = BarcodeValidator.validateBarcodeData(data)
        
        assertTrue(result.isValid)
        assertEquals("123456789012", result.sanitizedData)
        assertFalse(result.sanitizedData.contains(" "))
    }
    
    // ========== Real-World Test Cases ==========
    
    @Test
    fun `validateBarcodeData handles typical product barcodes`() {
        val productCodes = listOf(
            "012345678905" to BarcodeFormat.UPC_A,    // UPC-A
            "5901234123457" to BarcodeFormat.EAN_13,  // EAN-13
            "96385074" to BarcodeFormat.EAN_8          // EAN-8
        )
        
        productCodes.forEach { (code, expectedFormat) ->
            val result = BarcodeValidator.validateBarcodeData(code)
            assertEquals("Format mismatch for $code", expectedFormat, result.format)
            assertTrue("Should be valid: $code", result.isValid)
        }
    }
    
    @Test
    fun `validateBarcodeData handles typical kit identifiers`() {
        val kitIds = listOf(
            "KIT-2024-001",
            "PILOT_KIT_123",
            "A1B2C3D4",
            "kit-bundle-2024"
        )
        
        kitIds.forEach { kitId ->
            val result = BarcodeValidator.validateBarcodeData(kitId)
            assertTrue("Kit ID should be valid: $kitId", result.isValid)
            assertNotEquals("Should detect format: $kitId", BarcodeFormat.UNKNOWN, result.format)
        }
    }
}
