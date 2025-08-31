package com.joeycarlson.qrscanner.data

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Data model representing a kit bundle with component associations.
 * 
 * A kit bundle is an immutable record of components assigned to a specific kit.
 * The kitId includes the base kit code and current date (MM/DD) for uniqueness.
 * 
 * @property kitId Unique identifier format: "K123-08/30"
 * @property baseKitCode Original kit code from QR scan (e.g., "K123")
 * @property creationDate Date in MM/DD format when bundle was created
 * @property glasses Component: Glasses QR code (nullable)
 * @property controller Component: Controller QR code (nullable)
 * @property battery01 Component: Battery 01 QR code (nullable)
 * @property battery02 Component: Battery 02 QR code (nullable)
 * @property battery03 Component: Battery 03 QR code (nullable)
 * @property pads Component: Pads QR code (nullable)
 * @property unused01 Component: Unused slot 01 (nullable)
 * @property unused02 Component: Unused slot 02 (nullable)
 * @property timestamp ISO-8601 timestamp of bundle creation
 */
data class KitBundle(
    @SerializedName("kitId")
    val kitId: String,
    
    @SerializedName("baseKitCode")
    val baseKitCode: String,
    
    @SerializedName("creationDate")
    val creationDate: String,
    
    @SerializedName("glasses")
    val glasses: String? = null,
    
    @SerializedName("controller")
    val controller: String? = null,
    
    @SerializedName("battery01")
    val battery01: String? = null,
    
    @SerializedName("battery02")
    val battery02: String? = null,
    
    @SerializedName("battery03")
    val battery03: String? = null,
    
    @SerializedName("pads")
    val pads: String? = null,
    
    @SerializedName("unused01")
    val unused01: String? = null,
    
    @SerializedName("unused02")
    val unused02: String? = null,
    
    @SerializedName("timestamp")
    val timestamp: String = Instant.now().toString()
) {
    
    /**
     * Gets a count of non-null components in the bundle
     */
    fun getFilledComponentCount(): Int {
        return listOfNotNull(
            glasses, controller, battery01, battery02, 
            battery03, pads, unused01, unused02
        ).count()
    }
    
    /**
     * Gets a list of all components with their field names
     */
    fun getComponentList(): List<Pair<String, String?>> {
        return listOf(
            "glasses" to glasses,
            "controller" to controller,
            "battery01" to battery01,
            "battery02" to battery02,
            "battery03" to battery03,
            "pads" to pads,
            "unused01" to unused01,
            "unused02" to unused02
        )
    }
    
    /**
     * Validates that at least one component is assigned
     */
    fun isValid(): Boolean {
        return getFilledComponentCount() > 0
    }
    
    companion object {
        /**
         * Creates a Kit ID from base kit code and current date
         * @param baseKitCode The original kit QR code (e.g., "K123")
         * @return Formatted kit ID (e.g., "K123-08/30")
         */
        fun generateKitId(baseKitCode: String): String {
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("MM/dd")
            return "$baseKitCode-${today.format(formatter)}"
        }
        
        /**
         * Extracts the creation date from a kit ID
         * @param kitId The full kit ID (e.g., "K123-08/30")
         * @return The date portion (e.g., "08/30")
         */
        fun extractCreationDate(kitId: String): String {
            return kitId.substringAfterLast("-", "")
        }
        
        /**
         * Extracts the base kit code from a kit ID
         * @param kitId The full kit ID (e.g., "K123-08/30")
         * @return The base kit code (e.g., "K123")
         */
        fun extractBaseKitCode(kitId: String): String {
            return kitId.substringBeforeLast("-", kitId)
        }
    }
}
