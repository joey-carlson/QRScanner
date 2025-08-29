package com.joeycarlson.qrscanner.config

/**
 * Centralized application configuration object.
 * Contains all app-wide constants, settings, and build-specific configurations.
 */
object AppConfig {
    
    // Application Info
    const val APP_NAME = "PilotScanner"
    const val VERSION_NAME = "1.7.2"
    const val VERSION_CODE = 18
    const val APPLICATION_ID = "com.joeycarlson.qrscanner"
    
    // Build configuration - will be set dynamically when BuildConfig is available
    val DEBUG: Boolean by lazy {
        try {
            Class.forName("com.joeycarlson.qrscanner.BuildConfig")
                .getDeclaredField("DEBUG")
                .getBoolean(null)
        } catch (e: Exception) {
            false // Default to release mode if BuildConfig not available
        }
    }
    
    val BUILD_TYPE: String by lazy {
        try {
            Class.forName("com.joeycarlson.qrscanner.BuildConfig")
                .getDeclaredField("BUILD_TYPE")
                .get(null) as String
        } catch (e: Exception) {
            "release" // Default to release
        }
    }
    
    // File Provider
    const val FILE_PROVIDER_AUTHORITY = "com.joeycarlson.qrscanner.fileprovider"
    
    // Animation Durations (milliseconds)
    const val FLASH_ANIMATION_DURATION = 600L
    const val CONFIRMATION_FADE_DURATION = 300L
    const val TEMP_FILE_CLEANUP_DELAY = 5000L
    const val UNDO_BUTTON_TIMEOUT = 10000L
    const val CHECKOUT_CONFIRMATION_DISPLAY_TIME = 2000L
    const val STATUS_MESSAGE_DELAY = 1000L
    
    // MIME Types
    const val MIME_TYPE_JSON = "application/json"
    const val MIME_TYPE_CSV = "text/csv"
    const val MIME_TYPE_EMAIL = "message/rfc822"
    const val MIME_TYPE_ALL = "*/*"
    
    // File Extensions
    const val FILE_EXT_JSON = "json"
    const val FILE_EXT_CSV = "csv"
    
    // File Naming
    const val EXPORT_FILE_PREFIX = "qr_checkouts"
    const val DATE_FORMAT_FILENAME = "MM-dd-yy"
    const val DATE_FORMAT_DISPLAY = "MMM d, yyyy"
    const val DATE_FORMAT_SHORT = "MMM d"
    const val TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm"
    
    // CSV Format
    const val CSV_HEADER = "User,Kit,Timestamp,Location"
    
    // Cache Directory Names
    const val EXPORTS_CACHE_DIR = "exports"
    const val TEMP_EXPORT_DIR = "exports"
    
    // File Management
    const val MAX_FILENAME_LENGTH = 255
    const val MAX_FILE_VARIANTS = 100
    
    // Haptic Feedback Parameters
    const val HAPTIC_SUCCESS_DURATION = 50L
    const val HAPTIC_SUCCESS_AMPLITUDE = 100
    val HAPTIC_FAILURE_PATTERN = longArrayOf(0, 100, 50, 100)
    val HAPTIC_FAILURE_AMPLITUDES = intArrayOf(0, 180, 0, 180)
    
    // Barcode Types
    const val BARCODE_TYPE_USER = "USER"
    const val BARCODE_TYPE_KIT = "KIT"
    const val BARCODE_TYPE_OTHER = "OTHER"
    
    // Barcode Prefixes
    val USER_PREFIXES = listOf("U", "USER")
    val KIT_PREFIXES = listOf("K", "KIT")
    
    // Intent Extras
    const val INTENT_EXTRA_START_DATE = "start_date"
    const val INTENT_EXTRA_END_DATE = "end_date"
    
    // Build-specific configurations
    object Build {
        val IS_DEBUG: Boolean = DEBUG
        val IS_RELEASE: Boolean = BUILD_TYPE == "release"
        val ENABLE_LOGGING: Boolean = DEBUG
        val ENABLE_CRASH_REPORTING: Boolean = IS_RELEASE
    }
    
    // Export configurations
    object Export {
        const val MAX_FILES_PER_EXPORT = 31 // One month max
        const val MAX_RECORDS_PER_FILE = 10000
        const val TEMP_FILE_PREFIX = "temp_export_"
    }
    
    // UI Configuration
    object UI {
        const val SCAN_OVERLAY_ALPHA = 0.8f
        const val FLASH_OVERLAY_ALPHA = 0.8f
        const val CONFIRMATION_OVERLAY_ALPHA = 1.0f
    }
}
