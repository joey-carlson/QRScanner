package com.joeycarlson.qrscanner.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import com.joeycarlson.qrscanner.util.Constants

class HapticManager(private val context: Context) {
    
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Performs a success haptic feedback - single light tap
     */
    fun performSuccessHaptic() {
        if (!vibrator.hasVibrator()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Modern approach with VibrationEffect
            val effect = VibrationEffect.createOneShot(Constants.HAPTIC_SUCCESS_DURATION, Constants.HAPTIC_SUCCESS_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            // Legacy approach for older devices
            @Suppress("DEPRECATION")
            vibrator.vibrate(Constants.HAPTIC_SUCCESS_DURATION)
        }
    }
    
    /**
     * Performs a failure haptic feedback - double buzz pattern
     */
    fun performFailureHaptic() {
        if (!vibrator.hasVibrator()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Modern approach with pattern: buzz-pause-buzz
            val effect = VibrationEffect.createWaveform(Constants.HAPTIC_FAILURE_PATTERN, Constants.HAPTIC_FAILURE_AMPLITUDES, -1)
            vibrator.vibrate(effect)
        } else {
            // Legacy approach with pattern
            @Suppress("DEPRECATION")
            vibrator.vibrate(Constants.HAPTIC_FAILURE_PATTERN, -1)
        }
    }
    
    /**
     * Cancels any ongoing vibration
     */
    fun cancelHaptic() {
        vibrator.cancel()
    }
}
