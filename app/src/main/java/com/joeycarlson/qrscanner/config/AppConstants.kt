package com.joeycarlson.qrscanner.config

/**
 * Application-wide constants.
 * Centralizes magic strings, numbers, and configuration values for better maintainability.
 * 
 * Following ClineRules:
 * - Single source of truth for constants
 * - Easy to update and test
 * - Prevents magic number/string proliferation
 */
object AppConstants {
    
    // File and Storage Constants
    object Storage {
        const val MAX_INPUT_LENGTH = 200
        const val PREFERENCES_NAME = "com.joeycarlson.qrscanner_preferences"
        const val ERROR_REPORTS_DIR = "qr_error_reports"
        const val DATE_FORMAT_PATTERN = "MM-dd-yy"
        const val TIMESTAMP_FORMAT_PATTERN = "yyyy-MM-dd_HH-mm-ss"
    }
    
    // Export Constants
    object Export {
        const val TYPE_CHECKOUT = "checkout"
        const val TYPE_CHECKIN = "checkin"
        const val TYPE_KIT_BUNDLE = "kit_bundle"
        const val TYPE_INVENTORY = "inventory"
        const val TYPE_LOGS = "logs"
        
        const val METHOD_DOWNLOAD = "download"
        const val METHOD_SHARE = "share"
        const val METHOD_EMAIL = "email"
        const val METHOD_SMS = "sms"
        const val METHOD_S3 = "s3"
    }
    
    // OCR and Scanning Constants
    object Scanning {
        const val MIN_DSN_LENGTH = 8
        const val MAX_DSN_LENGTH = 20
        const val HIGH_CONFIDENCE_THRESHOLD = 0.95
        const val MEDIUM_CONFIDENCE_THRESHOLD = 0.80
        const val LOW_CONFIDENCE_THRESHOLD = 0.60
        
        // Camera frame analysis intervals (milliseconds)
        const val CAMERA_FRAME_INTERVAL_MS = 100L
        const val CAMERA_PREVIEW_WIDTH = 1920
        const val CAMERA_PREVIEW_HEIGHT = 1080
    }
    
    // Validation Patterns
    object Validation {
        const val DSN_PATTERN = """^\d{4}-\d{4,5}$"""
        const val USER_ID_PREFIX = "USER"
        const val KIT_ID_PREFIX = "KIT"
        const val COMPONENT_ID_PREFIX = "COMP-"
        
        // Dangerous characters for sanitization
        const val DANGEROUS_CHARS_REGEX = """["'`\\<>{}\[\];:,]"""
        const val WHITESPACE_NORMALIZE_REGEX = """\s+"""
    }
    
    // Performance Thresholds
    object Performance {
        const val SLOW_OPERATION_THRESHOLD_MS = 1000L
        const val FILE_OPERATION_TIMEOUT_MS = 5000L
        const val NETWORK_TIMEOUT_MS = 30000L
        const val MAX_MEMORY_USAGE_MB = 150
    }
    
    // UI Constants
    object UI {
        const val BUTTON_COLOR_PRIMARY = "#6200EE"
        const val HAPTIC_FEEDBACK_DURATION_MS = 50L
        const val SCAN_FLASH_DURATION_MS = 200L
        const val TOAST_DURATION_SHORT = 2000
        const val TOAST_DURATION_LONG = 3500
    }
    
    // Error Reporting
    object ErrorReporting {
        const val ERROR_TYPE_CRASH = "CRASH"
        const val ERROR_TYPE_EXCEPTION = "EXCEPTION"
        const val ERROR_TYPE_ISSUE = "ISSUE"
        const val ERROR_TYPE_PERFORMANCE = "PERFORMANCE"
        
        const val ERROR_SUMMARY_FILE = "error_summary.log"
    }
    
    // Location Configuration
    object Location {
        const val LOCATION_ID_UNKNOWN = "unknown"
        const val LOCATION_ID_KEY = "location_id"
    }
    
    // S3 Configuration Keys
    object S3Config {
        const val BUCKET_NAME_KEY = "s3_bucket_name"
        const val REGION_KEY = "s3_region"
        const val ACCESS_KEY_ID_KEY = "s3_access_key_id"
        const val SECRET_KEY_KEY = "s3_secret_key"
    }
    
    // Intent Extras
    object IntentExtras {
        const val EXPORT_TYPE = "export_type"
        const val EXPORT_DISPLAY_NAME = "export_display_name"
        const val SUPPORTS_DATE_RANGE = "supports_date_range"
    }
    
    // Component Types
    object ComponentTypes {
        const val LAPTOP = "LAPTOP"
        const val DOCK = "DOCK"
        const val KEYBOARD = "KEYBOARD"
        const val MOUSE = "MOUSE"
        const val TABLET = "TABLET"
        const val CHARGER = "CHARGER"
        const val GLASSES = "GLASSES"
        const val CONTROLLER = "CONTROLLER"
        const val BATTERY = "BATTERY"
        const val PADS = "PADS"
    }
}
