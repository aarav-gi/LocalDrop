package com.localdrop.ads

import android.app.Activity
import androidx.compose.foundation.background
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
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.localdrop.ui.theme.PrimaryBlue
import com.localdrop.ui.theme.SurfaceBorderDark
import com.localdrop.ui.theme.SurfaceWhite

@Composable
fun SmartAdContainer(
    isSharingActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    val hasInternet = remember(isSharingActive) {
        IronSourceAdManager.hasActiveInternet(context)
    }

    // Only render banner on home/idle when internet is active
    if (!isSharingActive && hasInternet) {
        var bannerLayout by remember { mutableStateOf<IronSourceBannerLayout?>(null) }

        DisposableEffect(Unit) {
            IronSourceAdManager.init(activity)
            bannerLayout = IronSourceAdManager.createBanner(activity)

            onDispose {
                IronSourceAdManager.destroyBanner(bannerLayout)
                bannerLayout = null
            }
        }

        bannerLayout?.let { banner ->
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceWhite)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { banner },
                        modifier = Modifier.wrapContentSize()
                    )
                }
            }
        }
    }
}
