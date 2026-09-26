package com.localdrop.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localdrop.ads.SmartAdContainer
import com.localdrop.core.format.SizeFormatter
import com.localdrop.model.PickedFileUi
import com.localdrop.model.SharingState
import com.localdrop.ui.theme.*

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onFilesPicked(uris)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "LocalDrop",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
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
            when (val state = uiState.sharingState) {
                is SharingState.Idle -> {
                    item {
                        // Drop zone for picking files
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
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
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Pick Files",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = "Tap to select files to share",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Videos, Photos, ZIP, APKs or Documents",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }

                    if (uiState.selectedFiles.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Selected Files (${uiState.selectedFiles.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                TextButton(onClick = { viewModel.clearSelectedFiles() }) {
                                    Text("Clear All", color = ErrorRed)
                                }
                            }
                        }

                        items(uiState.selectedFiles) { file ->
                            FileItemCard(file = file)
                        }

                        item {
                            Button(
                                onClick = { viewModel.startSharing() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Start Sharing",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    item {
                        HowItWorksCard()
                    }

                    item {
                        // Rectangular Box Ad in Idle State
                        SmartAdContainer(isSharingActive = false)
                    }
                }

                is SharingState.Active -> {
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
                                    text = "Scan or Open Link to Download",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )

                                uiState.qrCodeBitmap?.let { qr ->
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

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("LocalDrop URL", state.serverUrl))
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
                                            text = state.serverUrl,
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

                    item {
                        // Native Video Ad during active sharing
                        SmartAdContainer(isSharingActive = true)
                    }

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

@Composable
fun HowItWorksCard() {
    var isHindi by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
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

fun resolveFileIcon(mimeType: String): ImageVector {
    return when {
        mimeType.startsWith("video/") -> Icons.Default.PlayArrow
        mimeType.startsWith("audio/") -> Icons.Default.Star
        mimeType.startsWith("image/") -> Icons.Default.ThumbUp
        else -> Icons.Default.MoreVert
    }
}
