package com.joeycarlson.qrscanner.export.datasource

import android.content.Context
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.data.CheckInRepository
import com.joeycarlson.qrscanner.data.CheckoutRepository
import com.joeycarlson.qrscanner.data.CheckoutRecord
import com.joeycarlson.qrscanner.data.InventoryRepository
import com.joeycarlson.qrscanner.data.KitBundle
import com.joeycarlson.qrscanner.data.KitRepository
import com.joeycarlson.qrscanner.export.ExportFormat
import com.joeycarlson.qrscanner.inventory.InventoryRecord
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.time.LocalDate

/**
 * Unit tests for the [ExportDataSource] implementations.
 *
 * The repository is injected (test-only constructor default) and mocked, so
 * these tests exercise the adapters' own logic — date-range iteration, record
 * counting, has-data checks, kit-label CSV generation, and inventory grouping —
 * without touching file I/O. Context is mocked and unused on these paths.
 */
class ExportDataSourceTest {

    private val context = mock<Context>()

    private fun kit(
        baseKitCode: String,
        controller: String? = null,
        glasses: String? = null,
        battery01: String? = null,
        battery02: String? = null,
        battery03: String? = null,
        pads: String? = null,
        unused01: String? = null,
        unused02: String? = null
    ) = KitBundle(
        kitId = "$baseKitCode-08/10",
        baseKitCode = baseKitCode,
        creationDate = "08/10",
        controller = controller,
        glasses = glasses,
        battery01 = battery01,
        battery02 = battery02,
        battery03 = battery03,
        pads = pads,
        unused01 = unused01,
        unused02 = unused02
    )

    // ========== Static metadata ==========

    @Test
    fun `each data source reports its export type`() {
        assertEquals(AppConfig.EXPORT_TYPE_CHECKOUT, CheckoutDataSource(context, mock()).getExportType())
        assertEquals(AppConfig.EXPORT_TYPE_CHECKIN, CheckInDataSource(context, mock()).getExportType())
        assertEquals(AppConfig.EXPORT_TYPE_KIT_BUNDLE, KitBundleDataSource(context, mock()).getExportType())
        assertEquals(AppConfig.EXPORT_TYPE_INVENTORY, InventoryDataSource(context, mock()).getExportType())
    }

    @Test
    fun `checkout supports date range but inventory does not`() {
        assertTrue(CheckoutDataSource(context, mock()).supportsDateRange())
        assertFalse(InventoryDataSource(context, mock()).supportsDateRange())
    }

    @Test
    fun `kit bundle default format is JSON and includes kit labels csv`() {
        val formats = KitBundleDataSource(context, mock()).getSupportedFormats()

        assertEquals(ExportFormat.JSON, formats.first { it.isDefault }.format)
        assertTrue(formats.any { it.format == ExportFormat.KIT_LABELS_CSV })
    }

    @Test
    fun `inventory supports only JSON`() {
        val formats = InventoryDataSource(context, mock()).getSupportedFormats()

        assertEquals(1, formats.size)
        assertEquals(ExportFormat.JSON, formats.single().format)
    }

    // ========== Date-range iteration & counting (CheckoutDataSource) ==========

    @Test
    fun `getRecordCount sums records across an inclusive date range`() = runTest {
        val repo = mock<CheckoutRepository>()
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 8, 3)   // 3 inclusive days
        repo.stub { onBlocking { getRecordsForDate(any()) } doReturn listOf(checkoutRecord()) }
        val source = CheckoutDataSource(context, repo)

        val count = source.getRecordCount(start, end)

        assertEquals(3, count)
    }

    @Test
    fun `getDataForDateRange omits dates with no records`() = runTest {
        val repo = mock<CheckoutRepository>()
        val day = LocalDate.of(2026, 8, 2)
        repo.stub {
            onBlocking { getRecordsForDate(day) } doReturn listOf(checkoutRecord())
            onBlocking { getRecordsForDate(LocalDate.of(2026, 8, 1)) } doReturn emptyList()
            onBlocking { getRecordsForDate(LocalDate.of(2026, 8, 3)) } doReturn emptyList()
        }
        val source = CheckoutDataSource(context, repo)

        val data = source.getDataForDateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3))

        assertEquals(setOf(day), data.keys)
    }

    @Test
    fun `hasData returns false when the repository is empty`() = runTest {
        val repo = mock<CheckoutRepository>()
        repo.stub { onBlocking { getRecordsForDate(any()) } doReturn emptyList() }
        val source = CheckoutDataSource(context, repo)

        assertFalse(source.hasData())
    }

    @Test
    fun `hasData returns true when any date has records`() = runTest {
        val repo = mock<CheckInRepository>()
        repo.stub { onBlocking { getRecordsForDate(any()) } doReturn listOf(checkInRecord()) }
        val source = CheckInDataSource(context, repo)

        assertTrue(source.hasData())
    }

    // ========== Kit labels CSV generation (KitBundleDataSource) ==========

    @Test
    fun `kit labels strip the K prefix and name components by type`() = runTest {
        val repo = mock<KitRepository>()
        repo.stub {
            onBlocking { getBundlesForDate(any()) } doReturn emptyList()
            onBlocking { getBundlesForDate(LocalDate.of(2026, 8, 10)) } doReturn listOf(
                kit("K123", controller = "ctrl", glasses = "gl", pads = "pad")
            )
        }
        val source = KitBundleDataSource(context, repo)

        val labels = source.generateKitLabelsContent(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10))
            .lines()

        assertEquals(listOf("Kit 123", "Puck 123", "G 123", "Pads 123"), labels)
    }

    @Test
    fun `kit labels number batteries sequentially and skip empty slots`() = runTest {
        val repo = mock<KitRepository>()
        repo.stub {
            onBlocking { getBundlesForDate(any()) } doReturn emptyList()
            onBlocking { getBundlesForDate(LocalDate.of(2026, 8, 10)) } doReturn listOf(
                kit("K7", battery01 = "b1", battery03 = "b3")   // battery02 intentionally empty
            )
        }
        val source = KitBundleDataSource(context, repo)

        val labels = source.generateKitLabelsContent(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10))
            .lines()

        // Two present batteries -> sequential 7-1, 7-2 (not 7-1, 7-3).
        assertEquals(listOf("Kit 7", "Battery 7-1", "Battery 7-2"), labels)
    }

    @Test
    fun `kit labels exclude unused slots`() = runTest {
        val repo = mock<KitRepository>()
        repo.stub {
            onBlocking { getBundlesForDate(any()) } doReturn emptyList()
            onBlocking { getBundlesForDate(LocalDate.of(2026, 8, 10)) } doReturn listOf(
                kit("K5", controller = "c", unused01 = "u1", unused02 = "u2")
            )
        }
        val source = KitBundleDataSource(context, repo)

        val content = source.generateKitLabelsContent(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10))

        assertFalse(content.contains("u1"))
        assertFalse(content.contains("Unused"))
        assertEquals(listOf("Kit 5", "Puck 5"), content.lines())
    }

    // ========== Inventory summary grouping (InventoryDataSource) ==========

    @Test
    fun `inventory summary groups devices by component type`() = runTest {
        val repo = mock<InventoryRepository>()
        doReturn(
            listOf(
                inventoryRecord("d1", "glasses"),
                inventoryRecord("d2", "controller"),
                inventoryRecord("d3", "battery"),
                inventoryRecord("d4", "battery")
            )
        ).`when`(repo).getAllRecords()
        val source = InventoryDataSource(context, repo)

        val summary = source.getInventorySummary()

        assertEquals(4, summary.totalDevices)
        assertEquals(1, summary.glassesCount)
        assertEquals(1, summary.controllerCount)
        assertEquals(2, summary.batteryCount)
    }

    @Test
    fun `inventory getRecordCount reflects repository size`() = runTest {
        val repo = mock<InventoryRepository>()
        doReturn(listOf(inventoryRecord("d1", "glasses"))).`when`(repo).getAllRecords()
        val source = InventoryDataSource(context, repo)

        assertEquals(1, source.getRecordCount())
        assertTrue(source.hasData())
    }

    // ---- record builders ---------------------------------------------------

    private fun checkoutRecord() =
        CheckoutRecord(userId = "user", kitId = "K1-08/10", type = "CHECKOUT", value = "K1")

    private fun checkInRecord() = com.joeycarlson.qrscanner.data.CheckInRecord(kitId = "K1", value = "K1")

    private fun inventoryRecord(deviceId: String, type: String) =
        InventoryRecord(deviceId = deviceId, componentType = type, scanMode = "BARCODE")
}
