package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AudioPlayerManager
import com.example.data.AudioProfile
import com.example.data.Playlist
import com.example.data.Song
import com.example.ui.theme.*
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    viewModel: MusicViewModel,
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Bottom Insets and Navigation Bars Safe Paddings
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            TbmBottomNavigationBar(
                currentScreen = currentScreen,
                onScreenSelected = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .drawBehind {
                    // Futuristic atmosphere brush based on active song mood
                    val baseBrush = Brush.linearGradient(
                        colors = if (themeMode == ThemeMode.LIGHT) {
                            listOf(Color(0xFFE2E8F0), Color(0xFFF1F5F9))
                        } else {
                            listOf(ObsidianBackground, Color(0xFF040209))
                        }
                    )
                    drawRect(baseBrush)
                }
        ) {
            // Background cosmic bubbles overlay (for premium visual polish)
            CosmicAtmosphereBubbles(viewModel = viewModel, themeMode = themeMode)

            // Animated content transitions
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                }
            ) { screen ->
                when (screen) {
                    MusicViewModel.Screen.DASHBOARD -> DashboardScreen(viewModel = viewModel, themeMode = themeMode)
                    MusicViewModel.Screen.LIBRARY -> LibraryScreen(viewModel = viewModel, themeMode = themeMode)
                    MusicViewModel.Screen.SOUND_EFFECTS -> SoundEffectsScreen(viewModel = viewModel, themeMode = themeMode)
                    MusicViewModel.Screen.NOW_PLAYING -> NowPlayingScreen(viewModel = viewModel, themeMode = themeMode, onNavigateBack = { viewModel.navigateTo(MusicViewModel.Screen.LIBRARY) })
                    MusicViewModel.Screen.SMART_ASSISTANT -> AIPlaybackPanel(viewModel = viewModel, themeMode = themeMode)
                }
            }

            // Quick floating now-playing strip if we are on any other screen
            val currentSong by viewModel.playerManager.currentSong.collectAsState()
            if (currentSong != null && currentScreen != MusicViewModel.Screen.NOW_PLAYING) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    MiniPlayerStrip(
                        viewModel = viewModel,
                        onClick = { viewModel.navigateTo(MusicViewModel.Screen.NOW_PLAYING) }
                    )
                }
            }

            // Global floating Settings menu (to choose Light/Dark/AMOLED theme modes)
            ThemeDialFloat(
                themeMode = themeMode,
                onThemeSelected = onThemeChanged,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 44.dp, end = 16.dp)
            )

            // Global Metadata Tag Editor dialog overlay
            val songToEdit by viewModel.songToEdit.collectAsState()
            if (songToEdit != null) {
                TagEditorDialog(
                    song = songToEdit!!,
                    onSave = { id, t, a, al, g, l -> viewModel.saveSongTags(id, t, a, al, g, l) },
                    onDismiss = { viewModel.cancelTagEditing() }
                )
            }
        }
    }
}

@Composable
fun TbmBottomNavigationBar(
    currentScreen: MusicViewModel.Screen,
    onScreenSelected: (MusicViewModel.Screen) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xE6080808),
        modifier = Modifier
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                ),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            .windowInsetsPadding(WindowInsets.navigationBars), // MANDATORY: avoids camera or gesture overlapping
        tonalElevation = 0.dp
    ) {
        val dark = isSystemInDarkTheme()
        val accent = CyberCyan
 
        NavigationBarItem(
            selected = currentScreen == MusicViewModel.Screen.DASHBOARD,
            onClick = { onScreenSelected(MusicViewModel.Screen.DASHBOARD) },
            icon = { Icon(Icons.Filled.SpaceDashboard, contentDescription = "Dashboard") },
            label = { Text("Portal", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = accent,
                selectedTextColor = accent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = accent.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_dashboard")
        )
        NavigationBarItem(
            selected = currentScreen == MusicViewModel.Screen.LIBRARY,
            onClick = { onScreenSelected(MusicViewModel.Screen.LIBRARY) },
            icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = "Library") },
            label = { Text("Library", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = accent,
                selectedTextColor = accent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = accent.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_library")
        )
        NavigationBarItem(
            selected = currentScreen == MusicViewModel.Screen.SOUND_EFFECTS,
            onClick = { onScreenSelected(MusicViewModel.Screen.SOUND_EFFECTS) },
            icon = { Icon(Icons.Filled.GraphicEq, contentDescription = "Sound Effects") },
            label = { Text("Studio", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = accent,
                selectedTextColor = accent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = accent.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_sound_effects")
        )
        NavigationBarItem(
            selected = currentScreen == MusicViewModel.Screen.SMART_ASSISTANT,
            onClick = { onScreenSelected(MusicViewModel.Screen.SMART_ASSISTANT) },
            icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = "Smart AI") },
            label = { Text("AI Assistant", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = accent,
                selectedTextColor = accent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = accent.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_ai")
        )
    }
}

@Composable
fun CosmicAtmosphereBubbles(
    viewModel: MusicViewModel,
    themeMode: ThemeMode
) {
    if (themeMode == ThemeMode.LIGHT) return // Clean white lines for light theme
    val activeSong by viewModel.playerManager.currentSong.collectAsState()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()

    // Animators responding to playing status
    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val floatOffset1 by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floaty1"
    )

    // Select ambient neon colors depending on mood of song
    val c1 = when (activeSong?.mood?.lowercase()) {
        "relaxed" -> OrbitViolet
        "melancholic" -> Color(0xFF581C87)
        "energetic" -> NeonPink
        else -> CyberCyan // Editorial Purple
    }
    val c2 = when (activeSong?.mood?.lowercase()) {
        "relaxed" -> CyberCyan
        "melancholic" -> Color(0xFF1E1E1E)
        "energetic" -> OrbitViolet
        else -> NeonPink // Editorial Orange
    }
    val c3 = OrbitViolet // Editorial Deep Indigo/Blue glow

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alphaAnim)
    ) {
        val w = size.width
        val h = size.height

        // 1. Top-Left ambient glow (Purple-600/20)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c1.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(w * -0.05f + floatOffset1, h * -0.05f),
                radius = w * 0.75f
            )
        )

        // 2. Middle-Right ambient glow (Orange-500/15)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c2.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(w * 1.05f - floatOffset1, h * 0.5f),
                radius = w * 0.85f
            )
        )

        // 3. Bottom-Left ambient glow (Blue-600/20)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c3.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(w * 0.25f, h * 1.05f + floatOffset1),
                radius = w * 0.75f
            )
        )
    }
}

@Composable
fun ThemeDialFloat(
    themeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .background(Color(0x3B667085), CircleShape)
                .size(38.dp)
        ) {
            val icon = when (themeMode) {
                ThemeMode.LIGHT -> Icons.Filled.LightMode
                ThemeMode.DARK -> Icons.Filled.DarkMode
                ThemeMode.AMOLED -> Icons.Filled.Contrast
            }
            Icon(
                imageVector = icon,
                contentDescription = "Theme Controls",
                tint = HighResColor,
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceCoal)
        ) {
            DropdownMenuItem(
                text = { Text("Cyber Dark Theme", color = TextWhite) },
                leadingIcon = { Icon(Icons.Filled.DarkMode, "Dark", tint = CyberCyan) },
                onClick = {
                    onThemeSelected(ThemeMode.DARK)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Pure AMOLED Black", color = TextWhite) },
                leadingIcon = { Icon(Icons.Filled.Contrast, "Amoled", tint = NeonPink) },
                onClick = {
                    onThemeSelected(ThemeMode.AMOLED)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Luxury Light View", color = if (themeMode == ThemeMode.LIGHT) TextDark else TextWhite) },
                leadingIcon = { Icon(Icons.Filled.LightMode, "Light", tint = OrbitViolet) },
                onClick = {
                    onThemeSelected(ThemeMode.LIGHT)
                    expanded = false
                }
            )
        }
    }
}

// --------------------------------------------------------
// SECTION 1: DASHBOARD PORTAL
// --------------------------------------------------------
@Composable
fun DashboardScreen(viewModel: MusicViewModel, themeMode: ThemeMode) {
    val isScanning by viewModel.isScanning.collectAsState()
    val localSongs by viewModel.allSongs.collectAsState()
    val recentSongs by viewModel.recentlyPlayedSongs.collectAsState()
    val duplicates by viewModel.duplicateSongs.collectAsState()
    val missingLyrics by viewModel.missingLyricsSongs.collectAsState()
    val context = LocalContext.current

    val mainText = if (themeMode == ThemeMode.LIGHT) TextDark else TextWhite
    val subText = if (themeMode == ThemeMode.LIGHT) TextDark.copy(alpha = 0.6f) else TextMuted
    val dividerColor = if (themeMode == ThemeMode.LIGHT) Color(0xFFE2E8F0) else Color(0x1F6B7280)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 94.dp, bottom = 90.dp)
    ) {
        item {
            // High-end typographic branding header
            Text(
                text = "TBM MUSIC",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                color = CyberCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Hyper-Sonic Portal",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = mainText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.ElectricBolt,
                    contentDescription = null,
                    tint = NeonPink,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Professional offline processing studio",
                fontSize = 14.sp,
                color = subText
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 1. Ultra glassmorphic Library Scanning Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Filled.RotateRight else Icons.Filled.BluetoothSearching,
                        contentDescription = "Scan icon",
                        tint = CyberCyan,
                        modifier = Modifier
                            .graphicsLayer {
                                if (isScanning) {
                                    rotationZ += 12f // speed rot
                                }
                            }
                            .rotate(if (isScanning) rememberInfiniteTransition().animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                                label = "scanspin"
                            ).value else 0f)
                            .size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isScanning) "Indexing audio tracks (120 FPS)..." else "Smart Device Catalog Scanner",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = mainText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scanned: ${localSongs.size} tracks mapped offline. Audits formats (FLAC, M4A, OGG, WAV).",
                        fontSize = 12.sp,
                        color = subText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.scanDeviceMediaCatalog() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3B00E5FF)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .testTag("scan_button")
                            .border(1.dp, CyberCyan, RoundedCornerShape(24.dp))
                            .height(40.dp)
                    ) {
                        Text(
                            text = if (isScanning) "SCANNING INDICES..." else "RUN QUICK SCAN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                }
            }
        }

        // 2. Playback History horiz cards
        item {
            Text(
                text = "RECENT JOURNEYS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = NeonPink,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            if (recentSongs.isEmpty()) {
                Text(
                    text = "Welcome aboard! No recently played audio detected in cache. Tap any song to kickstart playback.",
                    color = subText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentSongs) { song ->
                        RecentClassicSongCard(
                            song = song,
                            onClick = { viewModel.playSongDirect(song, recentSongs) }
                        )
                    }
                }
            }
        }

        // 3. MOST PLAYED ANALYTICS MODULE
        item {
            Text(
                text = "ENGINE METRICS & TELEMETRY",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = OrbitViolet,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            AnalyticsPanel(viewModel = viewModel, themeMode = themeMode)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. SMART DIAGNOSTIC / METADATA AUDITS CARD
        item {
            Text(
                text = "LIBRARY REPAIR & INTEGRITY",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = HighResColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.RunningWithErrors, null, tint = NeonPink, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Duplicate Tracks Found", color = mainText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("${duplicates.size}", color = NeonPink, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(color = dividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FindInPage, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tracks Missing Lyrics", color = mainText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("${missingLyrics.size}", color = CyberCyan, fontWeight = FontWeight.Black)
                    }

                    if (duplicates.isNotEmpty() || missingLyrics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "💡 Tip: Go to the Library component and swipe items to edit metadata and seed custom offline lyrics!",
                            color = subText,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentClassicSongCard(
    song: Song,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(135.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCoal)
            .clickable(onClick = onClick)
            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Album art simulation with neon circle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonPink, OrbitViolet)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudSync,
                    contentDescription = null,
                    tint = TextWhite.copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp)
                )

                if (song.isHighRes) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color(0xE03E2723), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("HI-RES", fontSize = 8.sp, color = HighResColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = song.title,
                fontSize = 13.sp,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontSize = 11.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AnalyticsPanel(viewModel: MusicViewModel, themeMode: ThemeMode) {
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val mainText = if (themeMode == ThemeMode.LIGHT) TextDark else TextWhite
    val subText = if (themeMode == ThemeMode.LIGHT) TextDark.copy(alpha = 0.6f) else TextMuted

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Most Played Telemetry",
                    fontWeight = FontWeight.Bold,
                    color = mainText,
                    fontSize = 15.sp
                )
                Icon(Icons.Filled.QueryStats, null, tint = OrbitViolet, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (mostPlayedSongs.all { it.playCount == 0 }) {
                Text(
                    text = "Telemetry calibration active. Play songs to populate analytics mapping dials.",
                    fontSize = 12.sp,
                    color = subText
                )
            } else {
                mostPlayedSongs.take(4).forEach { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = mainText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = subText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${song.playCount} cycles",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            // progress bar simulation
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray.copy(alpha = 0.2f))
                            ) {
                                val percentage = (song.playCount.toFloat() / 15f).coerceIn(0.1f, 1.0f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(percentage)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(CyberCyan, NeonPink)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------
// SECTION 2: LIBRARY MANAGEMENT
// --------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(viewModel: MusicViewModel, themeMode: ThemeMode) {
    val songs by viewModel.searchedSongs.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    val favs by viewModel.favoriteSongs.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var activeSubTab by remember { mutableStateOf(LibrarySubTab.SONGS) }
    var playlistDialogOpen by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var playlistDescInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val mainText = if (themeMode == ThemeMode.LIGHT) TextDark else TextWhite
    val subText = if (themeMode == ThemeMode.LIGHT) TextDark.copy(alpha = 0.6f) else TextMuted
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Space header to account for setting dial
        Spacer(modifier = Modifier.height(94.dp))

        // Large search input with premium design
        val searchQuery by viewModel.searchQuery.collectAsState()
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input")
                .background(if (themeMode == ThemeMode.LIGHT) Color.White else SurfaceCoal, RoundedCornerShape(12.dp)),
            placeholder = { Text("Search title, artist, or album...", color = subText, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = CyberCyan) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = NeonPink)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Grid of Library Subtabs
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibrarySubTab.values().toList()) { tab ->
                val active = tab == activeSubTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) Color(0x3B00E5FF) else SurfaceCoal)
                        .clickable { activeSubTab = tab }
                        .border(
                            1.dp,
                            if (active) CyberCyan else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab.label,
                        color = if (active) CyberCyan else TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Render depending on active tab
        Box(modifier = Modifier.weight(1f)) {
            when (activeSubTab) {
                LibrarySubTab.SONGS -> {
                    if (songs.isEmpty()) {
                        EmptyLibraryState(isScanning, "No matching audio indices found. Run quick scan to pull media files!")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(songs) { song ->
                                SongRowItem(
                                    song = song,
                                    onPlay = { viewModel.playSongDirect(song, songs) },
                                    onFavorite = { viewModel.toggleFavorite(song) },
                                    onEditTags = { viewModel.openTagEditorFor(song) },
                                    onDelete = { viewModel.deleteSongFromLibrary(song.id) },
                                    playlists = playlists,
                                    onAddToPlaylist = { pId -> viewModel.addSongToPlaylist(pId, song.id) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(40.dp)) }
                        }
                    }
                }
                LibrarySubTab.FAVORITES -> {
                    if (favs.isEmpty()) {
                        EmptyLibraryState(false, "No stellar collections mapped. Heart your tracks to show them here!")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(favs) { song ->
                                SongRowItem(
                                    song = song,
                                    onPlay = { viewModel.playSongDirect(song, favs) },
                                    onFavorite = { viewModel.toggleFavorite(song) },
                                    onEditTags = { viewModel.openTagEditorFor(song) },
                                    onDelete = { viewModel.deleteSongFromLibrary(song.id) },
                                    playlists = playlists,
                                    onAddToPlaylist = { pId -> viewModel.addSongToPlaylist(pId, song.id) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(40.dp)) }
                        }
                    }
                }
                LibrarySubTab.PLAYLISTS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Your Custom Portals", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = mainText)
                            TextButton(
                                onClick = { playlistDialogOpen = true },
                                modifier = Modifier.testTag("create_playlist_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Custom", color = CyberCyan, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (playlists.isEmpty()) {
                            EmptyLibraryState(false, "No playlists saved. Create one below of click Quick Scan!")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(playlists) { playlist ->
                                    PlaylistItemRow(
                                        playlist = playlist,
                                        onDelete = { viewModel.deletePlaylist(playlist) },
                                        onClick = {
                                            viewModel.selectPlaylist(playlist.id)
                                            Toast.makeText(context, "${playlist.name} active", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(40.dp)) }
                            }
                        }
                    }
                }
                else -> {
                    // Fallback other indexing types
                    SongsCategorizationHelper(viewModel = viewModel, typeName = activeSubTab.label)
                }
            }
        }
    }

    if (playlistDialogOpen) {
        Dialog(
            onDismissRequest = { playlistDialogOpen = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCoal)
                    .border(1.dp, OrbitViolet, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text("Create Music Portal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        label = { Text("Playlist Name", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.Gray),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = playlistDescInput,
                        onValueChange = { playlistDescInput = it },
                        label = { Text("Portal Description", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.Gray),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { playlistDialogOpen = false }) {
                            Text("CANCEL", color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (playlistNameInput.isNotBlank()) {
                                    viewModel.createPlaylist(playlistNameInput, playlistDescInput)
                                    playlistNameInput = ""
                                    playlistDescInput = ""
                                    playlistDialogOpen = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrbitViolet)
                        ) {
                            Text("READY", color = TextWhite)
                        }
                    }
                }
            }
        }
    }
}

enum class LibrarySubTab(val label: String) {
    SONGS("Songs"),
    FAVORITES("Favorites"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres")
}

@Composable
fun EmptyLibraryState(isScanning: Boolean, msg: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isScanning) Icons.Filled.WifiProtectedSetup else Icons.Outlined.MusicOff,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = msg,
            fontSize = 14.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SongRowItem(
    song: Song,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onEditTags: () -> Unit,
    onDelete: () -> Unit,
    playlists: List<Playlist>,
    onAddToPlaylist: (Long) -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCoal)
            .border(1.dp, Color(0x1F6B7280), RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniature album circle icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(OrbitViolet, CyberCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (song.isHighRes) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0x3BFFB300), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("FLAC", fontSize = 8.sp, color = HighResColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        text = "${song.artist} • ${song.album}",
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Instantly actionable Favorite heart toggle
                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) NeonPink else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(onClick = { expandedMenu = !expandedMenu }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Color.Gray)
                    }

                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        modifier = Modifier.background(SurfaceCoal)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Metadata / Lyrics", color = TextWhite) },
                            leadingIcon = { Icon(Icons.Filled.Edit, null, tint = CyberCyan) },
                            onClick = {
                                onEditTags()
                                expandedMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Track Link", color = TextWhite) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = NeonPink) },
                            onClick = {
                                onDelete()
                                expandedMenu = false
                            }
                        )

                        if (playlists.isNotEmpty()) {
                            Divider(color = Color.Gray.copy(alpha = 0.2f))
                            playlists.forEach { pl ->
                                DropdownMenuItem(
                                    text = { Text("Add to: ${pl.name}", color = TextWhite) },
                                    leadingIcon = { Icon(Icons.Filled.PlaylistAdd, null, tint = OrbitViolet) },
                                    onClick = {
                                        onAddToPlaylist(pl.id)
                                        expandedMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistItemRow(
    playlist: Playlist,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCoal)
            .border(1.dp, Color(0x1F6B7280), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Folder, null, tint = OrbitViolet, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(playlist.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text(playlist.description, fontSize = 11.sp, color = TextMuted)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Playlist", tint = NeonPink.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun SongsCategorizationHelper(viewModel: MusicViewModel, typeName: String) {
    val songs by viewModel.allSongs.collectAsState()
    val groupMap = remember(songs, typeName) {
        when (typeName.lowercase()) {
            "albums" -> songs.groupBy { it.album }
            "artists" -> songs.groupBy { it.artist }
            else -> songs.groupBy { it.genre }
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groupMap.forEach { (categoryName, songList) ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCoal)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Audiotrack, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(categoryName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                            Text("${songList.size} tracks", color = TextMuted, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.playerManager.setQueue(songList, 0) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1F00E5FF)),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("PLAY MATRIX MIX", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

// --------------------------------------------------------
// SECTION 3: SOUND EFFECTS STUDIO
// --------------------------------------------------------
@Composable
fun SoundEffectsScreen(viewModel: MusicViewModel, themeMode: ThemeMode) {
    val profiles by viewModel.allAudioProfiles.collectAsState()
    val context = LocalContext.current

    val pBass by viewModel.playerManager.bassBoost.collectAsState()
    val pTreble by viewModel.playerManager.trebleBoost.collectAsState()
    val pVocal by viewModel.playerManager.vocalBoost.collectAsState()
    val pVirt by viewModel.playerManager.virtualizer.collectAsState()
    val pSurr by viewModel.playerManager.surroundSound.collectAsState()
    val pSound3D by viewModel.playerManager.sound3D.collectAsState()
    val pReverb by viewModel.playerManager.reverbPreset.collectAsState()
    val pEQBands by viewModel.playerManager.equalizerBands.collectAsState()
    
    val pHeadphoneConnected by viewModel.playerManager.headphoneConnected.collectAsState()
    val pHeadphoneType by viewModel.playerManager.headphoneType.collectAsState()
    val pAutoPauseDisconnected by viewModel.playerManager.autoPauseDisconnected.collectAsState()
    val pAutoResumeConnected by viewModel.playerManager.autoResumeConnected.collectAsState()
    val pHearingSafetyLimit by viewModel.playerManager.hearingSafetyLimit.collectAsState()
    val pHearingSafetyLevelDb by viewModel.playerManager.hearingSafetyLevelDb.collectAsState()
    val pSoundCalibrationPassed by viewModel.playerManager.soundCalibrationPassed.collectAsState()
    val pBluetoothOptimizationEnabled by viewModel.playerManager.bluetoothOptimizationEnabled.collectAsState()
    val pLowLatencyMode by viewModel.playerManager.lowLatencyMode.collectAsState()
    val pHiResWirelessAudio by viewModel.playerManager.hiResWirelessAudio.collectAsState()
    val pSmartCodec by viewModel.playerManager.smartCodec.collectAsState()
    val pActiveBluetoothDevice by viewModel.playerManager.activeBluetoothDevice.collectAsState()
    val pBitPerfectMode by viewModel.playerManager.bitPerfectMode.collectAsState()
    val pLosslessPlaybackEngine by viewModel.playerManager.losslessPlaybackEngine.collectAsState()
    val pAudioProcessingBitDepth by viewModel.playerManager.audioProcessingBitDepth.collectAsState()
    val pAiAudioTuningEnabled by viewModel.playerManager.aiAudioTuningEnabled.collectAsState()
    val pDynamicLoudness by viewModel.playerManager.dynamicLoudness.collectAsState()
    val pAudioEnhancementMode by viewModel.playerManager.audioEnhancementMode.collectAsState()

    val mainText = if (themeMode == ThemeMode.LIGHT) TextDark else TextWhite
    val subText = if (themeMode == ThemeMode.LIGHT) TextDark.copy(alpha = 0.6f) else TextMuted

    var customPresetName by remember { mutableStateOf("") }
    var calibrationProgress by remember { mutableStateOf(0f) }
    var isCalibrating by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 94.dp, bottom = 90.dp)
    ) {
        // HEADER TITLE block
        item {
            Text(
                text = "SOUND EFFECTS STUDIO",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                color = OrbitViolet
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Acoustic Tuning Grid",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = mainText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.Tune, null, tint = CyberCyan, modifier = Modifier.size(24.dp))
            }
            Text("Professional-grade 64-band mixing, intelligent headphone detection, and safe hearing logic", fontSize = 12.sp, color = subText)
            Spacer(modifier = Modifier.height(20.dp))
        }

        // SMART HEADPHONE DETECTION PANEL
        item {
            Text(
                text = "Smart Headphone Tuning Grid",
                color = mainText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Status Badge Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (pHeadphoneConnected) Color(0x3B00E5FF) else Color(0x1F667085))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (pHeadphoneConnected) Icons.Filled.Headphones else Icons.Filled.HeadsetOff,
                            contentDescription = "Headphone status",
                            tint = if (pHeadphoneConnected) CyberCyan else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (pHeadphoneConnected) "HEADPHONES ACTIVE" else "INTERNAL SPEAKER ACTIVE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pHeadphoneConnected) CyberCyan else TextWhite
                            )
                            Text(
                                text = if (pHeadphoneConnected) "Profile calibrated: $pHeadphoneType" else "Dynamic outrun equalizers active.",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Simulated hardware selector buttons for test environments
                    Text("Simulate Connected Hardware Devices:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Disconnected", "Wired", "Sony WH-1000XM5", "Wireless Studio Pro Earbuds").forEach { type ->
                            val active = (type == "Disconnected" && !pHeadphoneConnected) || (pHeadphoneConnected && pHeadphoneType == type)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) OrbitViolet.copy(alpha = 0.3f) else Color(0x1F2B2B2B))
                                    .border(1.dp, if (active) OrbitViolet else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (type == "Disconnected") {
                                            viewModel.playerManager.setHeadphoneConnected(false)
                                        } else {
                                            viewModel.playerManager.applyHeadphoneOptimizedEQ(type)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (type) {
                                        "Sony WH-1000XM5" -> "XM5 BT"
                                        "Wireless Studio Pro Earbuds" -> "Buds Pro"
                                        else -> type
                                    },
                                    fontSize = 10.sp,
                                    color = if (active) CyberCyan else TextWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Safe hearing monitoring circular bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("Hearing Safety Decibels Analyzer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("Safe limit: 80dB. Prevent acoustic shock.", fontSize = 11.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val overRisk = pHearingSafetyLevelDb > 80f
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (overRisk) NeonPink.copy(alpha = 0.2f) else Color(0x1F00E5FF))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (overRisk) "⚠️ ACOUSTIC EXP HAZARD ALERT" else "● PASSIVE VOLUME SAFE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (overRisk) NeonPink else CyberCyan
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0x1F000000))
                                .border(1.dp, if (pHearingSafetyLevelDb > 80f) NeonPink else CyberCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.0f", pHearingSafetyLevelDb),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (pHearingSafetyLevelDb > 80f) NeonPink else CyberCyan
                                )
                                Text("dB", fontSize = 9.sp, color = TextMuted)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Headphone settings toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto Pause unplugged headphones", fontSize = 12.sp, color = TextWhite)
                        Switch(
                            checked = pAutoPauseDisconnected,
                            onCheckedChange = { viewModel.playerManager.setAutoPauseDisconnected(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0x3B00E5FF))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto Resume plugged headphones", fontSize = 12.sp, color = TextWhite)
                        Switch(
                            checked = pAutoResumeConnected,
                            onCheckedChange = { viewModel.playerManager.setAutoResumeConnected(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0x3B00E5FF))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Limit volume protecting safe levels", fontSize = 12.sp, color = TextWhite)
                        Switch(
                            checked = pHearingSafetyLimit,
                            onCheckedChange = { viewModel.playerManager.setHearingSafetyLimit(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0x3B00E5FF))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Personalized sound calibration sweep action
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isCalibrating = true
                                calibrationProgress = 0f
                                while (calibrationProgress < 1f) {
                                    delay(80)
                                    calibrationProgress += 0.05f
                                }
                                viewModel.playerManager.setSoundCalibrationPassed(true)
                                isCalibrating = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (pSoundCalibrationPassed) Color(0x3B00E5FF) else OrbitViolet),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCalibrating
                    ) {
                        Text(
                            text = if (isCalibrating) "CALIBRATING SWEEP ${String.format("%.0f%%", calibrationProgress * 100f)}"
                                   else if (pSoundCalibrationPassed) "RE-RUN PERSONAL SOUND CALIBRATION (PASSED)"
                                   else "START PERSONAL AUDIO CALIBRATION",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // PROFESSIONAL 64-BAND EQUALIZER PANEL
        item {
            Text(
                text = "64-Band Professional Equalizer Grid",
                color = mainText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Touch-drag curve Canvas or scroll 64 frequency faders precisely:",
                        fontSize = 11.sp,
                        color = subText
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. NEON CUSTOM INTERACTIVE DRAGGABLE CANVAS
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF060606))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        var isDraggingOnCanvas by remember { mutableStateOf(false) }
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { isDraggingOnCanvas = true },
                                        onDragEnd = { isDraggingOnCanvas = false },
                                        onDragCancel = { isDraggingOnCanvas = false },
                                        onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                            change.consume()
                                            val xRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                                            val bandIdx = (xRatio * 63).toInt().coerceIn(0, 63)
                                            val yRatio = (change.position.y / size.height).coerceIn(0f, 1f)
                                            val dbVal = 12f - (yRatio * 24f) // Map 0..1 to +12..-12 dB
                                            
                                            val copyBands = pEQBands.copyOf()
                                            copyBands[bandIdx] = dbVal
                                            
                                            // Smooth out adjacent bands for organic fluid curves
                                            for (offset in 1..4) {
                                                val influence = 1.0f - (offset * 0.22f)
                                                if (influence > 0) {
                                                    if (bandIdx - offset >= 0) {
                                                        val prev = copyBands[bandIdx - offset]
                                                        copyBands[bandIdx - offset] = prev + (dbVal - prev) * influence
                                                    }
                                                    if (bandIdx + offset < 64) {
                                                        val next = copyBands[bandIdx + offset]
                                                        copyBands[bandIdx + offset] = next + (dbVal - next) * influence
                                                    }
                                                }
                                            }
                                            viewModel.playerManager.setEqualizerBands(copyBands)
                                        }
                                    )
                                }
                        ) {
                            // Draw frequency background grid lines
                            val gridCols = 8
                            for (c in 1 until gridCols) {
                                val gridX = (size.width / gridCols) * c
                                drawLine(
                                    color = Color.White.copy(alpha = 0.05f),
                                    start = androidx.compose.ui.geometry.Offset(gridX, 0f),
                                    end = androidx.compose.ui.geometry.Offset(gridX, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            val gridRows = 4
                            for (r in 1 until gridRows) {
                                val gridY = (size.height / gridRows) * r
                                drawLine(
                                    color = Color.White.copy(alpha = 0.05f),
                                    start = androidx.compose.ui.geometry.Offset(0f, gridY),
                                    end = androidx.compose.ui.geometry.Offset(size.width, gridY),
                                    strokeWidth = 1f
                                )
                            }

                            // Draw continuous equalizer bezier signal path
                            val path = androidx.compose.ui.graphics.Path()
                            val bandWidth = size.width / 64f
                            pEQBands.forEachIndexed { i, db ->
                                val x = i * bandWidth + bandWidth / 2f
                                val normY = (12f - db) / 24f // Normalized 0..1
                                val y = normY * size.height
                                
                                if (i == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }
                            
                            drawPath(
                                path = path,
                                brush = Brush.horizontalGradient(listOf(CyberCyan, OrbitViolet)),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 4f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.cornerPathEffect(12f)
                                )
                            )
                        }
                        
                        Text(
                            text = if (isDraggingOnCanvas) "DRAWING FREQUENCY GRID CURVE" else "DRAG TO SHAPE MASTER SIGNAL PATH",
                            color = (if (isDraggingOnCanvas) CyberCyan else OrbitViolet).copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. SCROLLABLE MANUAL MICRO-FADERS
                    Box(modifier = Modifier.height(150.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pEQBands.forEachIndexed { index, decibelValue ->
                                val label = when (index) {
                                    0 -> "10"
                                    8 -> "30"
                                    16 -> "80"
                                    24 -> "250"
                                    32 -> "1k"
                                    40 -> "4k"
                                    48 -> "12k"
                                    56 -> "18k"
                                    63 -> "24k"
                                    else -> ""
                                }
                                
                                Column(
                                    modifier = Modifier.fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = String.format("%.0f", decibelValue),
                                        fontSize = 9.sp,
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Touch drag track
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .width(22.dp)
                                            .pointerInput(index) {
                                            detectDragGestures(
                                                onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                                    change.consume()
                                                    val yRatio = (change.position.y / size.height).coerceIn(0f, 1f)
                                                    val dbVal = 12f - (yRatio * 24f)
                                                    val copyBands = pEQBands.copyOf()
                                                    copyBands[index] = dbVal.coerceIn(-12f, 12f)
                                                    viewModel.playerManager.setEqualizerBands(copyBands)
                                                }
                                            )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Grey background vertical fader
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(2.dp)
                                                .background(Color.Gray.copy(alpha = 0.2f))
                                        )

                                        // Vertical slider highlight
                                        val fillRatio = ((decibelValue + 12f) / 24f).coerceIn(0f, 1f)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxHeight(fillRatio)
                                                .width(2.dp)
                                                .background(CyberCyan)
                                        )

                                        // Thumb point
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxHeight(fillRatio)
                                                .offset(y = (-4).dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (index % 2 == 0) CyberCyan else OrbitViolet)
                                        )
                                    }

                                    if (label.isNotEmpty()) {
                                        Text(label, fontSize = 8.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text(".", fontSize = 8.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset Quick Switch buttons
                    Text("Pre-Architected Tuning Presets:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Studio Mastering", "Audiophile Reference", "Concert Hall").forEach { mode ->
                                val active = pAudioEnhancementMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) CyberCyan.copy(alpha = 0.25f) else Color(0x3B505050))
                                        .border(1.dp, if (active) CyberCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.playerManager.setAudioEnhancementMode(mode) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(mode.split(" ")[0], fontSize = 10.sp, color = if (active) CyberCyan else TextWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Cinema Sound", "Gaming Sound", "Podcast").forEach { mode ->
                                val active = pAudioEnhancementMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) CyberCyan.copy(alpha = 0.25f) else Color(0x3B505050))
                                        .border(1.dp, if (active) CyberCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.playerManager.setAudioEnhancementMode(mode) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(mode.split(" ")[0], fontSize = 10.sp, color = if (active) CyberCyan else TextWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. STORE UNLIMITED CUSTOM PRESETS IN DB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customPresetName,
                            onValueChange = { customPresetName = it },
                            placeholder = { Text("Preset Name...", fontSize = 11.sp, color = Color.Gray) },
                            textStyle = TextStyle(fontSize = 11.sp, color = TextWhite),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                focusedContainerColor = Color(0xFF0F0F0F)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customPresetName.isNotBlank()) {
                                    viewModel.createAudioProfile(
                                        name = customPresetName,
                                        bandsList = pEQBands,
                                        bassBoost = pBass,
                                        virt = pVirt,
                                        surround = pSurr,
                                        reverb = pReverb,
                                        sound3D = pSound3D
                                    )
                                    customPresetName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("SAVE PRESET", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val neutral = FloatArray(64) { 0f }
                            viewModel.playerManager.setEqualizerBands(neutral)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3B667085)),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("RESET INDIVIDUAL FREQUENCIES", color = TextWhite, fontSize = 11.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // PHYSICAL HARDWARE KNOBS (ECHO & SOUND EFFECTS sliders)
        item {
            Text("Hardware DSP Accelerators", color = mainText, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Bass Booster
                    CustomEffectControlRow(
                        title = "Subwoofer Bass Booster",
                        value = pBass,
                        onValueChange = { viewModel.playerManager.setBassBoost(it) },
                        accentColor = NeonPink
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Treble Enhancer
                    CustomEffectControlRow(
                        title = "Air Treble Enhancer",
                        value = pTreble,
                        onValueChange = { viewModel.playerManager.setTrebleBoost(it) },
                        accentColor = CyberCyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Vocal Booster
                    CustomEffectControlRow(
                        title = "Dialogue Vocal Booster",
                        value = pVocal,
                        onValueChange = { viewModel.playerManager.setVocalBoost(it) },
                        accentColor = OrbitViolet
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Atmospheric Virtualizer
                    CustomEffectControlRow(
                        title = "Atmospheric Sound Field Virtualizer",
                        value = pVirt,
                        onValueChange = { viewModel.playerManager.setVirtualizer(it) },
                        accentColor = CyberCyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Surround Sound Engine
                    CustomEffectControlRow(
                        title = "Surround Audio Engine Stage",
                        value = pSurr,
                        onValueChange = { viewModel.playerManager.setSurroundSound(it) },
                        accentColor = OrbitViolet
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // AUDIOPHILE SETTINGS & BLUETOOTH ENHANCER
        item {
            Text("Audiophile Matrix Settings", color = mainText, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Bit-Perfect Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Direct Bit-Perfect Output Mode", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Bypass Android system mixer core directly.", fontSize = 10.sp, color = TextMuted)
                        }
                        Switch(
                            checked = pBitPerfectMode,
                            onCheckedChange = { viewModel.playerManager.setBitPerfectMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0x3B00E5FF))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lossless Engine
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("32-Bit Lossless Audio Engine", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Render lossless formats (FLAC/ALAC) with high float precision.", fontSize = 10.sp, color = TextMuted)
                        }
                        Switch(
                            checked = pLosslessPlaybackEngine,
                            onCheckedChange = { viewModel.playerManager.setLosslessPlaybackEngine(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0x3B00E5FF))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // DSP Bit Depth selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Floating DSP Bit-Depth Precise", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Render calculations in extreme float depths.", fontSize = 10.sp, color = TextMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf(24, 32).forEach { depth ->
                                val selected = depth == pAudioProcessingBitDepth
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
                                        .border(1.dp, if (selected) CyberCyan else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.playerManager.setAudioProcessingBitDepth(depth) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("${depth}-bit", fontSize = 10.sp, color = if (selected) CyberCyan else TextMuted, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bluetooth Optimization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bluetooth 5.3 Low-Latency Optimization", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Mitigate transport delays specifically when in games and calls.", fontSize = 10.sp, color = TextMuted)
                        }
                        Switch(
                            checked = pLowLatencyMode,
                            onCheckedChange = { viewModel.playerManager.setLowLatencyMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0x3B00E5FF))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // High-Res Codec Switcher status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("High-Res Wireless Bluetooth Codec", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Active codec: $pSmartCodec", fontSize = 10.sp, color = TextMuted)
                        }
                        Box {
                            var expCodec by remember { mutableStateOf(false) }
                            Button(
                                onClick = { expCodec = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1F00E5FF)),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(pSmartCodec.split(" ")[0], color = CyberCyan, fontSize = 10.sp)
                            }
                            DropdownMenu(
                                expanded = expCodec,
                                onDismissRequest = { expCodec = false },
                                modifier = Modifier.background(SurfaceCoal)
                            ) {
                                listOf("LDAC (24bit/96kHz - 990kbps)", "aptX Adaptive (flexible/high bitrate)", "AAC (High Quality M4A)", "SBC (Standard Audio)").forEach { codec ->
                                    DropdownMenuItem(
                                        text = { Text(codec, color = TextWhite, fontSize = 11.sp) },
                                        onClick = {
                                            viewModel.playerManager.setSmartCodec(codec)
                                            expCodec = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // AI Auto tuning logic toggle switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Dynamic Spatial-Chamber Tuning", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Analyze environment noise and optimize acoustic clarity.", fontSize = 10.sp, color = TextMuted)
                        }
                        Switch(
                            checked = pAiAudioTuningEnabled,
                            onCheckedChange = { viewModel.playerManager.setAiAudioTuningEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0x3B00E5FF))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reverb preset selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Hall Reverb preset", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Box {
                            var expMenu by remember { mutableStateOf(false) }
                            Button(
                                onClick = { expMenu = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1F00E5FF)),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(pReverb, color = CyberCyan)
                            }

                            DropdownMenu(
                                expanded = expMenu,
                                onDismissRequest = { expMenu = false },
                                modifier = Modifier.background(SurfaceCoal)
                            ) {
                                listOf("Off", "Studio", "Club", "Hall", "Arena").forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(preset, color = TextWhite) },
                                        onClick = {
                                            viewModel.playerManager.setReverbPreset(preset)
                                            expMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Loudness auto balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Master Dynamic Loudness Equalization", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Maintain balanced dynamic ranges spanning different audio tracks.", fontSize = 10.sp, color = TextMuted)
                        }
                        Switch(
                            checked = pDynamicLoudness,
                            onCheckedChange = { viewModel.playerManager.setDynamicLoudness(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0x3B00E5FF))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Spatial Logic direct
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include 3D Audio Decoders", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Switch(
                            checked = pSound3D,
                            onCheckedChange = { viewModel.playerManager.setSound3D(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0x3B00E5FF))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CustomEffectControlRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    accentColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("$value%", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.Gray.copy(alpha = 0.2f)
            )
        )
    }
}

// --------------------------------------------------------
// SECTION 4: DETAILED NOW PLAYING SCREENS
// --------------------------------------------------------
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    themeMode: ThemeMode,
    onNavigateBack: () -> Unit
) {
    val currentSong by viewModel.playerManager.currentSong.collectAsState()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()
    val positionMs by viewModel.playerManager.currentPositionMs.collectAsState()

    val pSpeed by viewModel.playerManager.playbackSpeed.collectAsState()
    val pPitch by viewModel.playerManager.pitch.collectAsState()
    val shuffleMode by viewModel.playerManager.shuffleMode.collectAsState()
    val repeatMode by viewModel.playerManager.repeatMode.collectAsState()
    val visualizerAmplitudes by viewModel.playerManager.visualizerAmplitudes.collectAsState()
    val timerRemaining by viewModel.playerManager.sleepTimerSec.collectAsState()

    var isLyricsOpen by remember { mutableStateOf(false) }
    var isAdvancedOptionsOpen by remember { mutableStateOf(false) }

    val mainColor = if (themeMode == ThemeMode.LIGHT) TextDark else TextWhite
    val secondaryColor = if (themeMode == ThemeMode.LIGHT) Color.DarkGray else TextMuted

    // Rotating and album glow triggers
    val infiniteTransition = rememberInfiniteTransition(label = "rotating_artwork")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "art_rot"
    )

    if (currentSong == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.MusicOff, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text("Engine Offline", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = mainColor)
            Text("Navigate to Library, select a premium master track matrix to start streaming.", textAlign = TextAlign.Center, color = secondaryColor, fontSize = 13.sp)
        }
        return
    }

    val song = currentSong!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Swipe left/right gesture controls - Premium Microinteraction
                detectHorizontalDragGestures(
                    onDragEnd = {},
                    onHorizontalDrag = { change, dragAmount ->
                        if (abs(dragAmount) > 40) {
                            if (dragAmount < 0) {
                                viewModel.playerManager.nextTrack()
                            } else {
                                viewModel.playerManager.previousTrack()
                            }
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Back to list", tint = mainColor, modifier = Modifier.size(28.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = CyberCyan
                    )
                    if (song.isHighRes) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.WorkspacePremium, null, tint = HighResColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(song.highResFormat, fontSize = 9.sp, color = HighResColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                IconButton(onClick = { isAdvancedOptionsOpen = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Equalizers Preset", tint = mainColor, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Elegant Editorial Artwork Container with Glassmorphic Accent Ring
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .rotate(if (isPlaying) rotationAngle else 0f),
                contentAlignment = Alignment.Center
            ) {
                // Glassmorphic Outer Blur Glow Ring (from-white/10 to transparent border)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(36.dp)
                        )
                )

                // Main Square Rounded Artwork Card (rounded-[32px])
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF581C87).copy(alpha = 0.4f),      // Purple-900/40
                                    Color(0xFF1A1A1A),                          // Charcoal Dark
                                    Color(0xFF7C2D12).copy(alpha = 0.4f)       // Orange-900/40
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Concentric Vinyl Engraved Lines (Subtle Overlay radial texture)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (i in 1..9) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.04f),
                                radius = size.width * (0.04f * i),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                            )
                        }
                    }

                    // Large outer circle structure (w-32 h-32 -> ~110dp)
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .border(4.dp, Color.White.copy(alpha = 0.05f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner circle with gradient from purple (CyberCyan) to orange (NeonPink)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(CyberCyan, NeonPink)
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚡",
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Track detailed descriptive title
            Text(
                text = song.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = mainColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${song.artist} • ${song.album}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Light,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = secondaryColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Real-time bouncing visualizer spectrum graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    visualizerAmplitudes.forEach { weight ->
                        val barHeight = (weight * 36).dp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(barHeight)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(CyberCyan, OrbitViolet)
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Seek Position bar slider
            val totalSec = song.durationMs / 1000L
            val elapsedSec = positionMs / 1000L
            Slider(
                value = positionMs.toFloat(),
                onValueChange = { viewModel.playerManager.seekTo(it.toLong()) },
                valueRange = 0f..song.durationMs.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = CyberCyan,
                    activeTrackColor = CyberCyan,
                    inactiveTrackColor = Color.Gray.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60),
                    fontSize = 11.sp,
                    color = secondaryColor,
                    fontWeight = FontWeight.Bold
                )

                if (timerRemaining > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Timer, null, tint = NeonPink, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%02d:%02d left", timerRemaining / 60, timerRemaining % 60),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPink
                        )
                    }
                }

                Text(
                    text = String.format("%02d:%02d", totalSec / 60, totalSec % 60),
                    fontSize = 11.sp,
                    color = secondaryColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Main Media Player keys row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle mode toggle button
                IconButton(
                    onClick = {
                        val n = when (shuffleMode) {
                            AudioPlayerManager.ShuffleMode.OFF -> AudioPlayerManager.ShuffleMode.MOOD_SHUFFLE
                            else -> AudioPlayerManager.ShuffleMode.OFF
                        }
                        viewModel.playerManager.setShuffleMode(n)
                    }
                ) {
                    Icon(
                        imageVector = if (shuffleMode == AudioPlayerManager.ShuffleMode.OFF) Icons.Filled.Shuffle else Icons.Filled.PowerSettingsNew,
                        contentDescription = "Shuffle",
                        tint = if (shuffleMode != AudioPlayerManager.ShuffleMode.OFF) CyberCyan else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Previous
                IconButton(onClick = { viewModel.playerManager.previousTrack() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev", tint = mainColor, modifier = Modifier.size(36.dp))
                }

                // Play Pause with high-end spring feedback
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CyberCyan, OrbitViolet)
                            )
                        )
                        .clickable { viewModel.playerManager.togglePlayPause() }
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "PlayPauseButton",
                        tint = TextWhite,
                        modifier = Modifier
                            .testTag("play_pause_button")
                            .size(36.dp)
                    )
                }

                // Next
                IconButton(onClick = { viewModel.playerManager.nextTrack() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = mainColor, modifier = Modifier.size(36.dp))
                }

                // Repeat mode
                IconButton(
                    onClick = {
                        val n = when (repeatMode) {
                            AudioPlayerManager.RepeatMode.OFF -> AudioPlayerManager.RepeatMode.REPEAT_ALL
                            AudioPlayerManager.RepeatMode.REPEAT_ALL -> AudioPlayerManager.RepeatMode.REPEAT_ONE
                            AudioPlayerManager.RepeatMode.REPEAT_ONE -> AudioPlayerManager.RepeatMode.OFF
                        }
                        viewModel.playerManager.setRepeatMode(n)
                    }
                ) {
                    val repeatIcon = when (repeatMode) {
                        AudioPlayerManager.RepeatMode.REPEAT_ONE -> Icons.Filled.RepeatOne
                        else -> Icons.Filled.Repeat
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != AudioPlayerManager.RepeatMode.OFF) CyberCyan else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Swipe / lyrics expansion bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { isLyricsOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3B667085)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.QueueMusic, null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VIEW SONGS LYRICS", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Lyrics Drawer Sheet
        if (isLyricsOpen) {
            LyricsDisplaySheet(
                song = song,
                currentPosMs = positionMs,
                onClose = { isLyricsOpen = false }
            )
        }

        // Advanced Tuning drawer
        if (isAdvancedOptionsOpen) {
            TuningSettingsSheet(
                viewModel = viewModel,
                themeMode = themeMode,
                speedValue = pSpeed,
                pitchValue = pPitch,
                onClose = { isAdvancedOptionsOpen = false }
            )
        }
    }
}

@Composable
fun LyricsDisplaySheet(
    song: Song,
    currentPosMs: Long,
    onClose: () -> Unit
) {
    // Elegant sliding dialog backdrop representing custom translucent lyrics layout
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBackground.copy(alpha = 0.94f))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(song.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Active Sync Karaoke", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.HighlightOff, "Close", tint = NeonPink, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (song.lyrics.isBlank()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Description, null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No stored lyrics matched for this index.", color = TextMuted)
                            Text("Customize using the Library tag editor!", color = CyberCyan, fontSize = 12.sp)
                        }
                    }
                } else {
                    val parsedLyrics = remember(song.lyrics) { parseLrcLyrics(song.lyrics) }
                    val lazyListState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(lazyListState)
                    ) {
                        parsedLyrics.forEach { line ->
                            val isActive = currentPosMs >= line.timeMs &&
                                    (parsedLyrics.indexOf(line) == parsedLyrics.lastIndex || currentPosMs < parsedLyrics[parsedLyrics.indexOf(line) + 1].timeMs)

                            val animatedScale by animateFloatAsState(if (isActive) 1.15f else 1.0f, label = "karaokescale")
                            val textA = if (isActive) 1.0f else 0.4f

                            Text(
                                text = line.text,
                                fontSize = 18.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (isActive) CyberCyan else TextWhite,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .alpha(textA)
                                    .graphicsLayer {
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                    },
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

data class LrcLine(val timeMs: Long, val text: String)

fun parseLrcLyrics(lrc: String): List<LrcLine> {
    val results = ArrayList<LrcLine>()
    val lines = lrc.split("\n")
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            val bracketIndex = trimmed.indexOf("]")
            val timePortion = trimmed.substring(1, bracketIndex)
            val lyricContent = trimmed.substring(bracketIndex + 1).trim()

            // Time format is MM:SS.SS or MM:SS
            try {
                val parts = timePortion.split(":")
                val minutes = parts[0].toLong()
                val secondsParts = parts[1].split(".")
                val seconds = secondsParts[0].toLong()
                val millisFraction = if (secondsParts.size > 1) secondsParts[1].toInt() * 10 else 0
                val totalMs = (minutes * 60 * 1000) + (seconds * 1000) + millisFraction
                results.add(LrcLine(totalMs, lyricContent))
            } catch (e: Exception) {
                // Ignore matching formatting issues
            }
        } else {
            results.add(LrcLine(0L, trimmed))
        }
    }
    return results
}

@Composable
fun TuningSettingsSheet(
    viewModel: MusicViewModel,
    themeMode: ThemeMode,
    speedValue: Float,
    pitchValue: Float,
    onClose: () -> Unit
) {
    val gaplessEnabled by viewModel.playerManager.gaplessPlayback.collectAsState()
    val timerSec by viewModel.playerManager.sleepTimerSec.collectAsState()

    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCoal)
                .border(1.dp, CyberCyan, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Digital Physics Console", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 16.sp)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, null, tint = NeonPink)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Playback speed control
                Text(
                    text = String.format("Playback Speed: %.2fx", speedValue),
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Slider(
                    value = speedValue,
                    onValueChange = { viewModel.playerManager.setPlaybackSpeed(it) },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pitch adjustment
                Text(
                    text = String.format("Acoustic Pitch: %.2fx", pitchValue),
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Slider(
                    value = pitchValue,
                    onValueChange = { viewModel.playerManager.setPitch(it) },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = OrbitViolet, activeTrackColor = OrbitViolet)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Gapless playback toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gapless Playback Logic", color = TextWhite, fontSize = 13.sp)
                    Switch(
                        checked = gaplessEnabled,
                        onCheckedChange = { viewModel.playerManager.setGaplessPlayback(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = Color(0x3B00E5FF))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sleep Timer countdown setting
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Grid Sleep Timer", color = TextWhite, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (timerSec > 0) {
                            TextButton(onClick = { viewModel.playerManager.cancelSleepTimer() }) {
                                Text("CANCEL", color = NeonPink, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            listOf(15, 30, 45).forEach { min ->
                                Button(
                                    onClick = { viewModel.playerManager.startSleepTimer(min) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x1F00E5FF)),
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("${min}m", color = CyberCyan, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------
// SECTION 5: SMART AI PLAYLIST ASSISTANT
// --------------------------------------------------------
@Composable
fun AIPlaybackPanel(viewModel: MusicViewModel, themeMode: ThemeMode) {
    val inputVal by viewModel.aiRecommendationQuery.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val recNotes by viewModel.aiRecommendationNotes.collectAsState()
    val recIds by viewModel.aiRecommendedSongIds.collectAsState()
    val localSongs by viewModel.allSongs.collectAsState()

    val matchingSongsList = remember(recIds, localSongs) {
        localSongs.filter { recIds.contains(it.id) }
    }

    val mainText = if (themeMode == ThemeMode.LIGHT) TextDark else TextWhite
    val subText = if (themeMode == ThemeMode.LIGHT) TextDark.copy(alpha = 0.6f) else TextMuted

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 94.dp, bottom = 90.dp)
    ) {
        item {
            Text(
                text = "TBM SMART COGNITIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                color = CyberCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Smart AI Music Guide",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = mainText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.AutoAwesome, null, tint = HighResColor, modifier = Modifier.size(24.dp))
            }
            Text("Powered by offline local engines & Gemini cloud networks", fontSize = 12.sp, color = subText)
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Prompt input block
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Acoustic Natural Query",
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Prompt examples: 'give me uplifting beats with bpm > 120', 'night city drive lofi music', or 'sad melancholic cinematic mood'.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = { viewModel.updateAiRecommendationQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCoal, RoundedCornerShape(12.dp)),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.Gray),
                        placeholder = { Text("Describe the theme of your next journey...", color = TextMuted, fontSize = 13.sp) },
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.generateAIPlaylist() },
                        colors = ButtonDefaults.buttonColors(containerColor = OrbitViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("smart_generate_button")
                            .height(44.dp),
                        enabled = !isAiLoading && inputVal.isNotBlank()
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(color = TextWhite, modifier = Modifier.size(20.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Cyclone, null, tint = TextWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("LAUNCH RECURSIVE MATRIX", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // AI Curation Narrative Out
        if (recNotes.isNotEmpty()) {
            item {
                Text("AI Curation Log", fontWeight = FontWeight.Bold, color = mainText, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SelectionContainer {
                        Text(
                            text = recNotes,
                            color = TextWhite,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Resulting songs
        if (matchingSongsList.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Matrix Playlist Matches", fontWeight = FontWeight.Bold, color = mainText, fontSize = 15.sp)
                    Button(
                        onClick = { viewModel.startAIAssistantPlay() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3B00E5FF)),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("PLAY SELECTION", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            items(matchingSongsList) { song ->
                SongRowItem(
                    song = song,
                    onPlay = { viewModel.playSongDirect(song, matchingSongsList) },
                    onFavorite = { viewModel.toggleFavorite(song) },
                    onEditTags = { viewModel.openTagEditorFor(song) },
                    onDelete = { viewModel.deleteSongFromLibrary(song.id) },
                    playlists = emptyList(),
                    onAddToPlaylist = {}
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

// --------------------------------------------------------
// SHARDED LAYOUT ELEMENTS (GLASSMORPHISM CARDS)
// --------------------------------------------------------
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCoal)
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(GlassBorderColor, Color.Transparent)
                ),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(content = content)
    }
}

@Composable
fun MiniPlayerStrip(
    viewModel: MusicViewModel,
    onClick: () -> Unit
) {
    val currentSong by viewModel.playerManager.currentSong.collectAsState()
    val isPlaying by viewModel.playerManager.isPlaying.collectAsState()

    if (currentSong == null) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCoal)
            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Spinning mini artwork disc
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NeonPink, OrbitViolet)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Album, null, tint = TextWhite, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        currentSong!!.title,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        currentSong!!.artist,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.playerManager.togglePlayPause() }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                        contentDescription = "Mini player action",
                        tint = CyberCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = { viewModel.playerManager.nextTrack() }) {
                    Icon(Icons.Filled.SkipNext, null, tint = TextWhite, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// --------------------------------------------------------
// GLOBAL TAG LOGIC / METADATA DIALOG
// --------------------------------------------------------
@Composable
fun TagEditorDialog(
    song: Song,
    onSave: (Long, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var genre by remember { mutableStateOf(song.genre) }
    var lyrics by remember { mutableStateOf(song.lyrics) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCoal)
                .border(1.dp, OrbitViolet, RoundedCornerShape(16.dp))
                .padding(18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                Text(
                    text = "Professional Tag & Lyrics Editor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextWhite
                )
                Text(
                    text = "Audio format: ${song.highResFormat}.",
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.Gray),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist Name", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.Gray),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album Title", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.Gray),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre Type", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.Gray),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it },
                    label = { Text("Stored Offline Karaoke Lyrics ([MM:SS.SS] Text)", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.Gray),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(song.id, title, artist, album, genre, lyrics) },
                        colors = ButtonDefaults.buttonColors(containerColor = OrbitViolet)
                    ) {
                        Text("SAVE REVISIONS", color = TextWhite)
                    }
                }
            }
        }
    }
}
