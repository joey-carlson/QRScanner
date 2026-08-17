package com.joeycarlson.qrscanner.inventory

import com.joeycarlson.qrscanner.ocr.ScanMode
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [InventoryRecord.create] — the scan-mode/component-type
 * mapping applied when building a record from scan data. Plain JVM.
 */
class InventoryRecordTest {

    @Test
    fun `create maps a barcode scan of a controller`() {
        val record = InventoryRecord.create("DEV-1", ComponentType.CONTROLLER, ScanMode.BARCODE_ONLY)

        assertEquals("DEV-1", record.deviceId)
        assertEquals("CONTROLLER", record.componentType)
        assertEquals("BARCODE", record.scanMode)
    }

    @Test
    fun `create maps an OCR scan of glasses`() {
        val record = InventoryRecord.create("DEV-2", ComponentType.GLASSES, ScanMode.OCR_ONLY)

        assertEquals("GLASSES", record.componentType)
        assertEquals("OCR", record.scanMode)
    }

    @Test
    fun `create stores the component type's own name not its display name`() {
        // Guards against the mapping drifting to displayName ("Battery") instead of name ("BATTERY").
        val record = InventoryRecord.create("DEV-3", ComponentType.BATTERY, ScanMode.BARCODE_ONLY)

        assertEquals("BATTERY", record.componentType)
    }
}
