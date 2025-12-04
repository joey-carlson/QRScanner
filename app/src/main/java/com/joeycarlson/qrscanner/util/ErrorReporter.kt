package com.joeycarlson.qrscanner.util

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    
    /**
     * Initialize the error reporter - call this in Application.onCreate()
     */
    fun init(context: Context) {
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
    
    /**
     * Report a crash with full details
     */
    private fun reportCrash(exception: Throwable, threadName: String) {
        val errorReport = ErrorReport(
            type = "CRASH",
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
    
    /**
     * Report a non-fatal exception
     */
    fun reportException(tag: String, exception: Throwable, context: String = "") {
        if (!isInitialized) return
        
        val errorReport = ErrorReport(
            type = "EXCEPTION",
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
    
    /**
     * Report a custom error or issue
     */
    fun reportIssue(tag: String, message: String, details: Map<String, Any> = emptyMap()) {
        if (!isInitialized) return
        
        val errorReport = ErrorReport(
            type = "ISSUE",
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            message = message,
            tag = tag,
            details = details,
            deviceInfo = getDeviceInfo(),
            appVersion = getAppVersion()
        )
        
        saveErrorReport(errorReport)
    }
    
    /**
     * Report performance issues
     */
    fun reportPerformanceIssue(operation: String, durationMs: Long, threshold: Long = 1000) {
        if (!isInitialized || durationMs < threshold) return
        
        reportIssue(
            tag = "PERFORMANCE",
            message = "Slow operation detected: $operation took ${durationMs}ms",
            details = mapOf(
                "operation" to operation,
                "duration_ms" to durationMs,
                "threshold_ms" to threshold
            )
        )
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
            
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
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
     */
    private fun appendToSummaryLog(errorReport: ErrorReport) {
        try {
            val summaryFile = File(getErrorReportsDirectory(), "error_summary.log")
            val summaryLine = "${errorReport.timestamp} [${errorReport.type}] ${errorReport.exception ?: errorReport.message}\n"
            
            summaryFile.appendText(summaryLine)
            
        } catch (e: Exception) {
            android.util.Log.e("ErrorReporter", "Failed to append to summary log", e)
        }
    }
    
    /**
     * Get error reports directory
     */
    private fun getErrorReportsDirectory(): File {
        return File(context.getExternalFilesDir(null), "qr_error_reports")
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
     */
    fun getAllErrorReports(): List<File> {
        return try {
            val reportsDir = getErrorReportsDirectory()
            if (!reportsDir.exists()) {
                emptyList()
            } else {
                reportsDir.listFiles()?.filter { it.isFile && it.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get summary of recent errors
     */
    fun getErrorSummary(): String {
        return try {
            val summaryFile = File(getErrorReportsDirectory(), "error_summary.log")
            if (summaryFile.exists()) {
                summaryFile.readText()
            } else {
                "No errors reported yet."
            }
        } catch (e: Exception) {
            "Failed to read error summary: ${e.message}"
        }
    }
    
    /**
     * Clear all error reports
     */
    fun clearErrorReports(): Boolean {
        return try {
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
