package com.joeycarlson.qrscanner.util

import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.config.PreferenceKeys

/**
 * Legacy Constants object - migrated to AppConfig and PreferenceKeys.
 * This object now provides backward compatibility while we transition.
 * 
 * @deprecated Use AppConfig and PreferenceKeys instead
 */
@Deprecated("Use AppConfig and PreferenceKeys instead", ReplaceWith("AppConfig"))
object Constants {
    
    // Animation durations - migrated to AppConfig
    @Deprecated("Use AppConfig.FLASH_ANIMATION_DURATION", ReplaceWith("AppConfig.FLASH_ANIMATION_DURATION"))
    const val FLASH_ANIMATION_DURATION = AppConfig.FLASH_ANIMATION_DURATION
    
    @Deprecated("Use AppConfig.TEMP_FILE_CLEANUP_DELAY", ReplaceWith("AppConfig.TEMP_FILE_CLEANUP_DELAY"))
    const val TEMP_FILE_CLEANUP_DELAY = AppConfig.TEMP_FILE_CLEANUP_DELAY
    
    @Deprecated("Use AppConfig.UNDO_BUTTON_TIMEOUT", ReplaceWith("AppConfig.UNDO_BUTTON_TIMEOUT"))
    const val UNDO_BUTTON_TIMEOUT = AppConfig.UNDO_BUTTON_TIMEOUT
    
    // MIME types - migrated to AppConfig
    @Deprecated("Use AppConfig.MIME_TYPE_JSON", ReplaceWith("AppConfig.MIME_TYPE_JSON"))
    const val MIME_TYPE_JSON = AppConfig.MIME_TYPE_JSON
    
    @Deprecated("Use AppConfig.MIME_TYPE_CSV", ReplaceWith("AppConfig.MIME_TYPE_CSV"))
    const val MIME_TYPE_CSV = AppConfig.MIME_TYPE_CSV
    
    @Deprecated("Use AppConfig.MIME_TYPE_EMAIL", ReplaceWith("AppConfig.MIME_TYPE_EMAIL"))
    const val MIME_TYPE_EMAIL = AppConfig.MIME_TYPE_EMAIL
    
    @Deprecated("Use AppConfig.MIME_TYPE_ALL", ReplaceWith("AppConfig.MIME_TYPE_ALL"))
    const val MIME_TYPE_ALL = AppConfig.MIME_TYPE_ALL
    
    // File extensions - migrated to AppConfig
    @Deprecated("Use AppConfig.FILE_EXT_JSON", ReplaceWith("AppConfig.FILE_EXT_JSON"))
    const val FILE_EXT_JSON = AppConfig.FILE_EXT_JSON
    
    @Deprecated("Use AppConfig.FILE_EXT_CSV", ReplaceWith("AppConfig.FILE_EXT_CSV"))
    const val FILE_EXT_CSV = AppConfig.FILE_EXT_CSV
    
    // Shared preferences keys - migrated to PreferenceKeys
    @Deprecated("Use PreferenceKeys.LOCATION_ID", ReplaceWith("PreferenceKeys.LOCATION_ID"))
    const val PREF_LOCATION_ID = PreferenceKeys.LOCATION_ID
    
    @Deprecated("Use PreferenceKeys.DEVICE_NAME", ReplaceWith("PreferenceKeys.DEVICE_NAME"))
    const val PREF_DEVICE_NAME = PreferenceKeys.DEVICE_NAME
    
    // Haptic feedback parameters - migrated to AppConfig
    @Deprecated("Use AppConfig.HAPTIC_SUCCESS_DURATION", ReplaceWith("AppConfig.HAPTIC_SUCCESS_DURATION"))
    const val HAPTIC_SUCCESS_DURATION = AppConfig.HAPTIC_SUCCESS_DURATION
    
    @Deprecated("Use AppConfig.HAPTIC_SUCCESS_AMPLITUDE", ReplaceWith("AppConfig.HAPTIC_SUCCESS_AMPLITUDE"))
    const val HAPTIC_SUCCESS_AMPLITUDE = AppConfig.HAPTIC_SUCCESS_AMPLITUDE
    
    @Deprecated("Use AppConfig.HAPTIC_FAILURE_PATTERN", ReplaceWith("AppConfig.HAPTIC_FAILURE_PATTERN"))
    val HAPTIC_FAILURE_PATTERN = AppConfig.HAPTIC_FAILURE_PATTERN
    
    @Deprecated("Use AppConfig.HAPTIC_FAILURE_AMPLITUDES", ReplaceWith("AppConfig.HAPTIC_FAILURE_AMPLITUDES"))
    val HAPTIC_FAILURE_AMPLITUDES = AppConfig.HAPTIC_FAILURE_AMPLITUDES
    
    // CSV format - migrated to AppConfig
    @Deprecated("Use AppConfig.CSV_HEADER", ReplaceWith("AppConfig.CSV_HEADER"))
    const val CSV_HEADER = AppConfig.CSV_HEADER
    
    // Export intents - migrated to AppConfig
    @Deprecated("Use AppConfig.INTENT_EXTRA_START_DATE", ReplaceWith("AppConfig.INTENT_EXTRA_START_DATE"))
    const val INTENT_EXTRA_START_DATE = AppConfig.INTENT_EXTRA_START_DATE
    
    @Deprecated("Use AppConfig.INTENT_EXTRA_END_DATE", ReplaceWith("AppConfig.INTENT_EXTRA_END_DATE"))
    const val INTENT_EXTRA_END_DATE = AppConfig.INTENT_EXTRA_END_DATE
    
    // File provider authority suffix - migrated to AppConfig
    @Deprecated("Use AppConfig.FILE_PROVIDER_AUTHORITY", ReplaceWith("AppConfig.FILE_PROVIDER_AUTHORITY"))
    const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"
    
    // Cache directory names - migrated to AppConfig
    @Deprecated("Use AppConfig.EXPORTS_CACHE_DIR", ReplaceWith("AppConfig.EXPORTS_CACHE_DIR"))
    const val EXPORTS_CACHE_DIR = AppConfig.EXPORTS_CACHE_DIR
    
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
