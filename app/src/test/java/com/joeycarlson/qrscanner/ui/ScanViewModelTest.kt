package com.joeycarlson.qrscanner.ui

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.joeycarlson.qrscanner.data.CheckoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*
import kotlinx.coroutines.test.StandardTestDispatcher

/**
 * Unit tests for ScanViewModel following ClineRules testing guidelines.
 * Tests state management, barcode processing, and checkout workflow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class ScanViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var mockApplication: Application
    
    @Mock
    private lateinit var mockRepository: CheckoutRepository
    
    private lateinit var viewModel: ScanViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ScanViewModel(mockApplication, mockRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    // ========== Gist Test ==========
    
    @Test
    fun gistTest_completeCheckoutFlow() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckout(anyString(), anyString())).thenReturn(true)
        
        // Act - Scan user barcode
        viewModel.processBarcode("USER123")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert - Should be in USER_SCANNED state
        assertEquals(ScanState.USER_SCANNED, viewModel.scanState.first())
        
        // Act - Scan kit barcode
        viewModel.processBarcode("KIT456")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert - Should be in REVIEW_PENDING state
        assertEquals(ScanState.REVIEW_PENDING, viewModel.scanState.first())
        assertEquals("USER123", viewModel.reviewUserId.first())
        assertEquals("KIT456", viewModel.reviewKitId.first())
        
        // Act - Confirm review
        viewModel.confirmReview()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert - Repository should have saved checkout
        verify(mockRepository).saveCheckout("USER123", "KIT456")
    }
    
    // ========== Initial State Tests ==========
    
    @Test
    fun initialState_isIdle() = runTest {
        // Assert
        assertEquals(ScanState.IDLE, viewModel.scanState.first())
        assertTrue(viewModel.isScanning.first())
        assertEquals("Ready to scan", viewModel.statusMessage.first())
        assertFalse(viewModel.showUndoButton.first())
    }
    
    // ========== Barcode Processing Tests ==========
    
    @Test
    fun processBarcode_userFirst_transitionsToUserScanned() = runTest {
        // Act
        viewModel.processBarcode("USER123")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        assertEquals(ScanState.USER_SCANNED, viewModel.scanState.first())
        assertTrue(viewModel.statusMessage.first().contains("USER123"))
        assertTrue(viewModel.scanSuccess.first())
    }
    
    @Test
    fun processBarcode_kitFirst_transitionsToKitScanned() = runTest {
        // Act
        viewModel.processBarcode("KIT456")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        assertEquals(ScanState.KIT_SCANNED, viewModel.scanState.first())
        assertTrue(viewModel.statusMessage.first().contains("KIT456"))
        assertTrue(viewModel.scanSuccess.first())
    }
    
    @Test
    fun processBarcode_userThenKit_entersReviewMode() = runTest {
        // Arrange
        viewModel.processBarcode("USER123")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.processBarcode("KIT456")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        assertEquals(ScanState.REVIEW_PENDING, viewModel.scanState.first())
        assertEquals("USER123", viewModel.reviewUserId.first())
        assertEquals("KIT456", viewModel.reviewKitId.first())
        assertFalse(viewModel.isScanning.first())
    }
    
    @Test
    fun processBarcode_kitThenUser_entersReviewMode() = runTest {
        // Arrange
        viewModel.processBarcode("KIT456")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.processBarcode("USER123")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        assertEquals(ScanState.REVIEW_PENDING, viewModel.scanState.first())
        assertEquals("USER123", viewModel.reviewUserId.first())
        assertEquals("KIT456", viewModel.reviewKitId.first())
    }
    
    @Test
    fun processBarcode_invalidBarcode_showsError() = runTest {
        // Act - Empty barcode is invalid
        viewModel.processBarcode("")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        assertTrue(viewModel.scanFailure.first())
        assertEquals("Empty barcode data", viewModel.statusMessage.first())
        assertEquals(ScanState.IDLE, viewModel.scanState.first())
    }
    
    @Test
    fun processBarcode_twoUsers_replacesFirstUser() = runTest {
        // Arrange
        viewModel.processBarcode("USER123")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act - Scan another user
        viewModel.processBarcode("USER789")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert - Should still be in USER_SCANNED state with updated user
        assertEquals(ScanState.USER_SCANNED, viewModel.scanState.first())
        assertTrue(viewModel.statusMessage.first().contains("USER789"))
    }
    
    @Test
    fun processBarcode_twoKits_replacesFirstKit() = runTest {
        // Arrange
        viewModel.processBarcode("KIT456")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act - Scan another kit
        viewModel.processBarcode("KIT789")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert - Should still be in KIT_SCANNED state with updated kit
        assertEquals(ScanState.KIT_SCANNED, viewModel.scanState.first())
        assertTrue(viewModel.statusMessage.first().contains("KIT789"))
    }
    
    @Test
    fun processBarcode_otherType_savesImmediately() = runTest {
        // Arrange
        `when`(mockRepository.saveOtherEntry(anyString())).thenReturn(true)
        
        // Act - Scan OTHER type (doesn't start with U or K)
        viewModel.processBarcode("12345")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        verify(mockRepository).saveOtherEntry("12345")
        assertTrue(viewModel.scanSuccess.first())
    }
    
    // ========== Review Mode Tests ==========
    
    @Test
    fun confirmReview_savesCheckout() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckout("USER123", "KIT456")).thenReturn(true)
        viewModel.processBarcode("USER123")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.processBarcode("KIT456")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.confirmReview()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        verify(mockRepository).saveCheckout("USER123", "KIT456")
    }
    
    @Test
    fun confirmReview_success_resetsToIdle() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckout(anyString(), anyString())).thenReturn(true)
        viewModel.processBarcode("USER123")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.processBarcode("KIT456")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.confirmReview()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert - After delays, should be back to IDLE
        assertEquals(ScanState.IDLE, viewModel.scanState.first())
        assertTrue(viewModel.isScanning.first())
    }
    
    @Test
    fun updateReviewUserId_updatesReviewField() = runTest {
        // Arrange
        viewModel.processBarcode("USER123")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.processBarcode("KIT456")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.updateReviewUserId("USER999")
        
        // Assert
        assertEquals("USER999", viewModel.reviewUserId.first())
    }
    
    @Test
    fun updateReviewKitId_updatesReviewField() = runTest {
        // Arrange
        viewModel.processBarcode("USER123")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.processBarcode("KIT456")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.updateReviewKitId("KIT999")
        
        // Assert
        assertEquals("KIT999", viewModel.reviewKitId.first())
    }
    
    @Test
    fun processBarcode_inReviewMode_rejectsNewScans() = runTest {
        // Arrange - Enter review mode
        viewModel.processBarcode("USER123")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.processBarcode("KIT456")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act - Try to scan another barcode in review mode
        viewModel.processBarcode("USER789")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert - Should reject the scan
        assertTrue(viewModel.scanFailure.first())
        assertEquals(ScanState.REVIEW_PENDING, viewModel.scanState.first())
    }
    
    // ========== Undo Tests ==========
    
    @Test
    fun undoLastCheckout_deletesCheckout() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckout(anyString(), anyString())).thenReturn(true)
        `when`(mockRepository.deleteLastCheckout()).thenReturn(true)
        
        viewModel.processBarcode("USER123")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.processBarcode("KIT456")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Confirm review and advance through the delays (2s confirmation + 1s reset)
        viewModel.confirmReview()
        testDispatcher.scheduler.advanceTimeBy(3100) // Just past the delays
        testDispatcher.scheduler.runCurrent()
        
        // At this point undo button should be visible
        assertTrue("Undo button should be visible", viewModel.showUndoButton.first())
        
        // Act - Undo the checkout
        viewModel.undoLastCheckout()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        verify(mockRepository).deleteLastCheckout()
        assertFalse(viewModel.showUndoButton.first())
    }
    
    // ========== State Management Tests ==========
    
    @Test
    fun clearState_resetsAllState() = runTest {
        // Arrange - Enter review mode
        viewModel.processBarcode("USER123")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.processBarcode("KIT456")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Act
        viewModel.clearState()
        
        // Assert
        assertEquals(ScanState.IDLE, viewModel.scanState.first())
        assertEquals("", viewModel.reviewUserId.first())
        assertEquals("", viewModel.reviewKitId.first())
        assertFalse(viewModel.showUndoButton.first())
    }
    
    @Test
    fun pauseScanning_setsScanningToFalse() = runTest {
        // Act
        viewModel.pauseScanning()
        
        // Assert
        assertFalse(viewModel.isScanning.first())
    }
    
    @Test
    fun resumeScanning_setsScanningToTrue() = runTest {
        // Arrange
        viewModel.pauseScanning()
        
        // Act
        viewModel.resumeScanning()
        
        // Assert
        assertTrue(viewModel.isScanning.first())
    }
    
    // ========== Format Detection Tests ==========
    
    @Test
    fun processBarcode_detectsQRCodeFormat() = runTest {
        // Arrange
        `when`(mockRepository.saveOtherEntry(anyString())).thenReturn(true)
        
        // Act - URL is detected as QR Code (but saved as OTHER since no U/K prefix)
        viewModel.processBarcode("https://example.com")
        
        // Advance time partially to let saveOtherEntry complete but before reset
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert - Should show QR format in status message for OTHER entry
        val message = viewModel.statusMessage.first()
        assertTrue("Message should contain QR format: $message", message.contains("QR"))
        verify(mockRepository).saveOtherEntry("https://example.com")
    }
    
    @Test
    fun processBarcode_detectsUPCAFormat() = runTest {
        // Arrange
        `when`(mockRepository.saveOtherEntry(anyString())).thenReturn(true)
        
        // Act - 12 digits is UPC-A (but saved as OTHER since no U/K prefix)
        viewModel.processBarcode("123456789012")
        
        // Advance time partially to let saveOtherEntry complete but before reset
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert - Should show UPC-A format in status message for OTHER entry
        val message = viewModel.statusMessage.first()
        assertTrue("Message should contain UPC-A format: $message", message.contains("UPC-A"))
        verify(mockRepository).saveOtherEntry("123456789012")
    }
}
