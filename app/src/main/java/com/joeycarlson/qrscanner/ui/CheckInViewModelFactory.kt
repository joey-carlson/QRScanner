package com.joeycarlson.qrscanner.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.joeycarlson.qrscanner.data.CheckInRepository

/**
 * Factory for creating CheckInViewModel instances with required dependencies.
 */
class CheckInViewModelFactory(
    private val application: Application,
    private val repository: CheckInRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CheckInViewModel::class.java)) {
            return CheckInViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
