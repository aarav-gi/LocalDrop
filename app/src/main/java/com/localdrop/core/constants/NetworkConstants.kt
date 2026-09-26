package com.localdrop.core.constants

object NetworkConstants {
    // Starting port to try; the server will probe forward if this is busy.
    // Deliberately NOT hard-coded as the only option — see ServerController.
    const val DEFAULT_PORT = 8080
    const val PORT_PROBE_ATTEMPTS = 20

    const val ROUTE_INDEX = "/"
    const val ROUTE_SESSION = "/api/session"
    const val ROUTE_FILES = "/api/files"
    const val ROUTE_DOWNLOAD_PREFIX = "/api/download/"
    const val ROUTE_HEALTH = "/api/health"
}
