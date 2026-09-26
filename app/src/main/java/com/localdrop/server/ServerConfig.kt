package com.localdrop.server

data class ServerConfig(
    val ipAddress: String,
    val port: Int,
    val sessionToken: String
) {
    /** e.g. http://192.168.49.1:8080/s/AbCd123... — always built at runtime, never hard-coded. */
    fun connectionUrl(): String = "http://$ipAddress:$port/s/$sessionToken"
}
