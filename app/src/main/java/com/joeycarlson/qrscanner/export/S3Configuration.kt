package com.joeycarlson.qrscanner.export

import android.content.Context
import androidx.preference.PreferenceManager
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions

/**
 * S3 Configuration management for AWS uploads
 * Handles credentials, bucket settings, and region configuration
 */
class S3Configuration(private val context: Context) {
    
    companion object {
        private const val PREF_S3_ACCESS_KEY = "s3_access_key"
        private const val PREF_S3_SECRET_KEY = "s3_secret_key"
        private const val PREF_S3_BUCKET_NAME = "s3_bucket_name"
        private const val PREF_S3_REGION = "s3_region"
        private const val PREF_S3_FOLDER_PREFIX = "s3_folder_prefix"
        private const val PREF_S3_ENABLED = "s3_enabled"
        
        // Default values
        private const val DEFAULT_REGION = "us-east-1"
        private const val DEFAULT_FOLDER_PREFIX = "qr-checkouts"
    }
    
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    
    /**
     * Check if S3 is properly configured
     */
    fun isConfigured(): Boolean {
        return !getAccessKey().isNullOrEmpty() &&
               !getSecretKey().isNullOrEmpty() &&
               !getBucketName().isNullOrEmpty() &&
               isEnabled()
    }
    
    /**
     * Get AWS credentials
     */
    fun getCredentials(): BasicAWSCredentials? {
        val accessKey = getAccessKey()
        val secretKey = getSecretKey()
        
        return if (!accessKey.isNullOrEmpty() && !secretKey.isNullOrEmpty()) {
            BasicAWSCredentials(accessKey, secretKey)
        } else {
            null
        }
    }
    
    /**
     * Get AWS region
     */
    fun getRegion(): Region {
        val regionString = prefs.getString(PREF_S3_REGION, DEFAULT_REGION) ?: DEFAULT_REGION
        return try {
            Region.getRegion(Regions.fromName(regionString))
        } catch (e: Exception) {
            Region.getRegion(Regions.US_EAST_1)
        }
    }
    
    /**
     * Get S3 bucket name
     */
    fun getBucketName(): String? {
        return prefs.getString(PREF_S3_BUCKET_NAME, null)
    }
    
    /**
     * Get folder prefix for organizing files in S3
     */
    fun getFolderPrefix(): String {
        return prefs.getString(PREF_S3_FOLDER_PREFIX, DEFAULT_FOLDER_PREFIX) ?: DEFAULT_FOLDER_PREFIX
    }
    
    /**
     * Check if S3 export is enabled
     */
    fun isEnabled(): Boolean {
        return prefs.getBoolean(PREF_S3_ENABLED, false)
    }
    
    /**
     * Generate S3 key for file upload
     */
    fun generateS3Key(filename: String, locationId: String): String {
        val folderPrefix = getFolderPrefix()
        return "$folderPrefix/$locationId/$filename"
    }
    
    /**
     * Configuration validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val missingFields: List<String> = emptyList(),
        val errors: List<String> = emptyList()
    )
    
    /**
     * Validate current configuration
     */
    fun validateConfiguration(): ValidationResult {
        val missingFields = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        if (!isEnabled()) {
            errors.add("S3 export is disabled")
        }
        
        if (getAccessKey().isNullOrEmpty()) {
            missingFields.add("Access Key")
        }
        
        if (getSecretKey().isNullOrEmpty()) {
            missingFields.add("Secret Key")
        }
        
        if (getBucketName().isNullOrEmpty()) {
            missingFields.add("Bucket Name")
        }
        
        // Validate region
        try {
            val regionString = prefs.getString(PREF_S3_REGION, DEFAULT_REGION)
            if (!regionString.isNullOrEmpty()) {
                Regions.fromName(regionString)
            }
        } catch (e: Exception) {
            errors.add("Invalid AWS region: ${prefs.getString(PREF_S3_REGION, "")}")
        }
        
        return ValidationResult(
            isValid = missingFields.isEmpty() && errors.isEmpty(),
            missingFields = missingFields,
            errors = errors
        )
    }
    
    // Private getter methods
    private fun getAccessKey(): String? {
        return prefs.getString(PREF_S3_ACCESS_KEY, null)
    }
    
    private fun getSecretKey(): String? {
        return prefs.getString(PREF_S3_SECRET_KEY, null)
    }
    
    /**
     * Get available AWS regions for configuration UI
     */
    fun getAvailableRegions(): List<Pair<String, String>> {
        return listOf(
            "us-east-1" to "US East (N. Virginia)",
            "us-east-2" to "US East (Ohio)",
            "us-west-1" to "US West (N. California)",
            "us-west-2" to "US West (Oregon)",
            "eu-west-1" to "Europe (Ireland)",
            "eu-west-2" to "Europe (London)",
            "eu-central-1" to "Europe (Frankfurt)",
            "ap-northeast-1" to "Asia Pacific (Tokyo)",
            "ap-southeast-1" to "Asia Pacific (Singapore)",
            "ap-southeast-2" to "Asia Pacific (Sydney)"
        )
    }
}
