package com.localdrop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.localdrop.ads.IronSourceAdManager
import com.localdrop.ui.MainScreen
import com.localdrop.ui.MainViewModel
import com.localdrop.ui.theme.LocalDropTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris?.let { viewModel.onFilesPicked(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ironSource/Unity LevelPlay Ads safely on app launch
        IronSourceAdManager.init(this)

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

    override fun onResume() {
        super.onResume()
        IronSourceAdManager.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        IronSourceAdManager.onPause(this)
    }
}
