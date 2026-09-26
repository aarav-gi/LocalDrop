package com.localdrop.core.constants

object SecurityConstants {
    const val SESSION_TOKEN_BYTES = 32 // 256-bit, base64url-encoded
    const val FILE_TOKEN_BYTES = 24    // 192-bit per-file token
}
