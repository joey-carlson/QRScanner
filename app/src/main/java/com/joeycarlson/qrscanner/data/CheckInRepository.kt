package com.joeycarlson.qrscanner.data

import android.content.Context
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Repository for managing kit check-in records.
 * Extends BaseRepository to leverage common file I/O operations.
 * Saves records to JSON files with "qr_checkins" prefix.
 */
class CheckInRepository(context: Context) : BaseRepository<CheckInRecord>(context) {
    
    override fun getFileNamePrefix(): String = "qr_checkins"
    
    override fun getListType(): java.lang.reflect.Type = 
        object : TypeToken<MutableList<CheckInRecord>>() {}.type
    
    suspend fun saveCheckIn(kitId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val record = CheckInRecord(
                kitId = kitId,
                type = "CHECKIN",
                value = "Kit $kitId checked in"
            )
            val existingRecords = loadRecords()
            existingRecords.add(record)
            
            val jsonContent = gson.toJson(existingRecords)
            return@withContext saveJsonContent(jsonContent)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
    
    suspend fun getAllCheckIns(): List<CheckInRecord> = withContext(Dispatchers.IO) {
        loadRecords()
    }
    
    suspend fun getRecordsForDate(date: LocalDate): List<CheckInRecord> = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileNameForDate(date)
            loadRecords(fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getLastCheckIn(): CheckInRecord? = withContext(Dispatchers.IO) {
        try {
            val records = loadRecords()
            records.maxByOrNull { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun deleteLastCheckIn(): Boolean = withContext(Dispatchers.IO) {
        try {
            val records = loadRecords()
            val lastCheckIn = records.maxByOrNull { it.timestamp }
            
            if (lastCheckIn != null) {
                records.remove(lastCheckIn)
                val jsonContent = gson.toJson(records)
                return@withContext saveJsonContent(jsonContent)
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
