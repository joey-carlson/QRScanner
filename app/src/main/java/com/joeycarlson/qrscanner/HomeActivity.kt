package com.joeycarlson.qrscanner

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.joeycarlson.qrscanner.databinding.ActivityHomeBinding

/**
 * Home screen activity that serves as the main entry point for the application.
 * Provides feature selection between Check Out and Kit Bundle modes.
 */
class HomeActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityHomeBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // Read version from the real APK manifest — never from hardcoded constants.
        // This mirrors SettingsActivity so both screens always show the same version.
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            binding.versionText.text = "Version $versionName (Build $versionCode)"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.versionText.text = "Version unknown"
        }
    }
    
    private fun setupClickListeners() {
        // Check Out button - launches existing MainActivity
        binding.checkoutButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        
        // Check In button - launches CheckInActivity
        binding.checkinButton.setOnClickListener {
            val intent = Intent(this, CheckInActivity::class.java)
            startActivity(intent)
        }
        
        // User Check In button - launches UserCheckInActivity (placeholder)
        binding.userCheckinButton.setOnClickListener {
            val intent = Intent(this, UserCheckInActivity::class.java)
            startActivity(intent)
        }
        
        // Kit Bundle button - launches KitBundleActivity
        binding.kitBundleButton.setOnClickListener {
            val intent = Intent(this, com.joeycarlson.qrscanner.kitbundle.KitBundleActivity::class.java)
            startActivity(intent)
        }
        
        // Inventory Management button - launches InventoryManagementActivity
        binding.inventoryManagementButton.setOnClickListener {
            val intent = Intent(this, com.joeycarlson.qrscanner.inventory.InventoryManagementActivity::class.java)
            startActivity(intent)
        }
        
        // Settings button
        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}
