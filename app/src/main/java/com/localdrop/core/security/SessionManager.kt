package com.localdrop.core.security

import android.net.Uri
import com.localdrop.core.constants.AppConstants
import java.util.concurrent.ConcurrentHashMap

data class SharedFile(
    val fileToken: String,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String
)

data class ActiveSession(
    val sessionToken: String,
    val createdAtMillis: Long,
    val files: Map<String, SharedFile> // keyed by fileToken
)

/**
 * Owns the single active sharing session: the session token, the set of
 * files authorized for this session (each behind its own per-file token —
 * never a raw filesystem path), and expiry. Thread-safe since the HTTP
 * server calls into it from NanoHTTPD's worker threads.
 */
class SessionManager {

    @Volatile
    private var session: ActiveSession? = null

    private val lastActivityMillis = ConcurrentHashMap<String, Long>()

    fun startSession(files: List<SharedFile>): ActiveSession {
        val token = TokenGenerator.generateSessionToken()
        val newSession = ActiveSession(
            sessionToken = token,
            createdAtMillis = System.currentTimeMillis(),
            files = files.associateBy { it.fileToken }
        )
        session = newSession
        lastActivityMillis[token] = System.currentTimeMillis()
        return newSession
    }

    fun currentSession(): ActiveSession? = session

    fun stopSession() {
        session = null
        lastActivityMillis.clear()
    }

    /** Validates a session token, checking expiry due to inactivity. */
    fun validateSession(sessionToken: String): Boolean {
        val active = session ?: return false
        if (active.sessionToken != sessionToken) return false
        val last = lastActivityMillis[sessionToken] ?: return false
        if (System.currentTimeMillis() - last > AppConstants.SESSION_TIMEOUT_MILLIS) {
            stopSession()
            return false
        }
        lastActivityMillis[sessionToken] = System.currentTimeMillis()
        return true
    }

    /** Resolves a file token to its authorized SharedFile, or null if unknown/expired. */
    fun resolveFile(sessionToken: String?, fileToken: String): SharedFile? {
        val s = session ?: return null
        if (sessionToken != null && !validateSession(sessionToken)) return null
        return s.files[fileToken]
    }

    fun getFileByToken(fileToken: String): SharedFile? {
        return session?.files?.get(fileToken)
    }

    fun isActive(): Boolean = session != null
}
