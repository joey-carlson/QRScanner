package com.joeycarlson.qrscanner.kitbundle

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.joeycarlson.qrscanner.data.KitBundle
import com.joeycarlson.qrscanner.data.KitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking

/**
 * Unit tests for [KitBundleViewModel] — the scan-to-save workflow logic.
 *
 * Focus is the parts the testability survey flagged as bug-prone:
 *  - confidence-based routing in `handleComponentScan` (HIGH auto-assigns,
 *    MEDIUM/LOW open a dialog),
 *  - duplicate-DSN detection and slot reassignment,
 *  - the three parallel slot-mapping `when`-blocks (`getSuggestedSlot`,
 *    `getComponentTypeForSlot`, `getSlotDisplayName`) exercised end-to-end
 *    through `createKitBundle`, and
 *  - the review-mode state rebuild in `confirmReview`.
 *
 * No Robolectric: `AndroidViewModel` only stores the mocked `Application`, and
 * `BarcodeValidator`/`DsnValidator` are pure. A `StandardTestDispatcher` drives
 * the `viewModelScope` coroutines, matching `ScanViewModelTest`.
 *
 * Real-world DSN fixtures (match `DsnValidator` HIGH-confidence patterns):
 *   controller `G0G46K…`, battery `G0G4NU…`, glasses `G0G348…`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class KitBundleViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockRepository: KitRepository

    private lateinit var viewModel: KitBundleViewModel

    private val kitCode = "K123"
    private val controllerDsn = "G0G46K025224001"
    private val glassesDsn = "G0G348025246001"
    private val battery1Dsn = "G0G4NU015166001"
    private val battery2Dsn = "G0G4NU015166002"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = KitBundleViewModel(mockApplication, mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Scan the kit code, then advance past the re-enable delay. */
    private fun scanKit() {
        viewModel.processBarcode(kitCode)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    /** Scan a component DSN at the given OCR confidence and settle coroutines. */
    private fun scanComponent(dsn: String, ocrConfidence: Float = 1.0f) {
        viewModel.processBarcode(dsn, ocrConfidence)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    // ========== Kit scan bootstrapping ==========

    @Test
    fun `first scan initializes kit and prompts for components`() = runTest {
        scanKit()

        assertTrue(viewModel.statusMessage.first().contains(kitCode))
        assertTrue("scanning should re-enable after the kit scan", viewModel.isScanning.first())
    }

    // ========== Confidence-based routing ==========

    @Test
    fun `high-confidence component auto-assigns without a dialog`() = runTest {
        scanKit()

        scanComponent(controllerDsn, ocrConfidence = 1.0f)

        // Auto-assigned: no confirmation dialog, and it lands in the summary.
        assertNull(viewModel.componentDetectionResult.first())
        assertTrue(viewModel.componentSummary.first().contains("Controller"))
    }

    @Test
    fun `medium-confidence component opens a confirmation dialog instead of assigning`() = runTest {
        scanKit()

        // ocr 0.8 (>= MEDIUM 0.7, < HIGH 0.9) on a HIGH-pattern DSN -> MEDIUM.
        scanComponent(controllerDsn, ocrConfidence = 0.8f)

        val result = viewModel.componentDetectionResult.first()
        assertNotNull("MEDIUM confidence should surface a detection result", result)
        assertTrue(result!!.requiresConfirmation)
        assertFalse("scanning pauses while the dialog is up", viewModel.isScanning.first())
        assertEquals("", viewModel.componentSummary.first())
    }

    @Test
    fun `low-confidence unrecognized value opens the manual selection dialog`() = runTest {
        scanKit()

        // Valid barcode but infers no component type -> LOW.
        scanComponent("ABCD1234", ocrConfidence = 1.0f)

        val result = viewModel.componentDetectionResult.first()
        assertNotNull(result)
        assertNull("no component type inferred", result!!.componentType)
        assertFalse(viewModel.isScanning.first())
    }

    // ========== Slot mapping end-to-end (createKitBundle via save) ==========

    @Test
    fun `battery DSNs fill sequential battery slots`() = runTest {
        scanKit()
        scanComponent(battery1Dsn)
        scanComponent(battery2Dsn)

        val bundle = saveAndCapture()

        assertEquals(battery1Dsn, bundle.battery01)
        assertEquals(battery2Dsn, bundle.battery02)
        assertNull(bundle.battery03)
    }

    @Test
    fun `each component type maps to its own field in the saved bundle`() = runTest {
        scanKit()
        scanComponent(glassesDsn)
        scanComponent(controllerDsn)
        scanComponent(battery1Dsn)
        scanComponent(battery2Dsn)

        val bundle = saveAndCapture()

        assertEquals(kitCode, bundle.baseKitCode)
        assertEquals(glassesDsn, bundle.glasses)
        assertEquals(controllerDsn, bundle.controller)
        assertEquals(battery1Dsn, bundle.battery01)
        assertEquals(battery2Dsn, bundle.battery02)
        assertEquals(4, bundle.getFilledComponentCount())
        // kitId is derived from the base code + today's date.
        assertEquals(kitCode, KitBundle.extractBaseKitCode(bundle.kitId))
    }

    // ========== Duplicate detection + reassignment ==========

    @Test
    fun `re-scanning the same DSN raises a duplicate result instead of a second assignment`() = runTest {
        scanKit()
        scanComponent(battery1Dsn)

        scanComponent(battery1Dsn)

        val dup = viewModel.duplicateComponentResult.first()
        assertNotNull("duplicate DSN should surface a duplicate result", dup)
        assertEquals(battery1Dsn, dup!!.dsn)
        assertEquals("battery01", dup.currentSlot)
        assertEquals("Battery 01", dup.currentSlotDisplayName)
    }

    @Test
    fun `reassigning a duplicate moves the DSN to the new slot`() = runTest {
        scanKit()
        scanComponent(battery1Dsn)          // lands in battery01
        scanComponent(battery1Dsn)          // duplicate -> dialog
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.reassignDuplicateComponent("battery02")
        testDispatcher.scheduler.advanceUntilIdle()

        val bundle = saveAndCapture()
        assertNull("original slot vacated", bundle.battery01)
        assertEquals(battery1Dsn, bundle.battery02)
        assertNull(viewModel.duplicateComponentResult.first())
    }

    @Test
    fun `ignoring a duplicate clears the result and resumes scanning`() = runTest {
        scanKit()
        scanComponent(battery1Dsn)
        scanComponent(battery1Dsn)

        viewModel.ignoreDuplicateComponent()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.duplicateComponentResult.first())
        assertTrue(viewModel.isScanning.first())
    }

    // ========== Review mode ==========

    @Test
    fun `meeting minimum requirements enters review mode`() = runTest {
        scanKit()
        scanComponent(glassesDsn)
        scanComponent(controllerDsn)
        scanComponent(battery1Dsn)
        scanComponent(battery2Dsn)

        assertTrue(viewModel.isReviewMode.first())
        assertTrue(viewModel.showSaveButton.first())
        assertEquals(kitCode, viewModel.reviewKitCode.first())
    }

    @Test
    fun `confirmReview rebuilds state from edited review values and saves`() = runTest {
        scanKit()
        scanComponent(glassesDsn)
        scanComponent(controllerDsn)
        scanComponent(battery1Dsn)
        scanComponent(battery2Dsn)

        // Edit the kit code and a battery DSN in review, then confirm.
        viewModel.updateReviewKitCode("K999")
        viewModel.updateReviewComponent("battery02", "G0G4NU015166999")
        val bundle = saveAndCapture { viewModel.confirmReview() }

        assertEquals("K999", bundle.baseKitCode)
        assertEquals("G0G4NU015166999", bundle.battery02)
        assertFalse(viewModel.isReviewMode.first())
    }

    @Test
    fun `updateReviewComponent with a blank value removes the slot`() = runTest {
        scanKit()
        scanComponent(glassesDsn)
        scanComponent(controllerDsn)
        scanComponent(battery1Dsn)
        scanComponent(battery2Dsn)

        viewModel.updateReviewComponent("glasses", "")

        assertFalse(viewModel.reviewComponents.first().containsKey("glasses"))
    }

    // ========== Invalid input ==========

    @Test
    fun `invalid barcode flashes failure and never initializes a kit`() = runTest {
        // Empty barcode is invalid. Advance only partially so the flash is still
        // up (handleInvalidBarcode resets scanFailure after a 600ms delay).
        viewModel.processBarcode("")
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.scanFailure.first())

        // No kit was created, so a following valid scan is still the kit code.
        scanKit()
        assertTrue(viewModel.statusMessage.first().contains(kitCode))
    }

    // ========== Helpers ==========

    /**
     * Stubs the repo to accept a save, triggers [action] (default: `saveKitBundle`),
     * settles coroutines, and returns the [KitBundle] handed to the repository.
     */
    private fun saveAndCapture(action: () -> Unit = { viewModel.saveKitBundle() }): KitBundle {
        mockRepository.stub { onBlocking { saveKitBundle(any()) } doReturn true }

        action()
        testDispatcher.scheduler.advanceUntilIdle()

        val captor = argumentCaptor<KitBundle>()
        verifyBlocking(mockRepository) { saveKitBundle(captor.capture()) }
        return captor.firstValue
    }
}
