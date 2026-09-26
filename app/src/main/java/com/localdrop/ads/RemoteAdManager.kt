package com.localdrop.ads

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AdConfig(
    val adsEnabled: Boolean = false,
    val bannerAdId: String = "ca-app-pub-3940256099942544/6300978111",
    val interstitialAdId: String = "ca-app-pub-3940256099942544/1033173712",
    val nativeVideoAdId: String = "ca-app-pub-3940256099942544/2247696110",
    val showBottomBanner: Boolean = true,
    val showNativeVideoDuringSharing: Boolean = true,
    val showInterstitialOnStop: Boolean = true
)

object RemoteAdManager {
    private const val CONFIG_URL = "https://raw.githubusercontent.com/aarav-gi/LocalDrop/main/ad_config.json"
    private const val PREFS_NAME = "localdrop_ad_prefs"

    private val _configFlow = MutableStateFlow(AdConfig())
    val configFlow = _configFlow.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        val appContext = context.applicationContext
        loadCachedConfig(appContext)

        CoroutineScope(Dispatchers.IO).launch {
            if (hasActiveInternet(appContext)) {
                fetchRemoteConfig(appContext)
            }
        }
    }

    private fun loadCachedConfig(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("ads_enabled", true)
        val bannerId = prefs.getString("banner_ad_id", "ca-app-pub-3940256099942544/6300978111") ?: "ca-app-pub-3940256099942544/6300978111"
        val interstitialId = prefs.getString("interstitial_ad_id", "ca-app-pub-3940256099942544/1033173712") ?: "ca-app-pub-3940256099942544/1033173712"
        val nativeVideoId = prefs.getString("native_video_ad_id", "ca-app-pub-3940256099942544/2247696110") ?: "ca-app-pub-3940256099942544/2247696110"
        val showBanner = prefs.getBoolean("show_bottom_banner", true)
        val showNativeVideo = prefs.getBoolean("show_native_video_during_sharing", true)
        val showInterstitial = prefs.getBoolean("show_interstitial_on_stop", true)

        _configFlow.value = AdConfig(
            adsEnabled = enabled,
            bannerAdId = bannerId,
            interstitialAdId = interstitialId,
            nativeVideoAdId = nativeVideoId,
            showBottomBanner = showBanner,
            showNativeVideoDuringSharing = showNativeVideo,
            showInterstitialOnStop = showInterstitial
        )

        if (enabled && !isInitialized) {
            initializeMobileAds(context)
        }
    }

    private fun fetchRemoteConfig(context: Context) {
        try {
            val url = URL(CONFIG_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonText)

                val enabled = json.optBoolean("ads_enabled", true)
                val bannerId = json.optString("banner_ad_id", "ca-app-pub-3940256099942544/6300978111")
                val interstitialId = json.optString("interstitial_ad_id", "ca-app-pub-3940256099942544/1033173712")
                val nativeVideoId = json.optString("native_video_ad_id", "ca-app-pub-3940256099942544/2247696110")
                val showBanner = json.optBoolean("show_bottom_banner", true)
                val showNativeVideo = json.optBoolean("show_native_video_during_sharing", true)
                val showInterstitial = json.optBoolean("show_interstitial_on_stop", true)

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("ads_enabled", enabled)
                    .putString("banner_ad_id", bannerId)
                    .putString("interstitial_ad_id", interstitialId)
                    .putString("native_video_ad_id", nativeVideoId)
                    .putBoolean("show_bottom_banner", showBanner)
                    .putBoolean("show_native_video_during_sharing", showNativeVideo)
                    .putBoolean("show_interstitial_on_stop", showInterstitial)
                    .apply()

                _configFlow.value = AdConfig(
                    adsEnabled = enabled,
                    bannerAdId = bannerId,
                    interstitialAdId = interstitialId,
                    nativeVideoAdId = nativeVideoId,
                    showBottomBanner = showBanner,
                    showNativeVideoDuringSharing = showNativeVideo,
                    showInterstitialOnStop = showInterstitial
                )

                if (enabled && !isInitialized) {
                    initializeMobileAds(context)
                }
            }
        } catch (_: Exception) {}
    }

    private fun initializeMobileAds(context: Context) {
        try {
            MobileAds.initialize(context) {}
            isInitialized = true
        } catch (_: Exception) {}
    }

    private fun hasActiveInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
