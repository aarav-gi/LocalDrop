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
) : NanoHTTPD("0.0.0.0", port) {

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
                uri.startsWith("/s/") || uri == "/" || uri == "/index.html" || uri == NetworkConstants.ROUTE_INDEX -> serveWebUi()
                else -> serveWebUi()
            }
        } catch (e: Exception) {
            jsonError(Response.Status.INTERNAL_ERROR, "Server error: ${e.message}")
        }
    }

    private fun handleHealth(): Response {
        val json = JSONObject().put("status", "ok").put("activeReceivers", transferManager.activeReceiverCount())
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
    }

    private fun handleSessionInfo(session: IHTTPSession): Response {
        val active = sessionManager.currentSession()
            ?: return jsonError(Response.Status.NOT_FOUND, "No active session")

        val json = JSONObject()
            .put("valid", true)
            .put("sessionToken", active.sessionToken)
            .put("fileCount", active.files.size)
            .put("createdAt", active.createdAtMillis)
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
    }

    private fun handleFileList(session: IHTTPSession): Response {
        val activeSession = sessionManager.currentSession()
            ?: return jsonError(Response.Status.NOT_FOUND, "No active sharing session")

        val token = session.parms["s"]
        if (token != null && !sessionManager.validateSession(token)) {
            return jsonError(Response.Status.UNAUTHORIZED, "Invalid session")
        }

        val array = JSONArray()
        activeSession.files.values.forEach { f ->
            array.put(
                JSONObject()
                    .put("token", f.fileToken)
                    .put("name", f.displayName)
                    .put("size", f.sizeBytes)
                    .put("mime", f.mimeType)
            )
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", array.toString())
    }

    private fun handleDownload(session: IHTTPSession, remoteAddress: String): Response {
        val activeSession = sessionManager.currentSession()
            ?: return jsonError(Response.Status.NOT_FOUND, "No active session")

        val fileToken = session.uri.removePrefix(NetworkConstants.ROUTE_DOWNLOAD_PREFIX)
        val file = sessionManager.getFileByToken(fileToken) 
            ?: activeSession.files[fileToken]
            ?: return jsonError(Response.Status.NOT_FOUND, "File not found")

        val rangeHeader = session.headers["range"]
        val parsedRange = RangeHeader.parse(rangeHeader, file.sizeBytes)
        val startByte = parsedRange?.start ?: 0
        val endByte = parsedRange?.endInclusive

        if (startByte < 0 || startByte >= file.sizeBytes) {
            val resp = newFixedLengthResponse(
                Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "Requested range not satisfiable"
            )
            resp.addHeader("Content-Range", "bytes */${file.sizeBytes}")
            return resp
        }

        val opened = try {
            fileStreamer.openRange(file.uri, file.sizeBytes, startByte, endByte)
        } catch (e: IOException) {
            return jsonError(Response.Status.INTERNAL_ERROR, "Cannot read file: ${e.message}")
        }

        transferManager.beginTransfer(file.fileToken, file.displayName, file.sizeBytes, remoteAddress, startByte)

        val status = if (parsedRange != null) Response.Status.PARTIAL_CONTENT else Response.Status.OK
        val response = newFixedLengthResponse(status, file.mimeType, opened.stream, opened.lengthToServe)
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Content-Disposition", "attachment; filename=\"${file.displayName}\"")

        if (parsedRange != null) {
            val actualEnd = startByte + opened.lengthToServe - 1
            response.addHeader("Content-Range", "bytes $startByte-$actualEnd/${file.sizeBytes}")
        }

        return response
    }

    private fun serveWebUi(): Response {
        return try {
            val html = context.assets.open("web/index.html").bufferedReader().use { it.readText() }
            newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Web assets missing: ${e.message}")
        }
    }

    private fun jsonError(status: Response.Status, message: String): Response {
        val json = JSONObject().put("error", message)
        return newFixedLengthResponse(status, "application/json", json.toString())
    }
}
