package com.joeycarlson.qrscanner.data

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

data class CheckoutRecord(
    @SerializedName("user")
    val userId: String,
    @SerializedName("kit")
    val kitId: String,
    @SerializedName("timestamp")
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())
)
