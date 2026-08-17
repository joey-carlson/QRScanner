package com.joeycarlson.qrscanner.inventory

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [ComponentType.fromString] round-tripping and display names.
 * Plain JVM.
 */
class ComponentTypeTest {

    @Test
    fun `fromString round-trips every enum name`() {
        ComponentType.values().forEach { type ->
            assertEquals(type, ComponentType.fromString(type.name))
        }
    }

    @Test
    fun `fromString returns null for an unknown value`() {
        assertNull(ComponentType.fromString("SPEAKER"))
    }

    @Test
    fun `fromString is case-sensitive and matches name not displayName`() {
        // Matches on `name` (uppercase), so the display name / wrong case do not resolve.
        assertNull(ComponentType.fromString("Battery"))
        assertNull(ComponentType.fromString("battery"))
        assertEquals(ComponentType.BATTERY, ComponentType.fromString("BATTERY"))
    }

    @Test
    fun `display names are human-friendly`() {
        assertEquals("Glasses", ComponentType.GLASSES.displayName)
        assertEquals("Controller", ComponentType.CONTROLLER.displayName)
        assertEquals("Battery", ComponentType.BATTERY.displayName)
    }
}
