package com.adapter.logreader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.adapter.logreader.data.LogBuffer
import com.adapter.logreader.data.LogExporter
import com.adapter.logreader.data.LogRecorder
import com.adapter.logreader.ssh.SshLogReader
import com.adapter.logreader.ui.screens.LogViewerScreen
import com.adapter.logreader.ui.screens.MainScreen
import com.adapter.logreader.ui.screens.SavedLogsScreen
import com.adapter.logreader.ui.theme.AdapterLogReaderTheme
import java.io.File

sealed class Screen {
    data object Main : Screen()
    data object SavedLogs : Screen()
    data class LogViewer(val file: File) : Screen()
}

class MainActivity : ComponentActivity() {

    private val sshLogReader = SshLogReader()
    private val logBuffer = LogBuffer()
    private lateinit var logExporter: LogExporter
    private lateinit var logRecorder: LogRecorder

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logExporter = LogExporter(applicationContext)
        logRecorder = LogRecorder(applicationContext)

        // Request notification permission on Android 13+
        requestNotificationPermissionIfNeeded()

        enableEdgeToEdge()

        setContent {
            AdapterLogReaderTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val screen = currentScreen) {
                        is Screen.Main -> MainScreen(
                            sshLogReader = sshLogReader,
                            logBuffer = logBuffer,
                            logExporter = logExporter,
                            logRecorder = logRecorder,
                            onNavigateToSavedLogs = { currentScreen = Screen.SavedLogs }
                        )
                        is Screen.SavedLogs -> SavedLogsScreen(
                            logExporter = logExporter,
                            onBack = { currentScreen = Screen.Main },
                            onViewLog = { file -> currentScreen = Screen.LogViewer(file) }
                        )
                        is Screen.LogViewer -> LogViewerScreen(
                            file = screen.file,
                            logExporter = logExporter,
                            onBack = { currentScreen = Screen.SavedLogs }
                        )
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        logRecorder.stopRecording()
        sshLogReader.disconnect()
    }
}
