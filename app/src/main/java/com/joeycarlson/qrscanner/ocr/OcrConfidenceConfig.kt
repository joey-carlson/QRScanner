package com.joeycarlson.qrscanner.ocr

import com.joeycarlson.qrscanner.ocr.DsnValidator.ComponentType

/**
 * Configuration for OCR confidence tuning system
 * Provides sophisticated control over confidence thresholds and behavior
 */
data class OcrConfidenceConfig(
    // Global confidence settings
    val sensitivityMode: SensitivityMode = SensitivityMode.BALANCED,
    val baseConfidenceThreshold: Float = 0.85f,
    val manualVerificationThreshold: Float = 0.9f,
    
    // Component-specific thresholds
    val componentThresholds: Map<ComponentType, ComponentConfidenceConfig> = defaultComponentThresholds(),
    
    // Environmental adaptation settings
    val enableEnvironmentalAdaptation: Boolean = true,
    val lightingAdjustmentFactor: Float = 0.1f,
    val stabilityAdjustmentFactor: Float = 0.05f,
    
    // Multi-factor scoring weights
    val mlKitConfidenceWeight: Float = 0.5f,
    val patternMatchWeight: Float = 0.25f,
    val stabilityWeight: Float = 0.15f,
    val environmentalWeight: Float = 0.1f,
    
    // Advanced features
    val enableRealTimeVisualization: Boolean = false,
    val enableConfidenceHistory: Boolean = true,
    val confidenceHistorySize: Int = 10,
    
    // Analysis parameters
    val analysisIntervalMs: Long = 300L,
    val boundingBoxStabilityFrames: Int = 3,
    val minimumTextLength: Int = 5
) {
    
    /**
     * Sensitivity modes for different use cases
     */
    enum class SensitivityMode {
        CONSERVATIVE,  // Higher thresholds, more manual verification
        BALANCED,      // Default balanced approach
        AGGRESSIVE     // Lower thresholds, trust OCR more
    }
    
    /**
     * Component-specific confidence configuration
     */
    data class ComponentConfidenceConfig(
        val baseThreshold: Float,
        val manualVerificationThreshold: Float,
        val patternStrictness: PatternStrictness = PatternStrictness.MEDIUM
    )
    
    /**
     * Pattern matching strictness levels
     */
    enum class PatternStrictness {
        LOOSE,    // Accept more variations
        MEDIUM,   // Balanced pattern matching
        STRICT    // Require exact pattern match
    }
    
    companion object {
        /**
         * Default component-specific thresholds
         * Batteries have higher thresholds for safety
         */
        fun defaultComponentThresholds(): Map<ComponentType, ComponentConfidenceConfig> {
            return mapOf(
                ComponentType.GLASSES to ComponentConfidenceConfig(
                    baseThreshold = 0.85f,
                    manualVerificationThreshold = 0.9f,
                    patternStrictness = PatternStrictness.MEDIUM
                ),
                ComponentType.CONTROLLER to ComponentConfidenceConfig(
                    baseThreshold = 0.85f,
                    manualVerificationThreshold = 0.9f,
                    patternStrictness = PatternStrictness.MEDIUM
                ),
                // Batteries require higher confidence for safety
                ComponentType.BATTERY_01 to ComponentConfidenceConfig(
                    baseThreshold = 0.9f,
                    manualVerificationThreshold = 0.95f,
                    patternStrictness = PatternStrictness.STRICT
                ),
                ComponentType.BATTERY_02 to ComponentConfidenceConfig(
                    baseThreshold = 0.9f,
                    manualVerificationThreshold = 0.95f,
                    patternStrictness = PatternStrictness.STRICT
                ),
                ComponentType.BATTERY_03 to ComponentConfidenceConfig(
                    baseThreshold = 0.9f,
                    manualVerificationThreshold = 0.95f,
                    patternStrictness = PatternStrictness.STRICT
                ),
                // Accessories have lower thresholds
                ComponentType.PADS to ComponentConfidenceConfig(
                    baseThreshold = 0.8f,
                    manualVerificationThreshold = 0.85f,
                    patternStrictness = PatternStrictness.LOOSE
                ),
                ComponentType.UNUSED_01 to ComponentConfidenceConfig(
                    baseThreshold = 0.8f,
                    manualVerificationThreshold = 0.85f,
                    patternStrictness = PatternStrictness.LOOSE
                ),
                ComponentType.UNUSED_02 to ComponentConfidenceConfig(
                    baseThreshold = 0.8f,
                    manualVerificationThreshold = 0.85f,
                    patternStrictness = PatternStrictness.LOOSE
                )
            )
        }
        
        /**
         * Create config based on sensitivity mode
         */
        fun fromSensitivityMode(mode: SensitivityMode): OcrConfidenceConfig {
            return when (mode) {
                SensitivityMode.CONSERVATIVE -> OcrConfidenceConfig(
                    sensitivityMode = mode,
                    baseConfidenceThreshold = 0.9f,
                    manualVerificationThreshold = 0.95f,
                    mlKitConfidenceWeight = 0.4f,
                    patternMatchWeight = 0.35f,
                    stabilityWeight = 0.15f,
                    environmentalWeight = 0.1f
                )
                SensitivityMode.BALANCED -> OcrConfidenceConfig(
                    sensitivityMode = mode
                )
                SensitivityMode.AGGRESSIVE -> OcrConfidenceConfig(
                    sensitivityMode = mode,
                    baseConfidenceThreshold = 0.75f,
                    manualVerificationThreshold = 0.85f,
                    mlKitConfidenceWeight = 0.6f,
                    patternMatchWeight = 0.2f,
                    stabilityWeight = 0.1f,
                    environmentalWeight = 0.1f,
                    analysisIntervalMs = 200L
                )
            }
        }
    }
}
