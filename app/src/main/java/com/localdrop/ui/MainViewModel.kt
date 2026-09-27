package com.localdrop.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localdrop.core.security.SharedFile
import com.localdrop.core.utils.FileUtils
import com.localdrop.core.utils.QrCodeGenerator
import com.localdrop.core.security.TokenGenerator
import com.localdrop.hotspot.HotspotCapabilities
import com.localdrop.server.ServerConfig
import com.localdrop.server.TransferSession
import com.localdrop.service.SharingForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PickedFileUi(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String
)

data class UiState(
    val pickedFiles: List<PickedFileUi> = emptyList(),
    val sharingActive: Boolean = false,
    val starting: Boolean = false,
    val serverConfig: ServerConfig? = null,
    val transfers: List<TransferSession> = emptyList(),
    val errorMessage: String? = null,
    val capabilitiesWarning: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var service: SharingForegroundService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = (binder as SharingForegroundService.LocalBinder).getService()
            service = svc
            bound = true
            observeService(svc)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    init {
        val intent = SharingForegroundService.bindIntent(app)
        app.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun observeService(svc: SharingForegroundService) {
        viewModelScope.launch {
            combine(svc.sharingActive, svc.serverConfig, svc.lastError, svc.transferManager.sessions) {
                active, config, error, sessions ->
                _uiState.value = _uiState.value.copy(
                    sharingActive = active,
                    starting = false,
                    serverConfig = config,
                    transfers = sessions.values.sortedByDescending { it.startTimeMillis },
                    errorMessage = error
                )
            }.collect {}
        }
    }

    fun onFilesPicked(uris: List<Uri>) {
        val context = getApplication<Application>()
        val picked = uris.map { uri ->
            val meta = FileUtils.readMeta(context, uri)
            PickedFileUi(meta.uri, meta.displayName, meta.sizeBytes, meta.mimeType)
        }
        _uiState.value = _uiState.value.copy(pickedFiles = picked, errorMessage = null)
    }

    fun clearPickedFiles() {
        _uiState.value = _uiState.value.copy(pickedFiles = emptyList())
    }

    fun checkCapabilities() {
        val report = HotspotCapabilities(getApplication()).evaluate()
        val warning = when {
            report.alreadyOnWifi -> null // will just reuse existing Wi-Fi, nothing to warn about
            !report.canAttemptLocalOnlyHotspot -> "Location permission is needed to create a local hotspot on this device/Android version."
            !report.wifiHardwareAvailable -> "Wi-Fi appears to be off. Turn on Wi-Fi to share files."
            else -> null
        }
        _uiState.value = _uiState.value.copy(capabilitiesWarning = warning)
    }

    fun startSharing() {
        val svc = service ?: return
        val files = _uiState.value.pickedFiles
        if (files.isEmpty()) return

        val sharedFiles = files.map {
            SharedFile(
                fileToken = TokenGenerator.generateFileToken(),
                uri = it.uri,
                displayName = it.displayName,
                sizeBytes = it.sizeBytes,
                mimeType = it.mimeType
            )
        }

        _uiState.value = _uiState.value.copy(starting = true, errorMessage = null)
        viewModelScope.launch {
            val ok = svc.startSharing(sharedFiles)
            if (!ok) {
                _uiState.value = _uiState.value.copy(starting = false)
            }
        }
    }

    fun stopSharing() {
        service?.stopSharing()
        _uiState.value = _uiState.value.copy(sharingActive = false, serverConfig = null, transfers = emptyList())
    }

    fun qrBitmapFor(config: ServerConfig) = QrCodeGenerator.generate(config.connectionUrl())

    override fun onCleared() {
        if (bound) {
            getApplication<Application>().unbindService(connection)
            bound = false
        }
        super.onCleared()
    }


    fun toggleItemSelection(uri: Uri, name: String, sizeBytes: Long, mimeType: String) {
        val current = _uiState.value.pickedFiles.toMutableList()
        val existingIndex = current.indexOfFirst { it.uri == uri }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        } else {
            current.add(
                PickedFileUi(
                    uri = uri,
                    displayName = name,
                    sizeBytes = sizeBytes,
                    mimeType = mimeType
                )
            )
        }
        _uiState.value = _uiState.value.copy(pickedFiles = current)
    }
}
