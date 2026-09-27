package com.localdrop.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.widget.FrameLayout
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.InitializationListener
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener

object IronSourceAdManager {
    const val APP_KEY = "285c7a485"
    const val BANNER_AD_UNIT_ID = "ujlck63sj6qj1dpb"
    const val INTERSTITIAL_AD_UNIT_ID = "x4kjf8mhdk2l0p9h"

    private var isInitialized = false

    fun init(activity: Activity) {
        if (isInitialized) return
        if (!hasActiveInternet(activity)) return

        IronSource.init(
            activity,
            APP_KEY,
            InitializationListener {
                isInitialized = true
                loadInterstitial()
            },
            IronSource.AD_UNIT.BANNER,
            IronSource.AD_UNIT.INTERSTITIAL
        )
    }

    fun hasActiveInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun loadInterstitial() {
        if (isInitialized) {
            IronSource.loadInterstitial()
        }
    }

    fun showInterstitial(activity: Activity) {
        if (isInitialized && hasActiveInternet(activity) && IronSource.isInterstitialReady()) {
            IronSource.showInterstitial(INTERSTITIAL_AD_UNIT_ID)
        }
    }

    fun createBanner(activity: Activity): IronSourceBannerLayout? {
        if (!hasActiveInternet(activity)) return null
        return try {
            val banner = IronSource.createBanner(activity, ISBannerSize.BANNER)
            banner?.let {
                IronSource.loadBanner(it, BANNER_AD_UNIT_ID)
            }
            banner
        } catch (_: Exception) {
            null
        }
    }

    fun destroyBanner(banner: IronSourceBannerLayout?) {
        banner?.let {
            IronSource.destroyBanner(it)
        }
    }

    fun onResume(activity: Activity) {
        IronSource.onResume(activity)
    }

    fun onPause(activity: Activity) {
        IronSource.onPause(activity)
    }
}
