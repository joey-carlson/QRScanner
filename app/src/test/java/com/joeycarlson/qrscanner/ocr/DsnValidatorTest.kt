package com.joeycarlson.qrscanner.ocr

import com.joeycarlson.qrscanner.TestLogger
import com.joeycarlson.qrscanner.ocr.DsnValidator.ComponentType
import com.joeycarlson.qrscanner.ocr.DsnValidator.ConfidenceLevel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [DsnValidator].
 *
 * Covers DSN pattern validation, component-type inference with confidence,
 * OCR mistake correction, normalization, similarity matching, and manual-entry
 * validation. Pure JVM tests — no Android framework classes are instantiated
 * ([RecognizedText.boundingBox] is left null).
 */
class DsnValidatorTest {

    @get:Rule
    val testLogger = TestLogger()

    private lateinit var validator: DsnValidator

    // Real-world sample DSNs (9+ digits after the known prefixes).
    private val controllerDsn = "G0G46K025224001"
    private val batteryDsn = "G0G4NU015166001"
    private val glassesDsn = "G0G348025246001"

    @Before
    fun setUp() {
        validator = DsnValidator()
    }

    // ========== isValidDsn ==========

    @Test
    fun `isValidDsn accepts real-world controller pattern`() {
        assertTrue(validator.isValidDsn(controllerDsn))
    }

    @Test
    fun `isValidDsn accepts real-world battery pattern`() {
        assertTrue(validator.isValidDsn(batteryDsn))
    }

    @Test
    fun `isValidDsn accepts real-world glasses pattern`() {
        assertTrue(validator.isValidDsn(glassesDsn))
    }

    @Test
    fun `isValidDsn normalizes case and whitespace before matching`() {
        assertTrue(validator.isValidDsn("  g0g46k025224001  "))
    }

    @Test
    fun `isValidDsn accepts common dashed-serial pattern`() {
        assertTrue(validator.isValidDsn("12-34-56-78"))
    }

    @Test
    fun `isValidDsn accepts common prefixed pattern`() {
        assertTrue(validator.isValidDsn("GL-123456"))
    }

    @Test
    fun `isValidDsn accepts generic 8-plus alphanumeric`() {
        assertTrue(validator.isValidDsn("ABCD1234"))
    }

    @Test
    fun `isValidDsn rejects short alphanumeric`() {
        assertFalse(validator.isValidDsn("ABC12"))
    }

    @Test
    fun `isValidDsn rejects text with invalid characters`() {
        assertFalse(validator.isValidDsn("hello world!"))
    }

    // ========== isValidDsnWithConfidence ==========

    @Test
    fun `isValidDsnWithConfidence accepts at threshold`() {
        assertTrue(validator.isValidDsnWithConfidence(controllerDsn, 0.8f))
    }

    @Test
    fun `isValidDsnWithConfidence rejects just below threshold`() {
        assertFalse(validator.isValidDsnWithConfidence(controllerDsn, 0.79f))
    }

    @Test
    fun `isValidDsnWithConfidence rejects valid confidence but invalid dsn`() {
        assertFalse(validator.isValidDsnWithConfidence("!!!", 0.99f))
    }

    // ========== inferComponentTypeWithConfidence ==========

    @Test
    fun `inferComponentType returns HIGH confidence for controller`() {
        val (type, confidence) = validator.inferComponentTypeWithConfidence(controllerDsn)

        assertEquals(ComponentType.CONTROLLER, type)
        assertEquals(ConfidenceLevel.HIGH, confidence)
    }

    @Test
    fun `inferComponentType returns HIGH confidence for battery`() {
        val (type, confidence) = validator.inferComponentTypeWithConfidence(batteryDsn)

        assertEquals(ComponentType.BATTERY_01, type)
        assertEquals(ConfidenceLevel.HIGH, confidence)
    }

    @Test
    fun `inferComponentType returns HIGH confidence for glasses`() {
        val (type, confidence) = validator.inferComponentTypeWithConfidence(glassesDsn)

        assertEquals(ComponentType.GLASSES, type)
        assertEquals(ConfidenceLevel.HIGH, confidence)
    }

    @Test
    fun `inferComponentType returns MEDIUM confidence for GL prefix`() {
        val (type, confidence) = validator.inferComponentTypeWithConfidence("GL-123456")

        assertEquals(ComponentType.GLASSES, type)
        assertEquals(ConfidenceLevel.MEDIUM, confidence)
    }

    @Test
    fun `inferComponentType returns MEDIUM confidence for CTRL prefix`() {
        val (type, confidence) = validator.inferComponentTypeWithConfidence("CTRL-9")

        assertEquals(ComponentType.CONTROLLER, type)
        assertEquals(ConfidenceLevel.MEDIUM, confidence)
    }

    @Test
    fun `inferComponentType returns LOW confidence for unused-01 marker`() {
        val (type, confidence) = validator.inferComponentTypeWithConfidence("UN01XYZ")

        assertEquals(ComponentType.UNUSED_01, type)
        assertEquals(ConfidenceLevel.LOW, confidence)
    }

    @Test
    fun `inferComponentType returns null for unrecognized text`() {
        val (type, confidence) = validator.inferComponentTypeWithConfidence("1234567890")

        assertNull(type)
        assertEquals(ConfidenceLevel.LOW, confidence)
    }

    @Test
    fun `inferComponentType backward-compat delegates to confidence variant`() {
        assertEquals(ComponentType.CONTROLLER, validator.inferComponentType(controllerDsn))
    }

    // ========== getDetectionConfidence ==========

    @Test
    fun `getDetectionConfidence is HIGH for high pattern and high ocr`() {
        assertEquals(ConfidenceLevel.HIGH, validator.getDetectionConfidence(controllerDsn, 0.95f))
    }

    @Test
    fun `getDetectionConfidence downgrades to MEDIUM when ocr is moderate`() {
        assertEquals(ConfidenceLevel.MEDIUM, validator.getDetectionConfidence(controllerDsn, 0.75f))
    }

    @Test
    fun `getDetectionConfidence is LOW when ocr is poor`() {
        assertEquals(ConfidenceLevel.LOW, validator.getDetectionConfidence(controllerDsn, 0.5f))
    }

    @Test
    fun `getDetectionConfidence is MEDIUM for medium pattern even with high ocr`() {
        assertEquals(ConfidenceLevel.MEDIUM, validator.getDetectionConfidence("GL-123456", 0.95f))
    }

    @Test
    fun `getDetectionConfidence is LOW when component type is unknown`() {
        assertEquals(ConfidenceLevel.LOW, validator.getDetectionConfidence("1234567890", 0.99f))
    }

    // ========== isBatteryDsn ==========

    @Test
    fun `isBatteryDsn accepts real-world battery pattern`() {
        assertTrue(validator.isBatteryDsn(batteryDsn))
    }

    @Test
    fun `isBatteryDsn accepts BAT prefix`() {
        assertTrue(validator.isBatteryDsn("BAT-01"))
    }

    @Test
    fun `isBatteryDsn accepts text containing BATTERY`() {
        assertTrue(validator.isBatteryDsn("main battery pack"))
    }

    @Test
    fun `isBatteryDsn rejects controller dsn`() {
        assertFalse(validator.isBatteryDsn(controllerDsn))
    }

    // ========== correctOcrMistakes ==========

    @Test
    fun `correctOcrMistakes fixes letter-O in controller prefix`() {
        assertEquals(controllerDsn, DsnValidator.correctOcrMistakes("GOG46K025224001"))
    }

    @Test
    fun `correctOcrMistakes fixes letter-O in battery prefix`() {
        assertEquals(batteryDsn, DsnValidator.correctOcrMistakes("GOG4NU015166001"))
    }

    @Test
    fun `correctOcrMistakes trims and uppercases`() {
        assertEquals(controllerDsn, DsnValidator.correctOcrMistakes("  g0g46k025224001 "))
    }

    @Test
    fun `correctOcrMistakes fixes confused characters in numeric portion of G0G dsn`() {
        // O->0, S->5 in the serial portion after the G0G46K prefix.
        assertEquals(controllerDsn, DsnValidator.correctOcrMistakes("G0G46KO2S224001"))
    }

    // ========== isSimilarText ==========

    @Test
    fun `isSimilarText matches identical text ignoring case and whitespace`() {
        assertTrue(DsnValidator.isSimilarText(" ABC123 ", "abc123"))
    }

    @Test
    fun `isSimilarText matches single-character OCR difference`() {
        assertTrue(DsnValidator.isSimilarText("G0G46K025224001", "G0G46K025224002"))
    }

    @Test
    fun `isSimilarText rejects clearly different strings`() {
        assertFalse(DsnValidator.isSimilarText("ABCDEFGH", "12345678"))
    }

    // ========== normalizeDsn ==========

    @Test
    fun `normalizeDsn replaces spaces with dashes`() {
        assertEquals("G0G46K-025224-001", validator.normalizeDsn("g0g46k 025224 001"))
    }

    @Test
    fun `normalizeDsn strips invalid characters`() {
        assertEquals("ABCD12", validator.normalizeDsn("AB@CD#12"))
    }

    // ========== validateManualEntry ==========

    @Test
    fun `validateManualEntry accepts a valid dsn and infers type`() {
        val result = validator.validateManualEntry(controllerDsn)

        assertTrue(result.isValid)
        assertEquals(controllerDsn, result.normalizedDsn)
        assertEquals(ComponentType.CONTROLLER, result.inferredType)
        assertNull(result.error)
    }

    @Test
    fun `validateManualEntry rejects text shorter than six characters`() {
        val result = validator.validateManualEntry("ABC")

        assertFalse(result.isValid)
        assertEquals("DSN must be at least 6 characters", result.error)
    }

    @Test
    fun `validateManualEntry strips invalid characters before validating`() {
        val result = validator.validateManualEntry("AB@CD#12")

        assertTrue(result.isValid)
        assertEquals("ABCD12", result.normalizedDsn)
    }

    // ========== findDsns ==========

    @Test
    fun `findDsns keeps only confident valid dsns sorted by confidence`() {
        val texts = listOf(
            RecognizedText(controllerDsn, 0.95f, null),
            RecognizedText(batteryDsn, 0.85f, null),
            RecognizedText(glassesDsn, 0.5f, null),   // below 0.8 confidence -> dropped
            RecognizedText("!!!", 0.99f, null)          // invalid dsn -> dropped
        )

        val candidates = validator.findDsns(texts)

        assertEquals(2, candidates.size)
        assertEquals(controllerDsn, candidates[0].dsn)
        assertEquals(ComponentType.CONTROLLER, candidates[0].componentType)
        assertEquals(batteryDsn, candidates[1].dsn)
    }

    @Test
    fun `findDsns returns empty list when nothing qualifies`() {
        val texts = listOf(RecognizedText("!!!", 0.99f, null))

        assertTrue(validator.findDsns(texts).isEmpty())
    }
}
