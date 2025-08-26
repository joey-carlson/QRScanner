package com.joeycarlson.qrscanner.data

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CheckoutRecord(
    @SerializedName("user")
    val userId: String,
    @SerializedName("kit")
    val kitId: String,
    @SerializedName("date")
    val date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    @SerializedName("timestamp")
    val timestamp: String = Instant.now().toString()
)
