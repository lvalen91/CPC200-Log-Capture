package com.adapter.logreader.data

import android.content.Context
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles streaming log recording with file rotation.
 * - Max file size: 10MB
 * - Max file count: 20 (oldest deleted when exceeded)
 */
class LogRecorder(private val context: Context) {

    companion object {
        private const val TAG = "LogRecorder"
        private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L  // 10MB
        private const val MAX_FILE_COUNT = 20
        private val fileNameFormat = SimpleDateFormat("HHmmss_ddMMMyy", Locale.US)
    }

    private var currentFile: File? = null
    private var currentWriter: BufferedWriter? = null
    private var currentFileSize: Long = 0
    private var isRecording = false

    private val logsDir: File
        get() = File(context.filesDir, "logs").apply { mkdirs() }

    /**
     * Start a new recording session.
     */
    @Synchronized
    fun startRecording(): File? {
        if (isRecording) {
            Log.w(TAG, "Already recording, stopping previous session")
            stopRecording()
        }

        return try {
            enforceFileCountLimit()
            val file = createNewFile()
            currentFile = file
            currentWriter = BufferedWriter(FileWriter(file, true))
            currentFileSize = 0
            isRecording = true

            // Write header
            val header = buildHeader()
            currentWriter?.write(header)
            currentWriter?.flush()
            currentFileSize += header.toByteArray().size

            Log.d(TAG, "Started recording to: ${file.name}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            null
        }
    }

    /**
     * Write a log line to the current recording.
     * Automatically rotates to a new file if size limit is reached.
     */
    @Synchronized
    fun writeLine(line: String) {
        if (!isRecording || currentWriter == null) return

        try {
            val formattedLine = "[${formatTime()}] $line\n"
            val lineBytes = formattedLine.toByteArray().size

            // Check if we need to rotate
            if (currentFileSize + lineBytes > MAX_FILE_SIZE_BYTES) {
                Log.d(TAG, "File size limit reached (${currentFileSize / 1024}KB), rotating...")
                rotateFile()
            }

            currentWriter?.write(formattedLine)
            currentFileSize += lineBytes

            // Flush periodically (every ~100KB)
            if (currentFileSize % (100 * 1024) < lineBytes) {
                currentWriter?.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing line: ${e.message}")
        }
    }

    /**
     * Stop the current recording session.
     */
    @Synchronized
    fun stopRecording(): File? {
        if (!isRecording) return null

        val file = currentFile
        try {
            currentWriter?.flush()
            currentWriter?.close()
            Log.d(TAG, "Stopped recording: ${file?.name} (${currentFileSize / 1024}KB)")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing writer: ${e.message}")
        }

        currentWriter = null
        currentFile = null
        currentFileSize = 0
        isRecording = false

        return file
    }

    /**
     * Check if currently recording.
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Get current file being recorded to.
     */
    fun getCurrentFile(): File? = currentFile

    /**
     * Get current file size in bytes.
     */
    fun getCurrentFileSize(): Long = currentFileSize

    private fun rotateFile() {
        try {
            // Close current file
            currentWriter?.flush()
            currentWriter?.close()
            Log.d(TAG, "Closed file: ${currentFile?.name}")

            // Enforce file count before creating new
            enforceFileCountLimit()

            // Create new file
            val newFile = createNewFile()
            currentFile = newFile
            currentWriter = BufferedWriter(FileWriter(newFile, true))
            currentFileSize = 0

            // Write header to new file
            val header = buildHeader()
            currentWriter?.write(header)
            currentWriter?.flush()
            currentFileSize += header.toByteArray().size

            Log.d(TAG, "Rotated to new file: ${newFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating file: ${e.message}", e)
        }
    }

    private fun createNewFile(): File {
        val timestamp = fileNameFormat.format(Date()).uppercase()
        return File(logsDir, "adapter_tty_$timestamp.log")
    }

    private fun enforceFileCountLimit() {
        val files = logsDir.listFiles()?.sortedBy { it.lastModified() } ?: return

        // Delete oldest files if we're at or over the limit
        val filesToDelete = files.size - MAX_FILE_COUNT + 1  // +1 to make room for new file
        if (filesToDelete > 0) {
            files.take(filesToDelete).forEach { file ->
                if (file.delete()) {
                    Log.d(TAG, "Deleted old file: ${file.name}")
                }
            }
        }
    }

    private fun buildHeader(): String {
        return buildString {
            appendLine("=".repeat(60))
            appendLine("Adapter TTY Log")
            appendLine("Started: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine("=".repeat(60))
            appendLine()
        }
    }

    private fun formatTime(): String {
        return SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
    }
}
