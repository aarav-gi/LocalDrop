package com.localdrop.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.localdrop.MainActivity
import com.localdrop.core.constants.AppConstants
import com.localdrop.core.security.SessionManager
import com.localdrop.core.security.SharedFile
import com.localdrop.hotspot.HotspotState
import com.localdrop.hotspot.WifiNetworkManager
import com.localdrop.server.ServerConfig
import com.localdrop.server.ServerController
import com.localdrop.server.TransferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SharingForegroundService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob())
    private var notificationTicker: Job? = null

    val sessionManager = SessionManager()
    val transferManager = TransferManager()
    lateinit var wifiNetworkManager: WifiNetworkManager
        private set
    lateinit var serverController: ServerController
        private set

    private val _sharingActive = MutableStateFlow(false)
    val sharingActive: StateFlow<Boolean> = _sharingActive

    private val _serverConfig = MutableStateFlow<ServerConfig?>(null)
    val serverConfig: StateFlow<ServerConfig?> = _serverConfig

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    inner class LocalBinder : Binder() {
        fun getService(): SharingForegroundService = this@SharingForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        wifiNetworkManager = WifiNetworkManager(this)
        serverController = ServerController(this, sessionManager, transferManager)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    suspend fun startSharing(files: List<SharedFile>): Boolean {
        _lastError.value = null
        val hotspotResult = wifiNetworkManager.establishConnectivity()
        if (hotspotResult is HotspotState.Failed) {
            _lastError.value = hotspotResult.reason
            return false
        }

        return try {
            val config = serverController.start(files)
            _serverConfig.value = config
            _sharingActive.value = true
            startForeground(AppConstants.NOTIFICATION_ID, buildNotification(0, 0.0))
            startNotificationTicker()
            true
        } catch (e: Exception) {
            _lastError.value = e.message ?: "Failed to start server"
            wifiNetworkManager.teardown()
            false
        }
    }

    fun stopSharing() {
        notificationTicker?.cancel()
        try {
            serverController.stop()
        } catch (_: Exception) {}
        wifiNetworkManager.teardown()
        _serverConfig.value = null
        _sharingActive.value = false
        @Suppress("DEPRECATION")
        stopForeground(true)
        stopSelf()
    }

    private fun startNotificationTicker() {
        notificationTicker = serviceScope.launch {
            while (true) {
                delay(2000)
                val sessions = transferManager.sessions.value.values
                val active = sessions.count { it.state.name == "TRANSFERRING" }
                val totalSpeed = sessions
                    .filter { it.state.name == "TRANSFERRING" }
                    .sumOf { transferManager.currentSpeedBytesPerSecond(it.transferId, it.transferredBytes) }
                updateNotification(active, totalSpeed)
            }
        }
    }

    private fun updateNotification(receivers: Int, speedBytesPerSec: Double) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(AppConstants.NOTIFICATION_ID, buildNotification(receivers, speedBytesPerSec))
    }

    private fun buildNotification(receivers: Int, speedBytesPerSec: Double): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val speedText = com.localdrop.core.utils.SizeFormatter.formatSpeed(speedBytesPerSec)
        return NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("LocalDrop is sharing")
            .setContentText("Receivers: $receivers · Speed: $speedText")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_ID,
                "LocalDrop sharing",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        notificationTicker?.cancel()
        // Unconditionally teardown so Wi-Fi state is never locked
        try {
            serverController.stop()
        } catch (_: Exception) {}
        wifiNetworkManager.teardown()
        super.onDestroy()
    }

    companion object {
        fun bindIntent(context: Context) = Intent(context, SharingForegroundService::class.java)
    }
}
