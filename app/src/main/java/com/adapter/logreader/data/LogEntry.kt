package com.adapter.logreader.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Represents a single log entry with timestamp and unique ID.
 */
data class LogEntry(
    val id: Long,
    val timestamp: Long,
    val line: String
) {
    companion object {
        private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        private val idCounter = AtomicLong(0)

        fun create(line: String): LogEntry {
            return LogEntry(
                id = idCounter.incrementAndGet(),
                timestamp = System.currentTimeMillis(),
                line = line
            )
        }
    }

    fun formattedTime(): String = timeFormat.format(Date(timestamp))

    fun toExportLine(): String = "[${formattedTime()}] $line"
}
