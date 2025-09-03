package com.joeycarlson.qrscanner.data

import com.google.gson.annotations.SerializedName
import java.time.Instant

/**
 * Data model representing a kit check-in record.
 * Unlike CheckoutRecord, this only requires a kit ID (no user ID needed).
 */
data class CheckInRecord(
    @SerializedName("kit")
    val kitId: String,
    @SerializedName("type")
    val type: String = "CHECKIN",
    @SerializedName("value")
    val value: String,
    @SerializedName("timestamp")
    val timestamp: String = Instant.now().toString()
)
