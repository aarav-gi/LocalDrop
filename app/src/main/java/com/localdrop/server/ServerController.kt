package com.localdrop.server

import android.content.Context
import android.util.Log
import com.localdrop.core.constants.NetworkConstants
import com.localdrop.core.network.NetworkInterfaceDetector
import com.localdrop.core.security.SessionManager
import com.localdrop.core.security.SharedFile
import java.io.IOException
import com.localdrop.service.SharingWakeLockManager

/**
 * Owns the LocalHttpServer's lifecycle: discovers a real local IP (never
 * assumes 192.168.43.1), probes forward from DEFAULT_PORT if it's busy,
 * starts the server, and produces the actual connection URL for the QR
 * code and "copy link" action.
 */
class ServerController(
    private val context: Context,
    private val sessionManager: SessionManager,
    val transferManager: TransferManager
) {
    private var server: LocalHttpServer? = null
    private val wakeLockManager = SharingWakeLockManager(context)
    var config: ServerConfig? = null
        private set

    var lastLogLine: String = ""
        private set

    /** Starts the server on the currently active local network, returns the resulting config. */
    fun start(files: List<SharedFile>): ServerConfig {
        stop() // ensure clean slate

        val detector = NetworkInterfaceDetector(context)
        wakeLockManager.acquire()
        val localAddress = detector.findLocalIpv4Address()
            ?: throw IllegalStateException("No usable local network address found")

        val session = sessionManager.startSession(files)

        var boundPort: Int? = null
        var lastError: IOException? = null
        for (attempt in 0 until NetworkConstants.PORT_PROBE_ATTEMPTS) {
            val candidatePort = NetworkConstants.DEFAULT_PORT + attempt
            try {
                val instance = LocalHttpServer(
                    context = context,
                    port = candidatePort,
                    sessionManager = sessionManager,
                    transferManager = transferManager,
                    onRequestLog = { line -> lastLogLine = line; Log.d(TAG, line) }
                )
                instance.start(fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                server = instance
                boundPort = candidatePort
                break
            } catch (e: IOException) {
                lastError = e // port busy or similar — try next
            }
        }

        if (boundPort == null) {
            sessionManager.stopSession()
            throw lastError ?: IllegalStateException("Unable to bind server to any port")
        }

        val newConfig = ServerConfig(
            ipAddress = localAddress.ipv4,
            port = boundPort,
            sessionToken = session.sessionToken
        )
        config = newConfig
        return newConfig
    }

    fun stop() {
        server?.stop()
        server = null
        wakeLockManager.release()
        sessionManager.stopSession()
        transferManager.clear()
        config = null
    }

    fun isRunning(): Boolean = server?.isAlive == true

    companion object {
        private const val TAG = "LocalDrop.Server"
    }
}
