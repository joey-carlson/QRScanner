package com.joeycarlson.qrscanner.inventory

/**
 * Enum representing the types of components that can be inventoried.
 * Simplified to just three main component types as per requirements.
 */
enum class ComponentType(val displayName: String) {
    GLASSES("Glasses"),
    CONTROLLER("Controller"),
    BATTERY("Battery");
    
    companion object {
        /**
         * Get ComponentType from its string value
         */
        fun fromString(value: String): ComponentType? {
            return values().find { it.name == value }
        }
    }
}
