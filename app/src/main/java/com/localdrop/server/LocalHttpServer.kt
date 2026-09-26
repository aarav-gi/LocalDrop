package com.localdrop.server

import android.content.Context
import com.localdrop.core.constants.NetworkConstants
import com.localdrop.core.security.AccessController
import com.localdrop.core.security.SessionManager
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * The embedded HTTP server the receiver's ordinary browser talks to.
 * Built on NanoHTTPD (single dependency, no full servlet stack needed for
 * an on-device server). All routes are read-only downloads for v1 — no
 * arbitrary filesystem paths are ever exposed, only opaque per-file tokens
 * resolved through SessionManager.
 */
class LocalHttpServer(
    private val context: Context,
    port: Int,
    private val sessionManager: SessionManager,
    private val transferManager: TransferManager,
    private val onRequestLog: (String) -> Unit
) : NanoHTTPD(port) {

    private val fileStreamer = FileStreamer(context)
    private val rateLimiter = RateLimiter()

    /** NanoHTTPD 2.3.1's Response.Status enum has no 429; define it explicitly. */
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
                uri == NetworkConstants.ROUTE_INDEX -> serveWebUi()
                else -> jsonError(Response.Status.NOT_FOUND, "Not found")
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
        if (!AccessController.isPlausibleToken(token) || !sessionManager.validateSession(token!!)) {
            return jsonError(Response.Status.UNAUTHORIZED, "Invalid or expired session")
        }
        val active = sessionManager.currentSession()!!
        val array = JSONArray()
        active.files.values.forEach { f ->
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
        val sessionToken = session.parms["s"]
        val fileToken = session.uri.removePrefix(NetworkConstants.ROUTE_DOWNLOAD_PREFIX)

        if (!AccessController.isPlausibleToken(sessionToken) || !AccessController.isPlausibleToken(fileToken)) {
            return jsonError(Response.Status.FORBIDDEN, "Malformed request")
        }

        val file = sessionManager.resolveFile(sessionToken!!, fileToken)
            ?: return jsonError(Response.Status.FORBIDDEN, "Unauthorized or unknown file")

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
            return jsonError(Response.Status.INTERNAL_ERROR, "Unable to read file")
        }

        val transferId = transferManager.beginTransfer(
            fileToken = file.fileToken,
            fileName = file.displayName,
            totalBytes = file.sizeBytes,
            remoteAddress = remoteAddress,
            resumeFromBytes = startByte
        )

        val progressStream = ProgressReportingInputStream(opened.stream, startByte) { cumulative ->
            transferManager.updateProgress(transferId, cumulative)
            if (cumulative >= file.sizeBytes) transferManager.completeTransfer(transferId)
        }

        val status = if (parsedRange != null) Response.Status.PARTIAL_CONTENT else Response.Status.OK
        val response = newFixedLengthResponse(status, file.mimeType, progressStream, opened.lengthToServe)
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader(
            "Content-Disposition",
            "attachment; filename=\"${file.displayName.replace("\"", "")}\""
        )
        if (parsedRange != null) {
            val end = startByte + opened.lengthToServe - 1
            response.addHeader("Content-Range", "bytes $startByte-$end/${file.sizeBytes}")
        }
        return response
    }

    private fun serveWebUi(): Response {
        val html = context.assets.open("web/index.html").bufferedReader().use { it.readText() }
        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun jsonError(status: Response.Status, message: String): Response {
        val json = JSONObject().put("error", message)
        return newFixedLengthResponse(status, "application/json", json.toString())
    }
}
