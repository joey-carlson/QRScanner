package com.joeycarlson.qrscanner.ui

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.joeycarlson.qrscanner.data.CheckInRepository
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

/**
 * Unit tests for CheckInViewModel following ClineRules testing guidelines.
 * Tests kit check-in workflow, state management, and undo functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class CheckInViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var mockApplication: Application
    
    @Mock
    private lateinit var mockRepository: CheckInRepository
    
    private lateinit var viewModel: CheckInViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CheckInViewModel(mockApplication, mockRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    // ========== Gist Test ==========
    
    @Test
    fun gistTest_completeCheckInFlow() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(true)
        
        // Act - Scan kit barcode
        viewModel.processBarcode("KIT456")
        
        // Advance time partially to catch state before it resets
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert - Should have saved check-in and show undo button
        verify(mockRepository).saveCheckIn("KIT456")
        assertTrue(viewModel.scanSuccess.first())
        assertTrue(viewModel.showUndoButton.first())
    }
    
    // ========== Initial State Tests ==========
    
    @Test
    fun initialState_isReadyToScan() = runTest {
        // Assert
        assertEquals("Ready to scan kit", viewModel.statusMessage.first())
        assertTrue(viewModel.isScanning.first())
        assertFalse(viewModel.scanSuccess.first())
        assertFalse(viewModel.scanFailure.first())
        assertFalse(viewModel.showUndoButton.first())
        assertFalse(viewModel.showCheckInConfirmation.first())
    }
    
    // ========== Barcode Processing Tests ==========
    
    @Test
    fun processBarcode_validKit_savesCheckIn() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn("KIT123")).thenReturn(true)
        
        // Act
        viewModel.processBarcode("KIT123")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        verify(mockRepository).saveCheckIn("KIT123")
    }
    
    @Test
    fun processBarcode_success_updatesStatusMessage() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(true)
        
        // Act
        viewModel.processBarcode("KIT456")
        
        // Advance time partially to catch the success message
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert
        val message = viewModel.statusMessage.first()
        assertTrue("Status should indicate success", message.contains("KIT456") && message.contains("✓"))
    }
    
    @Test
    fun processBarcode_success_showsUndoButton() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(true)
        
        // Act
        viewModel.processBarcode("KIT789")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert
        assertTrue(viewModel.showUndoButton.first())
    }
    
    @Test
    fun processBarcode_success_showsConfirmationOverlay() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(true)
        
        // Act
        viewModel.processBarcode("KIT111")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert
        assertTrue(viewModel.showCheckInConfirmation.first())
        val confirmMessage = viewModel.checkInConfirmationMessage.first()
        assertTrue(confirmMessage.contains("CHECK-IN COMPLETE"))
        assertTrue(confirmMessage.contains("KIT111"))
    }
    
    @Test
    fun processBarcode_success_hidesConfirmationAfterDelay() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(true)
        
        // Act
        viewModel.processBarcode("KIT222")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Verify confirmation is showing
        assertTrue(viewModel.showCheckInConfirmation.first())
        
        // Advance past the 2 second confirmation delay
        testDispatcher.scheduler.advanceTimeBy(2100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert - Confirmation should be hidden
        assertFalse(viewModel.showCheckInConfirmation.first())
    }
    
    @Test
    fun processBarcode_success_resetsToReadyAfterDelays() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(true)
        
        // Act
        viewModel.processBarcode("KIT333")
        
        // Advance through all delays (2s confirmation + 1s reset)
        testDispatcher.scheduler.advanceTimeBy(3200)
        testDispatcher.scheduler.runCurrent()
        
        // Assert - Should be back to ready state
        assertEquals("Ready to scan kit", viewModel.statusMessage.first())
        assertTrue(viewModel.isScanning.first())
    }
    
    @Test
    fun processBarcode_failure_showsErrorMessage() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(false)
        
        // Act
        viewModel.processBarcode("KIT444")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert
        val message = viewModel.statusMessage.first()
        assertTrue("Status should indicate failure", message.contains("Failed"))
        assertTrue(viewModel.scanFailure.first())
    }
    
    @Test
    fun processBarcode_failure_resetsAfterDelay() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(false)
        
        // Act
        viewModel.processBarcode("KIT555")
        
        // Advance through failure delay (2 seconds)
        testDispatcher.scheduler.advanceTimeBy(2100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert - Should be back to ready state
        assertEquals("Ready to scan kit", viewModel.statusMessage.first())
        assertTrue(viewModel.isScanning.first())
    }
    
    @Test
    fun processBarcode_invalidBarcode_showsError() = runTest {
        // Act - Empty barcode is invalid
        viewModel.processBarcode("")
        
        // Advance time to let validation complete but before state resets
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert
        assertTrue(viewModel.scanFailure.first())
        assertTrue(viewModel.statusMessage.first().contains("Invalid"))
    }
    
    @Test
    fun processBarcode_whenNotScanning_ignoresBarcode() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(true)
        
        // First scan to set isScanning to false
        viewModel.processBarcode("KIT666")
        testDispatcher.scheduler.advanceTimeBy(50) // Just enough to start processing
        testDispatcher.scheduler.runCurrent()
        
        // Verify scanning is paused
        assertFalse(viewModel.isScanning.first())
        
        // Act - Try to scan another barcode while processing
        viewModel.processBarcode("KIT777")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert - Only first barcode should have been processed
        verify(mockRepository, times(1)).saveCheckIn("KIT666")
        verify(mockRepository, never()).saveCheckIn("KIT777")
    }
    
    // ========== Undo Tests ==========
    
    @Test
    fun undoLastCheckIn_deletesCheckIn() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(true)
        `when`(mockRepository.deleteLastCheckIn()).thenReturn(true)
        
        viewModel.processBarcode("KIT888")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Verify undo button is showing
        assertTrue(viewModel.showUndoButton.first())
        
        // Act
        viewModel.undoLastCheckIn()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        verify(mockRepository).deleteLastCheckIn()
    }
    
    @Test
    fun undoLastCheckIn_success_hidesUndoButton() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(true)
        `when`(mockRepository.deleteLastCheckIn()).thenReturn(true)
        
        viewModel.processBarcode("KIT999")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Act
        viewModel.undoLastCheckIn()
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert
        assertFalse(viewModel.showUndoButton.first())
    }
    
    @Test
    fun undoLastCheckIn_success_showsConfirmationMessage() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(true)
        `when`(mockRepository.deleteLastCheckIn()).thenReturn(true)
        
        viewModel.processBarcode("KITABC")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Act
        viewModel.undoLastCheckIn()
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert
        assertTrue(viewModel.statusMessage.first().contains("undone"))
    }
    
    @Test
    fun undoLastCheckIn_failure_showsErrorMessage() = runTest {
        // Arrange
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(true)
        `when`(mockRepository.deleteLastCheckIn()).thenReturn(false)
        
        viewModel.processBarcode("KITDEF")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Act
        viewModel.undoLastCheckIn()
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Assert
        assertTrue(viewModel.statusMessage.first().contains("Failed to undo"))
    }
    
    // ========== State Management Tests ==========
    
    @Test
    fun clearState_resetsToReadyState() = runTest {
        // Arrange - Set some state
        `when`(mockRepository.saveCheckIn(anyString())).thenReturn(true)
        viewModel.processBarcode("KITGHI")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        
        // Act
        viewModel.clearState()
        
        // Assert
        assertEquals("Ready to scan kit", viewModel.statusMessage.first())
        assertTrue(viewModel.isScanning.first())
        assertFalse(viewModel.scanSuccess.first())
        assertFalse(viewModel.scanFailure.first())
    }
    
    @Test
    fun clearState_canBeCalledMultipleTimes() = runTest {
        // Act & Assert - Should not throw exception
        viewModel.clearState()
        viewModel.clearState()
        viewModel.clearState()
        
        // Verify state is still valid
        assertEquals("Ready to scan kit", viewModel.statusMessage.first())
        assertTrue(viewModel.isScanning.first())
    }
}
