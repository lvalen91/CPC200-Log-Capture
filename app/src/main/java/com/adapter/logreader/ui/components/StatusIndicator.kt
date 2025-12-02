package com.adapter.logreader.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.adapter.logreader.ssh.ConnectionState
import com.adapter.logreader.ui.theme.*

@Composable
fun StatusIndicator(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (state) {
        is ConnectionState.Disconnected -> StatusDisconnected to "Disconnected"
        is ConnectionState.Connecting -> StatusConnecting to "Connecting (${state.attempt}/${state.maxAttempts})"
        is ConnectionState.Connected -> StatusConnected to "Connected"
        is ConnectionState.Reconnecting -> StatusConnecting to "Reconnecting (${state.attempt}/${state.maxAttempts})"
        is ConnectionState.Error -> StatusError to "Error"
    }

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(300),
        label = "status_color"
    )

    // Pulsing animation for connecting states
    val alpha by if (state is ConnectionState.Connecting || state is ConnectionState.Reconnecting) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    } else {
        animateFloatAsState(targetValue = 1f, label = "static_alpha")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(animatedColor.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
