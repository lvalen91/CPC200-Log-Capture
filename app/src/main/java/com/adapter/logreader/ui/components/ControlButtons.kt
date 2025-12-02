package com.adapter.logreader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adapter.logreader.ssh.ConnectionState

@Composable
fun ControlButtons(
    connectionState: ConnectionState,
    isRecording: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onClearLogs: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting ||
            connectionState is ConnectionState.Reconnecting

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Connect/Disconnect button
        if (isConnected || isConnecting) {
            Button(
                onClick = onDisconnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Disconnect")
            }
        } else {
            Button(
                onClick = onConnect,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Connect")
            }
        }

        // Record button
        if (isRecording) {
            FilledTonalButton(
                onClick = onStopRecord,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Stop")
            }
        } else {
            FilledTonalButton(
                onClick = onStartRecord,
                enabled = isConnected,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FiberManualRecord, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Record")
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Clear logs button
        OutlinedButton(
            onClick = onClearLogs,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Clear")
        }

        // Export button
        OutlinedButton(
            onClick = onExport,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Export")
        }
    }
}
