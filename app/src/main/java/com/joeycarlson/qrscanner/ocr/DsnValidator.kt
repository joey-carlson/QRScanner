package com.joeycarlson.qrscanner.ocr

import android.graphics.Rect

/**
 * Validates and parses Device Serial Numbers (DSN) from recognized text
 * Supports common DSN patterns and component type inference
 */
class DsnValidator {
    
    /**
     * Enum representing the different component types in a kit bundle
     */
    enum class ComponentType {
        GLASSES,
        CONTROLLER,
        BATTERY_01,
        BATTERY_02,
        BATTERY_03,
        PADS,
        UNUSED_01,
        UNUSED_02
    }
    
    companion object {
        // Common DSN patterns (can be extended based on actual patterns)
        private val DSN_PATTERNS = listOf(
            // Pattern 1: Component prefix followed by digits (e.g., GL-123456, CTRL-789012)
            Regex("^(GL|CTRL|BAT|PAD|UN)-\\d{6,}$"),
            
            // Pattern 2: Alphanumeric with specific length (e.g., ABC123DEF456)
            Regex("^[A-Z]{3}\\d{3}[A-Z]{3}\\d{3}$"),
            
            // Pattern 3: Serial number with dashes (e.g., 12-34-56-78)
            Regex("^\\d{2}-\\d{2}-\\d{2}-\\d{2}$"),
            
            // Pattern 4: Alphanumeric with underscores (e.g., SN_ABC_123456)
            Regex("^SN_[A-Z]+_\\d{6,}$"),
            
            // Pattern 5: Simple alphanumeric (minimum 8 characters)
            Regex("^[A-Z0-9]{8,}$")
        )
        
        // Component type inference patterns
        private val COMPONENT_TYPE_PATTERNS = mapOf(
            ComponentType.GLASSES to listOf(
                Regex("^GL[-_]?.*"),
                Regex(".*GLASS.*"),
                Regex(".*LENS.*")
            ),
            ComponentType.CONTROLLER to listOf(
                Regex("^CTRL[-_]?.*"),
                Regex(".*CONTROL.*"),
                Regex(".*REMOTE.*")
            ),
            ComponentType.BATTERY_01 to listOf(
                Regex("^BAT[-_]?0?1.*"),
                Regex(".*BATTERY[-_]?0?1.*")
            ),
            ComponentType.BATTERY_02 to listOf(
                Regex("^BAT[-_]?0?2.*"),
                Regex(".*BATTERY[-_]?0?2.*")
            ),
            ComponentType.BATTERY_03 to listOf(
                Regex("^BAT[-_]?0?3.*"),
                Regex(".*BATTERY[-_]?0?3.*")
            ),
            ComponentType.PADS to listOf(
                Regex("^PAD[-_]?.*"),
                Regex(".*PADS?.*")
            ),
            ComponentType.UNUSED_01 to listOf(
                Regex("^UN[-_]?0?1.*"),
                Regex(".*UNUSED[-_]?0?1.*")
            ),
            ComponentType.UNUSED_02 to listOf(
                Regex("^UN[-_]?0?2.*"),
                Regex(".*UNUSED[-_]?0?2.*")
            )
        )
        
        private const val MIN_CONFIDENCE_THRESHOLD = 0.8f
    }
    
    /**
     * Validates if the recognized text matches DSN patterns
     */
    fun isValidDsn(text: String): Boolean {
        val normalizedText = text.trim().uppercase()
        return DSN_PATTERNS.any { pattern -> 
            pattern.matches(normalizedText)
        }
    }
    
    /**
     * Validates DSN with confidence threshold
     */
    fun isValidDsnWithConfidence(text: String, confidence: Float): Boolean {
        return confidence >= MIN_CONFIDENCE_THRESHOLD && isValidDsn(text)
    }
    
    /**
     * Attempts to infer component type from DSN
     */
    fun inferComponentType(dsn: String): ComponentType? {
        val normalizedDsn = dsn.trim().uppercase()
        
        for ((componentType, patterns) in COMPONENT_TYPE_PATTERNS) {
            if (patterns.any { pattern -> pattern.matches(normalizedDsn) }) {
                return componentType
            }
        }
        
        return null
    }
    
    /**
     * Normalizes DSN for consistent storage
     */
    fun normalizeDsn(text: String): String {
        return text.trim()
            .uppercase()
            .replace(Regex("\\s+"), "-") // Replace spaces with dashes
            .replace(Regex("[^A-Z0-9\\-_/.]"), "") // Remove invalid characters
    }
    
    /**
     * Finds all potential DSNs in a list of recognized texts
     */
    fun findDsns(recognizedTexts: List<RecognizedText>): List<DsnCandidate> {
        return recognizedTexts
            .filter { isValidDsnWithConfidence(it.text, it.confidence) }
            .map { recognizedText ->
                DsnCandidate(
                    dsn = normalizeDsn(recognizedText.text),
                    confidence = recognizedText.confidence,
                    componentType = inferComponentType(recognizedText.text),
                    originalText = recognizedText.text,
                    boundingBox = recognizedText.boundingBox
                )
            }
            .sortedByDescending { it.confidence }
    }
    
    /**
     * Validates if a manually entered text could be a valid DSN
     */
    fun validateManualEntry(text: String): ValidationResult {
        val normalized = normalizeDsn(text)
        
        return when {
            normalized.length < 6 -> ValidationResult(
                isValid = false,
                error = "DSN must be at least 6 characters"
            )
            normalized.isEmpty() -> ValidationResult(
                isValid = false,
                error = "DSN cannot be empty"
            )
            !Regex("^[A-Z0-9\\-_/.]+$").matches(normalized) -> ValidationResult(
                isValid = false,
                error = "DSN contains invalid characters"
            )
            else -> ValidationResult(
                isValid = true,
                normalizedDsn = normalized,
                inferredType = inferComponentType(normalized)
            )
        }
    }
}

/**
 * Represents a potential DSN candidate from OCR
 */
data class DsnCandidate(
    val dsn: String,
    val confidence: Float,
    val componentType: DsnValidator.ComponentType?,
    val originalText: String,
    val boundingBox: android.graphics.Rect?
)

/**
 * Result of manual DSN validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val normalizedDsn: String? = null,
    val inferredType: DsnValidator.ComponentType? = null,
    val error: String? = null
)
