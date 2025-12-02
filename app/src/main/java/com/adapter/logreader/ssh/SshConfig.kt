package com.adapter.logreader.ssh

/**
 * SSH connection configuration for the adapter.
 */
object SshConfig {
    const val HOST = "192.168.43.1"
    const val PORT = 22
    const val USER = "root"
    const val PASSWORD = ""
    const val REMOTE_FILE = "/tmp/ttyLog"

    const val CONNECT_TIMEOUT_MS = 2000  // Don't change unless actual SSH session issues occur. 
    const val MAX_RETRY_ATTEMPTS = 10
    const val RETRY_INTERVAL_MS = 2000L
}
