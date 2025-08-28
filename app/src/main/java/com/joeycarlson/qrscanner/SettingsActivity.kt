package com.joeycarlson.qrscanner

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.joeycarlson.qrscanner.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: android.content.SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        
        // Load saved settings
        loadSettings()
        
        // Display version information
        displayVersionInfo()
        
        // Set up save button
        binding.saveButton.setOnClickListener {
            saveSettings()
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
        
        // Load Export Preferences
        // TODO: Add more settings as we implement integrations
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
}
