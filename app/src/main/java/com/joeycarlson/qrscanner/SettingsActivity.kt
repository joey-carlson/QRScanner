package com.joeycarlson.qrscanner

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.joeycarlson.qrscanner.databinding.ActivitySettingsBinding
import com.joeycarlson.qrscanner.export.S3Configuration
import com.joeycarlson.qrscanner.export.S3ExportManager
import com.joeycarlson.qrscanner.export.S3TestResult
import com.joeycarlson.qrscanner.ui.DialogUtils
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var s3Configuration: S3Configuration
    private lateinit var s3ExportManager: S3ExportManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        s3Configuration = S3Configuration(this)
        s3ExportManager = S3ExportManager(this)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        
        // Set up S3 region spinner
        setupS3RegionSpinner()
        
        // Set up S3 field visibility based on enabled state
        setupS3FieldVisibility()
        
        // Load saved settings
        loadSettings()
        
        // Display version information
        displayVersionInfo()
        
        // Set up save button
        binding.saveButton.setOnClickListener {
            saveSettings()
        }
        
        // Set up test S3 connection button
        binding.testS3Button.setOnClickListener {
            testS3Connection()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private fun loadSettings() {
        // Load Location ID
        val locationId = prefs.getString("location_id", "")
        binding.locationIdInput.setText(locationId)
        
        // Load Device Name (optional)
        val deviceName = prefs.getString("device_name", "")
        binding.deviceNameInput.setText(deviceName)
        
        // Load S3 settings
        loadS3Settings()
    }
    
    private fun saveSettings() {
        val locationId = binding.locationIdInput.text.toString().trim()
        
        // Validate Location ID
        if (locationId.isEmpty()) {
            binding.locationIdLayout.error = "Location ID is required"
            return
        }
        
        // Validate format - only alphanumeric and hyphens
        if (!locationId.matches(Regex("^[A-Za-z0-9-]+$"))) {
            binding.locationIdLayout.error = "Only letters, numbers, and hyphens allowed"
            return
        }
        
        // Clear errors
        binding.locationIdLayout.error = null
        
        // Save S3 settings if validation passes
        if (!saveS3Settings()) {
            return
        }
        
        // Save to SharedPreferences
        with(prefs.edit()) {
            putString("location_id", locationId)
            putString("device_name", binding.deviceNameInput.text.toString().trim())
            apply()
        }
        
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun displayVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            
            binding.versionText.text = "Version $versionName (Build $versionCode)"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.versionText.text = "Version information unavailable"
        }
    }
    
    /**
     * Set up the S3 region dropdown with available AWS regions
     */
    private fun setupS3RegionSpinner() {
        val regions = s3Configuration.getAvailableRegions()
        val regionDisplayNames = regions.map { "${it.second} (${it.first})" }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, regionDisplayNames)
        binding.s3RegionSpinner.setAdapter(adapter)
        
        // Set click listener to expand dropdown
        binding.s3RegionSpinner.setOnClickListener {
            binding.s3RegionSpinner.showDropDown()
        }
    }
    
    /**
     * Set up S3 field visibility based on enabled state
     */
    private fun setupS3FieldVisibility() {
        binding.s3EnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateS3FieldVisibility(isChecked)
        }
    }
    
    /**
     * Update visibility of S3 configuration fields
     */
    private fun updateS3FieldVisibility(enabled: Boolean) {
        val visibility = if (enabled) android.view.View.VISIBLE else android.view.View.GONE
        
        binding.s3AccessKeyLayout.visibility = visibility
        binding.s3SecretKeyLayout.visibility = visibility
        binding.s3BucketLayout.visibility = visibility
        binding.s3RegionLayout.visibility = visibility
        binding.s3FolderLayout.visibility = visibility
        binding.testS3Button.visibility = visibility
    }
    
    /**
     * Load S3 settings from SharedPreferences
     */
    private fun loadS3Settings() {
        binding.s3EnabledSwitch.isChecked = prefs.getBoolean("s3_enabled", false)
        
        // Load S3 credentials and configuration
        binding.s3AccessKeyInput.setText(prefs.getString("s3_access_key", ""))
        binding.s3SecretKeyInput.setText(prefs.getString("s3_secret_key", ""))
        binding.s3BucketInput.setText(prefs.getString("s3_bucket_name", ""))
        binding.s3FolderInput.setText(prefs.getString("s3_folder_prefix", "qr-checkouts"))
        
        // Load and set region
        val savedRegion = prefs.getString("s3_region", "us-east-1")
        val regions = s3Configuration.getAvailableRegions()
        val regionEntry = regions.find { it.first == savedRegion }
        if (regionEntry != null) {
            binding.s3RegionSpinner.setText("${regionEntry.second} (${regionEntry.first})", false)
        }
        
        // Update field visibility
        updateS3FieldVisibility(binding.s3EnabledSwitch.isChecked)
    }
    
    /**
     * Save S3 settings to SharedPreferences with validation
     */
    private fun saveS3Settings(): Boolean {
        val isS3Enabled = binding.s3EnabledSwitch.isChecked
        
        // Clear any previous errors
        binding.s3AccessKeyLayout.error = null
        binding.s3SecretKeyLayout.error = null
        binding.s3BucketLayout.error = null
        binding.s3RegionLayout.error = null
        
        if (isS3Enabled) {
            // Validate required S3 fields
            val accessKey = binding.s3AccessKeyInput.text.toString().trim()
            val secretKey = binding.s3SecretKeyInput.text.toString().trim()
            val bucketName = binding.s3BucketInput.text.toString().trim()
            val regionText = binding.s3RegionSpinner.text.toString().trim()
            
            var hasErrors = false
            
            if (accessKey.isEmpty()) {
                binding.s3AccessKeyLayout.error = "Access Key ID is required"
                hasErrors = true
            }
            
            if (secretKey.isEmpty()) {
                binding.s3SecretKeyLayout.error = "Secret Access Key is required"
                hasErrors = true
            }
            
            if (bucketName.isEmpty()) {
                binding.s3BucketLayout.error = "Bucket name is required"
                hasErrors = true
            } else if (!bucketName.matches(Regex("^[a-z0-9][a-z0-9.-]*[a-z0-9]$"))) {
                binding.s3BucketLayout.error = "Invalid bucket name format"
                hasErrors = true
            }
            
            if (regionText.isEmpty()) {
                binding.s3RegionLayout.error = "Please select a region"
                hasErrors = true
            }
            
            if (hasErrors) {
                return false
            }
            
            // Extract region code from display text
            val regionCode = regionText.substringAfterLast("(").substringBeforeLast(")")
            
            // Save S3 settings
            with(prefs.edit()) {
                putBoolean("s3_enabled", true)
                putString("s3_access_key", accessKey)
                putString("s3_secret_key", secretKey)
                putString("s3_bucket_name", bucketName)
                putString("s3_region", regionCode)
                putString("s3_folder_prefix", binding.s3FolderInput.text.toString().trim().ifEmpty { "qr-checkouts" })
                apply()
            }
        } else {
            // Just save the enabled state
            with(prefs.edit()) {
                putBoolean("s3_enabled", false)
                apply()
            }
        }
        
        return true
    }
    
    /**
     * Test S3 connection with current settings
     */
    private fun testS3Connection() {
        // First save the current settings temporarily for testing
        if (!saveS3Settings()) {
            return
        }
        
        if (!binding.s3EnabledSwitch.isChecked) {
            Toast.makeText(this, "Please enable S3 export first", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val progressDialog = DialogUtils.createProgressDialog(
                this@SettingsActivity,
                "Testing S3 Connection",
                "Connecting to AWS S3..."
            )
            progressDialog.show()
            
            try {
                when (val result = s3ExportManager.testS3Connection()) {
                    is S3TestResult.Success -> {
                        progressDialog.dismiss()
                        DialogUtils.showSuccessDialog(
                            this@SettingsActivity,
                            "S3 Connection Test",
                            result.message
                        )
                    }
                    is S3TestResult.Error -> {
                        progressDialog.dismiss()
                        DialogUtils.showErrorDialog(
                            this@SettingsActivity,
                            "S3 Connection Failed",
                            result.message
                        )
                    }
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                DialogUtils.showErrorDialog(
                    this@SettingsActivity,
                    "S3 Connection Failed",
                    "Unexpected error: ${e.message}"
                )
            }
        }
    }
}
