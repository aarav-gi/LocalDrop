import com.localdrop.ads.IronSourceAdManager
package com.localdrop

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.localdrop.ui.MainScreen
import com.localdrop.ui.MainViewModel
import com.localdrop.ui.theme.LocalDropTheme
import com.localdrop.service.SharingForegroundService

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Persist read access so the foreground service can still stream
            // these files even if this Activity is destroyed while sharing.
            uris.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    // Some providers don't support persistable permissions;
                    // the Uri will still work for the current process lifetime.
                }
            }
            viewModel.onFilesPicked(uris)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkCapabilities()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.localdrop.ads.RemoteAdManager.init(this)

        // Ensure the (already-declared, non-foreground-triggering) service
        // instance exists so the ViewModel's binding succeeds immediately.
        startService(SharingForegroundService.bindIntent(this))

        requestRuntimePermissionsIfNeeded()
        viewModel.checkCapabilities()

        setContent {
            LocalDropTheme {
                MainScreen(
                    viewModel = viewModel,
                    onPickFiles = {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    }
                )
            }
        }
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val needed = mutableListOf<String>()

        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nearbyGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
            if (!nearbyGranted && !fineLocationGranted) needed += Manifest.permission.NEARBY_WIFI_DEVICES

            val notifGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) needed += Manifest.permission.POST_NOTIFICATIONS

            // Android 13+ Media permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                needed += Manifest.permission.READ_MEDIA_IMAGES
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                needed += Manifest.permission.READ_MEDIA_VIDEO
            }
        } else {
            if (!fineLocationGranted) needed += Manifest.permission.ACCESS_FINE_LOCATION
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                needed += Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        IronSourceAdManager.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        IronSourceAdManager.onPause(this)
    }

}