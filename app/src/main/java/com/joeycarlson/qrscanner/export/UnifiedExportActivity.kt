package com.joeycarlson.qrscanner.export

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.joeycarlson.qrscanner.R
import com.joeycarlson.qrscanner.databinding.ActivityUnifiedExportBinding
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
    
    // Export parameters
    private var exportType: String? = null
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
        
        // Initialize log manager
        logManager = LogManager.getInstance(this)
        
        // Get export type from intent
        exportType = intent.getStringExtra("export_type")
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = when (exportType) {
            "kit_bundle" -> "Export Kit Bundles"
            "kit_labels" -> "Export Kit Labels"
            else -> "Export Data"
        }
        
        // Handle different export types
        when (exportType) {
            "kit_labels" -> {
                // Kit labels export - hide date selection
                csvContent = intent.getStringExtra("csv_content")
                filename = intent.getStringExtra("filename")
                binding.dateSelectionCard.visibility = View.GONE
                setupExportMethods()
            }
            "kit_bundle" -> {
                // Kit bundle export - show date selection
                binding.dateSelectionCard.visibility = View.VISIBLE
                updateDateDisplays()
                setupDatePickers()
                setupExportMethods()
            }
            else -> {
                // Regular export - show date selection
                binding.dateSelectionCard.visibility = View.VISIBLE
                updateDateDisplays()
                setupDatePickers()
                setupExportMethods()
            }
        }
        
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
        when (method.name) {
            "Export Logs" -> {
                exportLogs()
            }
            else -> {
                // Pass to the existing export method activity for now
                // In a future update, we can handle all exports directly here
                if (startDate.isAfter(endDate)) {
                    Toast.makeText(this, "Start date must be before or equal to end date", Toast.LENGTH_SHORT).show()
                    return
                }
                
                val intent = Intent(this, ExportMethodActivity::class.java).apply {
                    putExtra("export_type", exportType)
                    putExtra("start_date", startDate.toString())
                    putExtra("end_date", endDate.toString())
                    csvContent?.let { putExtra("csv_content", it) }
                    filename?.let { putExtra("filename", it) }
                }
                startActivity(intent)
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
