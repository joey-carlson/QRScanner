package com.joeycarlson.qrscanner.data

import android.content.Context
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class CheckoutRepository(context: Context) : BaseRepository<CheckoutRecord>(context) {
    
    override fun getFileNamePrefix(): String = "qr_checkouts"
    
    override fun getListType(): java.lang.reflect.Type = 
        object : TypeToken<MutableList<CheckoutRecord>>() {}.type
    
    suspend fun saveCheckout(userId: String, kitId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val record = CheckoutRecord(
                userId = userId,
                kitId = kitId,
                type = "CHECKOUT",
                value = "User $userId checked out Kit $kitId"
            )
            val existingRecords = loadRecords()
            existingRecords.add(record)
            
            val jsonContent = gson.toJson(existingRecords)
            saveJsonContent(jsonContent)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    
    suspend fun saveOtherEntry(value: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sanitizedValue = sanitizeInput(value)
            
            val record = CheckoutRecord(
                userId = null,
                kitId = null,
                type = "OTHER",
                value = sanitizedValue
            )
            val existingRecords = loadRecords()
            existingRecords.add(record)
            
            val jsonContent = gson.toJson(existingRecords)
            saveJsonContent(jsonContent)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun getAllCheckouts(): List<CheckoutRecord> = withContext(Dispatchers.IO) {
        loadRecords()
    }
    
    suspend fun getRecordsForDate(date: LocalDate): List<CheckoutRecord> = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileNameForDate(date)
            loadRecords(fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getLastCheckout(): CheckoutRecord? = withContext(Dispatchers.IO) {
        try {
            val records = loadRecords()
            records.filter { it.type == "CHECKOUT" }.maxByOrNull { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun deleteLastCheckout(): Boolean = withContext(Dispatchers.IO) {
        try {
            val records = loadRecords()
            val lastCheckout = records.filter { it.type == "CHECKOUT" }.maxByOrNull { it.timestamp }
            
            if (lastCheckout != null) {
                records.remove(lastCheckout)
                val jsonContent = gson.toJson(records)
                saveJsonContent(jsonContent)
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
