package com.joeycarlson.qrscanner.export

import android.content.Context
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.util.LogManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.time.LocalDate

/**
 * Unit tests for [LogsDataSource].
 *
 * `LogManager` is injected (test-only constructor default) and mocked, so
 * these tests exercise the adapter's own logic — metadata, the collapse of
 * any date range onto a single "today" entry, has-data / record-count
 * thresholds — without touching the real log store. Context is mocked and
 * unused on these paths.
 */
class LogsDataSourceTest {

    private val context = mock<Context>()

    private fun sourceWith(logs: String): LogsDataSource {
        val logManager = mock<LogManager>().stub {
            onBlocking { exportLogs() }.thenReturn(logs)
        }
        return LogsDataSource(context, logManager)
    }

    // ========== Static metadata ==========

    @Test
    fun `getExportType returns the logs export type`() {
        assertEquals(AppConfig.EXPORT_TYPE_LOGS, sourceWith("").getExportType())
    }

    @Test
    fun `getDisplayName is Diagnostic Logs`() {
        assertEquals("Diagnostic Logs", sourceWith("").getDisplayName())
    }

    @Test
    fun `logs do not support date range`() {
        assertFalse(sourceWith("").supportsDateRange())
    }

    @Test
    fun `filename prefix is fixed regardless of date`() {
        val source = sourceWith("")

        assertEquals("qrscanner_logs", source.getFilenamePrefix(null))
        assertEquals("qrscanner_logs", source.getFilenamePrefix(LocalDate.of(2026, 8, 10)))
    }

    @Test
    fun `only supported format is default TXT`() {
        val formats = sourceWith("").getSupportedFormats()

        assertEquals(1, formats.size)
        assertEquals(ExportFormat.TXT, formats[0].format)
        assertTrue(formats[0].isDefault)
    }

    // ========== Data retrieval ==========

    @Test
    fun `getAllData returns the exported log text verbatim`() = runTest {
        assertEquals("line1\nline2", sourceWith("line1\nline2").getAllData())
    }

    @Test
    fun `getDataForDateRange collapses any range onto a single today entry`() = runTest {
        val source = sourceWith("log-body")

        val data = source.getDataForDateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))

        assertEquals(1, data.size)
        assertEquals("log-body", data.values.single())
    }

    // ========== has-data / record-count thresholds ==========

    @Test
    fun `hasData is true when logs are non-empty`() = runTest {
        assertTrue(sourceWith("something").hasData())
    }

    @Test
    fun `hasData is false when logs are empty`() = runTest {
        assertFalse(sourceWith("").hasData())
    }

    @Test
    fun `record count is 1 when logs exist and 0 when empty`() = runTest {
        assertEquals(1, sourceWith("something").getRecordCount())
        assertEquals(0, sourceWith("").getRecordCount())
    }
}
