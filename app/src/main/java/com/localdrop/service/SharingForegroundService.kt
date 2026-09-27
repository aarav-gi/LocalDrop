package com.localdrop.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.localdrop.R
import com.localdrop.core.constants.AppConstants
import com.localdrop.core.model.SharedFile
import com.localdrop.core.utils.SizeFormatter
import com.localdrop.hotspot.HotspotState
import com.localdrop.hotspot.WifiNetworkManager
import com.localdrop.server.ServerConfig
import com.localdrop.server.ServerController
import com.localdrop.server.TransferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SharingForegroundService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): SharingForegroundService = this@SharingForegroundService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var wifiNetworkManager: WifiNetworkManager
    private lateinit var serverController: ServerController
    val transferManager = TransferManager()

    private val _sharingActive = MutableStateFlow(false)
    val sharingActive: StateFlow<Boolean> = _sharingActive

    private val _serverConfig = MutableStateFlow<ServerConfig?>(null)
    val serverConfig: StateFlow<ServerConfig?> = _serverConfig

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    private var notificationTicker: Job? = null

    override fun onCreate() {
        super.onCreate()
        wifiNetworkManager = WifiNetworkManager(applicationContext)
        serverController = ServerController(applicationContext, transferManager)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

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
            try {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cm?.bindProcessToNetwork(null)
                }
            } catch (_: Exception) {}
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
        notificationTicker?.cancel()
        notificationTicker = serviceScope.launch {
            while (isActive) {
                delay(1000)
                val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(
                    AppConstants.NOTIFICATION_ID,
                    buildNotification(transferManager.activeReceiverCount(), transferManager.totalThroughputBytesPerSec())
                )
            }
        }
    }

    private fun buildNotification(receivers: Int, throughputBytesPerSec: Double): Notification {
        val speedStr = "${SizeFormatter.formatBytes(throughputBytesPerSec.toLong())}/s"
        val content = if (receivers > 0) {
            "$receivers receiver(s) connected · $speedStr"
        } else {
            "Ready for receivers to connect"
        }
        return NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Quick Share Active")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_ID,
                AppConstants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows transfer activity while sharing files"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopSharing()
        super.onDestroy()
    }

    companion object {
        fun bindIntent(context: Context): Intent = Intent(context, SharingForegroundService::class.java)
    }
}
