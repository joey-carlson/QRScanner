package com.joeycarlson.qrscanner.util

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Centralized logging manager for the QR Scanner app.
 * Provides structured logging with timestamps, categories, and log levels.
 * Supports exporting logs for troubleshooting.
 */
class LogManager private constructor(private val context: Context) {
    
    private val logs = ConcurrentLinkedQueue<LogEntry>()
    private val maxLogs = 5000 // Keep last 5000 log entries
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val logFile: File
    
    init {
        // Create logs directory in app's internal storage
        val logsDir = File(context.filesDir, "logs")
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
        
        // Create or get current log file
        logFile = File(logsDir, "qrscanner_log.txt")
    }
    
    /**
     * Log a message with the specified level
     */
    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            level = level
        )
        
        // Add to memory queue
        logs.offer(entry)
        
        // Trim queue if needed
        while (logs.size > maxLogs) {
            logs.poll()
        }
        
        // Also log to Android Logcat
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARNING -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
        
        // Write to file asynchronously
        writeToFile(entry)
    }
    
    /**
     * Log an error with exception
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\nException: ${throwable.message}\nStacktrace: ${throwable.stackTraceToString()}"
        } else {
            message
        }
        log(tag, fullMessage, LogLevel.ERROR)
    }
    
    /**
     * Log a scan event
     */
    fun logScan(componentType: String, dsn: String, scanMode: String, success: Boolean) {
        val message = "Scan - Type: $componentType, DSN: $dsn, Mode: $scanMode, Success: $success"
        log("ScanEvent", message, LogLevel.INFO)
    }
    
    /**
     * Log an export event
     */
    fun logExport(exportType: String, method: String, fileCount: Int, success: Boolean) {
        val message = "Export - Type: $exportType, Method: $method, Files: $fileCount, Success: $success"
        log("ExportEvent", message, LogLevel.INFO)
    }
    
    /**
     * Log app lifecycle events
     */
    fun logLifecycle(activity: String, event: String) {
        log("Lifecycle", "$activity - $event", LogLevel.DEBUG)
    }
    
    /**
     * Log configuration changes
     */
    fun logConfig(key: String, value: String) {
        log("Config", "Changed: $key = $value", LogLevel.INFO)
    }
    
    /**
     * Export logs to a string for sharing
     */
    suspend fun exportLogs(): String = withContext(Dispatchers.IO) {
        val builder = StringBuilder()
        
        // Add header information
        builder.appendLine("=== QR Scanner Diagnostic Logs ===")
        builder.appendLine("Generated: ${dateFormat.format(Date())}")
        builder.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        builder.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        builder.appendLine("App Version: ${getAppVersion()}")
        builder.appendLine("Total Logs: ${logs.size}")
        builder.appendLine("=====================================\n")
        
        // Add all log entries
        logs.forEach { entry ->
            builder.appendLine(formatLogEntry(entry))
        }
        
        builder.toString()
    }
    
    /**
     * Clear all logs
     */
    fun clearLogs() {
        logs.clear()
        try {
            if (logFile.exists()) {
                logFile.writeText("")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear log file", e)
        }
    }
    
    /**
     * Get logs filtered by level
     */
    fun getLogsByLevel(level: LogLevel): List<LogEntry> {
        return logs.filter { it.level == level }
    }
    
    /**
     * Get logs for a specific tag
     */
    fun getLogsByTag(tag: String): List<LogEntry> {
        return logs.filter { it.tag == tag }
    }
    
    /**
     * Get recent logs (last N entries)
     */
    fun getRecentLogs(count: Int = 100): List<LogEntry> {
        return logs.toList().takeLast(count)
    }
    
    private fun writeToFile(entry: LogEntry) {
        try {
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(formatLogEntry(entry))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    private fun formatLogEntry(entry: LogEntry): String {
        val timestamp = dateFormat.format(Date(entry.timestamp))
        val levelStr = entry.level.name.padEnd(7)
        return "$timestamp $levelStr [${entry.tag}] ${entry.message}"
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "${packageInfo.versionName} ($versionCode)"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    companion object {
        private const val TAG = "LogManager"
        
        @Volatile
        private var INSTANCE: LogManager? = null
        
        /**
         * Get singleton instance of LogManager
         */
        fun getInstance(context: Context): LogManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LogManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Log entry data class
     */
    data class LogEntry(
        val timestamp: Long,
        val tag: String,
        val message: String,
        val level: LogLevel
    )
    
    /**
     * Log levels
     */
    enum class LogLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }
}
