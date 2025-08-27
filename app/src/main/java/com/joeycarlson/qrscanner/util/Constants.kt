package com.joeycarlson.qrscanner.util

object Constants {
    
    // Animation durations
    const val FLASH_ANIMATION_DURATION = 600L
    const val TEMP_FILE_CLEANUP_DELAY = 5000L
    const val UNDO_BUTTON_TIMEOUT = 10000L
    
    // MIME types
    const val MIME_TYPE_JSON = "application/json"
    const val MIME_TYPE_CSV = "text/csv"
    const val MIME_TYPE_EMAIL = "message/rfc822"
    const val MIME_TYPE_ALL = "*/*"
    
    // File extensions
    const val FILE_EXT_JSON = "json"
    const val FILE_EXT_CSV = "csv"
    
    // Shared preferences keys
    const val PREF_LOCATION_ID = "location_id"
    const val PREF_DEVICE_NAME = "device_name"
    
    // Haptic feedback parameters
    const val HAPTIC_SUCCESS_DURATION = 50L
    const val HAPTIC_SUCCESS_AMPLITUDE = 100
    val HAPTIC_FAILURE_PATTERN = longArrayOf(0, 100, 50, 100)
    val HAPTIC_FAILURE_AMPLITUDES = intArrayOf(0, 180, 0, 180)
    
    // CSV format
    const val CSV_HEADER = "User,Kit,Timestamp,Location"
    
    // Export intents
    const val INTENT_EXTRA_START_DATE = "start_date"
    const val INTENT_EXTRA_END_DATE = "end_date"
    
    // File provider authority suffix
    const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"
    
    // Cache directory names
    const val EXPORTS_CACHE_DIR = "exports"
    
    // Dialog titles (commonly used)
    object DialogTitles {
        const val EXPORT_COMPLETE = "Export Complete"
        const val EXPORT_FAILED = "Export Failed" 
        const val NO_DATA = "No Data"
        const val CONFIGURATION_REQUIRED = "Configuration Required"
        const val SMS_EXPORT_NOTICE = "SMS Export Notice"
    }
    
    // Common messages
    object Messages {
        const val NO_DATA_FOUND = "No checkout records found for the selected date range"
        const val UNEXPECTED_ERROR = "An unexpected error occurred"
        const val LOCATION_ID_REQUIRED = "Please configure Location ID in Settings before exporting."
        const val SMS_WARNING = "File attachments via SMS/MMS may not work with all carriers or messaging apps. The files will be attached, but some recipients may not receive them.\n\nContinue anyway?"
    }
    
    // Progress messages
    object ProgressMessages {
        const val EXPORTING = "Exporting"
        const val SAVING_TO_DOWNLOADS = "Saving files to Downloads..."
        const val SAVING_CSV_TO_DOWNLOADS = "Saving CSV files to Downloads..."
        const val PREPARING_FOR_SHARING = "Preparing files for sharing..."
        const val PREPARING_CSV_FOR_SHARING = "Preparing CSV files for sharing..."
        const val PREPARING_EMAIL = "Preparing files for email..."
        const val PREPARING_SMS = "Preparing files for SMS..."
    }
}
