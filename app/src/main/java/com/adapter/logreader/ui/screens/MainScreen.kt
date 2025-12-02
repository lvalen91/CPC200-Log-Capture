package com.adapter.logreader.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adapter.logreader.data.LogBuffer
import com.adapter.logreader.data.LogExporter
import com.adapter.logreader.data.LogRecorder
import com.adapter.logreader.ssh.ConnectionState
import com.adapter.logreader.ssh.SshLogReader
import com.adapter.logreader.ui.components.ControlButtons
import com.adapter.logreader.ui.components.LogList
import com.adapter.logreader.ui.components.StatusIndicator
import com.adapter.logreader.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    sshLogReader: SshLogReader,
    logBuffer: LogBuffer,
    logExporter: LogExporter,
    logRecorder: LogRecorder,
    onNavigateToSavedLogs: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val connectionState by sshLogReader.connectionState.collectAsState()
    val logs by logBuffer.logs.collectAsState()
    val logCount by logBuffer.entryCount.collectAsState()

    var isRecording by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    // Collect log lines from SSH reader and write to recorder if recording
    LaunchedEffect(Unit) {
        sshLogReader.logLines.collect { line ->
            logBuffer.addLine(line)
            // Write to file if recording
            if (isRecording) {
                logRecorder.writeLine(line)
            }
        }
    }

    // Document picker for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            val result = logExporter.saveToUri(it, logBuffer)
            if (result.isSuccess) {
                Toast.makeText(context, "Logs exported successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Export failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adapter Log Reader") },
                actions = {
                    IconButton(onClick = onNavigateToSavedLogs) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Saved Recordings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!autoScroll && logs.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        autoScroll = true
                        scope.launch {
                            listState.animateScrollToItem(logs.size - 1)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Status bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                StatusIndicator(state = connectionState)
                Text(
                    text = "$logCount lines",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Error message if any
            if (connectionState is ConnectionState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = (connectionState as ConnectionState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Control buttons
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ControlButtons(
                    connectionState = connectionState,
                    isRecording = isRecording,
                    onConnect = {
                        scope.launch {
                            sshLogReader.connect()
                        }
                    },
                    onDisconnect = {
                        if (isRecording) {
                            logRecorder.stopRecording()
                        }
                        sshLogReader.disconnect()
                        isRecording = false
                    },
                    onStartRecord = {
                        logBuffer.clear()
                        val file = logRecorder.startRecording()
                        if (file != null) {
                            isRecording = true
                            Toast.makeText(context, "Recording to: ${file.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onStopRecord = {
                        isRecording = false
                        val file = logRecorder.stopRecording()
                        if (file != null) {
                            Toast.makeText(context, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClearLogs = {
                        logBuffer.clear()
                    },
                    onExport = {
                        exportLauncher.launch(LogExporter.generateFileName())
                    }
                )
            }

            // Auto-scroll toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Checkbox(
                    checked = autoScroll,
                    onCheckedChange = { autoScroll = it }
                )
                Text(
                    text = "Auto-scroll",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Divider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Log display
            LogList(
                logs = logs,
                autoScroll = autoScroll,
                listState = listState,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
