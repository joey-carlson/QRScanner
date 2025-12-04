package com.joeycarlson.qrscanner

import android.app.Application
import com.joeycarlson.qrscanner.util.ErrorReporter

/**
 * Application class for QRScanner app.
 * Initializes error reporting and other global components.
 */
class QRScannerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize error reporting system
        ErrorReporter.init(this)
        
        // Log app startup
        ErrorReporter.reportIssue(
            tag = "APP_LIFECYCLE",
            message = "App started successfully",
            details = mapOf("startup_time" to System.currentTimeMillis())
        )
    }
}
