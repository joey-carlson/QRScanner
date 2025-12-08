package com.joeycarlson.qrscanner.export

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.joeycarlson.qrscanner.R
import com.joeycarlson.qrscanner.databinding.ActivityUnifiedExportBinding
import com.joeycarlson.qrscanner.export.datasource.ExportDataSource
import com.joeycarlson.qrscanner.ui.DialogUtils
import com.joeycarlson.qrscanner.util.LogManager
import com.joeycarlson.qrscanner.util.WindowInsetsHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Unified export activity that provides a consistent export experience across all features.
 * Handles regular data exports, kit bundle exports, kit label exports, and log exports.
 */
class UnifiedExportActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityUnifiedExportBinding
    private lateinit var logManager: LogManager
    private lateinit var exportHandler: UnifiedExportHandler
    
    // Export parameters
    private var exportType: String? = null
    private var exportDisplayName: String? = null
    private var supportsDateRange: Boolean = true
    private var csvContent: String? = null
    private var filename: String? = null
    private var startDate: LocalDate = LocalDate.now()
    private var endDate: LocalDate = LocalDate.now()
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnifiedExportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up window insets to handle system UI overlaps
        WindowInsetsHelper.setupWindowInsets(this)
        WindowInsetsHelper.applySystemWindowInsetsPadding(binding.root)
        
        // Initialize managers
        logManager = LogManager.getInstance(this)
        exportHandler = UnifiedExportHandler(this)
        
        // Get export parameters from intent
        exportType = intent.getStringExtra("export_type")
        exportDisplayName = intent.getStringExtra("export_display_name") ?: when (exportType) {
            "checkout" -> "Kit Checkouts"
            "checkin" -> "Kit Check-ins"
            "kit_bundle" -> "Kit Bundles"
            "inventory" -> "Device Inventory"
            "logs" -> "Diagnostic Logs"
            else -> "Data"
        }
        supportsDateRange = intent.getBooleanExtra("supports_date_range", true)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Export $exportDisplayName"
        
        // Handle date selection based on data source capabilities
        if (supportsDateRange) {
            binding.dateSelectionCard.visibility = View.VISIBLE
            updateDateDisplays()
            setupDatePickers()
        } else {
            binding.dateSelectionCard.visibility = View.GONE
        }
        
        setupExportMethods()
        
        logManager.log("UnifiedExportActivity", "Export activity opened - Type: $exportType")
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private fun setupDatePickers() {
        // Start date picker
        binding.startDateLayout.setOnClickListener {
            showDatePicker(true)
        }
        
        // End date picker
        binding.endDateLayout.setOnClickListener {
            showDatePicker(false)
        }
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val currentDate = if (isStartDate) startDate else endDate
        val year = currentDate.year
        val month = currentDate.monthValue - 1
        val day = currentDate.dayOfMonth
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val newDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                if (isStartDate) {
                    startDate = newDate
                    if (endDate.isBefore(startDate)) {
                        endDate = startDate
                    }
                } else {
                    endDate = newDate
                }
                updateDateDisplays()
            },
            year, month, day
        )
        
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }
    
    private fun updateDateDisplays() {
        binding.startDateText.text = startDate.format(dateFormatter)
        binding.endDateText.text = endDate.format(dateFormatter)
        
        val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
        binding.dateRangeSummary.text = when (days) {
            1L -> "1 day selected"
            else -> "$days days selected"
        }
    }
    
    private fun setupExportMethods() {
        val exportMethods = when (exportType) {
            "kit_labels" -> getKitLabelExportMethods()
            else -> getStandardExportMethods()
        }
        
        val adapter = ExportMethodAdapter(exportMethods) { method ->
            handleExportMethodClick(method)
        }
        
        binding.exportMethodsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.exportMethodsRecyclerView.adapter = adapter
    }
    
    private fun getStandardExportMethods(): List<ExportMethod> {
        return listOf(
            ExportMethod(
                "Save to Downloads",
                "Save files to your device's Downloads folder",
                "📁",
                true
            ),
            ExportMethod(
                "Share",
                "Share files using any installed app",
                "📤",
                true
            ),
            ExportMethod(
                "Email",
                "Send files as email attachments",
                "📧",
                true
            ),
            ExportMethod(
                "SMS/Text",
                "Send file links via text message",
                "💬",
                true
            ),
            ExportMethod(
                "S3 Bucket",
                "Upload files directly to AWS S3",
                "☁️",
                isS3Configured()
            ),
            ExportMethod(
                "Export Logs",
                "Export diagnostic logs for troubleshooting",
                "🔍",
                true
            )
        )
    }
    
    private fun getKitLabelExportMethods(): List<ExportMethod> {
        return listOf(
            ExportMethod(
                "Save to Downloads",
                "Save kit labels CSV to your device's Downloads folder",
                "📁",
                true
            ),
            ExportMethod(
                "Share",
                "Share kit labels CSV using any installed app",
                "📤",
                true
            ),
            ExportMethod(
                "Export Logs",
                "Export diagnostic logs for troubleshooting",
                "🔍",
                true
            )
        )
    }
    
    private fun isS3Configured(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return !prefs.getString("s3_bucket_name", "").isNullOrEmpty() &&
               !prefs.getString("s3_region", "").isNullOrEmpty()
    }
    
    private fun handleExportMethodClick(method: ExportMethod) {
        // Validate date range if applicable
        if (supportsDateRange && startDate.isAfter(endDate)) {
            DialogUtils.showToast(this, "Start date must be before or equal to end date")
            return
        }
        
        // Handle export logs separately
        if (method.name == "Export Logs") {
            exportLogs()
            return
        }
        
        lifecycleScope.launch {
            val progressDialog = DialogUtils.createProgressDialog(
                this@UnifiedExportActivity,
                "Exporting",
                "Preparing your export..."
            )
            progressDialog.show()
            
            try {
                val dataSource = exportHandler.createDataSource(exportType ?: "checkout")
                val format = method.format ?: ExportFormat.JSON
                
                val result = when {
                    method.name.startsWith("Save") -> {
                        exportHandler.exportToDownloads(dataSource, startDate, endDate, format)
                    }
                    method.name.startsWith("Share") -> {
                        exportHandler.exportViaShare(dataSource, startDate, endDate, format)
                    }
                    method.name == "Email" -> {
                        exportHandler.exportViaShare(dataSource, startDate, endDate, format)
                    }
                    method.name == "SMS/Text" -> {
                        exportHandler.exportViaShare(dataSource, startDate, endDate, format)
                    }
                    method.name.startsWith("S3") -> {
                        // Use S3 upload manager directly with progress listener
                        uploadToS3WithProgress(dataSource, startDate, endDate, format, progressDialog)
                    }
                    else -> ExportResult.Error("Unknown export method")
                }
                
                progressDialog.dismiss()
                handleExportResult(result, method)
                
            } catch (e: Exception) {
                progressDialog.dismiss()
                DialogUtils.showErrorDialog(
                    this@UnifiedExportActivity,
                    "Export Failed",
                    "Error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Upload to S3 with progress updates
     */
    private suspend fun uploadToS3WithProgress(
        dataSource: ExportDataSource,
        startDate: LocalDate,
        endDate: LocalDate,
        format: ExportFormat,
        progressDialog: androidx.appcompat.app.AlertDialog
    ): ExportResult {
        val s3UploadManager = S3UploadManager(this)
        
        val progressListener = object : S3UploadManager.UploadProgressListener {
            override fun onUploadStarted(totalFiles: Int) {
                runOnUiThread {
                    progressDialog.setMessage("Uploading ${totalFiles} file(s) to S3...")
                }
            }
            
            override fun onFileUploadStarted(filename: String, fileNumber: Int, totalFiles: Int) {
                runOnUiThread {
                    progressDialog.setMessage("Uploading file $fileNumber of $totalFiles:\n$filename")
                }
            }
            
            override fun onFileUploadProgress(filename: String, bytesUploaded: Long, totalBytes: Long) {
                // Progress updates (simplified since AWS SDK doesn't provide detailed progress)
            }
            
            override fun onFileUploadCompleted(filename: String, s3Key: String) {
                runOnUiThread {
                    logManager.log("UnifiedExportActivity", "Uploaded: $s3Key")
                }
            }
            
            override fun onFileUploadFailed(filename: String, error: String) {
                runOnUiThread {
                    logManager.log("UnifiedExportActivity", "Failed to upload $filename: $error")
                }
            }
            
            override fun onAllUploadsCompleted(uploadedFiles: List<String>) {
                runOnUiThread {
                    progressDialog.setMessage("Upload complete! ${uploadedFiles.size} file(s) uploaded.")
                }
            }
            
            override fun onUploadError(error: String) {
                runOnUiThread {
                    progressDialog.setMessage("Upload error: $error")
                }
            }
        }
        
        return s3UploadManager.uploadToS3(dataSource, startDate, endDate, format, progressListener)
    }
    
    private fun handleExportResult(result: ExportResult, method: ExportMethod) {
        when (result) {
            is ExportResult.Success -> {
                DialogUtils.showSuccessDialog(
                    this,
                    "Export Complete",
                    "Successfully exported ${result.fileUris.size} file(s)"
                ) {
                    finish()
                }
            }
            is ExportResult.ShareReady -> {
                // Create share intent
                val shareIntent = if (result.fileUris.size == 1) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = result.mimeType
                        putExtra(Intent.EXTRA_STREAM, result.fileUris.first())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = result.mimeType
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(result.fileUris))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                
                // Handle specific intents
                when (method.name) {
                    "Email" -> {
                        shareIntent.type = "message/rfc822"
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "QR Scanner Export - $exportDisplayName")
                    }
                    "SMS/Text" -> {
                        shareIntent.action = Intent.ACTION_SENDTO
                        shareIntent.data = android.net.Uri.parse("smsto:")
                    }
                }
                
                val chooser = Intent.createChooser(shareIntent, "Share Export")
                startActivity(chooser)
                
                // Clean up temp files after a delay
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(5000)
                    result.tempFiles.forEach { it.delete() }
                }
                
                finish()
            }
            is ExportResult.S3Success -> {
                DialogUtils.showSuccessDialog(
                    this,
                    "S3 Upload Complete",
                    "Successfully uploaded ${result.uploadedFiles.size} file(s) to S3"
                ) {
                    finish()
                }
            }
            is ExportResult.NoData -> {
                DialogUtils.showWarningDialog(
                    this,
                    "No Data",
                    "No data found for the selected date range"
                ) {
                    // Don't finish, let user adjust dates
                }
            }
            is ExportResult.Error -> {
                DialogUtils.showErrorDialog(
                    this,
                    "Export Failed",
                    result.message
                )
            }
            else -> {
                // Handle any other result types
                finish()
            }
        }
    }
    
    private fun exportLogs() {
        lifecycleScope.launch {
            val progressDialog = DialogUtils.createProgressDialog(
                this@UnifiedExportActivity,
                "Exporting Logs",
                "Preparing diagnostic logs..."
            )
            progressDialog.show()
            
            try {
                val logContent = logManager.exportLogs()
                val tempFileManager = TempFileManager(this@UnifiedExportActivity)
                val logFile = tempFileManager.createTempFile("qrscanner_logs.txt", logContent)
                val uri = tempFileManager.getUriForFile(logFile)
                
                progressDialog.dismiss()
                
                // Create share intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "QR Scanner Diagnostic Logs")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = Intent.createChooser(shareIntent, "Share Logs")
                startActivity(chooser)
                
                // Clean up temp file after a delay
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(5000)
                    tempFileManager.cleanupFiles(listOf(logFile))
                }
                
                finish()
            } catch (e: Exception) {
                progressDialog.dismiss()
                DialogUtils.showErrorDialog(
                    this@UnifiedExportActivity,
                    "Export Failed",
                    "Error exporting logs: ${e.message}"
                )
            }
        }
    }
}
