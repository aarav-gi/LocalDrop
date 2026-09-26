package com.localdrop.server

enum class TransferState {
    QUEUED, STARTING, CONNECTING, TRANSFERRING, PAUSED, RESUMING,
    COMPLETED, CANCELLED, FAILED, DISCONNECTED
}

/** One independent transfer: one receiver downloading one file. */
data class TransferSession(
    val transferId: String,
    val fileToken: String,
    val fileName: String,
    val totalBytes: Long,
    val remoteAddress: String,
    val startTimeMillis: Long,
    val transferredBytes: Long = 0,
    val state: TransferState = TransferState.QUEUED,
    val resumedFromBytes: Long = 0
) {
    val progressFraction: Float
        get() = if (totalBytes <= 0) 0f else (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}
