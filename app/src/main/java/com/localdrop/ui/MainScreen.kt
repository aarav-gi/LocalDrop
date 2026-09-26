@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.localdrop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.localdrop.core.utils.SizeFormatter
import com.localdrop.server.ServerConfig
import com.localdrop.server.TransferSession

@Composable
fun MainScreen(viewModel: MainViewModel, onPickFiles: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("LocalDrop") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            state.capabilitiesWarning?.let { warning ->
                Card(
                    colors = CardDefaults.cardColors(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(warning, modifier = Modifier.padding(12.dp))
                }
            }

            state.errorMessage?.let { error ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Text("Error: $error", modifier = Modifier.padding(12.dp))
                }
            }

            if (!state.sharingActive) {
                FileSelectionSection(state, onPickFiles, onStart = { viewModel.startSharing() })
            } else {
                state.serverConfig?.let { config ->
                    SharingActiveSection(
                        config = config,
                        qrBitmap = { viewModel.qrBitmapFor(config) },
                        onStop = { viewModel.stopSharing() }
                    )
                }
            }

            if (state.starting) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (state.transfers.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Transfers", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(state.transfers) { transfer ->
                        TransferRow(transfer)
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun FileSelectionSection(
    state: UiState,
    onPickFiles: () -> Unit,
    onStart: () -> Unit
) {
    Column {
        Button(onClick = onPickFiles, modifier = Modifier.fillMaxWidth()) {
            Text("SELECT FILES")
        }

        Spacer(Modifier.height(12.dp))

        if (state.pickedFiles.isEmpty()) {
            Text("No files selected yet.")
        } else {
            LazyColumn(modifier = Modifier.height(220.dp)) {
                items(state.pickedFiles) { file ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(file.displayName)
                        Text(
                            SizeFormatter.formatBytes(file.sizeBytes) + " · " + file.mimeType,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Divider()
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.pickedFiles.isNotEmpty() && !state.starting
            ) {
                Text("START SHARING")
            }
        }
    }
}

@Composable
private fun SharingActiveSection(
    config: ServerConfig,
    qrBitmap: () -> android.graphics.Bitmap,
    onStop: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("SCAN TO CONNECT", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Image(
            bitmap = qrBitmap().asImageBitmap(),
            contentDescription = "QR code to connect",
            modifier = Modifier.size(220.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(config.connectionUrl(), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                clipboard.setText(AnnotatedString(config.connectionUrl()))
            }) {
                Text("COPY LINK")
            }
            Button(onClick = onStop) {
                Text("STOP SHARING")
            }
        }
    }
}

@Composable
private fun TransferRow(transfer: TransferSession) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(transfer.fileName)
            Text(transfer.state.name, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = transfer.progressFraction,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            SizeFormatter.formatBytes(transfer.transferredBytes) + " / " +
                SizeFormatter.formatBytes(transfer.totalBytes) + " · " + transfer.remoteAddress,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
