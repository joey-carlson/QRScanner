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
import com.joeycarlson.qrscanner.config.AppConfig
import kotlinx.coroutines.launch
import java.time.LocalDate

class ExportMethodActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityExportMethodBinding
    private lateinit var startDate: LocalDate
    private lateinit var endDate: LocalDate
    private lateinit var exportCoordinator: ExportCoordinator
    private lateinit var intentFactory: IntentFactory
    private lateinit var tempFileManager: TempFileManager
    private lateinit var s3ExportManager: S3ExportManager
    
    // Kit labels export specific
    private var exportType: String? = null
    private var csvContent: String? = null
    private var filename: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportMethodBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if this is kit labels export
        exportType = intent.getStringExtra("export_type")
        
        if (exportType == "kit_labels") {
            // Kit labels export mode
            csvContent = intent.getStringExtra("csv_content")
            filename = intent.getStringExtra("filename")
            // Use dummy dates for compatibility
            startDate = LocalDate.now()
            endDate = LocalDate.now()
        } else {
            // Regular export mode - get dates from intent
            startDate = LocalDate.parse(intent.getStringExtra("start_date"))
            endDate = LocalDate.parse(intent.getStringExtra("end_date"))
        }
        
        // Initialize export components
        exportCoordinator = ExportCoordinator(this)
        intentFactory = IntentFactory()
        tempFileManager = TempFileManager(this)
        s3ExportManager = S3ExportManager(this)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (exportType == "kit_labels") "Export Kit Labels" else "Export Method"
        
        // Set up export methods list
        setupExportMethods()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private fun setupExportMethods() {
        val exportMethods = if (exportType == "kit_labels") {
            // Limited export options for kit labels (CSV for label printing)
            listOf(
                ExportMethod(
                    "Save to Downloads",
                    "Save kit labels CSV to your device's Downloads folder",
                    "📁",
                    true
                ),
                ExportMethod(
                    "Share via Android",
                    "Share kit labels CSV using any installed app",
                    "📤",
                    true
                )
            )
        } else {
            // Full export options for regular exports
            listOf(
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
                    "Upload JSON files directly to AWS S3",
                    "☁️",
                    true
                ),
                ExportMethod(
                    "S3 CSV",
                    "Upload CSV files directly to AWS S3",
                    "☁️",
                    true
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
        }
        
        val adapter = ExportMethodAdapter(exportMethods) { method ->
            handleExportMethodClick(method)
        }
        
        binding.exportMethodsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.exportMethodsRecyclerView.adapter = adapter
    }
    
    private fun handleExportMethodClick(method: ExportMethod) {
        if (exportType == "kit_labels") {
            // Handle kit labels export
            when (method.name) {
                "Save to Downloads" -> {
                    saveKitLabelsToDownloads()
                }
                "Share via Android" -> {
                    shareKitLabelsViaAndroid()
                }
            }
        } else {
            // Handle regular exports
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
                "S3 Bucket" -> {
                    exportToS3()
                }
                "S3 CSV" -> {
                    exportCsvToS3()
                }
                else -> {
                    Toast.makeText(this, "${method.name} - Coming in future update", Toast.LENGTH_SHORT).show()
                }
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
                        AppConfig.DialogTitles.EXPORT_COMPLETE,
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
                is ExportResult.S3Success -> {
                    progressDialog.dismiss()
                    onSuccess(result)
                }
                is ExportResult.SlackSuccess -> {
                    progressDialog.dismiss()
                    onSuccess(result)
                }
                is ExportResult.NoData -> {
                    progressDialog.dismiss()
                    DialogUtils.showNoDataDialog(this)
                }
                is ExportResult.Error -> {
                    progressDialog.dismiss()
                    DialogUtils.showErrorDialog(this, AppConfig.DialogTitles.EXPORT_FAILED, result.message)
                }
                is ExportResult.Cancelled -> {
                    progressDialog.dismiss()
                    finish()
                }
            }
        } catch (e: Exception) {
            progressDialog.dismiss()
            DialogUtils.showErrorDialog(
                this,
                AppConfig.DialogTitles.EXPORT_FAILED,
                "${AppConfig.Messages.UNEXPECTED_ERROR}: ${e.message}"
            )
        }
    }

    private fun exportToDownloads() {
        lifecycleScope.launch {
            executeExportOperation(
                AppConfig.ProgressMessages.EXPORTING,
                AppConfig.ProgressMessages.SAVING_TO_DOWNLOADS,
                { exportCoordinator.exportToDownloads(startDate, endDate, ExportFormat.JSON) }
            ) { result ->
                if (result is ExportResult.Success) {
                    val fileCount = result.fileUris.size
                    DialogUtils.showSuccessDialog(
                        this@ExportMethodActivity,
                        AppConfig.DialogTitles.EXPORT_COMPLETE,
                        "Successfully exported $fileCount file(s) to Downloads folder"
                    ) { finish() }
                }
            }
        }
    }
    
    private fun shareViaAndroid() {
        lifecycleScope.launch {
            executeExportOperation(
                "Preparing",
                AppConfig.ProgressMessages.PREPARING_FOR_SHARING,
                { exportCoordinator.exportViaShare(startDate, endDate, ExportFormat.JSON) }
            ) { result ->
                if (result is ExportResult.ShareReady) {
                    // Create and launch share intent
                    val shareIntent = intentFactory.createShareIntent(result.fileUris, result.mimeType)
                    val chooser = Intent.createChooser(shareIntent, "Share QR Checkout Data")
                    startActivity(chooser)
                    
                    // Clean up temp files after a delay
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(AppConfig.TEMP_FILE_CLEANUP_DELAY)
                        tempFileManager.cleanupFiles(result.tempFiles)
                    }
                    
                    finish()
                }
            }
        }
    }
    
    private fun exportViaEmail() {
        lifecycleScope.launch {
            executeExportOperation(
                "Preparing Email",
                AppConfig.ProgressMessages.PREPARING_EMAIL,
                { exportCoordinator.exportViaEmail(startDate, endDate, ExportFormat.JSON) }
            ) { result ->
                if (result is ExportResult.EmailReady) {
                    // Create and launch email intent
                    val emailIntent = intentFactory.createEmailIntent(result.emailData)
                    val chooser = Intent.createChooser(emailIntent, "Send Email")
                    startActivity(chooser)
                    
                    // Clean up temp files after a delay
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(AppConfig.TEMP_FILE_CLEANUP_DELAY)
                        tempFileManager.cleanupFiles(result.emailData.tempFiles)
                    }
                    
                    finish()
                }
            }
        }
    }
    
    private fun exportViaSMS() {
        lifecycleScope.launch {
            DialogUtils.showWarningDialog(
                this@ExportMethodActivity,
                AppConfig.DialogTitles.SMS_EXPORT_NOTICE,
                AppConfig.Messages.SMS_WARNING
            ) {
                performSMSExport()
            }
        }
    }
    
    private fun performSMSExport() {
        lifecycleScope.launch {
            executeExportOperation(
                "Preparing SMS",
                AppConfig.ProgressMessages.PREPARING_SMS,
                { exportCoordinator.exportViaSMS(startDate, endDate, ExportFormat.JSON) }
            ) { result ->
                if (result is ExportResult.SMSReady) {
                    // Create and launch SMS intent
                    val smsIntent = intentFactory.createSMSIntent(result.smsData)
                    val chooser = Intent.createChooser(smsIntent, "Send via SMS/MMS")
                    startActivity(chooser)
                    
                    // Clean up temp files after a delay
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(AppConfig.TEMP_FILE_CLEANUP_DELAY)
                        tempFileManager.cleanupFiles(result.smsData.tempFiles)
                    }
                    
                    finish()
                }
            }
        }
    }
    
    private fun exportCsvToDownloads() {
        lifecycleScope.launch {
            executeExportOperation(
                AppConfig.ProgressMessages.EXPORTING,
                AppConfig.ProgressMessages.SAVING_CSV_TO_DOWNLOADS,
                { exportCoordinator.exportToDownloads(startDate, endDate, ExportFormat.CSV) }
            ) { result ->
                if (result is ExportResult.Success) {
                    val fileCount = result.fileUris.size
                    DialogUtils.showSuccessDialog(
                        this@ExportMethodActivity,
                        AppConfig.DialogTitles.EXPORT_COMPLETE,
                        "Successfully exported $fileCount CSV file(s) to Downloads folder"
                    ) { finish() }
                }
            }
        }
    }
    
    private fun shareCsvViaAndroid() {
        lifecycleScope.launch {
            executeExportOperation(
                "Preparing CSV",
                AppConfig.ProgressMessages.PREPARING_CSV_FOR_SHARING,
                { exportCoordinator.exportViaShare(startDate, endDate, ExportFormat.CSV) }
            ) { result ->
                if (result is ExportResult.ShareReady) {
                    // Create and launch share intent
                    val shareIntent = intentFactory.createShareIntent(result.fileUris, result.mimeType)
                    val chooser = Intent.createChooser(shareIntent, "Share CSV Data")
                    startActivity(chooser)
                    
                    // Clean up temp files after a delay
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(AppConfig.TEMP_FILE_CLEANUP_DELAY)
                        tempFileManager.cleanupFiles(result.tempFiles)
                    }
                    
                    finish()
                }
            }
        }
    }
    
    private fun exportToS3() {
        lifecycleScope.launch {
            executeExportOperation(
                "Uploading to S3",
                "Uploading JSON files to AWS S3...",
                { s3ExportManager.exportToS3(startDate, endDate) }
            ) { result ->
                if (result is ExportResult.S3Success) {
                    val fileCount = result.uploadedFiles.size
                    val message = buildString {
                        appendLine("Successfully uploaded $fileCount file(s) to S3:")
                        appendLine("Bucket: ${result.bucketName}")
                        appendLine("Region: ${result.region}")
                        appendLine()
                        result.uploadedFiles.forEach { file ->
                            appendLine("• $file")
                        }
                    }
                    DialogUtils.showSuccessDialog(
                        this@ExportMethodActivity,
                        "S3 Upload Complete",
                        message
                    ) { finish() }
                }
            }
        }
    }
    
    private fun exportCsvToS3() {
        lifecycleScope.launch {
            executeExportOperation(
                "Uploading CSV to S3",
                "Uploading CSV files to AWS S3...",
                { s3ExportManager.exportCsvToS3(startDate, endDate) }
            ) { result ->
                if (result is ExportResult.S3Success) {
                    val fileCount = result.uploadedFiles.size
                    val message = buildString {
                        appendLine("Successfully uploaded $fileCount CSV file(s) to S3:")
                        appendLine("Bucket: ${result.bucketName}")
                        appendLine("Region: ${result.region}")
                        appendLine()
                        result.uploadedFiles.forEach { file ->
                            appendLine("• $file")
                        }
                    }
                    DialogUtils.showSuccessDialog(
                        this@ExportMethodActivity,
                        "S3 CSV Upload Complete",
                        message
                    ) { finish() }
                }
            }
        }
    }
    
    // Kit labels export methods
    private fun saveKitLabelsToDownloads() {
        lifecycleScope.launch {
            val progressDialog = DialogUtils.createProgressDialog(
                this@ExportMethodActivity,
                "Saving Kit Labels",
                "Saving kit labels CSV to Downloads..."
            )
            progressDialog.show()
            
            try {
                val fileManager = com.joeycarlson.qrscanner.util.FileManager(this@ExportMethodActivity)
                when (val result = fileManager.saveToDownloads(
                    filename ?: "kit_labels.csv",
                    csvContent ?: "",
                    "text/csv"
                )) {
                    is com.joeycarlson.qrscanner.util.FileManager.FileResult.Success -> {
                        progressDialog.dismiss()
                        DialogUtils.showSuccessDialog(
                            this@ExportMethodActivity,
                            "Export Complete",
                            "Kit labels CSV saved to Downloads folder"
                        ) { finish() }
                    }
                    is com.joeycarlson.qrscanner.util.FileManager.FileResult.Error -> {
                        progressDialog.dismiss()
                        DialogUtils.showErrorDialog(
                            this@ExportMethodActivity,
                            "Export Failed",
                            "Failed to save kit labels CSV to Downloads: ${result.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                DialogUtils.showErrorDialog(
                    this@ExportMethodActivity,
                    "Export Failed",
                    "Error: ${e.message}"
                )
            }
        }
    }
    
    private fun shareKitLabelsViaAndroid() {
        lifecycleScope.launch {
            val progressDialog = DialogUtils.createProgressDialog(
                this@ExportMethodActivity,
                "Preparing Share",
                "Preparing kit labels CSV for sharing..."
            )
            progressDialog.show()
            
            try {
                // Create temp file for sharing
                val tempFile = tempFileManager.createTempFile(
                    filename ?: "kit_labels.csv",
                    csvContent ?: ""
                )
                
                val uri = tempFileManager.getUriForFile(tempFile)
                
                progressDialog.dismiss()
                
                // Create share intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Kit Labels CSV")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = Intent.createChooser(shareIntent, "Share Kit Labels CSV")
                startActivity(chooser)
                
                // Clean up temp file after a delay
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(AppConfig.TEMP_FILE_CLEANUP_DELAY)
                    tempFileManager.cleanupFiles(listOf(tempFile))
                }
                
                finish()
            } catch (e: Exception) {
                progressDialog.dismiss()
                DialogUtils.showErrorDialog(
                    this@ExportMethodActivity,
                    "Share Failed",
                    "Error: ${e.message}"
                )
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
