package com.adapter.logreader.ssh

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Manages SSH connection to the adapter and streams log data.
 */
class SshLogReader {

    companion object {
        private const val TAG = "SshLogReader"
    }

    private var session: Session? = null
    private var channel: ChannelExec? = null
    private var readerJob: Job? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _logLines = MutableSharedFlow<String>(extraBufferCapacity = 1000)
    val logLines: SharedFlow<String> = _logLines.asSharedFlow()

    @Volatile
    private var shouldReconnect = false

    @Volatile
    private var isManualDisconnect = false

    /**
     * Start connection and begin streaming logs.
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        Log.d(TAG, "connect() called")
        isManualDisconnect = false
        shouldReconnect = true
        connectWithRetry(isReconnect = false)
    }

    private suspend fun connectWithRetry(isReconnect: Boolean) {
        var attempt = 0

        while (attempt < SshConfig.MAX_RETRY_ATTEMPTS && shouldReconnect) {
            attempt++
            Log.d(TAG, "Connection attempt $attempt/${SshConfig.MAX_RETRY_ATTEMPTS} (reconnect=$isReconnect)")

            _connectionState.value = if (isReconnect) {
                ConnectionState.Reconnecting(attempt, SshConfig.MAX_RETRY_ATTEMPTS)
            } else {
                ConnectionState.Connecting(attempt, SshConfig.MAX_RETRY_ATTEMPTS)
            }

            try {
                establishConnection()
                Log.i(TAG, "Connection established successfully")
                _connectionState.value = ConnectionState.Connected
                startReading()
                return
            } catch (e: Exception) {
                Log.e(TAG, "Connection attempt $attempt failed: ${e.javaClass.simpleName}: ${e.message}")
                if (attempt >= SshConfig.MAX_RETRY_ATTEMPTS) {
                    val errorMsg = "Failed after $attempt attempts: ${e.message}"
                    Log.e(TAG, errorMsg)
                    _connectionState.value = ConnectionState.Error(errorMsg)
                    return
                }
                delay(SshConfig.RETRY_INTERVAL_MS)
            }
        }
    }

    private fun establishConnection() {
        Log.d(TAG, "Establishing SSH connection to ${SshConfig.USER}@${SshConfig.HOST}:${SshConfig.PORT}")
        val jsch = JSch()

        session = jsch.getSession(SshConfig.USER, SshConfig.HOST, SshConfig.PORT).apply {
            setPassword(SshConfig.PASSWORD)
            setConfig("StrictHostKeyChecking", "no")
            setConfig("PreferredAuthentications", "password,keyboard-interactive")
            Log.d(TAG, "Connecting session (timeout=${SshConfig.CONNECT_TIMEOUT_MS}ms)...")
            connect(SshConfig.CONNECT_TIMEOUT_MS)
        }
        Log.d(TAG, "Session connected, opening exec channel...")

        channel = (session?.openChannel("exec") as ChannelExec).apply {
            val cmd = "tail -f ${SshConfig.REMOTE_FILE}"
            Log.d(TAG, "Executing command: $cmd")
            setCommand(cmd)
            inputStream = null
            setErrStream(System.err)
            connect(SshConfig.CONNECT_TIMEOUT_MS)
        }
        Log.d(TAG, "Channel connected, ready to read")
    }

    private suspend fun startReading() = withContext(Dispatchers.IO) {
        readerJob = launch {
            try {
                val inputStream = channel?.inputStream
                if (inputStream == null) {
                    Log.e(TAG, "Channel inputStream is null!")
                    return@launch
                }
                val reader = BufferedReader(InputStreamReader(inputStream))
                Log.d(TAG, "Started reading from stream")

                var lineCount = 0
                var line: String?
                while (isActive && shouldReconnect) {
                    line = reader.readLine()
                    if (line != null) {
                        lineCount++
                        if (lineCount <= 5 || lineCount % 100 == 0) {
                            Log.d(TAG, "Received line #$lineCount: ${line.take(50)}...")
                        }
                        _logLines.emit(line)
                    } else {
                        Log.w(TAG, "Stream ended (null line received after $lineCount lines)")
                        break
                    }
                }
                Log.d(TAG, "Reading loop ended, total lines: $lineCount")
            } catch (e: Exception) {
                Log.e(TAG, "Error reading stream: ${e.javaClass.simpleName}: ${e.message}", e)
            } finally {
                if (shouldReconnect && !isManualDisconnect) {
                    Log.d(TAG, "Will attempt reconnect...")
                    handleDisconnect()
                }
            }
        }
        readerJob?.join()
    }

    private suspend fun handleDisconnect() {
        Log.d(TAG, "handleDisconnect called")
        cleanup()
        if (shouldReconnect && !isManualDisconnect) {
            Log.d(TAG, "Waiting ${SshConfig.RETRY_INTERVAL_MS}ms before reconnecting...")
            delay(SshConfig.RETRY_INTERVAL_MS)
            connectWithRetry(isReconnect = true)
        }
    }

    /**
     * Manually disconnect and stop reconnection attempts.
     */
    fun disconnect() {
        Log.d(TAG, "disconnect() called (manual)")
        isManualDisconnect = true
        shouldReconnect = false
        readerJob?.cancel()
        cleanup()
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun cleanup() {
        Log.d(TAG, "Cleaning up session and channel")
        try {
            channel?.disconnect()
            session?.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup error (ignored): ${e.message}")
        }
        channel = null
        session = null
    }

    /**
     * Check if currently connected.
     */
    fun isConnected(): Boolean {
        return session?.isConnected == true && channel?.isConnected == true
    }
}
