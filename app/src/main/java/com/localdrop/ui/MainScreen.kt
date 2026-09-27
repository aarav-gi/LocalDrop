@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.localdrop.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localdrop.ads.SmartAdContainer
import com.localdrop.core.utils.HistoryItem
import com.localdrop.core.utils.HistoryManager
import com.localdrop.core.utils.SizeFormatter
import com.localdrop.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPickFiles: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    LaunchedEffect(uiState.sharingActive, uiState.serverConfig) {
        if (uiState.sharingActive && uiState.serverConfig != null) {
            currentScreen = Screen.ShareQr
        } else if (!uiState.sharingActive && currentScreen is Screen.ShareQr) {
            currentScreen = Screen.Home
        }
    }

    var currentTab by remember { mutableStateOf(BottomTab.HOME) }

    Scaffold(
        containerColor = BgLight,
        bottomBar = {
            if (currentScreen is Screen.Home || currentScreen is Screen.History || currentScreen is Screen.Settings) {
                BottomNavBar(
                    selectedTab = currentTab,
                    onTabSelected = { tab ->
                        currentTab = tab
                        currentScreen = when (tab) {
                            BottomTab.HOME -> Screen.Home
                            BottomTab.HISTORY -> Screen.History
                            BottomTab.SETTINGS -> Screen.Settings
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                is Screen.Home -> HomeScreen(
                    onSendClicked = {
                        // Directly open internal category dashboard (Files, Photos, Videos, Apps)
                        currentScreen = Screen.SendFiles
                    },
                    onReceiveClicked = { currentScreen = Screen.ReceiveFiles },
                    onSettingsClicked = {
                        currentTab = BottomTab.SETTINGS
                        currentScreen = Screen.Settings
                    }
                )
                is Screen.SendFiles -> SendFilesScreen(
                    viewModel = viewModel,
                    onPickMore = onPickFiles,
                    onBack = { currentScreen = Screen.Home }
                )
                is Screen.ShareQr -> ShareQrScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.stopSharing()
                        currentScreen = Screen.Home
                    }
                )
                is Screen.ReceiveFiles -> ReceiveFilesScreen(
                    onBack = { currentScreen = Screen.Home }
                )
                is Screen.History -> HistoryScreen()
                is Screen.Settings -> SettingsScreen(
                    onClearHistory = {
                        currentTab = BottomTab.HISTORY
                        currentScreen = Screen.History
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 1. HOME SCREEN
// -------------------------------------------------------------
@Composable
fun HomeScreen(
    onSendClicked: () -> Unit,
    onReceiveClicked: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Share",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                IconButton(onClick = onSettingsClicked) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary
                    )
                }
            }

            // Hero Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "Share Files Easily",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Text(
                        text = "High-speed offline transfer via\nHotspot or local Wi-Fi.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(
                    onClick = onSendClicked,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Send Files",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Photos, Videos, Apps & Docs",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f))
                            )
                        }
                    }
                }

                Surface(
                    onClick = onReceiveClicked,
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Receive Files",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "How to download via Browser",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            }

            // Info Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PrimaryBlueLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Zero Internet Data Used",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        )
                        Text(
                            text = "Works purely over offline local frequency",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }

        SmartAdContainer(isSharingActive = false)
    }
}

// -------------------------------------------------------------
// 2. SEND FILES SCREEN
// -------------------------------------------------------------
@Composable
fun SendFilesScreen(
    viewModel: MainViewModel,
    onPickMore: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val repository = remember { com.localdrop.core.utils.MediaScannerRepository(context) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Files", "Photos", "Videos", "Apps")

    var filesList by remember { mutableStateOf<List<com.localdrop.core.utils.SelectableItem>>(emptyList()) }
    var photosList by remember { mutableStateOf<List<com.localdrop.core.utils.SelectableItem>>(emptyList()) }
    var videosList by remember { mutableStateOf<List<com.localdrop.core.utils.SelectableItem>>(emptyList()) }
    var appsList by remember { mutableStateOf<List<com.localdrop.core.utils.SelectableItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        isLoading = true
        when (selectedTab) {
            0 -> if (filesList.isEmpty()) filesList = repository.getDocuments()
            1 -> if (photosList.isEmpty()) photosList = repository.getPhotos()
            2 -> if (videosList.isEmpty()) videosList = repository.getVideos()
            3 -> if (appsList.isEmpty()) appsList = repository.getInstalledApps()
        }
        isLoading = false
    }

    val currentItems = when (selectedTab) {
        0 -> filesList
        1 -> photosList
        2 -> videosList
        3 -> appsList
        else -> emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = "Select Files to Send",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = PrimaryBlue,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) PrimaryBlue else TextSecondary
                            )
                        }
                    )
                }
            }

            // Selected Counter & System Explorer trigger
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryBlue)
                        }
                        Column {
                            Text(
                                text = "Selected (${uiState.pickedFiles.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            val totalBytes = uiState.pickedFiles.sumOf { it.sizeBytes }
                            Text(
                                text = SizeFormatter.formatBytes(totalBytes),
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onPickMore,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Browse", fontSize = 12.sp)
                    }
                }
            }

            // Media list
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (currentItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
                        .clickable { onPickMore() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No items found in ${tabs[selectedTab]}", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Tap Browse to pick from phone storage", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(currentItems, key = { it.uri.toString() }) { item ->
                        val isSelected = uiState.pickedFiles.any { it.uri == item.uri }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryBlueLight.copy(alpha = 0.5f) else SurfaceWhite
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) PrimaryBlue else BorderLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleItemSelection(
                                        uri = item.uri,
                                        name = item.name,
                                        sizeBytes = item.sizeBytes,
                                        mimeType = item.mimeType
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = resolveFileIcon(item.mimeType),
                                        contentDescription = null,
                                        tint = PrimaryBlue
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = SizeFormatter.formatBytes(item.sizeBytes),
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                                    )
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        viewModel.toggleItemSelection(
                                            uri = item.uri,
                                            name = item.name,
                                            sizeBytes = item.sizeBytes,
                                            mimeType = item.mimeType
                                        )
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Generate QR Button
        Button(
            onClick = { viewModel.startSharing() },
            enabled = uiState.pickedFiles.isNotEmpty() && !uiState.starting,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (uiState.starting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Text("Generate QR Code (${uiState.pickedFiles.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. SHARE QR SCREEN
// -------------------------------------------------------------
@Composable
fun ShareQrScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val serverCfg = uiState.serverConfig

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = "Scan & Download",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (serverCfg != null) {
                        val qrBitmap = remember(serverCfg) { viewModel.qrBitmapFor(serverCfg) }
                        qrBitmap?.let { qr ->
                            Image(
                                bitmap = qr.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .size(210.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }

                    Text(
                        text = "Scan with Camera or any QR Scanner",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = "Receiver can download directly from browser",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            }
        }

        item {
            val totalBytes = uiState.pickedFiles.sumOf { it.sizeBytes }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Files (${uiState.pickedFiles.size})",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Text(
                    text = "Total: " + SizeFormatter.formatBytes(totalBytes),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                )
            }
        }

        items(uiState.pickedFiles.take(3)) { file ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(resolveFileIcon(file.mimeType), contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    Text(
                        text = file.displayName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(SizeFormatter.formatBytes(file.sizeBytes), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }

        item {
            if (serverCfg != null) {
                val fullUrl = serverCfg.connectionUrl()
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("LocalDrop URL", fullUrl))
                        Toast.makeText(context, "Link Copied!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Browser URL", color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            SmartAdContainer(isSharingActive = true)
        }

        item {
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Stop Sharing", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// -------------------------------------------------------------
// 4. RECEIVE FILES SCREEN (Complete Information)
// -------------------------------------------------------------
@Composable
fun ReceiveFilesScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = "Receive Files",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            // Zero Internet Notice Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlueLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                    Column {
                        Text(
                            text = "Zero Internet Consumption",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "This app transfers files entirely offline. Neither sender nor receiver needs active internet data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Step-by-Step Instructions
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "How to receive without any app:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )

                    InstructionStep(
                        number = "1",
                        title = "Network Connection",
                        description = "Sender must turn on Hotspot OR both devices must be on the same local Wi-Fi router."
                    )
                    InstructionStep(
                        number = "2",
                        title = "Scan QR Code",
                        description = "Receiver connects Wi-Fi to Sender's hotspot and scans the sender's QR code using Camera, Google Lens, or any QR scanner."
                    )
                    InstructionStep(
                        number = "3",
                        title = "Direct Browser Download",
                        description = "A private download page opens in Chrome/Safari. Tap Download to save files directly to internal storage."
                    )
                }
            }
        }

        Button(
            onClick = onBack,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Back to Home", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InstructionStep(number: String, title: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Text(description, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

// -------------------------------------------------------------
// 5. HISTORY SCREEN (Persistent Transfer Logs)
// -------------------------------------------------------------
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    var historyItems by remember { mutableStateOf(HistoryManager.getHistory(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transfer History", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            if (historyItems.isNotEmpty()) {
                TextButton(
                    onClick = {
                        HistoryManager.clearHistory(context)
                        historyItems = emptyList()
                    }
                ) {
                    Text("Clear All", color = ErrorRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Text("No recent transfers found.", color = TextSecondary, fontWeight = FontWeight.Medium)
                    Text("Files you share will appear here.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(historyItems) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.fileName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${SizeFormatter.formatBytes(item.sizeBytes)} • ${sdf.format(Date(item.timestamp))}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. SETTINGS SCREEN (Useful Controls & Settings)
// -------------------------------------------------------------
@Composable
fun SettingsScreen(onClearHistory: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

        // Connection Modes Info
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transfer Protocols", fontWeight = FontWeight.Bold)
                Text(
                    text = "• Wi-Fi Access Point (Hotspot Mode)\n• Shared Local Router (Same Wi-Fi Mode)\n• High-throughput Local Stream Engine",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // Storage & Cache
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Data & History", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Transfer Logs", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Clear logged file transfer history", color = TextSecondary, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            HistoryManager.clearHistory(context)
                            Toast.makeText(context, "History cleared!", Toast.LENGTH_SHORT).show()
                            onClearHistory()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear", color = ErrorRed)
                    }
                }
            }
        }

        // App Information
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("About Quick Share", fontWeight = FontWeight.Bold)
                Text("Version 1.0.0 (Production Release)", color = TextSecondary, fontSize = 13.sp)
                Text("100% Offline peer-to-peer file sharing without third-party servers.", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// 7. BOTTOM NAVIGATION BAR
// -------------------------------------------------------------
@Composable
fun BottomNavBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceWhite,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == BottomTab.HOME,
            onClick = { onTabSelected(BottomTab.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                indicatorColor = PrimaryBlueLight
            )
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.HISTORY,
            onClick = { onTabSelected(BottomTab.HISTORY) },
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("History", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                indicatorColor = PrimaryBlueLight
            )
        )
        NavigationBarItem(
            selected = selectedTab == BottomTab.SETTINGS,
            onClick = { onTabSelected(BottomTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryBlue,
                selectedTextColor = PrimaryBlue,
                indicatorColor = PrimaryBlueLight
            )
        )
    }
}

fun resolveFileIcon(mimeType: String): ImageVector {
    return when {
        mimeType.startsWith("video/") -> Icons.Default.PlayArrow
        mimeType.startsWith("audio/") -> Icons.Default.Audiotrack
        mimeType.startsWith("image/") -> Icons.Default.Image
        mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
        else -> Icons.Default.InsertDriveFile
    }
}
