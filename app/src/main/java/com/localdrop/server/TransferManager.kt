package com.localdrop.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry of all in-flight/completed TransferSessions, one per
 * receiver+file. The HTTP server's DownloadRoute reports progress into
 * this manager from its worker thread; the UI observes [sessions] as a
 * StateFlow to show live speed/ETA per receiver without polling.
 */
class TransferManager {

    private val _sessions = MutableStateFlow<Map<String, TransferSession>>(emptyMap())
    val sessions: StateFlow<Map<String, TransferSession>> = _sessions

    // transferId -> (lastSampleTimeMillis, lastSampleBytes) for instantaneous speed
    private val speedSamples = ConcurrentHashMap<String, Pair<Long, Long>>()

    fun beginTransfer(
        fileToken: String,
        fileName: String,
        totalBytes: Long,
        remoteAddress: String,
        resumeFromBytes: Long
    ): String {
        val id = UUID.randomUUID().toString()
        val session = TransferSession(
            transferId = id,
            fileToken = fileToken,
            fileName = fileName,
            totalBytes = totalBytes,
            remoteAddress = remoteAddress,
            startTimeMillis = System.currentTimeMillis(),
            transferredBytes = resumeFromBytes,
            resumedFromBytes = resumeFromBytes,
            state = TransferState.TRANSFERRING
        )
        speedSamples[id] = System.currentTimeMillis() to resumeFromBytes
        _sessions.update { it + (id to session) }
        return id
    }

    fun updateProgress(transferId: String, transferredBytes: Long) {
        _sessions.update { map ->
            val existing = map[transferId] ?: return@update map
            map + (transferId to existing.copy(transferredBytes = transferredBytes, state = TransferState.TRANSFERRING))
        }
    }

    fun currentSpeedBytesPerSecond(transferId: String, transferredBytesNow: Long): Double {
        val prev = speedSamples[transferId]
        val now = System.currentTimeMillis()
        speedSamples[transferId] = now to transferredBytesNow
        if (prev == null) return 0.0
        val (prevTime, prevBytes) = prev
        val dtSeconds = (now - prevTime) / 1000.0
        if (dtSeconds <= 0) return 0.0
        return ((transferredBytesNow - prevBytes) / dtSeconds).coerceAtLeast(0.0)
    }

    fun completeTransfer(transferId: String) = setState(transferId, TransferState.COMPLETED)
    fun failTransfer(transferId: String) = setState(transferId, TransferState.FAILED)
    fun cancelTransfer(transferId: String) = setState(transferId, TransferState.CANCELLED)
    fun disconnectTransfer(transferId: String) = setState(transferId, TransferState.DISCONNECTED)

    private fun setState(transferId: String, state: TransferState) {
        _sessions.update { map ->
            val existing = map[transferId] ?: return@update map
            map + (transferId to existing.copy(state = state))
        }
        speedSamples.remove(transferId)
    }

    fun activeReceiverCount(): Int =
        _sessions.value.values.count { it.state == TransferState.TRANSFERRING }

    fun clear() {
        _sessions.value = emptyMap()
        speedSamples.clear()
    }
}
