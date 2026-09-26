@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.localdrop.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localdrop.core.security.SharedFile
import com.localdrop.core.utils.QrCodeGenerator
import com.localdrop.core.utils.SizeFormatter
import com.localdrop.hotspot.HotspotState
import com.localdrop.ui.theme.*

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val isSharing by viewModel.sharingActive.collectAsState()
    val serverConfig by viewModel.serverConfig.collectAsState()
    val hotspotState by viewModel.hotspotState.collectAsState()
    val activeTransfers by viewModel.activeTransfers.collectAsState()
    val lastError by viewModel.lastError.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onFilesSelected(uris)
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "LocalDrop",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSharing) SuccessGreen else PrimaryBlue)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Status Banner if any warning/error
            lastError?.let { err ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRedBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = err,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = isSharing,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenState"
            ) { sharing ->
                if (!sharing) {
                    // IDLE / FILE SELECTION STATE
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Upload Drop Zone Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, SurfaceBorderDark, RoundedCornerShape(16.dp))
                                    .clickable { filePickerLauncher.launch(arrayOf("*/*")) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = "Select Files",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Tap to choose files",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Any format · Direct device-to-device",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            // Selected Files List
                            if (selectedFiles.isNotEmpty()) {
                                Text(
                                    text = "Ready to send (${selectedFiles.size})",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                )
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(selectedFiles) { file ->
                                        FileItemCard(
                                            file = file,
                                            onRemove = { viewModel.removeFile(file) }
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Beam Button
                        Button(
                            onClick = { viewModel.startSharing() },
                            enabled = selectedFiles.isNotEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                disabledContainerColor = SurfaceDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = if (selectedFiles.isEmpty()) "Select files to begin" else "Start Sharing",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (selectedFiles.isEmpty()) TextMuted else TextPrimary
                                )
                            )
                        }
                    }
                } else {
                    // SHARING / TRANSMITTER STATE
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // QR Card
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = QRContainerWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .size(240.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    serverConfig?.shareUrl?.let { url ->
                                        val qrBitmap = remember(url) {
                                            QrCodeGenerator.generateQrBitmap(url, 512)
                                        }
                                        qrBitmap?.let {
                                            androidx.compose.foundation.Image(
                                                bitmap = it.asImageBitmap(),
                                                contentDescription = "QR Code",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } ?: CircularProgressIndicator(color = BackgroundDark)
                                }
                            }

                            Text(
                                text = "Scan using receiver's camera",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )

                            // Clickable URL Pill
                            serverConfig?.shareUrl?.let { url ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceDark,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderDark),
                                    modifier = Modifier.clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("LocalDrop URL", url))
                                        Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = url,
                                            style = MaterialTheme.typography.bodyMedium.copy(color = PrimaryLight),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Copy",
                                            tint = PrimaryLight,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Hotspot & Receivers info dock
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "HOTSPOT", style = MaterialTheme.typography.labelSmall)
                                        val ssid = when (val s = hotspotState) {
                                            is HotspotState.Running -> s.ssid ?: "Active"
                                            else -> "Starting..."
                                        }
                                        Text(
                                            text = ssid,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "CONNECTED", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = "${activeTransfers.size} devices",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        // Stop Sharing Action
                        Button(
                            onClick = { viewModel.stopSharing() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerButtonBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DangerButton.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "Stop Sharing",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = ErrorRed
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileItemCard(file: SharedFile, onRemove: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainerDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resolveFileIcon(file.mimeType),
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = SizeFormatter.formatBytes(file.sizeBytes),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun resolveFileIcon(mimeType: String): ImageVector {
    return when {
        mimeType.startsWith("video/") -> Icons.Default.PlayArrow
        mimeType.startsWith("audio/") -> Icons.Default.Star
        mimeType.startsWith("image/") -> Icons.Default.ThumbUp
        else -> Icons.Default.MoreVert
    }
}
