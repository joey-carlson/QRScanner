package com.joeycarlson.qrscanner.kitbundle

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.joeycarlson.qrscanner.data.KitRepository

class KitBundleViewModelFactory(
    private val application: Application,
    private val repository: KitRepository
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KitBundleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KitBundleViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
