@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
import com.localdrop.ads.SmartAdContainer
import com.localdrop.core.security.SharedFile
import com.localdrop.core.utils.SizeFormatter
import com.localdrop.model.PickedFile
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
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isSharing) SuccessGreen else PrimaryBlue)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!uiState.isSharing) {
                // 1. File Picker Box
                item {
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
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            )
                        }
                    }
                }

                // 2. Selected Files Header & List
                if (uiState.pickedFiles.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ready to send (${uiState.pickedFiles.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Clear all",
                                style = MaterialTheme.typography.labelMedium.copy(color = ErrorRed),
                                modifier = Modifier.clickable { viewModel.clearPickedFiles() }
                            )
                        }
                    }

                    items(uiState.pickedFiles) { file ->
                        FileItemCard(file = file)
                    }
                }

                // 3. Start Sharing Action Button
                item {
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
                    ) {
                        if (uiState.starting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (uiState.pickedFiles.isEmpty()) "Select files to begin" else "Start Sharing",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                // 4. Instructions Card
                item {
                    HowItWorksCard()
                }

                // 5. Box Ad in Idle Mode
                item {
                    SmartAdContainer(isSharingActive = false)
                    Spacer(modifier = Modifier.height(20.dp))
                }

            } else {
                // ACTIVE SHARING STATE
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Receiver scans QR or opens link",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            uiState.qrCode?.let { qr ->
                                Image(
                                    bitmap = qr.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BackgroundDark)
                                        .padding(8.dp)
                                )
                            }

                            uiState.serverConfig?.let { cfg ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("LocalDrop URL", cfg.baseUrl))
                                            Toast.makeText(context, "URL Copied!", Toast.LENGTH_SHORT).show()
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceContainerDark,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderDark)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = cfg.baseUrl,
                                            style = MaterialTheme.typography.bodyMedium.copy(color = PrimaryBlue),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Copy",
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Native Video Ad during sharing session
                item {
                    SmartAdContainer(isSharingActive = true)
                }

                // Stop Sharing Action
                item {
                    Button(
                        onClick = { viewModel.stopSharing() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerButtonBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DangerButton.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Stop Sharing",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ErrorRed
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun FileItemCard(file: PickedFile) {
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
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                )
            }
        }
    }
}

@Composable
fun HowItWorksCard() {
    var isHindi by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderDark),
        modifier = Modifier.fillMaxWidth()
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
                            .size(26.dp)
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

                Surface(
                    onClick = { isHindi = !isHindi },
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceContainerDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (isHindi) "English" else "हिंदी",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            InstructionStepItem(
                stepNumber = "1",
                title = if (isHindi) "फाइलें चुनें" else "Select Files",
                description = if (isHindi) 
                    "ऊपर दिए गए बॉक्स पर टैप करके वो वीडियो, फोटो या फाइल्स चुनें जिन्हें भेजना है।" 
                    else "Tap the box above to select any photos, videos, or documents you want to share."
            )

            InstructionStepItem(
                stepNumber = "2",
                title = if (isHindi) "Start Sharing दबाएं और कनेक्ट करें" else "Start Sharing & Connect",
                description = if (isHindi)
                    "Start Sharing दबाएं। दूसरे फोन से LocalDrop वाई-फाई हॉटस्पॉट से कनेक्ट करें।"
                    else "Tap Start Sharing. Connect the receiving device to LocalDrop hotspot or ensure both are on the same Wi-Fi."
            )

            InstructionStepItem(
                stepNumber = "3",
                title = if (isHindi) "QR स्कैन करें और डाउनलोड करें" else "Scan QR & Download",
                description = if (isHindi)
                    "दूसरे फोन के कैमरा या ब्राउज़र से QR कोड स्कैन करें। बिना इंटरनेट के हाई-स्पीड ट्रांसफर शुरू हो जाएगा!"
                    else "Scan the QR code or enter the link in any mobile browser to download directly without internet!"
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

fun resolveFileIcon(mimeType: String): ImageVector {
    return when {
        mimeType.startsWith("video/") -> Icons.Default.PlayArrow
        mimeType.startsWith("audio/") -> Icons.Default.Star
        mimeType.startsWith("image/") -> Icons.Default.ThumbUp
        else -> Icons.Default.MoreVert
    }
}
