package com.joeycarlson.qrscanner.ui

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar

/**
 * Centralized UI feedback utilities following ClineRules 02-solid-android
 * Provides single responsibility for all user messaging (toasts, snackbars, dialogs)
 */
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
    
    // ======= TOAST METHODS =======
    
    /**
     * Show short toast message
     */
    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show long toast message
     */
    fun showLongToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * Show success toast with standardized message
     */
    fun showSuccessToast(context: Context, message: String = "Success") {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show error toast with standardized formatting
     */
    fun showErrorToast(context: Context, message: String) {
        Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
    }
    
    // ======= SNACKBAR METHODS =======
    
    /**
     * Show snackbar with optional action
     */
    fun showSnackbar(
        view: View, 
        message: String, 
        actionText: String? = null, 
        action: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }
        snackbar.show()
    }
    
    /**
     * Show error snackbar
     */
    fun showErrorSnackbar(view: View, message: String) {
        Snackbar.make(view, "Error: $message", Snackbar.LENGTH_LONG).show()
    }
    
    /**
     * Show success snackbar
     */
    fun showSuccessSnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
}
