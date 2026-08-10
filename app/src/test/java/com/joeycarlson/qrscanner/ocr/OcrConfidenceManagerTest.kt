package com.joeycarlson.qrscanner.ocr

import com.joeycarlson.qrscanner.TestLogger
import com.joeycarlson.qrscanner.ocr.DsnValidator.ComponentType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for [OcrConfidenceManager].
 *
 * Focus is the multi-factor confidence math: pattern-match scoring per
 * strictness level, fallback confidence, stability, and the weighted
 * combination + component adjustments (clamping, manual-verification gating).
 *
 * Robolectric is required because the constructor touches `LogManager` and
 * `PreferenceManager`. Bounding boxes are left null to keep tests off the
 * Android graphics path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OcrConfidenceManagerTest {

    @get:Rule
    val testLogger = TestLogger()

    // Real-world DSNs (match REAL_WORLD_DSN_PATTERNS in DsnValidator).
    private val controllerDsn = "G0G46K025224001"   // 15 chars, CONTROLLER (MEDIUM strictness)
    private val batteryDsn = "G0G4NU015166001"       // 15 chars, BATTERY_01 (STRICT strictness)

    private val delta = 0.0001f

    private lateinit var manager: OcrConfidenceManager

    @Before
    fun setUp() {
        manager = OcrConfidenceManager(RuntimeEnvironment.getApplication())
    }

    private fun managerWith(config: OcrConfidenceConfig) =
        OcrConfidenceManager(RuntimeEnvironment.getApplication(), config)

    private fun confidenceFor(
        mlKit: Float?,
        text: String,
        type: ComponentType?,
        mgr: OcrConfidenceManager = manager
    ) = mgr.calculateConfidence(
        mlKitConfidence = mlKit,
        recognizedText = text,
        boundingBox = null,
        componentType = type,
        timestamp = 1_000L
    )

    // ========== Pattern match scoring ==========

    @Test
    fun `pattern score is low when text is not a valid dsn`() {
        val result = confidenceFor(0.9f, "!!!", null)

        assertEquals(0.3f, result.factors.patternMatchScore, delta)
    }

    @Test
    fun `pattern score for LOOSE component is high on any valid dsn`() {
        // PADS -> LOOSE strictness; "ABCD1234" matches the generic 8+ pattern.
        val result = confidenceFor(0.9f, "ABCD1234", ComponentType.PADS)

        assertEquals(0.9f, result.factors.patternMatchScore, delta)
    }

    @Test
    fun `pattern score for STRICT battery is perfect when component matches`() {
        val result = confidenceFor(0.9f, batteryDsn, ComponentType.BATTERY_01)

        assertEquals(1.0f, result.factors.patternMatchScore, delta)
    }

    /**
     * DOCUMENTS A LIKELY DEFECT (not a crash): for MEDIUM strictness the bonus
     * paths require `text.length in 10..12` and a leading-digits prefix
     * (`^[0-9]{5,}`). Real-world DSNs are 15 chars and start with letters
     * (G0G...), so a *perfect* controller DSN can never reach 0.95/0.85 and
     * always falls through to 0.75. Pinning current behavior; see notes to user.
     */
    @Test
    fun `pattern score for real controller dsn falls through to 0_75 (length gate never met)`() {
        val result = confidenceFor(0.9f, controllerDsn, ComponentType.CONTROLLER)

        assertEquals(0.75f, result.factors.patternMatchScore, delta)
    }

    /**
     * Proves the MEDIUM bonus branch CAN fire — just not for real DSNs.
     * "12345ABCDE" is 10 chars, valid generic pattern, 5 leading digits.
     */
    @Test
    fun `pattern score reaches 0_95 for synthetic value meeting length and prefix gates`() {
        val result = confidenceFor(0.9f, "12345ABCDE", ComponentType.CONTROLLER)

        assertEquals(0.95f, result.factors.patternMatchScore, delta)
    }

    // ========== Fallback confidence (mlKitConfidence == null) ==========

    @Test
    fun `fallback confidence is 0_5 for very short text`() {
        val result = confidenceFor(null, "AB", null)

        assertEquals(0.5f, result.factors.mlKitConfidence, delta)
    }

    @Test
    fun `fallback confidence is 0_7 when text contains special characters`() {
        val result = confidenceFor(null, "ABCD-1234", null)

        assertEquals(0.7f, result.factors.mlKitConfidence, delta)
    }

    @Test
    fun `fallback confidence is 0_8 for clean alphanumeric text`() {
        val result = confidenceFor(null, "ABCDEFGH", null)

        assertEquals(0.8f, result.factors.mlKitConfidence, delta)
    }

    @Test
    fun `fallback confidence is reduced for STRICT components`() {
        // Clean text -> 0.8, then *0.9 for STRICT battery = 0.72.
        val result = confidenceFor(null, "ABCDEFGH", ComponentType.BATTERY_01)

        assertEquals(0.72f, result.factors.mlKitConfidence, delta)
    }

    // ========== Stability ==========

    @Test
    fun `stability score defaults to 0_8 when no bounding box is provided`() {
        val result = confidenceFor(0.9f, controllerDsn, ComponentType.CONTROLLER)

        assertEquals(0.8f, result.factors.stabilityScore, delta)
    }

    // ========== Weighted combination + manual verification ==========

    @Test
    fun `weighted confidence combines factors with default weights`() {
        // base 1.0*0.5 + pattern 0.75*0.25 + stability 0.8*0.15 + env 1.0*0.1 = 0.9075
        val result = confidenceFor(1.0f, controllerDsn, ComponentType.CONTROLLER)

        assertEquals(0.9075f, result.confidence, delta)
        // 0.9075 >= CONTROLLER manual-verification threshold (0.9) -> no manual check.
        assertFalse(result.requiresManualVerification)
    }

    @Test
    fun `low overall confidence requires manual verification`() {
        val result = confidenceFor(0.0f, "!!!", null)

        assertTrue(result.requiresManualVerification)
        assertTrue(result.confidence < 0.9f)
    }

    // ========== Clamping (bug guard) ==========

    /**
     * Guards a fixed defect: [OcrConfidenceManager.applyComponentSpecificAdjustments]
     * returned the raw weighted score for the null-component path WITHOUT
     * clamping to [0,1], while every other path clamps. With non-normalized
     * weights (sum > 1) this leaked confidences above 1.0. Both null and
     * non-null paths must now stay within [0,1].
     */
    @Test
    fun `confidence stays within 0 to 1 even with oversized weights and null component`() {
        val oversized = OcrConfidenceConfig(
            mlKitConfidenceWeight = 1.0f,
            patternMatchWeight = 1.0f,
            stabilityWeight = 1.0f,
            environmentalWeight = 1.0f
        )
        val mgr = managerWith(oversized)

        val nullComponent = confidenceFor(1.0f, controllerDsn, null, mgr)
        val withComponent = confidenceFor(1.0f, controllerDsn, ComponentType.CONTROLLER, mgr)

        assertTrue("null-component confidence must be clamped", nullComponent.confidence <= 1.0f)
        assertTrue("component confidence must be clamped", withComponent.confidence <= 1.0f)
    }

    // ========== Sensitivity mode adjustment ==========

    @Test
    fun `aggressive sensitivity yields at least as much confidence as conservative`() {
        val aggressive = managerWith(
            OcrConfidenceConfig.fromSensitivityMode(OcrConfidenceConfig.SensitivityMode.AGGRESSIVE)
        )
        val conservative = managerWith(
            OcrConfidenceConfig.fromSensitivityMode(OcrConfidenceConfig.SensitivityMode.CONSERVATIVE)
        )

        val aggr = confidenceFor(0.9f, controllerDsn, ComponentType.CONTROLLER, aggressive)
        val cons = confidenceFor(0.9f, controllerDsn, ComponentType.CONTROLLER, conservative)

        assertTrue(aggr.confidence >= cons.confidence)
    }

    // ========== History tracking ==========

    @Test
    fun `average confidence falls back to base threshold when history is empty`() {
        assertEquals(0.85f, manager.getAverageConfidence(), delta)
    }

    @Test
    fun `confidence history accumulates and reset clears it`() {
        confidenceFor(0.9f, controllerDsn, ComponentType.CONTROLLER)
        confidenceFor(0.8f, controllerDsn, ComponentType.CONTROLLER)
        assertEquals(2, manager.getConfidenceHistory().size)

        manager.reset()

        assertTrue(manager.getConfidenceHistory().isEmpty())
    }

    @Test
    fun `environmental factor is clamped to 0 to 1`() {
        manager.updateEnvironmentalFactor(5.0f)

        // Env factor feeds environmentalScore (weight 0.1). A clamped 1.0 keeps
        // the perfect-input confidence at the expected 0.9075.
        val result = confidenceFor(1.0f, controllerDsn, ComponentType.CONTROLLER)

        assertEquals(0.9075f, result.confidence, delta)
    }
}
