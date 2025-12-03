package com.joeycarlson.qrscanner.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.google.gson.reflect.TypeToken
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for BaseRepository following ClineRules testing guidelines.
 * Uses AAA pattern (Arrange, Act, Assert) for clear, maintainable tests.
 * 
 * Tests use a concrete implementation of BaseRepository for testing abstract functionality.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q]) // Test with Android 10 (Q)
class BaseRepositoryTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockPrefs: SharedPreferences
    
    private lateinit var testRepository: TestRepository
    
    /**
     * Test implementation of BaseRepository for testing purposes
     */
    private class TestRepository(context: Context) : BaseRepository<TestData>(context) {
        override fun getFileNamePrefix(): String = "test_data"
        override fun getListType(): java.lang.reflect.Type = 
            object : TypeToken<MutableList<TestData>>() {}.type
        
        // Expose protected methods for testing
        fun testGetTodaysFileName() = getTodaysFileName()
        fun testGetFileNameForDate(date: LocalDate) = getFileNameForDate(date)
        fun testSanitizeInput(input: String) = sanitizeInput(input)
    }
    
    /**
     * Test data class for repository testing
     */
    private data class TestData(
        val id: String,
        val value: String
    )
    
    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
    }
    
    /**
     * Gist Test: Verify core BaseRepository functionality
     * Tests filename generation and sanitization in a single comprehensive test
     */
    @Test
    fun gistTest_baseRepositoryCoreFlow() {
        // Arrange: Setup preferences with location ID
        `when`(mockPrefs.getString("location_id", "unknown")).thenReturn("TestLoc")
        testRepository = TestRepository(mockContext)
        
        // Act: Generate filename
        val fileName = testRepository.testGetTodaysFileName()
        
        // Assert: Verify filename format includes date and location
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd-yy"))
        assertEquals("test_data_${today}_TestLoc.json", fileName)
        
        // Act: Sanitize input
        val sanitized = testRepository.testSanitizeInput("<script>alert('test')</script>")
        
        // Assert: Verify dangerous characters removed
        assertFalse(sanitized.contains("<"))
        assertFalse(sanitized.contains(">"))
    }
    
    @Test
    fun getTodaysFileName_withLocationId_includesLocation() {
        // Arrange
        `when`(mockPrefs.getString("location_id", "unknown")).thenReturn("Site123")
        testRepository = TestRepository(mockContext)
        
        // Act
        val fileName = testRepository.testGetTodaysFileName()
        
        // Assert
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd-yy"))
        assertEquals("test_data_${today}_Site123.json", fileName)
    }
    
    @Test
    fun getTodaysFileName_withoutLocationId_omitsLocation() {
        // Arrange
        `when`(mockPrefs.getString("location_id", "unknown")).thenReturn("unknown")
        testRepository = TestRepository(mockContext)
        
        // Act
        val fileName = testRepository.testGetTodaysFileName()
        
        // Assert
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd-yy"))
        assertEquals("test_data_${today}.json", fileName)
    }
    
    @Test
    fun getFileNameForDate_specificDate_correctFormat() {
        // Arrange
        `when`(mockPrefs.getString("location_id", "unknown")).thenReturn("LocA")
        testRepository = TestRepository(mockContext)
        val testDate = LocalDate.of(2024, 12, 25)
        
        // Act
        val fileName = testRepository.testGetFileNameForDate(testDate)
        
        // Assert
        assertEquals("test_data_12-25-24_LocA.json", fileName)
    }
    
    @Test
    fun sanitizeInput_removesQuotes() {
        // Arrange
        testRepository = TestRepository(mockContext)
        
        // Act
        val result = testRepository.testSanitizeInput("test\"value'with`quotes")
        
        // Assert
        assertEquals("testvaluewithquotes", result)
    }
    
    @Test
    fun sanitizeInput_removesBrackets() {
        // Arrange
        testRepository = TestRepository(mockContext)
        
        // Act
        val result = testRepository.testSanitizeInput("test[value]{with}brackets")
        
        // Assert
        assertEquals("testvaluewithbrackets", result)
    }
    
    @Test
    fun sanitizeInput_normalizesWhitespace() {
        // Arrange
        testRepository = TestRepository(mockContext)
        
        // Act
        val result = testRepository.testSanitizeInput("test  multiple   spaces")
        
        // Assert
        assertEquals("test multiple spaces", result)
    }
    
    @Test
    fun sanitizeInput_enforcesLengthLimit() {
        // Arrange
        testRepository = TestRepository(mockContext)
        val longInput = "a".repeat(300)
        
        // Act
        val result = testRepository.testSanitizeInput(longInput)
        
        // Assert
        assertEquals(200, result.length)
    }
    
    @Test
    fun sanitizeInput_trimsWhitespace() {
        // Arrange
        testRepository = TestRepository(mockContext)
        
        // Act
        val result = testRepository.testSanitizeInput("  test value  ")
        
        // Assert
        assertEquals("test value", result)
    }
}
