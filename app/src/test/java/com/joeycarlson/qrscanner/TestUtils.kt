package com.joeycarlson.qrscanner

import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Base test utilities and helpers for unit tests
 */

/**
 * JUnit rule for logging test execution
 */
class TestLogger : TestWatcher() {
    override fun starting(description: Description) {
        println("▶ Starting: ${description.methodName}")
    }

    override fun succeeded(description: Description) {
        println("✓ Passed: ${description.methodName}")
    }

    override fun failed(e: Throwable, description: Description) {
        println("✗ Failed: ${description.methodName} - ${e.message}")
    }
}

/**
 * Assertion helpers for common test scenarios
 */
object TestAssertions {
    fun assertValidJson(json: String) {
        require(json.trim().startsWith("{") || json.trim().startsWith("[")) {
            "Invalid JSON format"
        }
    }
    
    fun assertValidTimestamp(timestamp: Long) {
        require(timestamp > 0) { "Timestamp must be positive" }
        require(timestamp <= System.currentTimeMillis()) { "Timestamp cannot be in the future" }
    }
}
