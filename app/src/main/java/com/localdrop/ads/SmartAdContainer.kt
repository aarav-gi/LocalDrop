package com.localdrop.ads

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.localdrop.ui.theme.PrimaryBlue
import com.localdrop.ui.theme.SurfaceBorderDark
import com.localdrop.ui.theme.SurfaceDark

@Composable
fun SmartAdContainer(
    isSharingActive: Boolean,
    modifier: Modifier = Modifier
) {
    val config by RemoteAdManager.configFlow.collectAsState()
    val context = LocalContext.current

    if (!config.adsEnabled) return

    val hasInternet = remember(isSharingActive) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)
        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    if (isSharingActive) {
        if (hasInternet && config.showNativeVideoDuringSharing) {
            SenderVideoAdView(adUnitId = config.nativeVideoAdId, modifier = modifier)
        }
    } else {
        if (hasInternet && config.showBottomBanner) {
            SenderBoxAdView(adUnitId = config.bannerAdId, modifier = modifier)
        }
    }
}

@Composable
private fun SenderBoxAdView(adUnitId: String, modifier: Modifier = Modifier) {
    CardContainer(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AdHeaderTag(title = "Featured Sponsor")
            Spacer(modifier = Modifier.height(6.dp))
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(AdSize.MEDIUM_RECTANGLE)
                        this.adUnitId = adUnitId
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}

@Composable
private fun SenderVideoAdView(adUnitId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(adUnitId) {
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .build()

        val loader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad -> nativeAd = ad }
            .withNativeAdOptions(adOptions)
            .build()

        loader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
        }
    }

    if (nativeAd != null) {
        CardContainer(modifier = modifier) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AdHeaderTag(title = "Video Sponsor")
                Spacer(modifier = Modifier.height(8.dp))
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    factory = { ctx ->
                        val adView = NativeAdView(ctx)
                        val frame = FrameLayout(ctx)
                        val media = MediaView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                        frame.addView(media)
                        adView.addView(frame)
                        adView.mediaView = media
                        adView.setNativeAd(nativeAd!!)
                        adView
                    },
                    update = { adView ->
                        nativeAd?.let { adView.setNativeAd(it) }
                    }
                )
            }
        }
    }
}

@Composable
private fun CardContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        content()
    }
}

@Composable
private fun AdHeaderTag(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        )
        Box(
            modifier = Modifier
                .background(SurfaceBorderDark, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Ad",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}
