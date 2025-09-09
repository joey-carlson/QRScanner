package com.joeycarlson.qrscanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.joeycarlson.qrscanner.databinding.ActivityUserCheckinBinding

/**
 * Placeholder activity for the User Check In feature.
 * This feature is planned for future implementation.
 */
class UserCheckInActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityUserCheckinBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserCheckinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up the action bar with back button
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.user_check_in_mode_title)
        }
        
        // Set up the back button click listener
        binding.backButton.setOnClickListener {
            finish()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
