package com.joeycarlson.qrscanner.util

/**
 * Barcode validation utility with security protections and format-specific validation
 */
object BarcodeValidator {
    
    /**
     * Barcode format types detected from scanned data
     */
    enum class BarcodeFormat {
        QR_CODE,
        CODE_128,
        CODE_39,
        CODE_93,
        UPC_A,
        UPC_E,
        EAN_13,
        EAN_8,
        UNKNOWN
    }
    
    /**
     * Validation result with format information
     */
    data class ValidationResult(
        val isValid: Boolean,
        val format: BarcodeFormat,
        val sanitizedData: String,
        val errorMessage: String? = null
    )
    
    /**
     * Validate barcode data with security checks and format detection
     */
    fun validateBarcodeData(rawData: String): ValidationResult {
        val trimmedData = rawData.trim()
        
        // Basic security validations
        val securityCheck = performSecurityValidation(trimmedData)
        if (!securityCheck.isValid) {
            return ValidationResult(
                isValid = false,
                format = BarcodeFormat.UNKNOWN,
                sanitizedData = "",
                errorMessage = securityCheck.errorMessage
            )
        }
        
        // Detect barcode format and perform format-specific validation
        val format = detectBarcodeFormat(trimmedData)
        val formatValidation = validateFormat(trimmedData, format)
        
        return ValidationResult(
            isValid = formatValidation.isValid,
            format = format,
            sanitizedData = trimmedData,
            errorMessage = formatValidation.errorMessage
        )
    }
    
    /**
     * Security validation to prevent injection attacks
     */
    private fun performSecurityValidation(data: String): ValidationResult {
        // Empty data check
        if (data.isEmpty()) {
            return ValidationResult(false, BarcodeFormat.UNKNOWN, "", "Empty barcode data")
        }
        
        // Length limit to prevent buffer overflow
        if (data.length > 200) {
            return ValidationResult(false, BarcodeFormat.UNKNOWN, "", "Barcode data too long")
        }
        
        // Check for dangerous injection patterns
        val lowerData = data.lowercase()
        val dangerousPatterns = listOf(
            "script", "javascript", "vbscript", "onload", "onerror",
            "alert", "eval", "document", "window", "location",
            "<%", "%>", "<?", "?>", "{{", "}}", "${", "}",
            "drop", "delete", "insert", "update", "select",
            "union", "exec", "execute", "xp_", "sp_"
        )
        
        if (dangerousPatterns.any { lowerData.contains(it) }) {
            return ValidationResult(false, BarcodeFormat.UNKNOWN, "", "Suspicious content detected")
        }
        
        return ValidationResult(true, BarcodeFormat.UNKNOWN, data)
    }
    
    /**
     * Detect barcode format based on content patterns
     */
    private fun detectBarcodeFormat(data: String): BarcodeFormat {
        return when {
            // UPC-A: 12 digits
            data.matches(Regex("^\\d{12}$")) -> BarcodeFormat.UPC_A
            
            // UPC-E: 8 digits starting with 0
            data.matches(Regex("^0\\d{7}$")) -> BarcodeFormat.UPC_E
            
            // EAN-13: 13 digits
            data.matches(Regex("^\\d{13}$")) -> BarcodeFormat.EAN_13
            
            // EAN-8: 8 digits (but not UPC-E pattern)
            data.matches(Regex("^\\d{8}$")) && !data.startsWith("0") -> BarcodeFormat.EAN_8
            
            // Code 39: Alphanumeric with specific symbols
            data.matches(Regex("^[A-Z0-9\\-. $/+%]+$")) -> BarcodeFormat.CODE_39
            
            // Code 93: Similar to Code 39 but more restrictive
            data.matches(Regex("^[A-Z0-9\\-. $/+%]+$")) && data.length <= 47 -> BarcodeFormat.CODE_93
            
            // QR Code: More complex data patterns
            data.contains("http") || data.contains("://") || data.length > 50 -> BarcodeFormat.QR_CODE
            
            // Code 128: Default for other alphanumeric patterns
            data.matches(Regex("^[A-Za-z0-9._-]+$")) -> BarcodeFormat.CODE_128
            
            else -> BarcodeFormat.UNKNOWN
        }
    }
    
    /**
     * Format-specific validation rules
     */
    private fun validateFormat(data: String, format: BarcodeFormat): ValidationResult {
        return when (format) {
            BarcodeFormat.UPC_A -> validateUpcA(data)
            BarcodeFormat.UPC_E -> validateUpcE(data)
            BarcodeFormat.EAN_13 -> validateEan13(data)
            BarcodeFormat.EAN_8 -> validateEan8(data)
            BarcodeFormat.CODE_39 -> validateCode39(data)
            BarcodeFormat.CODE_93 -> validateCode93(data)
            BarcodeFormat.CODE_128 -> validateCode128(data)
            BarcodeFormat.QR_CODE -> validateQrCode(data)
            BarcodeFormat.UNKNOWN -> ValidationResult(false, format, data, "Unknown barcode format")
        }
    }
    
    private fun validateUpcA(data: String): ValidationResult {
        if (!data.matches(Regex("^\\d{12}$"))) {
            return ValidationResult(false, BarcodeFormat.UPC_A, data, "UPC-A must be 12 digits")
        }
        return ValidationResult(true, BarcodeFormat.UPC_A, data)
    }
    
    private fun validateUpcE(data: String): ValidationResult {
        if (!data.matches(Regex("^0\\d{7}$"))) {
            return ValidationResult(false, BarcodeFormat.UPC_E, data, "UPC-E must be 8 digits starting with 0")
        }
        return ValidationResult(true, BarcodeFormat.UPC_E, data)
    }
    
    private fun validateEan13(data: String): ValidationResult {
        if (!data.matches(Regex("^\\d{13}$"))) {
            return ValidationResult(false, BarcodeFormat.EAN_13, data, "EAN-13 must be 13 digits")
        }
        return ValidationResult(true, BarcodeFormat.EAN_13, data)
    }
    
    private fun validateEan8(data: String): ValidationResult {
        if (!data.matches(Regex("^\\d{8}$"))) {
            return ValidationResult(false, BarcodeFormat.EAN_8, data, "EAN-8 must be 8 digits")
        }
        return ValidationResult(true, BarcodeFormat.EAN_8, data)
    }
    
    private fun validateCode39(data: String): ValidationResult {
        if (!data.matches(Regex("^[A-Z0-9\\-. $/+%]+$"))) {
            return ValidationResult(false, BarcodeFormat.CODE_39, data, "Code 39 contains invalid characters")
        }
        return ValidationResult(true, BarcodeFormat.CODE_39, data)
    }
    
    private fun validateCode93(data: String): ValidationResult {
        if (!data.matches(Regex("^[A-Z0-9\\-. $/+%]+$"))) {
            return ValidationResult(false, BarcodeFormat.CODE_93, data, "Code 93 contains invalid characters")
        }
        if (data.length > 47) {
            return ValidationResult(false, BarcodeFormat.CODE_93, data, "Code 93 too long")
        }
        return ValidationResult(true, BarcodeFormat.CODE_93, data)
    }
    
    private fun validateCode128(data: String): ValidationResult {
        if (!data.matches(Regex("^[A-Za-z0-9._-]+$"))) {
            return ValidationResult(false, BarcodeFormat.CODE_128, data, "Code 128 contains invalid characters")
        }
        return ValidationResult(true, BarcodeFormat.CODE_128, data)
    }
    
    private fun validateQrCode(data: String): ValidationResult {
        // QR codes have more flexible content rules but still need basic security
        if (!data.matches(Regex("^[A-Za-z0-9._\\-:/\\?#\\[\\]@!$&'()*+,;= ]+$"))) {
            return ValidationResult(false, BarcodeFormat.QR_CODE, data, "QR code contains potentially unsafe characters")
        }
        return ValidationResult(true, BarcodeFormat.QR_CODE, data)
    }
}
