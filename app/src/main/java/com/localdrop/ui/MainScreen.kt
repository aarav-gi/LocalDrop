@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
package com.localdrop.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import com.localdrop.core.utils.SizeFormatter
import com.localdrop.ui.theme.*

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPickFiles: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

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
                                .background(if (uiState.sharingActive) SuccessGreen else PrimaryBlue)
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
            // Error Banner
            uiState.errorMessage?.let { err ->
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

            // Capabilities / Wi-Fi Warning Banner
            uiState.capabilitiesWarning?.let { warn ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningAmberBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = warn,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = uiState.sharingActive,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenState"
            ) { isSharing ->
                if (!isSharing) {
                    // IDLE / FILE SELECTION STATE
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // File Selection Drop Zone Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, SurfaceBorderDark, RoundedCornerShape(16.dp))
                                    .clickable { onPickFiles() },
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
                            if (uiState.pickedFiles.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ready to send (${uiState.pickedFiles.size})",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                                    )
                                    Text(
                                        text = "Clear all",
                                        style = MaterialTheme.typography.labelSmall.copy(color = PrimaryBlue),
                                        modifier = Modifier.clickable { viewModel.clearPickedFiles() }
                                    )
                                }
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(uiState.pickedFiles) { file ->
                                        FileItemCard(file = file)
                                    }
                                }
                            }
                        }

                        // Bottom Start Sharing Button
                        Button(
                            onClick = { viewModel.startSharing() },
                            enabled = uiState.pickedFiles.isNotEmpty() && !uiState.starting,
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
                            if (uiState.starting) {
                                CircularProgressIndicator(
                                    color = TextPrimary,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (uiState.pickedFiles.isEmpty()) "Select files to begin" else "Start Sharing",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (uiState.pickedFiles.isEmpty()) TextMuted else TextPrimary
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // SHARING / TRANSMITTER STATE
                    Column(
                        modifier = Modifier.fillMaxSize(),
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
                                    uiState.serverConfig?.let { config ->
                                        val qrBitmap: Bitmap? = remember(config) {
                                            try {
                                                viewModel.qrBitmapFor(config)
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }
                                        qrBitmap?.let {
                                            Image(
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
                            uiState.serverConfig?.connectionUrl()?.let { url ->
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
                                        Text(text = "NETWORK", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = "LocalDrop Active",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "CONNECTED", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = "${uiState.transfers.size} devices",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        HowItWorksCard()

                    com.localdrop.ads.BannerAdView()

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
fun FileItemCard(file: PickedFileUi) {
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


@Composable
fun HowItWorksCard()

                    com.localdrop.ads.BannerAdView() {
    var isHindi by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "?",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        )
                    }
                    Text(
                        text = if (isHindi) "यह कैसे काम करता है?" else "How LocalDrop Works",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // Language Switcher Button
                Surface(
                    onClick = { isHindi = !isHindi },
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceContainerDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isHindi) "English" else "हिंदी",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                        )
                    }
                }
            }

            InstructionStepItem(
                stepNumber = "1",
                title = if (isHindi) "फाइलें चुनें" else "Select Files",
                description = if (isHindi) 
                    "ऊपर दिए गए बॉक्स पर टैप करके वो वीडियो, फोटो या डॉक्युमेंट्स चुनें जिन्हें भेजना है।" 
                    else "Tap the drop zone above to pick any videos, photos, or documents you want to transfer."
            )

            InstructionStepItem(
                stepNumber = "2",
                title = if (isHindi) "Start Sharing दबाएं और कनेक्ट करें" else "Start Sharing & Connect",
                description = if (isHindi)
                    "Start Sharing दबाएं। दूसरे फोन से बने हुए LocalDrop हॉटस्पॉट से कनेक्ट करें या दोनों फोन एक ही वाई-फाई पर रखें।"
                    else "Tap Start Sharing. Connect the receiving device to LocalDrop hotspot or stay on the same local Wi-Fi."
            )

            InstructionStepItem(
                stepNumber = "3",
                title = if (isHindi) "QR स्कैन करें और डाउनलोड करें" else "Scan QR & Download",
                description = if (isHindi)
                    "दूसरे फोन के कैमरा या ब्राउज़र से QR कोड स्कैन करें या लिंक खोलें। बिना इंटरनेट के हाई-स्पीड ट्रांसफर शुरू हो जाएगा!"
                    else "Scan the QR code or enter the link in any mobile browser to download at maximum Wi-Fi speed without internet!"
            )
        }
    }
}

@Composable
fun InstructionStepItem(stepNumber: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SurfaceContainerDark)
                .border(1.dp, SurfaceBorderDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            )
        }
    }
}
