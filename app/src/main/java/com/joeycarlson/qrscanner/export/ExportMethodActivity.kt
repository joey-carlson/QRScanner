package com.joeycarlson.qrscanner.export

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.joeycarlson.qrscanner.databinding.ActivityExportMethodBinding
import com.joeycarlson.qrscanner.ui.DialogUtils
import com.joeycarlson.qrscanner.util.Constants
import kotlinx.coroutines.launch
import java.time.LocalDate

class ExportMethodActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityExportMethodBinding
    private lateinit var startDate: LocalDate
    private lateinit var endDate: LocalDate
    private lateinit var exportManager: ExportManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportMethodBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get dates from intent
        startDate = LocalDate.parse(intent.getStringExtra("start_date"))
        endDate = LocalDate.parse(intent.getStringExtra("end_date"))
        
        // Initialize export manager
        exportManager = ExportManager(this)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Export Method"
        
        // Set up export methods list
        setupExportMethods()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private fun setupExportMethods() {
        val exportMethods = listOf(
            ExportMethod(
                "Save to Downloads",
                "Save JSON files to your device's Downloads folder",
                "📁",
                true
            ),
            ExportMethod(
                "Save as CSV",
                "Save CSV files to Downloads (spreadsheet compatible)",
                "📊",
                true
            ),
            ExportMethod(
                "Share via Android",
                "Share JSON files using any installed app",
                "📤",
                true
            ),
            ExportMethod(
                "Share CSV",
                "Share CSV files (spreadsheet compatible)",
                "📈",
                true
            ),
            ExportMethod(
                "Email",
                "Send JSON files as email attachments",
                "📧",
                true
            ),
            ExportMethod(
                "SMS/Text",
                "Send JSON file links via text message",
                "💬",
                true
            ),
            ExportMethod(
                "Slack",
                "Upload to Slack channel or DM",
                "🔗",
                false
            ),
            ExportMethod(
                "S3 Bucket",
                "Upload directly to AWS S3",
                "☁️",
                false
            ),
            ExportMethod(
                "Google Drive",
                "Save to Google Drive",
                "📄",
                false
            ),
            ExportMethod(
                "Dropbox",
                "Save to Dropbox",
                "📦",
                false
            )
        )
        
        val adapter = ExportMethodAdapter(exportMethods) { method ->
            handleExportMethodClick(method)
        }
        
        binding.exportMethodsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.exportMethodsRecyclerView.adapter = adapter
    }
    
    private fun handleExportMethodClick(method: ExportMethod) {
        when (method.name) {
            "Save to Downloads" -> {
                exportToDownloads()
            }
            "Save as CSV" -> {
                exportCsvToDownloads()
            }
            "Share via Android" -> {
                shareViaAndroid()
            }
            "Share CSV" -> {
                shareCsvViaAndroid()
            }
            "Email" -> {
                exportViaEmail()
            }
            "SMS/Text" -> {
                exportViaSMS()
            }
            else -> {
                Toast.makeText(this, "${method.name} - Coming in future update", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Helper method to handle common export operations with progress dialog and error handling
     */
    private suspend fun executeExportOperation(
        progressTitle: String,
        progressMessage: String,
        operation: suspend () -> ExportResult,
        onSuccess: (ExportResult) -> Unit = { result ->
            when (result) {
                is ExportResult.Success -> {
                    val fileCount = result.fileUris.size
                    DialogUtils.showSuccessDialog(
                        this@ExportMethodActivity,
                        Constants.DialogTitles.EXPORT_COMPLETE,
                        "Successfully exported $fileCount file(s)"
                    ) { finish() }
                }
                else -> finish()
            }
        }
    ) {
        val progressDialog = DialogUtils.createProgressDialog(this, progressTitle, progressMessage)
        progressDialog.show()
        
        try {
            when (val result = operation()) {
                is ExportResult.Success -> {
                    progressDialog.dismiss()
                    onSuccess(result)
                }
                is ExportResult.ShareReady -> {
                    progressDialog.dismiss()
                    onSuccess(result)
                }
                is ExportResult.EmailReady -> {
                    progressDialog.dismiss()
                    onSuccess(result)
                }
                is ExportResult.SMSReady -> {
                    progressDialog.dismiss()
                    onSuccess(result)
                }
                is ExportResult.NoData -> {
                    progressDialog.dismiss()
                    DialogUtils.showNoDataDialog(this)
                }
                is ExportResult.Error -> {
                    progressDialog.dismiss()
                    DialogUtils.showErrorDialog(this, Constants.DialogTitles.EXPORT_FAILED, result.message)
                }
            }
        } catch (e: Exception) {
            progressDialog.dismiss()
            DialogUtils.showErrorDialog(
                this,
                Constants.DialogTitles.EXPORT_FAILED,
                "${Constants.Messages.UNEXPECTED_ERROR}: ${e.message}"
            )
        }
    }

    private fun exportToDownloads() {
        lifecycleScope.launch {
            executeExportOperation(
                Constants.ProgressMessages.EXPORTING,
                Constants.ProgressMessages.SAVING_TO_DOWNLOADS,
                { exportManager.exportToDownloads(startDate, endDate) }
            ) { result ->
                if (result is ExportResult.Success) {
                    val fileCount = result.fileUris.size
                    DialogUtils.showSuccessDialog(
                        this@ExportMethodActivity,
                        Constants.DialogTitles.EXPORT_COMPLETE,
                        "Successfully exported $fileCount file(s) to Downloads folder"
                    ) { finish() }
                }
            }
        }
    }
    
    private fun shareViaAndroid() {
        lifecycleScope.launch {
            // Show progress dialog
            val progressDialog = AlertDialog.Builder(this@ExportMethodActivity)
                .setTitle("Preparing")
                .setMessage("Preparing files for sharing...")
                .setCancelable(false)
                .create()
            progressDialog.show()
            
            try {
                when (val result = exportManager.exportViaShare(startDate, endDate)) {
                    is ExportResult.ShareReady -> {
                        progressDialog.dismiss()
                        
                        // Create and launch share intent
                        val shareIntent = exportManager.createShareIntent(result.fileUris)
                        val chooser = Intent.createChooser(shareIntent, "Share QR Checkout Data")
                        startActivity(chooser)
                        
                        // Clean up temp files after a delay
                        lifecycleScope.launch {
                            kotlinx.coroutines.delay(5000) // Wait 5 seconds
                            exportManager.cleanupTempFiles(result.tempFiles)
                        }
                        
                        finish()
                    }
                    is ExportResult.NoData -> {
                        progressDialog.dismiss()
                        AlertDialog.Builder(this@ExportMethodActivity)
                            .setTitle("No Data")
                            .setMessage("No checkout records found for the selected date range")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    is ExportResult.Error -> {
                        progressDialog.dismiss()
                        AlertDialog.Builder(this@ExportMethodActivity)
                            .setTitle("Export Failed")
                            .setMessage(result.message)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    else -> {
                        progressDialog.dismiss()
                    }
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                AlertDialog.Builder(this@ExportMethodActivity)
                    .setTitle("Export Failed")
                    .setMessage("An unexpected error occurred: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun exportViaEmail() {
        lifecycleScope.launch {
            // Show progress dialog
            val progressDialog = AlertDialog.Builder(this@ExportMethodActivity)
                .setTitle("Preparing Email")
                .setMessage("Preparing files for email...")
                .setCancelable(false)
                .create()
            progressDialog.show()
            
            try {
                when (val result = exportManager.exportViaEmail(startDate, endDate)) {
                    is ExportResult.EmailReady -> {
                        progressDialog.dismiss()
                        
                        // Create and launch email intent
                        val emailIntent = exportManager.createEmailIntent(result.emailData)
                        val chooser = Intent.createChooser(emailIntent, "Send Email")
                        startActivity(chooser)
                        
                        // Clean up temp files after a delay
                        lifecycleScope.launch {
                            kotlinx.coroutines.delay(5000) // Wait 5 seconds
                            exportManager.cleanupTempFiles(result.emailData.tempFiles)
                        }
                        
                        finish()
                    }
                    is ExportResult.NoData -> {
                        progressDialog.dismiss()
                        AlertDialog.Builder(this@ExportMethodActivity)
                            .setTitle("No Data")
                            .setMessage("No checkout records found for the selected date range")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    is ExportResult.Error -> {
                        progressDialog.dismiss()
                        AlertDialog.Builder(this@ExportMethodActivity)
                            .setTitle("Export Failed")
                            .setMessage(result.message)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    else -> {
                        progressDialog.dismiss()
                    }
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                AlertDialog.Builder(this@ExportMethodActivity)
                    .setTitle("Export Failed")
                    .setMessage("An unexpected error occurred: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun exportViaSMS() {
        lifecycleScope.launch {
            DialogUtils.showWarningDialog(
                this@ExportMethodActivity,
                Constants.DialogTitles.SMS_EXPORT_NOTICE,
                Constants.Messages.SMS_WARNING
            ) {
                performSMSExport()
            }
        }
    }
    
    private fun performSMSExport() {
        lifecycleScope.launch {
            executeExportOperation(
                "Preparing SMS",
                Constants.ProgressMessages.PREPARING_SMS,
                { exportManager.exportViaSMS(startDate, endDate) }
            ) { result ->
                if (result is ExportResult.SMSReady) {
                    // Create and launch SMS intent
                    val smsIntent = exportManager.createSMSIntent(result.smsData)
                    val chooser = Intent.createChooser(smsIntent, "Send via SMS/MMS")
                    startActivity(chooser)
                    
                    // Clean up temp files after a delay
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(Constants.TEMP_FILE_CLEANUP_DELAY)
                        exportManager.cleanupTempFiles(result.smsData.tempFiles)
                    }
                    
                    finish()
                }
            }
        }
    }
    
    private fun exportCsvToDownloads() {
        lifecycleScope.launch {
            executeExportOperation(
                Constants.ProgressMessages.EXPORTING,
                Constants.ProgressMessages.SAVING_CSV_TO_DOWNLOADS,
                { exportManager.exportCsvToDownloads(startDate, endDate) }
            ) { result ->
                if (result is ExportResult.Success) {
                    val fileCount = result.fileUris.size
                    DialogUtils.showSuccessDialog(
                        this@ExportMethodActivity,
                        Constants.DialogTitles.EXPORT_COMPLETE,
                        "Successfully exported $fileCount CSV file(s) to Downloads folder"
                    ) { finish() }
                }
            }
        }
    }
    
    private fun shareCsvViaAndroid() {
        lifecycleScope.launch {
            // Show progress dialog
            val progressDialog = AlertDialog.Builder(this@ExportMethodActivity)
                .setTitle("Preparing CSV")
                .setMessage("Preparing CSV files for sharing...")
                .setCancelable(false)
                .create()
            progressDialog.show()
            
            try {
                when (val result = exportManager.exportCsvViaShare(startDate, endDate)) {
                    is ExportResult.ShareReady -> {
                        progressDialog.dismiss()
                        
                        // Create share intent with CSV mime type
                        val shareIntent = if (result.fileUris.size == 1) {
                            Intent(Intent.ACTION_SEND).apply {
                                type = result.mimeType
                                putExtra(Intent.EXTRA_STREAM, result.fileUris[0])
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        } else {
                            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = result.mimeType
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(result.fileUris))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        }
                        
                        val chooser = Intent.createChooser(shareIntent, "Share CSV Data")
                        startActivity(chooser)
                        
                        // Clean up temp files after a delay
                        lifecycleScope.launch {
                            kotlinx.coroutines.delay(5000) // Wait 5 seconds
                            exportManager.cleanupTempFiles(result.tempFiles)
                        }
                        
                        finish()
                    }
                    is ExportResult.NoData -> {
                        progressDialog.dismiss()
                        AlertDialog.Builder(this@ExportMethodActivity)
                            .setTitle("No Data")
                            .setMessage("No checkout records found for the selected date range")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    is ExportResult.Error -> {
                        progressDialog.dismiss()
                        AlertDialog.Builder(this@ExportMethodActivity)
                            .setTitle("Export Failed")
                            .setMessage(result.message)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    else -> {
                        progressDialog.dismiss()
                    }
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                AlertDialog.Builder(this@ExportMethodActivity)
                    .setTitle("Export Failed")
                    .setMessage("An unexpected error occurred: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
}

data class ExportMethod(
    val name: String,
    val description: String,
    val icon: String,
    val isAvailable: Boolean
)
