package com.adapter.logreader.ssh

/**
 * Represents the current state of the SSH connection.
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val attempt: Int, val maxAttempts: Int) : ConnectionState()
    data object Connected : ConnectionState()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
