package com.joeycarlson.qrscanner.export

import com.joeycarlson.qrscanner.TestLogger
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for FileNamingService
 * Tests filename generation, sanitization, and pattern extraction
 */
class FileNamingServiceTest {
    
    @get:Rule
    val testLogger = TestLogger()
    
    private lateinit var service: FileNamingService
    private val testDate = LocalDate.of(2024, 10, 13)
    private val testLocationId = "TEST_LOC_001"
    
    @Before
    fun setup() {
        service = FileNamingService()
    }
    
    // ========== Checkout Filename Generation Tests ==========
    
    @Test
    fun `generateFilename creates correct format`() {
        val filename = service.generateFilename(testDate, testLocationId, ExportFormat.JSON)
        
        assertEquals("qr_checkouts_10-13-24_TEST_LOC_001.json", filename)
    }
    
    @Test
    fun `generateFilename handles different export formats`() {
        val formats = mapOf(
            ExportFormat.JSON to "qr_checkouts_10-13-24_TEST_LOC_001.json",
            ExportFormat.CSV to "qr_checkouts_10-13-24_TEST_LOC_001.csv",
            ExportFormat.TXT to "qr_checkouts_10-13-24_TEST_LOC_001.txt",
            ExportFormat.XML to "qr_checkouts_10-13-24_TEST_LOC_001.xml"
        )
        
        formats.forEach { (format, expected) ->
            val filename = service.generateFilename(testDate, testLocationId, format)
            assertEquals(expected, filename)
        }
    }
    
    @Test
    fun `generateS3Key creates correct folder structure`() {
        val s3Key = service.generateS3Key(testDate, testLocationId, ExportFormat.JSON)
        
        assertEquals("TEST_LOC_001/2024/10/qr_checkouts_10-13-24_TEST_LOC_001.json", s3Key)
    }
    
    @Test
    fun `generateRangeFilename creates correct date range format`() {
        val startDate = LocalDate.of(2024, 10, 1)
        val endDate = LocalDate.of(2024, 10, 13)
        
        val filename = service.generateRangeFilename(startDate, endDate, testLocationId, ExportFormat.JSON)
        
        assertEquals("qr_checkouts_10-01-24_to_10-13-24_TEST_LOC_001.json", filename)
    }
    
    @Test
    fun `generateTempFilename includes timestamp`() {
        val filename = service.generateTempFilename(testDate, testLocationId, ExportFormat.JSON)
        
        assertTrue(filename.startsWith("temp_qr_checkouts_10-13-24_TEST_LOC_001_"))
        assertTrue(filename.endsWith(".json"))
        assertTrue(filename.contains(Regex("\\d{13}"))) // Timestamp
    }
    
    // ========== Check-In Filename Generation Tests ==========
    
    @Test
    fun `generateCheckInFilename creates correct format`() {
        val filename = service.generateCheckInFilename(testDate, testLocationId, ExportFormat.JSON)
        
        assertEquals("qr_checkins_10-13-24_TEST_LOC_001.json", filename)
    }
    
    @Test
    fun `generateCheckInS3Key creates correct folder structure`() {
        val s3Key = service.generateCheckInS3Key(testDate, testLocationId, ExportFormat.JSON)
        
        assertEquals("TEST_LOC_001/2024/10/qr_checkins_10-13-24_TEST_LOC_001.json", s3Key)
    }
    
    @Test
    fun `generateCheckInRangeFilename creates correct format`() {
        val startDate = LocalDate.of(2024, 10, 1)
        val endDate = LocalDate.of(2024, 10, 13)
        
        val filename = service.generateCheckInRangeFilename(startDate, endDate, testLocationId, ExportFormat.CSV)
        
        assertEquals("qr_checkins_10-01-24_to_10-13-24_TEST_LOC_001.csv", filename)
    }
    
    @Test
    fun `generateTempCheckInFilename includes timestamp`() {
        val filename = service.generateTempCheckInFilename(testDate, testLocationId, ExportFormat.JSON)
        
        assertTrue(filename.startsWith("temp_qr_checkins_10-13-24_TEST_LOC_001_"))
        assertTrue(filename.endsWith(".json"))
    }
    
    // ========== Inventory Filename Generation Tests ==========
    
    @Test
    fun `generateInventoryFilename creates correct format`() {
        val filename = service.generateInventoryFilename(testDate, testLocationId, ExportFormat.JSON)
        
        assertEquals("device_inventory_10-13-24_TEST_LOC_001.json", filename)
    }
    
    @Test
    fun `generateInventoryS3Key creates correct folder structure`() {
        val s3Key = service.generateInventoryS3Key(testDate, testLocationId, ExportFormat.CSV)
        
        assertEquals("TEST_LOC_001/2024/10/device_inventory_10-13-24_TEST_LOC_001.csv", s3Key)
    }
    
    @Test
    fun `generateTempInventoryFilename includes timestamp`() {
        val filename = service.generateTempInventoryFilename(testDate, testLocationId, ExportFormat.JSON)
        
        assertTrue(filename.startsWith("temp_device_inventory_10-13-24_TEST_LOC_001_"))
        assertTrue(filename.endsWith(".json"))
    }
    
    // ========== Kit Bundle Filename Generation Tests ==========
    
    @Test
    fun `generateKitBundleFilename creates correct format`() {
        val filename = service.generateKitBundleFilename(testDate, testLocationId, ExportFormat.CSV)
        
        assertEquals("kit_bundles_10-13-24_TEST_LOC_001.csv", filename)
    }
    
    @Test
    fun `generateKitLabelFilename creates correct format with device name`() {
        val filename = service.generateKitLabelFilename(testDate, "Tablet01", "SITE-A")
        
        assertEquals("kit_labels_10-13_Tablet01_SITE-A.csv", filename)
    }
    
    @Test
    fun `generateKitLabelFilename handles null device name`() {
        val filename = service.generateKitLabelFilename(testDate, null, "SITE-A")
        
        assertEquals("kit_labels_10-13_SITE-A.csv", filename)
    }
    
    @Test
    fun `generateKitLabelFilename handles null location ID`() {
        val filename = service.generateKitLabelFilename(testDate, "Tablet01", null)
        
        assertEquals("kit_labels_10-13_Tablet01.csv", filename)
    }
    
    @Test
    fun `generateKitLabelFilename handles both nulls`() {
        val filename = service.generateKitLabelFilename(testDate, null, null)
        
        assertEquals("kit_labels_10-13.csv", filename)
    }
    
    @Test
    fun `generateTempKitLabelFilename includes timestamp`() {
        val filename = service.generateTempKitLabelFilename(testDate, "Tablet01", "SITE-A")
        
        assertTrue(filename.startsWith("temp_kit_labels_10-13_Tablet01_SITE-A_"))
        assertTrue(filename.endsWith(".csv"))
    }
    
    // ========== Location ID Sanitization Tests ==========
    
    @Test
    fun `sanitizeLocationId removes spaces`() {
        val filename = service.generateFilename(testDate, "SITE WITH SPACES", ExportFormat.JSON)
        
        assertTrue(filename.contains("SITE_WITH_SPACES"))
        assertFalse(filename.contains(" "))
    }
    
    @Test
    fun `sanitizeLocationId removes forward slashes`() {
        val filename = service.generateFilename(testDate, "SITE/A/B", ExportFormat.JSON)
        
        assertTrue(filename.contains("SITE-A-B"))
        assertFalse(filename.contains("/"))
    }
    
    @Test
    fun `sanitizeLocationId removes backslashes`() {
        val filename = service.generateFilename(testDate, "SITE\\A\\B", ExportFormat.JSON)
        
        assertTrue(filename.contains("SITE-A-B"))
        assertFalse(filename.contains("\\"))
    }
    
    @Test
    fun `sanitizeLocationId removes dangerous characters`() {
        val dangerousChars = listOf(":", "*", "?", "\"", "<", ">", "|")
        
        dangerousChars.forEach { char ->
            val filename = service.generateFilename(testDate, "SITE${char}TEST", ExportFormat.JSON)
            assertFalse("Should not contain $char", filename.contains(char))
        }
    }
    
    @Test
    fun `sanitizeLocationId trims whitespace`() {
        val filename = service.generateFilename(testDate, "  SITE  ", ExportFormat.JSON)
        
        assertTrue(filename.contains("SITE"))
        assertFalse(filename.endsWith("  .json"))
    }
    
    @Test
    fun `sanitizeLocationId handles complex location ID`() {
        val filename = service.generateFilename(testDate, "  SITE: A/B\\C * Test?  ", ExportFormat.JSON)
        
        assertTrue(filename.contains("SITE-_A-B-C__Test"))
    }
    
    // ========== Pattern Extraction Tests ==========
    
    @Test
    fun `extractDateFromFilename extracts correct date`() {
        val filename = "qr_checkouts_10-13-24_TEST_LOC_001.json"
        val extractedDate = service.extractDateFromFilename(filename)
        
        assertNotNull(extractedDate)
        assertEquals(testDate, extractedDate)
    }
    
    @Test
    fun `extractDateFromFilename returns null for invalid pattern`() {
        val invalidFilenames = listOf(
            "invalid_filename.json",
            "checkouts_10-13-24.json",
            "qr_checkouts_invalid_date.json"
        )
        
        invalidFilenames.forEach { filename ->
            val extractedDate = service.extractDateFromFilename(filename)
            assertNull("Should be null for: $filename", extractedDate)
        }
    }
    
    @Test
    fun `extractLocationIdFromFilename extracts correct location ID`() {
        val filename = "qr_checkouts_10-13-24_TEST_LOC_001.json"
        val locationId = service.extractLocationIdFromFilename(filename)
        
        assertNotNull(locationId)
        assertEquals("TEST_LOC_001", locationId)
    }
    
    @Test
    fun `extractLocationIdFromFilename returns null for invalid pattern`() {
        val invalidFilenames = listOf(
            "invalid_filename.json",
            "qr_checkouts.json",
            "checkouts_10-13-24_LOC.json"
        )
        
        invalidFilenames.forEach { filename ->
            val locationId = service.extractLocationIdFromFilename(filename)
            assertNull("Should be null for: $filename", locationId)
        }
    }
    
    @Test
    fun `extractLocationIdFromFilename handles sanitized location IDs`() {
        val filename = "qr_checkouts_10-13-24_SITE_WITH_SPACES.json"
        val locationId = service.extractLocationIdFromFilename(filename)
        
        assertNotNull(locationId)
        assertEquals("SITE_WITH_SPACES", locationId)
    }
    
    // ========== Edge Cases and Date Formatting Tests ==========
    
    @Test
    fun `filename generation handles single digit months and days`() {
        val date = LocalDate.of(2024, 1, 5)
        val filename = service.generateFilename(date, testLocationId, ExportFormat.JSON)
        
        assertEquals("qr_checkouts_01-05-24_TEST_LOC_001.json", filename)
    }
    
    @Test
    fun `filename generation handles end of year`() {
        val date = LocalDate.of(2024, 12, 31)
        val filename = service.generateFilename(date, testLocationId, ExportFormat.JSON)
        
        assertEquals("qr_checkouts_12-31-24_TEST_LOC_001.json", filename)
    }
    
    @Test
    fun `S3 key generation includes leading zeros for months`() {
        val date = LocalDate.of(2024, 1, 15)
        val s3Key = service.generateS3Key(date, testLocationId, ExportFormat.JSON)
        
        assertTrue(s3Key.contains("/01/"))
        assertFalse(s3Key.contains("/1/"))
    }
    
    @Test
    fun `temp filenames are unique when generated rapidly`() {
        val filenames = mutableSetOf<String>()
        
        repeat(10) {
            val filename = service.generateTempFilename(testDate, testLocationId, ExportFormat.JSON)
            filenames.add(filename)
            // Small delay to ensure different timestamps (currentTimeMillis precision is ~1ms)
            Thread.sleep(2)
        }
        
        assertEquals("All temp filenames should be unique", 10, filenames.size)
    }
    
    @Test
    fun `all filename types use consistent date formatting`() {
        val checkoutFile = service.generateFilename(testDate, testLocationId, ExportFormat.JSON)
        val checkinFile = service.generateCheckInFilename(testDate, testLocationId, ExportFormat.JSON)
        val inventoryFile = service.generateInventoryFilename(testDate, testLocationId, ExportFormat.JSON)
        val kitBundleFile = service.generateKitBundleFilename(testDate, testLocationId, ExportFormat.JSON)
        
        // All should contain the same date format
        val datePattern = "10-13-24"
        assertTrue(checkoutFile.contains(datePattern))
        assertTrue(checkinFile.contains(datePattern))
        assertTrue(inventoryFile.contains(datePattern))
        assertTrue(kitBundleFile.contains(datePattern))
    }
    
    @Test
    fun `kit label filename uses MM-DD format without year`() {
        val filename = service.generateKitLabelFilename(testDate, "Device", "LOC")
        
        assertTrue(filename.contains("10-13"))
        assertFalse(filename.contains("24"))
    }
}
