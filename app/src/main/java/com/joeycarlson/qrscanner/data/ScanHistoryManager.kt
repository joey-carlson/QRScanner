package com.joeycarlson.qrscanner.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages scan history persistence using SharedPreferences
 * Singleton pattern ensures single instance across the app
 */
class ScanHistoryManager private constructor(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "scan_history_prefs"
        private const val MAX_HISTORY_SIZE = 50
        
        // Keys for different activity types
        private const val KEY_CHECKOUT_HISTORY = "scan_history_checkout"
        private const val KEY_CHECKIN_HISTORY = "scan_history_checkin"
        private const val KEY_KIT_BUNDLE_HISTORY = "scan_history_kit_bundle"
        private const val KEY_INVENTORY_HISTORY = "scan_history_inventory"
        
        @Volatile
        private var instance: ScanHistoryManager? = null
        
        fun getInstance(context: Context): ScanHistoryManager {
            return instance ?: synchronized(this) {
                instance ?: ScanHistoryManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Get the SharedPreferences key for an activity type
     */
    private fun getKeyForActivityType(activityType: ScanHistoryItem.ActivityType): String {
        return when (activityType) {
            ScanHistoryItem.ActivityType.CHECKOUT -> KEY_CHECKOUT_HISTORY
            ScanHistoryItem.ActivityType.CHECKIN -> KEY_CHECKIN_HISTORY
            ScanHistoryItem.ActivityType.KIT_BUNDLE -> KEY_KIT_BUNDLE_HISTORY
            ScanHistoryItem.ActivityType.INVENTORY -> KEY_INVENTORY_HISTORY
        }
    }
    
    /**
     * Load history for a specific activity type
     * Returns most recent items first (up to MAX_HISTORY_SIZE items)
     */
    fun loadHistory(activityType: ScanHistoryItem.ActivityType): List<ScanHistoryItem> {
        val key = getKeyForActivityType(activityType)
        val json = sharedPreferences.getString(key, null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<ScanHistoryItem>>() {}.type
            val history: List<ScanHistoryItem> = gson.fromJson(json, type)
            // Return most recent first, limited to MAX_HISTORY_SIZE
            history.sortedByDescending { it.timestamp }.take(MAX_HISTORY_SIZE)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Add a new item to history
     * Automatically enforces MAX_HISTORY_SIZE limit
     */
    fun addToHistory(item: ScanHistoryItem) {
        val currentHistory = loadHistory(item.activityType).toMutableList()
        
        // Add new item at the beginning (most recent)
        currentHistory.add(0, item)
        
        // Enforce size limit
        val trimmedHistory = if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.take(MAX_HISTORY_SIZE)
        } else {
            currentHistory
        }
        
        saveHistory(item.activityType, trimmedHistory)
    }
    
    /**
     * Update an existing item in history by ID
     */
    fun updateHistoryItem(activityType: ScanHistoryItem.ActivityType, itemId: String, newValue: String): Boolean {
        val currentHistory = loadHistory(activityType).toMutableList()
        val index = currentHistory.indexOfFirst { it.id == itemId }
        
        if (index != -1) {
            currentHistory[index].value = newValue
            saveHistory(activityType, currentHistory)
            return true
        }
        return false
    }
    
    /**
     * Delete an item from history by ID
     */
    fun deleteHistoryItem(activityType: ScanHistoryItem.ActivityType, itemId: String): Boolean {
        val currentHistory = loadHistory(activityType).toMutableList()
        val removed = currentHistory.removeIf { it.id == itemId }
        
        if (removed) {
            saveHistory(activityType, currentHistory)
            return true
        }
        return false
    }
    
    /**
     * Clear all history for a specific activity type
     */
    fun clearHistory(activityType: ScanHistoryItem.ActivityType) {
        val key = getKeyForActivityType(activityType)
        sharedPreferences.edit().remove(key).apply()
    }
    
    /**
     * Clear all history for all activity types
     */
    fun clearAllHistory() {
        sharedPreferences.edit().clear().apply()
    }
    
    /**
     * Get count of items in history for a specific activity type
     */
    fun getHistoryCount(activityType: ScanHistoryItem.ActivityType): Int {
        return loadHistory(activityType).size
    }
    
    /**
     * Check if history exists for a specific activity type
     */
    fun hasHistory(activityType: ScanHistoryItem.ActivityType): Boolean {
        return getHistoryCount(activityType) > 0
    }
    
    /**
     * Save history list to SharedPreferences
     */
    private fun saveHistory(activityType: ScanHistoryItem.ActivityType, history: List<ScanHistoryItem>) {
        val key = getKeyForActivityType(activityType)
        val json = gson.toJson(history)
        sharedPreferences.edit().putString(key, json).apply()
    }
    
    /**
     * Export history as a list of values only (for including in exports)
     */
    fun exportHistoryValues(activityType: ScanHistoryItem.ActivityType): List<String> {
        return loadHistory(activityType).map { it.value }
    }
}
