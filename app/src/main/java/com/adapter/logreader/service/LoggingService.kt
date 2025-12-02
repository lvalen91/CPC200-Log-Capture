package com.adapter.logreader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.adapter.logreader.MainActivity
import com.adapter.logreader.R
import com.adapter.logreader.data.LogBuffer
import com.adapter.logreader.ssh.SshLogReader
import kotlinx.coroutines.*

/**
 * Foreground service that keeps SSH log capture running in the background.
 */
class LoggingService : Service() {

    companion object {
        const val CHANNEL_ID = "adapter_log_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.adapter.logreader.START"
        const val ACTION_STOP = "com.adapter.logreader.STOP"
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val sshLogReader = SshLogReader()
    val logBuffer = LogBuffer()

    private var logCollectionJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): LoggingService = this@LoggingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCapture()
            ACTION_STOP -> stopCapture()
        }
        return START_STICKY
    }

    private fun startCapture() {
        startForeground(NOTIFICATION_ID, createNotification())

        // Start collecting logs into buffer
        logCollectionJob = serviceScope.launch {
            sshLogReader.logLines.collect { line ->
                logBuffer.addLine(line)
            }
        }

        // Start SSH connection
        serviceScope.launch {
            sshLogReader.connect()
        }
    }

    private fun stopCapture() {
        sshLogReader.disconnect()
        logCollectionJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, LoggingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        sshLogReader.disconnect()
        super.onDestroy()
    }
}
