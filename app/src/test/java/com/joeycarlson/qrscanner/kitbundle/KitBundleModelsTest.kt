package com.joeycarlson.qrscanner.kitbundle

import com.joeycarlson.qrscanner.TestLogger
import com.joeycarlson.qrscanner.ocr.DsnValidator.ComponentType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the pure kit-bundle domain logic in [KitBundleModels]:
 * requirement fulfillment counting ([KitBundleState.getRequirementStatus]),
 * duplicate detection, battery-slot allocation, available-slot filtering, and
 * the user-facing progress/missing-component messages on [RequirementStatus].
 *
 * Plain JVM tests — no Android framework classes are instantiated.
 */
class KitBundleModelsTest {

    @get:Rule
    val testLogger = TestLogger()

    // ---- Helpers -----------------------------------------------------------

    private fun component(dsn: String, type: ComponentType, slot: String) =
        ScannedComponent(dsn = dsn, componentType = type, assignedSlot = slot, timestamp = 0L)

    /** Builds a [KitBundleState] from (slot -> ScannedComponent) pairs, deriving scannedDsns. */
    private fun stateOf(vararg components: ScannedComponent): KitBundleState {
        val bySlot = components.associateBy { it.assignedSlot }
        return KitBundleState(
            baseKitCode = "KIT-001",
            scannedComponents = bySlot,
            scannedDsns = components.map { it.dsn }.toSet()
        )
    }

    private fun glasses() = component("G0G348025246001", ComponentType.GLASSES, "glasses")
    private fun controller() = component("G0G46K025224001", ComponentType.CONTROLLER, "controller")
    private fun battery(n: Int) =
        component("G0G4NU01516600$n", ComponentType.valueOf("BATTERY_0$n"), "battery0$n")

    // ---- getRequirementStatus: counting -----------------------------------

    @Test
    fun `getRequirementStatus counts each component type`() {
        val status = stateOf(glasses(), controller(), battery(1), battery(2)).getRequirementStatus()

        assertEquals(1, status.glassesCount)
        assertEquals(1, status.controllerCount)
        assertEquals(2, status.batteryCount)
    }

    @Test
    fun `getRequirementStatus is complete when minimums met`() {
        val status = stateOf(glasses(), controller(), battery(1), battery(2)).getRequirementStatus()

        assertTrue(status.hasMinGlasses)
        assertTrue(status.hasMinController)
        assertTrue(status.hasMinBatteries)
        assertTrue(status.isComplete)
    }

    @Test
    fun `getRequirementStatus is incomplete with only one battery`() {
        val status = stateOf(glasses(), controller(), battery(1)).getRequirementStatus()

        assertFalse(status.hasMinBatteries)
        assertFalse(status.isComplete)
    }

    @Test
    fun `getRequirementStatus is empty for a fresh state`() {
        val status = KitBundleState(baseKitCode = "KIT-001").getRequirementStatus()

        assertEquals(0, status.glassesCount)
        assertFalse(status.isComplete)
    }

    @Test
    fun `getRequirementStatus counts all three battery types as batteries`() {
        val status = stateOf(battery(1), battery(2), battery(3)).getRequirementStatus()

        assertEquals(3, status.batteryCount)
        assertTrue(status.hasMinBatteries)
    }

    // ---- isDuplicateDsn ----------------------------------------------------

    @Test
    fun `isDuplicateDsn detects an already-scanned dsn`() {
        val state = stateOf(controller())

        assertTrue(state.isDuplicateDsn("G0G46K025224001"))
    }

    @Test
    fun `isDuplicateDsn returns false for an unseen dsn`() {
        val state = stateOf(controller())

        assertFalse(state.isDuplicateDsn("G0G348025246001"))
    }

    // ---- getNextAvailableBatterySlot --------------------------------------

    @Test
    fun `getNextAvailableBatterySlot returns first slot when none used`() {
        assertEquals("battery01", stateOf(glasses()).getNextAvailableBatterySlot())
    }

    @Test
    fun `getNextAvailableBatterySlot skips occupied slots`() {
        assertEquals("battery02", stateOf(battery(1)).getNextAvailableBatterySlot())
    }

    @Test
    fun `getNextAvailableBatterySlot returns null when all battery slots full`() {
        assertNull(stateOf(battery(1), battery(2), battery(3)).getNextAvailableBatterySlot())
    }

    // ---- getAvailableSlots -------------------------------------------------

    @Test
    fun `getAvailableSlots lists all eight slots for a fresh state`() {
        val slots = KitBundleState(baseKitCode = "KIT-001").getAvailableSlots()

        assertEquals(8, slots.size)
    }

    @Test
    fun `getAvailableSlots excludes occupied slots`() {
        val slots = stateOf(glasses(), controller()).getAvailableSlots()

        val ids = slots.map { it.id }
        assertFalse(ids.contains("glasses"))
        assertFalse(ids.contains("controller"))
        assertEquals(6, slots.size)
    }

    // ---- RequirementStatus.getProgressMessage ------------------------------

    @Test
    fun `getProgressMessage shows all checks passing when complete`() {
        val message = stateOf(glasses(), controller(), battery(1), battery(2))
            .getRequirementStatus().getProgressMessage()

        assertTrue(message.contains("Glasses: ✓"))
        assertTrue(message.contains("Controller: ✓"))
        assertTrue(message.contains("Batteries: ✓✓"))
    }

    @Test
    fun `getProgressMessage warns on missing components`() {
        val message = KitBundleState(baseKitCode = "KIT-001").getRequirementStatus().getProgressMessage()

        assertTrue(message.contains("Glasses: ⚠️"))
        assertTrue(message.contains("Batteries: ⚠️⚠️"))
    }

    @Test
    fun `getProgressMessage shows three checks for three batteries`() {
        val message = stateOf(battery(1), battery(2), battery(3))
            .getRequirementStatus().getProgressMessage()

        assertTrue(message.contains("Batteries: ✓✓✓"))
    }

    // ---- RequirementStatus.getMissingComponentsMessage ---------------------

    @Test
    fun `getMissingComponentsMessage is null when complete`() {
        val message = stateOf(glasses(), controller(), battery(1), battery(2))
            .getRequirementStatus().getMissingComponentsMessage()

        assertNull(message)
    }

    @Test
    fun `getMissingComponentsMessage lists everything missing for empty kit`() {
        val message = KitBundleState(baseKitCode = "KIT-001")
            .getRequirementStatus().getMissingComponentsMessage()

        assertNotNull(message)
        assertTrue(message!!.contains("1 glasses"))
        assertTrue(message.contains("1 controller"))
        assertTrue(message.contains("2 batteries"))
    }

    @Test
    fun `getMissingComponentsMessage uses singular battery when one short`() {
        val message = stateOf(glasses(), controller(), battery(1))
            .getRequirementStatus().getMissingComponentsMessage()

        assertNotNull(message)
        assertTrue(message!!.contains("1 battery"))
        assertFalse(message.contains("batteries"))
    }
}
