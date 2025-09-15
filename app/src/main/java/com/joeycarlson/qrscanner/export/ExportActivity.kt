package com.joeycarlson.qrscanner.export

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.joeycarlson.qrscanner.databinding.ActivityExportBinding
import com.joeycarlson.qrscanner.util.WindowInsetsHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class ExportActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityExportBinding
    private var startDate: LocalDate = LocalDate.now()
    private var endDate: LocalDate = LocalDate.now()
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up window insets to handle system UI overlaps
        WindowInsetsHelper.setupWindowInsets(this)
        WindowInsetsHelper.applySystemWindowInsetsPadding(binding.root)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Export Data"
        
        // Initialize date displays
        updateDateDisplays()
        
        // Set up click listeners
        setupClickListeners()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private fun setupClickListeners() {
        // Start date picker
        binding.startDateCard.setOnClickListener {
            showDatePicker(true)
        }
        
        // End date picker
        binding.endDateCard.setOnClickListener {
            showDatePicker(false)
        }
        
        // Export button
        binding.exportButton.setOnClickListener {
            if (startDate.isAfter(endDate)) {
                Toast.makeText(this, "Start date must be before or equal to end date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Open export method selection
            val intent = Intent(this, ExportMethodActivity::class.java).apply {
                putExtra("start_date", startDate.toString())
                putExtra("end_date", endDate.toString())
            }
            startActivity(intent)
        }
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val currentDate = if (isStartDate) startDate else endDate
        val year = currentDate.year
        val month = currentDate.monthValue - 1 // DatePicker uses 0-based months
        val day = currentDate.dayOfMonth
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val newDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                if (isStartDate) {
                    startDate = newDate
                    // Ensure end date is not before start date
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
        
        // Set max date to today
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        
        datePickerDialog.show()
    }
    
    private fun updateDateDisplays() {
        binding.startDateText.text = startDate.format(dateFormatter)
        binding.endDateText.text = endDate.format(dateFormatter)
        
        // Update date range summary
        val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
        binding.dateRangeSummary.text = when (days) {
            1L -> "1 day selected"
            else -> "$days days selected"
        }
    }
}
