package com.joeycarlson.qrscanner.kitbundle

import android.os.Parcelable
import com.joeycarlson.qrscanner.ocr.DsnValidator
import kotlinx.parcelize.Parcelize

/**
 * Represents the minimum requirements for a valid kit bundle
 */
data class KitRequirements(
    val minGlasses: Int = 1,
    val minController: Int = 1,
    val minBatteries: Int = 2,
    val maxBatteries: Int = 3
)

/**
 * Represents the current status of kit requirements fulfillment
 */
data class RequirementStatus(
    val hasMinGlasses: Boolean,
    val hasMinController: Boolean,
    val hasMinBatteries: Boolean,
    val isComplete: Boolean,
    val glassesCount: Int = 0,
    val controllerCount: Int = 0,
    val batteryCount: Int = 0
) {
    /**
     * Gets a user-friendly progress message
     */
    fun getProgressMessage(): String {
        val messages = mutableListOf<String>()
        
        // Glasses status
        messages.add(if (hasMinGlasses) {
            "Glasses: ✓ ($glassesCount/1)"
        } else {
            "Glasses: ⚠️ ($glassesCount/1)"
        })
        
        // Controller status
        messages.add(if (hasMinController) {
            "Controller: ✓ ($controllerCount/1)"
        } else {
            "Controller: ⚠️ ($controllerCount/1)"
        })
        
        // Battery status
        val batterySymbols = when (batteryCount) {
            0 -> "⚠️⚠️"
            1 -> "✓⚠️"
            2 -> "✓✓"
            3 -> "✓✓✓"
            else -> "✓✓✓"
        }
        messages.add("Batteries: $batterySymbols ($batteryCount/2 min)")
        
        return messages.joinToString(" | ")
    }
    
    /**
     * Gets a summary of what's still needed
     */
    fun getMissingComponentsMessage(): String? {
        val missing = mutableListOf<String>()
        
        if (!hasMinGlasses) {
            missing.add("1 glasses")
        }
        if (!hasMinController) {
            missing.add("1 controller")
        }
        if (!hasMinBatteries) {
            val needed = 2 - batteryCount
            missing.add("$needed ${if (needed == 1) "battery" else "batteries"}")
        }
        
        return if (missing.isEmpty()) {
            null
        } else {
            "Still need: ${missing.joinToString(", ")}"
        }
    }
}

/**
 * Result of component detection with confidence information
 */
data class ComponentDetectionResult(
    val dsn: String,
    val componentType: DsnValidator.ComponentType?,
    val confidenceLevel: DsnValidator.ConfidenceLevel,
    val requiresConfirmation: Boolean,
    val suggestedSlot: String? = null
)

/**
 * Represents a scanned component in the current kit session
 */
data class ScannedComponent(
    val dsn: String,
    val componentType: DsnValidator.ComponentType?,
    val assignedSlot: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Tracks the state of a kit bundle being assembled
 */
data class KitBundleState(
    val baseKitCode: String,
    val scannedComponents: Map<String, ScannedComponent> = emptyMap(),
    val scannedDsns: Set<String> = emptySet(),
    val requirements: KitRequirements = KitRequirements()
) {
    /**
     * Gets the current requirement fulfillment status
     */
    fun getRequirementStatus(): RequirementStatus {
        val glassesCount = scannedComponents.count { (_, component) ->
            component.componentType == DsnValidator.ComponentType.GLASSES
        }
        
        val controllerCount = scannedComponents.count { (_, component) ->
            component.componentType == DsnValidator.ComponentType.CONTROLLER
        }
        
        val batteryCount = scannedComponents.count { (_, component) ->
            component.componentType in listOf(
                DsnValidator.ComponentType.BATTERY_01,
                DsnValidator.ComponentType.BATTERY_02,
                DsnValidator.ComponentType.BATTERY_03
            )
        }
        
        return RequirementStatus(
            hasMinGlasses = glassesCount >= requirements.minGlasses,
            hasMinController = controllerCount >= requirements.minController,
            hasMinBatteries = batteryCount >= requirements.minBatteries,
            isComplete = glassesCount >= requirements.minGlasses && 
                        controllerCount >= requirements.minController && 
                        batteryCount >= requirements.minBatteries,
            glassesCount = glassesCount,
            controllerCount = controllerCount,
            batteryCount = batteryCount
        )
    }
    
    /**
     * Checks if a DSN has already been scanned in this kit
     */
    fun isDuplicateDsn(dsn: String): Boolean {
        return scannedDsns.contains(dsn)
    }
    
    /**
     * Gets the next available battery slot
     */
    fun getNextAvailableBatterySlot(): String? {
        val batterySlots = listOf("battery01", "battery02", "battery03")
        return batterySlots.firstOrNull { slot ->
            !scannedComponents.containsKey(slot)
        }
    }
    
    /**
     * Gets available component slots for manual selection
     */
    fun getAvailableSlots(): List<ComponentSlot> {
        val allSlots = listOf(
            ComponentSlot("glasses", "Glasses", DsnValidator.ComponentType.GLASSES),
            ComponentSlot("controller", "Controller", DsnValidator.ComponentType.CONTROLLER),
            ComponentSlot("battery01", "Battery 01", DsnValidator.ComponentType.BATTERY_01),
            ComponentSlot("battery02", "Battery 02", DsnValidator.ComponentType.BATTERY_02),
            ComponentSlot("battery03", "Battery 03", DsnValidator.ComponentType.BATTERY_03),
            ComponentSlot("pads", "Pads", DsnValidator.ComponentType.PADS),
            ComponentSlot("unused01", "Unused 01", DsnValidator.ComponentType.UNUSED_01),
            ComponentSlot("unused02", "Unused 02", DsnValidator.ComponentType.UNUSED_02)
        )
        
        return allSlots.filter { slot ->
            !scannedComponents.containsKey(slot.id)
        }
    }
}

/**
 * Represents a component slot in the kit bundle
 */
@Parcelize
data class ComponentSlot(
    val id: String,
    val displayName: String,
    val componentType: DsnValidator.ComponentType
) : Parcelable
