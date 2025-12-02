package com.adapter.logreader.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles exporting logs to files.
 */
class LogExporter(private val context: Context) {

    companion object {
        private const val MAX_FILE_COUNT = 20
        private val fileNameFormat = SimpleDateFormat("HHmmss_ddMMMyy", Locale.US)

        fun generateFileName(): String {
            val timestamp = fileNameFormat.format(Date()).uppercase()
            return "adapter_tty_$timestamp.log"
        }
    }

    private val logsDir: File
        get() = File(context.filesDir, "logs").apply { mkdirs() }

    /**
     * Save logs to internal app storage.
     * Returns the file path on success.
     * Enforces max file count limit.
     */
    fun saveToInternal(logBuffer: LogBuffer): Result<File> {
        return try {
            enforceFileCountLimit()
            val file = File(logsDir, generateFileName())

            FileOutputStream(file).use { stream ->
                writeHeader(stream)
                stream.write(logBuffer.exportAsText().toByteArray())
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Enforce max file count by deleting oldest files.
     */
    private fun enforceFileCountLimit() {
        val files = logsDir.listFiles()?.sortedBy { it.lastModified() } ?: return
        val filesToDelete = files.size - MAX_FILE_COUNT + 1
        if (filesToDelete > 0) {
            files.take(filesToDelete).forEach { it.delete() }
        }
    }

    /**
     * Save logs to a user-selected URI (via SAF/Document picker).
     */
    fun saveToUri(uri: Uri, logBuffer: LogBuffer): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                writeHeader(stream)
                stream.write(logBuffer.exportAsText().toByteArray())
            } ?: return Result.failure(Exception("Could not open output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun writeHeader(stream: OutputStream) {
        val header = buildString {
            appendLine("=".repeat(60))
            appendLine("Adapter TTY Log Export")
            appendLine("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine("=".repeat(60))
            appendLine()
        }
        stream.write(header.toByteArray())
    }

    /**
     * Get list of saved log files.
     */
    fun getSavedLogs(): List<File> {
        return logsDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Delete a saved log file.
     */
    fun deleteSavedLog(file: File): Boolean {
        return file.delete()
    }

    /**
     * Export a saved log file to a user-selected URI.
     */
    fun exportSavedLog(file: File, uri: Uri): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Result.failure(Exception("Could not open output stream"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Read contents of a saved log file.
     */
    fun readSavedLog(file: File): Result<String> {
        return try {
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
