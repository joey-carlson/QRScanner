package com.joeycarlson.qrscanner.config

/**
 * Centralized SharedPreferences keys configuration.
 * Contains all preference keys used throughout the application.
 */
object PreferenceKeys {
    
    // User Settings
    const val LOCATION_ID = "location_id"
    const val DEVICE_NAME = "device_name"
    
    // S3 Configuration
    const val S3_BUCKET_NAME = "s3_bucket_name"
    const val S3_REGION = "s3_region"
    const val S3_ACCESS_KEY = "s3_access_key"
    const val S3_SECRET_KEY = "s3_secret_key"
    const val S3_ENABLED = "s3_enabled"
    
    // App Preferences
    const val FIRST_LAUNCH = "first_launch"
    const val LAST_EXPORT_DATE = "last_export_date"
    const val HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled"
    const val SOUND_ENABLED = "sound_enabled"
    
    // Debug/Development
    const val DEBUG_MODE = "debug_mode"
    const val LOGGING_ENABLED = "logging_enabled"
    
    // Export Preferences
    const val DEFAULT_EXPORT_FORMAT = "default_export_format"
    const val AUTO_CLEANUP_TEMP_FILES = "auto_cleanup_temp_files"
    const val EXPORT_INCLUDE_LOCATION = "export_include_location"
    
    // UI Preferences
    const val THEME_MODE = "theme_mode"
    const val SHOW_SCAN_OVERLAY = "show_scan_overlay"
    const val ANIMATION_ENABLED = "animation_enabled"
}
