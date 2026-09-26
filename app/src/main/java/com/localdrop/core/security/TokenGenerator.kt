package com.localdrop.core.security

import android.util.Base64
import com.localdrop.core.constants.SecurityConstants
import java.security.SecureRandom

/** Generates cryptographically secure, URL-safe random tokens. */
object TokenGenerator {

    private val secureRandom = SecureRandom()

    fun generateSessionToken(): String = generate(SecurityConstants.SESSION_TOKEN_BYTES)

    fun generateFileToken(): String = generate(SecurityConstants.FILE_TOKEN_BYTES)

    private fun generate(byteLength: Int): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
