package com.joeycarlson.qrscanner.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog

object DialogUtils {
    
    /**
     * Creates a progress dialog with title and message
     */
    fun createProgressDialog(context: Context, title: String, message: String): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .create()
    }
    
    /**
     * Shows a success dialog with completion callback
     */
    fun showSuccessDialog(
        context: Context, 
        title: String, 
        message: String, 
        onComplete: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                onComplete?.invoke()
            }
            .show()
    }
    
    /**
     * Shows an error dialog with message
     */
    fun showErrorDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    /**
     * Shows a no data dialog
     */
    fun showNoDataDialog(context: Context) {
        showErrorDialog(
            context,
            "No Data",
            "No checkout records found for the selected date range"
        )
    }
    
    /**
     * Shows a warning dialog with continue/cancel options
     */
    fun showWarningDialog(
        context: Context,
        title: String,
        message: String,
        onContinue: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Continue") { _, _ ->
                onContinue()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
