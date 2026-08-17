package com.joeycarlson.qrscanner.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the [KitBundle] data model's own logic: component counting,
 * validity, the component-name/value listing, and the companion kit-ID
 * generate/extract helpers.
 *
 * Plain JVM tests — no Android framework classes are instantiated. The domain
 * logic in the separate `kitbundle` package (`KitBundleState`/`RequirementStatus`)
 * is covered by `KitBundleModelsTest`.
 */
class KitBundleTest {

    private fun bundle(
        baseKitCode: String = "K123",
        glasses: String? = null,
        controller: String? = null,
        battery01: String? = null,
        battery02: String? = null,
        battery03: String? = null,
        pads: String? = null,
        unused01: String? = null,
        unused02: String? = null
    ) = KitBundle(
        kitId = "$baseKitCode-08/30",
        baseKitCode = baseKitCode,
        creationDate = "08/30",
        glasses = glasses,
        controller = controller,
        battery01 = battery01,
        battery02 = battery02,
        battery03 = battery03,
        pads = pads,
        unused01 = unused01,
        unused02 = unused02
    )

    // ========== getFilledComponentCount / isValid ==========

    @Test
    fun `empty bundle has zero filled components and is invalid`() {
        val kit = bundle()

        assertEquals(0, kit.getFilledComponentCount())
        assertFalse(kit.isValid())
    }

    @Test
    fun `bundle with one component is valid and counts one`() {
        val kit = bundle(controller = "CTRL-1")

        assertEquals(1, kit.getFilledComponentCount())
        assertTrue(kit.isValid())
    }

    @Test
    fun `fully populated bundle counts all eight components`() {
        val kit = bundle(
            glasses = "G", controller = "C",
            battery01 = "B1", battery02 = "B2", battery03 = "B3",
            pads = "P", unused01 = "U1", unused02 = "U2"
        )

        assertEquals(8, kit.getFilledComponentCount())
        assertTrue(kit.isValid())
    }

    @Test
    fun `blank string is a non-null component and still counts`() {
        // getFilledComponentCount uses listOfNotNull, so "" (not null) counts.
        val kit = bundle(glasses = "")

        assertEquals(1, kit.getFilledComponentCount())
        assertTrue(kit.isValid())
    }

    // ========== getComponentList ==========

    @Test
    fun `component list returns all eight slots with names and values in order`() {
        val kit = bundle(glasses = "G", pads = "P")

        val list = kit.getComponentList()

        assertEquals(8, list.size)
        assertEquals("glasses" to "G", list[0])
        assertEquals("controller" to null, list[1])
        assertEquals("pads" to "P", list[5])
        assertEquals(listOf(
            "glasses", "controller", "battery01", "battery02",
            "battery03", "pads", "unused01", "unused02"
        ), list.map { it.first })
    }

    // ========== generateKitId ==========

    @Test
    fun `generateKitId formats base code with today's MM slash dd date`() {
        val id = KitBundle.generateKitId("K123")

        // Uses LocalDate.now(); assert structure, not a fixed date.
        assertTrue("expected 'K123-MM/dd', got '$id'", id.matches(Regex("^K123-\\d{2}/\\d{2}$")))
    }

    // ========== extractCreationDate / extractBaseKitCode ==========

    @Test
    fun `extract splits a normal kit id into base code and date`() {
        assertEquals("K123", KitBundle.extractBaseKitCode("K123-08/30"))
        assertEquals("08/30", KitBundle.extractCreationDate("K123-08/30"))
    }

    @Test
    fun `extract handles a base kit code that itself contains a dash`() {
        // substringBeforeLast/AfterLast split on the LAST '-', so a hyphenated
        // base code round-trips correctly rather than truncating at the first '-'.
        assertEquals("K-123", KitBundle.extractBaseKitCode("K-123-08/30"))
        assertEquals("08/30", KitBundle.extractCreationDate("K-123-08/30"))
    }

    @Test
    fun `extractCreationDate returns empty string when there is no dash`() {
        assertEquals("", KitBundle.extractCreationDate("K123"))
    }

    @Test
    fun `extractBaseKitCode returns the whole string when there is no dash`() {
        assertEquals("K123", KitBundle.extractBaseKitCode("K123"))
    }

    @Test
    fun `generated kit id round-trips through the extract helpers`() {
        val id = KitBundle.generateKitId("K999")

        assertEquals("K999", KitBundle.extractBaseKitCode(id))
        assertEquals(KitBundle.extractCreationDate(id), id.substringAfterLast("-"))
    }
}
