package com.localdrop.server

import android.content.Context
import com.localdrop.core.constants.NetworkConstants
import com.localdrop.core.security.AccessController
import com.localdrop.core.security.SessionManager
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class LocalHttpServer(
    private val context: Context,
    port: Int,
    private val sessionManager: SessionManager,
    private val transferManager: TransferManager,
    private val onRequestLog: (String) -> Unit
) : NanoHTTPD(port) {

    private val fileStreamer = FileStreamer(context)
    private val rateLimiter = RateLimiter()

    private object TooManyRequestsStatus : Response.IStatus {
        override fun getRequestStatus(): Int = 429
        override fun getDescription(): String = "429 Too Many Requests"
    }

    override fun serve(session: IHTTPSession): Response {
        val remoteAddress = session.headers["remote-addr"] ?: session.headers["http-client-ip"] ?: "unknown"

        if (!rateLimiter.allowRequest(remoteAddress)) {
            val json = JSONObject().put("error", "Rate limit exceeded")
            return newFixedLengthResponse(TooManyRequestsStatus, "application/json", json.toString())
        }

        onRequestLog("${session.method} ${session.uri} from $remoteAddress")

        val uri = session.uri
        return try {
            when {
                uri == NetworkConstants.ROUTE_HEALTH -> handleHealth()
                uri == NetworkConstants.ROUTE_SESSION -> handleSessionInfo(session)
                uri == NetworkConstants.ROUTE_FILES -> handleFileList(session)
                uri.startsWith(NetworkConstants.ROUTE_DOWNLOAD_PREFIX) -> handleDownload(session, remoteAddress)
                uri.startsWith("/s/") -> serveWebUi()
                uri == "/" || uri == "/index.html" -> handleRootRedirect()
                else -> handleRootRedirect()
            }
        } catch (e: Exception) {
            jsonError(Response.Status.INTERNAL_ERROR, "Server error: ${e.message}")
        }
    }

    private fun handleRootRedirect(): Response {
        val current = sessionManager.currentSession()
        return if (current != null) {
            val redirect = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "")
            redirect.addHeader("Location", "/s/${current.sessionToken}")
            redirect
        } else {
            serveWebUi()
        }
    }

    private fun handleHealth(): Response {
        val json = JSONObject().put("status", "ok").put("activeReceivers", transferManager.activeReceiverCount())
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
    }

    private fun handleSessionInfo(session: IHTTPSession): Response {
        val token = session.parms["s"]
        if (!AccessController.isPlausibleToken(token) || !sessionManager.validateSession(token!!)) {
            return jsonError(Response.Status.UNAUTHORIZED, "Invalid or expired session")
        }
        val active = sessionManager.currentSession()!!
        val json = JSONObject()
            .put("valid", true)
            .put("fileCount", active.files.size)
            .put("createdAt", active.createdAtMillis)
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
    }

    private fun handleFileList(session: IHTTPSession): Response {
        val token = session.parms["s"]
        val activeSession = sessionManager.currentSession()
        if (activeSession == null || (token != null && !sessionManager.validateSession(token))) {
            return jsonError(Response.Status.UNAUTHORIZED, "Invalid session")
        }
        val list = JSONArray()
        for (f in activeSession.files) {
            val item = JSONObject()
                .put("token", f.fileToken)
                .put("name", f.displayName)
                .put("size", f.sizeBytes)
                .put("mime", f.mimeType)
            list.put(item)
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", list.toString())
    }

    private fun handleDownload(session: IHTTPSession, remoteAddress: String): Response {
        val token = session.parms["s"]
        val fileToken = session.uri.removePrefix(NetworkConstants.ROUTE_DOWNLOAD_PREFIX)
        val activeSession = sessionManager.currentSession()
        if (activeSession == null || (token != null && !sessionManager.validateSession(token))) {
            return jsonError(Response.Status.UNAUTHORIZED, "Invalid session")
        }

        val file = activeSession.files.firstOrNull { it.fileToken == fileToken }
            ?: return jsonError(Response.Status.NOT_FOUND, "File not found")

        transferManager.onTransferStarted(remoteAddress, file.displayName, file.sizeBytes)
        val response = fileStreamer.stream(session, file)
        return response
    }

    private fun serveWebUi(): Response {
        return try {
            val html = context.assets.open("web/index.html").bufferedReader().use { it.readText() }
            newFixedLengthResponse(Response.Status.OK, "text/html", html)
        } catch (e: Exception) {
            jsonError(Response.Status.INTERNAL_ERROR, "Cannot load web assets: ${e.message}")
        }
    }

    private fun jsonError(status: Response.Status, message: String): Response {
        val json = JSONObject().put("error", message)
        return newFixedLengthResponse(status, "application/json", json.toString())
    }
}
