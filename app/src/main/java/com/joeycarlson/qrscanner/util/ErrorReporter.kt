package com.joeycarlson.qrscanner.util

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.joeycarlson.qrscanner.config.AppConstants
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Automatic error detection and reporting system.
 * Captures crashes, exceptions, and runtime issues for easy debugging.
 * 
 * Usage: 
 * - Call ErrorReporter.init(context) in Application onCreate
 * - Errors are automatically captured and saved to Downloads/qr_error_reports/
 * - Share error files with developers for automatic fixing
 */
object ErrorReporter {
    
    private lateinit var context: Context
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var isInitialized = false
    private val lock = ReentrantReadWriteLock()
    
    /**
     * Initialize the error reporter - call this in Application.onCreate()
     * Thread-safe initialization.
     */
    fun init(context: Context) {
        lock.write {
            if (isInitialized) return
            
            this.context = context.applicationContext
            isInitialized = true
        
            // Set up uncaught exception handler
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
                reportCrash(exception, thread.name)
                defaultHandler?.uncaughtException(thread, exception)
            }
        }
    }
    
    /**
     * Report a crash with full details
     * Thread-safe crash reporting.
     */
    private fun reportCrash(exception: Throwable, threadName: String) {
        lock.read {
            if (!isInitialized) return
            
            val errorReport = ErrorReport(
                type = AppConstants.ErrorReporting.ERROR_TYPE_CRASH,
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            exception = exception.javaClass.simpleName,
            message = exception.message ?: "No message",
            stackTrace = exception.stackTraceToString(),
            threadName = threadName,
            deviceInfo = getDeviceInfo(),
            appVersion = getAppVersion()
            )
            
            saveErrorReport(errorReport)
        }
    }
    
    /**
     * Report a non-fatal exception
     * Thread-safe exception reporting.
     */
    fun reportException(tag: String, exception: Throwable, context: String = "") {
        lock.read {
            if (!isInitialized) return
            
            val errorReport = ErrorReport(
                type = AppConstants.ErrorReporting.ERROR_TYPE_EXCEPTION,
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            exception = exception.javaClass.simpleName,
            message = exception.message ?: "No message",
            stackTrace = exception.stackTraceToString(),
            threadName = Thread.currentThread().name,
            tag = tag,
            context = context,
            deviceInfo = getDeviceInfo(),
            appVersion = getAppVersion()
            )
            
            saveErrorReport(errorReport)
        }
    }
    
    /**
     * Report a custom error or issue
     * Thread-safe issue reporting.
     */
    fun reportIssue(tag: String, message: String, details: Map<String, Any> = emptyMap()) {
        lock.read {
            if (!isInitialized) return
            
            val errorReport = ErrorReport(
                type = AppConstants.ErrorReporting.ERROR_TYPE_ISSUE,
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            message = message,
            tag = tag,
            details = details,
            deviceInfo = getDeviceInfo(),
            appVersion = getAppVersion()
            )
            
            saveErrorReport(errorReport)
        }
    }
    
    /**
     * Report performance issues
     * Thread-safe performance reporting.
     */
    fun reportPerformanceIssue(
        operation: String, 
        durationMs: Long, 
        threshold: Long = AppConstants.Performance.SLOW_OPERATION_THRESHOLD_MS
    ) {
        lock.read {
            if (!isInitialized || durationMs < threshold) return
            
            reportIssue(
                tag = AppConstants.ErrorReporting.ERROR_TYPE_PERFORMANCE,
            message = "Slow operation detected: $operation took ${durationMs}ms",
            details = mapOf(
                "operation" to operation,
                "duration_ms" to durationMs,
                "threshold_ms" to threshold
                )
            )
        }
    }
    
    /**
     * Save error report to file
     */
    private fun saveErrorReport(errorReport: ErrorReport) {
        try {
            val reportsDir = getErrorReportsDirectory()
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(AppConstants.Storage.TIMESTAMP_FORMAT_PATTERN))
            val fileName = "error_${errorReport.type.lowercase()}_$timestamp.json"
            val file = File(reportsDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.write(gson.toJson(errorReport))
            }
            
            // Also append to summary log
            appendToSummaryLog(errorReport)
            
        } catch (e: Exception) {
            // Fallback logging - don't let error reporting crash the app
            android.util.Log.e("ErrorReporter", "Failed to save error report", e)
        }
    }
    
    /**
     * Append error to summary log for quick overview
     * Thread-safe log appending.
     */
    private fun appendToSummaryLog(errorReport: ErrorReport) {
        try {
            val summaryFile = File(getErrorReportsDirectory(), AppConstants.ErrorReporting.ERROR_SUMMARY_FILE)
            val summaryLine = "${errorReport.timestamp} [${errorReport.type}] ${errorReport.exception ?: errorReport.message}\n"
            
            summaryFile.appendText(summaryLine)
            
        } catch (e: Exception) {
            android.util.Log.e("ErrorReporter", "Failed to append to summary log", e)
        }
    }
    
    /**
     * Get error reports directory - uses multiple locations for easy access
     * Thread-safe directory access.
     */
    private fun getErrorReportsDirectory(): File {
        lock.read {
            if (!isInitialized) {
                throw IllegalStateException("ErrorReporter not initialized")
            }
            // For Android Studio emulator, save to external files (easily accessible)
            // For real devices, save to app-specific external directory
            return File(context.getExternalFilesDir(null), AppConstants.Storage.ERROR_REPORTS_DIR)
        }
    }
    
    /**
     * Get all error report file paths for easy copy/paste access
     * Thread-safe path retrieval.
     */
    fun getErrorReportPaths(): List<String> {
        return lock.read {
            if (!isInitialized) return listOf("Error: Not initialized")
            
            try {
            val reportsDir = getErrorReportsDirectory()
            val paths = mutableListOf<String>()
            
            // Add directory path
            paths.add("Error Reports Directory: ${reportsDir.absolutePath}")
            
            // Add individual file paths
            if (reportsDir.exists()) {
                reportsDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        paths.add("File: ${file.absolutePath}")
                    }
                }
            }
            
                paths
            } catch (e: Exception) {
                listOf("Error getting paths: ${e.message}")
            }
        }
    }
    
    /**
     * Get device information for debugging
     */
    private fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "android_version" to Build.VERSION.RELEASE,
            "api_level" to Build.VERSION.SDK_INT.toString(),
            "device_model" to Build.MODEL,
            "device_manufacturer" to Build.MANUFACTURER,
            "device_brand" to Build.BRAND
        )
    }
    
    /**
     * Get app version info
     */
    private fun getAppVersion(): Map<String, String> {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            mapOf(
                "version_name" to (packageInfo.versionName ?: "unknown"),
                "version_code" to packageInfo.longVersionCode.toString()
            )
        } catch (e: Exception) {
            mapOf("version_name" to "unknown", "version_code" to "unknown")
        }
    }
    
    /**
     * Get all error report files for sharing
     * Thread-safe file retrieval.
     */
    fun getAllErrorReports(): List<File> {
        return lock.read {
            if (!isInitialized) return emptyList()
            
            try {
            val reportsDir = getErrorReportsDirectory()
            if (!reportsDir.exists()) {
                emptyList()
            } else {
                reportsDir.listFiles()?.filter { 
                    it.isFile && it.extension == "json" 
                }?.sortedByDescending { it.lastModified() } ?: emptyList()
            }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Get summary of recent errors
     * Thread-safe summary retrieval.
     */
    fun getErrorSummary(): String {
        return lock.read {
            if (!isInitialized) return "Error: Not initialized"
            
            try {
                val summaryFile = File(getErrorReportsDirectory(), AppConstants.ErrorReporting.ERROR_SUMMARY_FILE)
            if (summaryFile.exists()) {
                summaryFile.readText()
                } else {
                    "No errors reported yet."
                }
            } catch (e: Exception) {
                "Failed to read error summary: ${e.message}"
            }
        }
    }
    
    /**
     * Clear all error reports
     * Thread-safe clearing operation.
     */
    fun clearErrorReports(): Boolean {
        return lock.write {
            if (!isInitialized) return false
            
            try {
            val reportsDir = getErrorReportsDirectory()
            if (reportsDir.exists()) {
                reportsDir.deleteRecursively()
                true
                } else {
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Data class for error reports
     */
    private data class ErrorReport(
        val type: String, // CRASH, EXCEPTION, ISSUE, PERFORMANCE
        val timestamp: String,
        val exception: String? = null,
        val message: String? = null,
        val stackTrace: String? = null,
        val threadName: String? = null,
        val tag: String? = null,
        val context: String? = null,
        val details: Map<String, Any>? = null,
        val deviceInfo: Map<String, String>? = null,
        val appVersion: Map<String, String>? = null
    )
}
