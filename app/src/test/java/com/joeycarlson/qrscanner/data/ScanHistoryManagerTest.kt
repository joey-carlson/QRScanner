package com.joeycarlson.qrscanner.data

import com.joeycarlson.qrscanner.data.ScanHistoryItem.ActivityType
import com.joeycarlson.qrscanner.data.ScanHistoryItem.ScanType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for [ScanHistoryManager] — the SharedPreferences-backed scan
 * history store.
 *
 * Focus is the bug-prone logic: the 50-item cap, most-recent-first ordering,
 * update/delete-by-id, and per-activity-type key isolation.
 *
 * Robolectric provides real SharedPreferences. The manager is a process
 * singleton, so `clearAllHistory()` in @Before guarantees a clean slate
 * between tests regardless of instance reuse.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScanHistoryManagerTest {

    private lateinit var manager: ScanHistoryManager

    @Before
    fun setup() {
        manager = ScanHistoryManager.getInstance(RuntimeEnvironment.getApplication())
        manager.clearAllHistory()
    }

    private var clock = 1_000L

    /** Build an item with a strictly increasing timestamp unless one is given. */
    private fun item(
        value: String,
        activityType: ActivityType = ActivityType.CHECKOUT,
        timestamp: Long = clock++
    ) = ScanHistoryItem(
        value = value,
        timestamp = timestamp,
        scanType = ScanType.BARCODE,
        activityType = activityType
    )

    // ========== add / load ordering ==========

    @Test
    fun `empty history loads as an empty list`() {
        assertTrue(manager.loadHistory(ActivityType.CHECKOUT).isEmpty())
        assertFalse(manager.hasHistory(ActivityType.CHECKOUT))
        assertEquals(0, manager.getHistoryCount(ActivityType.CHECKOUT))
    }

    @Test
    fun `added items load most-recent-first by timestamp`() {
        manager.addToHistory(item("first", timestamp = 100))
        manager.addToHistory(item("second", timestamp = 200))
        manager.addToHistory(item("third", timestamp = 300))

        val history = manager.loadHistory(ActivityType.CHECKOUT)

        assertEquals(listOf("third", "second", "first"), history.map { it.value })
        assertTrue(manager.hasHistory(ActivityType.CHECKOUT))
    }

    @Test
    fun `load sorts by timestamp regardless of insertion order`() {
        // Insert out of chronological order; newest timestamp must come first.
        manager.addToHistory(item("middle", timestamp = 200))
        manager.addToHistory(item("oldest", timestamp = 100))
        manager.addToHistory(item("newest", timestamp = 300))

        val history = manager.loadHistory(ActivityType.CHECKOUT)

        assertEquals(listOf("newest", "middle", "oldest"), history.map { it.value })
    }

    // ========== MAX_HISTORY_SIZE trim ==========

    @Test
    fun `history is capped at 50 items keeping the most recent`() {
        // Add 55 items with increasing timestamps (1..55).
        (1..55).forEach { manager.addToHistory(item("item$it", timestamp = it.toLong())) }

        val history = manager.loadHistory(ActivityType.CHECKOUT)

        assertEquals(50, history.size)
        // Newest (55) first; oldest surviving is item6 (1..5 evicted).
        assertEquals("item55", history.first().value)
        assertEquals("item6", history.last().value)
        assertFalse(history.any { it.value == "item5" })
    }

    // ========== per-activity-type isolation ==========

    @Test
    fun `histories are isolated per activity type`() {
        manager.addToHistory(item("checkout-1", ActivityType.CHECKOUT))
        manager.addToHistory(item("checkin-1", ActivityType.CHECKIN))
        manager.addToHistory(item("checkin-2", ActivityType.CHECKIN))

        assertEquals(1, manager.getHistoryCount(ActivityType.CHECKOUT))
        assertEquals(2, manager.getHistoryCount(ActivityType.CHECKIN))
        assertEquals(0, manager.getHistoryCount(ActivityType.KIT_BUNDLE))
    }

    @Test
    fun `clearing one activity type leaves the others intact`() {
        manager.addToHistory(item("checkout-1", ActivityType.CHECKOUT))
        manager.addToHistory(item("checkin-1", ActivityType.CHECKIN))

        manager.clearHistory(ActivityType.CHECKOUT)

        assertEquals(0, manager.getHistoryCount(ActivityType.CHECKOUT))
        assertEquals(1, manager.getHistoryCount(ActivityType.CHECKIN))
    }

    // ========== update / delete by id ==========

    @Test
    fun `updateHistoryItem changes the value of the matching item`() {
        val target = item("original")
        manager.addToHistory(target)

        val updated = manager.updateHistoryItem(ActivityType.CHECKOUT, target.id, "changed")

        assertTrue(updated)
        assertEquals("changed", manager.loadHistory(ActivityType.CHECKOUT).single().value)
    }

    @Test
    fun `updateHistoryItem returns false for an unknown id`() {
        manager.addToHistory(item("original"))

        val updated = manager.updateHistoryItem(ActivityType.CHECKOUT, "no-such-id", "changed")

        assertFalse(updated)
        assertEquals("original", manager.loadHistory(ActivityType.CHECKOUT).single().value)
    }

    @Test
    fun `deleteHistoryItem removes only the matching item`() {
        val keep = item("keep", timestamp = 100)
        val remove = item("remove", timestamp = 200)
        manager.addToHistory(keep)
        manager.addToHistory(remove)

        val deleted = manager.deleteHistoryItem(ActivityType.CHECKOUT, remove.id)

        assertTrue(deleted)
        assertEquals(listOf("keep"), manager.loadHistory(ActivityType.CHECKOUT).map { it.value })
    }

    @Test
    fun `deleteHistoryItem returns false for an unknown id`() {
        manager.addToHistory(item("keep"))

        val deleted = manager.deleteHistoryItem(ActivityType.CHECKOUT, "no-such-id")

        assertFalse(deleted)
        assertEquals(1, manager.getHistoryCount(ActivityType.CHECKOUT))
    }

    // ========== export ==========

    @Test
    fun `exportHistoryValues returns values most-recent-first`() {
        manager.addToHistory(item("a", timestamp = 100))
        manager.addToHistory(item("b", timestamp = 200))

        assertEquals(listOf("b", "a"), manager.exportHistoryValues(ActivityType.CHECKOUT))
    }
}
