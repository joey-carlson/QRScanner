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
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Assert.*

/**
 * Unit tests for CheckoutRepository.
 * Focuses on testable functionality without complex file I/O operations.
 * 
 * Following ClineRules:
 * - AAA pattern (Arrange, Act, Assert)
 * - Test what can be tested reliably
 * - Keep tests simple and maintainable
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class CheckoutRepositoryTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockPrefs: SharedPreferences
    
    private lateinit var repository: CheckoutRepository
    
    /**
     * Test wrapper to expose protected methods for testing
     */
    private class TestableCheckoutRepository(context: Context) : CheckoutRepository(context) {
        fun testGetFileNamePrefix() = getFileNamePrefix()
        fun testGetTodaysFileName() = getTodaysFileName()
        fun testGetFileNameForDate(date: LocalDate) = getFileNameForDate(date)
        fun testSanitizeInput(input: String) = sanitizeInput(input)
    }
    
    private lateinit var testableRepository: TestableCheckoutRepository
    
    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        
        testableRepository = TestableCheckoutRepository(mockContext)
    }
    
    /**
     * Gist Test: Verifies checkout repository configuration
     * Tests file naming and data sanitization
     */
    @Test
    fun gistTest_checkoutRepositoryConfiguration() {
        // Arrange: Setup preferences with location ID
        `when`(mockPrefs.getString(AppConstants.Location.LOCATION_ID_KEY, AppConstants.Location.LOCATION_ID_UNKNOWN))
            .thenReturn("TestLoc")
        
        // Act: Get file prefix
        val prefix = testableRepository.testGetFileNamePrefix()
        
        // Assert: Verify correct prefix
        assertEquals("qr_checkouts", prefix)
        
        // Act: Generate filename
        val fileName = testableRepository.testGetTodaysFileName()
        
        // Assert: Verify filename format
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern(AppConstants.Storage.DATE_FORMAT_PATTERN))
        assertEquals("qr_checkouts_${today}_TestLoc.json", fileName)
    }
    
    @Test
    fun getFileNamePrefix_returnsCorrectPrefix() {
        // Arrange & Act
        val prefix = testableRepository.testGetFileNamePrefix()
        
        // Assert
        assertEquals("qr_checkouts", prefix)
    }
    
    @Test
    fun getTodaysFileName_withLocationId_includesLocation() {
        // Arrange
        `when`(mockPrefs.getString(AppConstants.Location.LOCATION_ID_KEY, AppConstants.Location.LOCATION_ID_UNKNOWN))
            .thenReturn("Site123")
        
        // Act
        val fileName = testableRepository.testGetTodaysFileName()
        
        // Assert
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern(AppConstants.Storage.DATE_FORMAT_PATTERN))
        assertEquals("qr_checkouts_${today}_Site123.json", fileName)
    }
    
    @Test
    fun getTodaysFileName_withoutLocationId_omitsLocation() {
        // Arrange
        `when`(mockPrefs.getString(AppConstants.Location.LOCATION_ID_KEY, AppConstants.Location.LOCATION_ID_UNKNOWN))
            .thenReturn(AppConstants.Location.LOCATION_ID_UNKNOWN)
        
        // Act
        val fileName = testableRepository.testGetTodaysFileName()
        
        // Assert
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern(AppConstants.Storage.DATE_FORMAT_PATTERN))
        assertEquals("qr_checkouts_${today}.json", fileName)
    }
    
    @Test
    fun getFileNameForDate_specificDate_correctFormat() {
        // Arrange
        `when`(mockPrefs.getString(AppConstants.Location.LOCATION_ID_KEY, AppConstants.Location.LOCATION_ID_UNKNOWN))
            .thenReturn("LocA")
        val testDate = LocalDate.of(2024, 12, 25)
        
        // Act
        val fileName = testableRepository.testGetFileNameForDate(testDate)
        
        // Assert
        assertEquals("qr_checkouts_12-25-24_LocA.json", fileName)
    }
    
    @Test
    fun sanitizeInput_removesQuotes() {
        // Arrange & Act
        val result = testableRepository.testSanitizeInput("test\"value'with`quotes")
        
        // Assert
        assertEquals("testvaluewithquotes", result)
    }
    
    @Test
    fun sanitizeInput_removesBrackets() {
        // Arrange & Act
        val result = testableRepository.testSanitizeInput("test[value]{with}brackets")
        
        // Assert
        assertEquals("testvaluewithbrackets", result)
    }
    
    @Test
    fun sanitizeInput_removesAngleBrackets() {
        // Arrange & Act
        val result = testableRepository.testSanitizeInput("<script>alert('xss')</script>")
        
        // Assert
        assertFalse("Should not contain <", result.contains("<"))
        assertFalse("Should not contain >", result.contains(">"))
        assertFalse("Should not contain '", result.contains("'"))
        // Dangerous chars removed: < > ' ` " (parentheses remain as they're not in the danger list)
        assertEquals("scriptalert(xss)/script", result)
    }
    
    @Test
    fun sanitizeInput_normalizesWhitespace() {
        // Arrange & Act
        val result = testableRepository.testSanitizeInput("test  multiple   spaces")
        
        // Assert
        assertEquals("test multiple spaces", result)
    }
    
    @Test
    fun sanitizeInput_enforcesLengthLimit() {
        // Arrange
        val longInput = "a".repeat(300)
        
        // Act
        val result = testableRepository.testSanitizeInput(longInput)
        
        // Assert
        assertEquals(AppConstants.Storage.MAX_INPUT_LENGTH, result.length)
    }
    
    @Test
    fun sanitizeInput_trimsWhitespace() {
        // Arrange & Act
        val result = testableRepository.testSanitizeInput("  test value  ")
        
        // Assert
        assertEquals("test value", result)
    }
}
