package com.joeycarlson.qrscanner.export

import android.content.Intent
import android.net.Uri
import java.time.format.DateTimeFormatter

/**
 * Factory for creating sharing intents for various export methods.
 * Handles the creation of properly configured intents for sharing, email, and SMS.
 */
class IntentFactory {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")
    
    /**
     * Creates a share intent for the given URIs
     */
    fun createShareIntent(uris: List<Uri>, mimeType: String = "application/json"): Intent {
        return if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
    
    /**
     * Creates an email intent with properly formatted subject and body
     */
    fun createEmailIntent(emailData: EmailExportData): Intent {
        val subject = buildEmailSubject(emailData)
        val body = buildEmailBody(emailData)
        
        return if (emailData.fileUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, emailData.fileUris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(emailData.fileUris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
    
    /**
     * Creates an SMS/MMS intent with concise message
     */
    fun createSMSIntent(smsData: SMSExportData): Intent {
        val body = buildSMSBody(smsData)
        
        return if (smsData.fileUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra("sms_body", body)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, smsData.fileUris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putExtra("sms_body", body)
                putExtra(Intent.EXTRA_TEXT, body)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(smsData.fileUris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
    
    /**
     * Creates an intent chooser with a custom title
     */
    fun createChooser(intent: Intent, title: String): Intent {
        return Intent.createChooser(intent, title)
    }
    
    /**
     * Builds the email subject line
     */
    private fun buildEmailSubject(emailData: EmailExportData): String {
        return "QR Checkout Export - ${emailData.locationId} " +
                "(${emailData.startDate.format(dateFormatter)} to " +
                "${emailData.endDate.format(dateFormatter)})"
    }
    
    /**
     * Builds the email body with detailed information
     */
    private fun buildEmailBody(emailData: EmailExportData): String {
        return buildString {
            appendLine("QR Checkout Data Export")
            appendLine("=".repeat(24))
            appendLine()
            appendLine("Location: ${emailData.locationId}")
            appendLine("Date Range: ${emailData.startDate.format(dateFormatter)} to ${emailData.endDate.format(dateFormatter)}")
            appendLine("Total Records: ${emailData.totalRecords}")
            appendLine("Files Attached: ${emailData.fileUris.size}")
            appendLine("Format: ${emailData.format.name}")
            appendLine()
            appendLine("The attached ${emailData.format.name} files contain checkout records for the specified date range.")
            appendLine("Each file represents one day of checkout data.")
            appendLine()
            appendLine("File format: qr_checkouts_MM-dd-yy_[LocationID].${emailData.format.extension}")
            
            if (emailData.format == ExportFormat.CSV) {
                appendLine()
                appendLine("CSV Format:")
                appendLine("- Headers: User, Kit, Timestamp, Location")
                appendLine("- Compatible with Excel, Google Sheets, and other spreadsheet applications")
            }
        }
    }
    
    /**
     * Builds a concise SMS body
     */
    private fun buildSMSBody(smsData: SMSExportData): String {
        val dateRange = if (smsData.startDate == smsData.endDate) {
            smsData.startDate.format(shortDateFormatter)
        } else {
            "${smsData.startDate.format(shortDateFormatter)}-${smsData.endDate.format(shortDateFormatter)}"
        }
        
        return "QR Export - ${smsData.locationId}: " +
                "${smsData.totalRecords} records ($dateRange). " +
                "${smsData.fileUris.size} ${smsData.format.name} file(s) attached."
    }
    
    /**
     * Creates a view intent for a single file URI
     */
    fun createViewIntent(uri: Uri, mimeType: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * Creates an intent to open the document provider for saving files
     */
    fun createDocumentIntent(filename: String, mimeType: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, filename)
        }
    }
}
