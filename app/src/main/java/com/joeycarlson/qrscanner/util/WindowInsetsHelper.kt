package com.joeycarlson.qrscanner.util

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Helper class to handle system UI insets (status bar, navigation bar, display cutouts)
 * to ensure app content doesn't overlap with system UI elements.
 */
object WindowInsetsHelper {
    
    /**
     * Apply window insets to an activity to handle edge-to-edge display properly.
     * This ensures content is positioned below status bar and above navigation bar.
     */
    fun setupWindowInsets(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11+ use WindowInsetsController
            activity.window.setDecorFitsSystemWindows(false)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6+ use system UI flags
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }
    
    /**
     * Apply padding to a view to respect system UI insets.
     * This is typically used for the root view or specific UI elements that need
     * to avoid overlapping with status bar, navigation bar, or display cutouts.
     * 
     * @param view The view to apply insets to
     * @param applyTop Whether to apply top inset (status bar)
     * @param applyBottom Whether to apply bottom inset (navigation bar)
     * @param applyLeft Whether to apply left inset
     * @param applyRight Whether to apply right inset
     */
    fun applySystemWindowInsetsPadding(
        view: View,
        applyTop: Boolean = true,
        applyBottom: Boolean = true,
        applyLeft: Boolean = true,
        applyRight: Boolean = true
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            v.setPadding(
                if (applyLeft) insets.left else v.paddingLeft,
                if (applyTop) insets.top else v.paddingTop,
                if (applyRight) insets.right else v.paddingRight,
                if (applyBottom) insets.bottom else v.paddingBottom
            )
            
            WindowInsetsCompat.CONSUMED
        }
    }
    
    /**
     * Apply margin to a view to respect system UI insets.
     * This is useful when you want the view to be offset but not have internal padding.
     * 
     * @param view The view to apply insets to
     * @param applyTop Whether to apply top inset (status bar)
     * @param applyBottom Whether to apply bottom inset (navigation bar)
     * @param applyLeft Whether to apply left inset
     * @param applyRight Whether to apply right inset
     */
    fun applySystemWindowInsetsMargin(
        view: View,
        applyTop: Boolean = true,
        applyBottom: Boolean = false,
        applyLeft: Boolean = false,
        applyRight: Boolean = false
    ) {
        val initialMargin = view.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        val initialTopMargin = initialMargin?.topMargin ?: 0
        val initialBottomMargin = initialMargin?.bottomMargin ?: 0
        val initialLeftMargin = initialMargin?.leftMargin ?: 0
        val initialRightMargin = initialMargin?.rightMargin ?: 0
        
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val layoutParams = v.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            
            layoutParams?.let {
                it.topMargin = initialTopMargin + if (applyTop) insets.top else 0
                it.bottomMargin = initialBottomMargin + if (applyBottom) insets.bottom else 0
                it.leftMargin = initialLeftMargin + if (applyLeft) insets.left else 0
                it.rightMargin = initialRightMargin + if (applyRight) insets.right else 0
                v.layoutParams = it
            }
            
            windowInsets
        }
    }
}
