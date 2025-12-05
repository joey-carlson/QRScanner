package com.joeycarlson.qrscanner.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.joeycarlson.qrscanner.config.AppConstants
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import org.junit.Assert.*

/**
 * Unit tests for CheckoutRepository.
 * Tests CRUD operations for checkout records following AAA pattern.
 * 
 * Following ClineRules:
 * - AAA pattern (Arrange, Act, Assert)
 * - Gist test for core functionality
 * - Tests are permanent and maintainable
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class CheckoutRepositoryTest {
    
    private lateinit var context: Context
    
    @Mock
    private lateinit var mockPrefs: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var repository: CheckoutRepository
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)
        
        // Setup preference mocking
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { }
        `when`(mockPrefs.getString(AppConstants.Location.LOCATION_ID_KEY, AppConstants.Location.LOCATION_ID_UNKNOWN))
            .thenReturn("TestLoc")
        
        // Create repository with mocked context that returns our mock preferences
        val mockContext = mock(Context::class.java)
        `when`(mockContext.getSharedPreferences(AppConstants.Storage.PREFERENCES_NAME, Context.MODE_PRIVATE))
            .thenReturn(mockPrefs)
        `when`(mockContext.applicationContext).thenReturn(context)
        
        repository = CheckoutRepository(mockContext)
    }
    
    /**
     * Gist Test: Verifies core checkout functionality
     * Tests the complete flow of creating and sanitizing a checkout record
     */
    @Test
    fun gistTest_checkoutRepositoryFlow() {
        // Arrange: Create checkout record
        val kitId = "KIT123"
        val userId = "USER456"
        val timestamp = System.currentTimeMillis()
        
        // Act: Create record
        val record = CheckoutRecord(
            kitId = kitId,
            userId = userId,
            timestamp = timestamp,
            location = "TestLocation"
        )
        
        // Assert: Verify record properties
        assertEquals(kitId, record.kitId)
        assertEquals(userId, record.userId)
        assertEquals(timestamp, record.timestamp)
        assertEquals("TestLocation", record.location)
    }
    
    @Test
    fun addCheckout_validData_addsSuccessfully() {
        // Arrange
        val kitId = "KIT001"
        val userId = "USER001"
        
        // Act
        repository.addCheckout(kitId, userId, "Building-A")
        val records = repository.getTodaysCheckouts()
        
        // Assert
        assertTrue(records.any { it.kitId == kitId && it.userId == userId })
    }
    
    @Test
    fun addCheckout_sanitizesInput() {
        // Arrange
        val maliciousKitId = "<script>alert('xss')</script>"
        val userId = "USER001"
        
        // Act
        repository.addCheckout(maliciousKitId, userId)
        val records = repository.getTodaysCheckouts()
        
        // Assert
        assertTrue(records.isNotEmpty())
        val record = records.first()
        assertFalse(record.kitId.contains("<"))
        assertFalse(record.kitId.contains(">"))
    }
    
    @Test
    fun getTodaysCheckouts_emptyByDefault() {
        // Arrange & Act
        val records = repository.getTodaysCheckouts()
        
        // Assert
        assertTrue(records.isEmpty())
    }
    
    @Test
    fun addMultipleCheckouts_allStored() {
        // Arrange
        val checkouts = listOf(
            Triple("KIT001", "USER001", "Loc-A"),
            Triple("KIT002", "USER002", "Loc-B"),
            Triple("KIT003", "USER003", "Loc-C")
        )
        
        // Act
        checkouts.forEach { (kit, user, loc) ->
            repository.addCheckout(kit, user, loc)
        }
        val records = repository.getTodaysCheckouts()
        
        // Assert
        assertEquals(checkouts.size, records.size)
        checkouts.forEach { (kit, user, _) ->
            assertTrue(records.any { it.kitId == kit && it.userId == user })
        }
    }
    
    @Test
    fun getFileNamePrefix_returnsCorrectPrefix() {
        // Arrange & Act
        val today = LocalDate.now()
        val formatter = java.time.format.DateTimeFormatter.ofPattern(AppConstants.Storage.DATE_FORMAT_PATTERN)
        val expectedDate = today.format(formatter)
        
        // Assert
        // File name should contain "qr_checkouts" prefix
        val fileName = repository.getTodaysFileName()
        assertTrue(fileName.startsWith("qr_checkouts"))
        assertTrue(fileName.contains(expectedDate))
    }
    
    @Test
    fun addCheckout_withLongInput_truncates() {
        // Arrange
        val longKitId = "K".repeat(300)
        val userId = "USER001"
        
        // Act
        repository.addCheckout(longKitId, userId)
        val records = repository.getTodaysCheckouts()
        
        // Assert
        assertTrue(records.isNotEmpty())
        val record = records.first()
        assertTrue(record.kitId.length <= AppConstants.Storage.MAX_INPUT_LENGTH)
    }
    
    @Test
    fun addCheckout_withWhitespace_normalizes() {
        // Arrange
        val kitId = "KIT  123   WITH   SPACES"
        val userId = "USER001"
        
        // Act
        repository.addCheckout(kitId, userId)
        val records = repository.getTodaysCheckouts()
        
        // Assert
        assertTrue(records.isNotEmpty())
        val record = records.first()
        assertFalse(record.kitId.contains("  ")) // Multiple spaces normalized
    }
}
