package com.adapter.logreader.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thread-safe circular buffer for log entries.
 * Maintains a fixed maximum size, removing oldest entries when full.
 */
class LogBuffer(private val maxSize: Int = 10000) {

    private val lock = ReentrantLock()
    private val entries = ArrayDeque<LogEntry>(maxSize)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _entryCount = MutableStateFlow(0)
    val entryCount: StateFlow<Int> = _entryCount.asStateFlow()

    /**
     * Add a new log entry to the buffer.
     */
    fun add(entry: LogEntry) {
        lock.withLock {
            if (entries.size >= maxSize) {
                entries.removeFirst()
            }
            entries.addLast(entry)
            _entryCount.value = entries.size
            _logs.value = entries.toList()
        }
    }

    /**
     * Add a raw log line (creates LogEntry automatically).
     */
    fun addLine(line: String) {
        add(LogEntry.create(line))
    }

    /**
     * Clear all log entries.
     */
    fun clear() {
        lock.withLock {
            entries.clear()
            _entryCount.value = 0
            _logs.value = emptyList()
        }
    }

    /**
     * Get all entries as a list.
     */
    fun getAll(): List<LogEntry> {
        return lock.withLock {
            entries.toList()
        }
    }

    /**
     * Get current entry count.
     */
    fun size(): Int = lock.withLock { entries.size }

    /**
     * Export all entries as formatted text.
     */
    fun exportAsText(): String {
        return lock.withLock {
            entries.joinToString("\n") { it.toExportLine() }
        }
    }
}
