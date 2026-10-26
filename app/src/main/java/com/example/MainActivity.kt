package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.JellyRemoteViewModel
import com.example.ui.Screen
import com.example.ui.components.CastDeviceDialog
import com.example.ui.components.HeaderBar
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.ServerDiscoveryDialog
import com.example.ui.screens.EpisodesScreen
import com.example.ui.screens.LibrariesScreen
import com.example.ui.screens.MediaDetailScreen
import com.example.ui.screens.MediaListScreen
import com.example.ui.screens.RemoteControlScreen
import com.example.ui.screens.SeasonsScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.theme.DeepVioletBg
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                JellyCastMainApp()
            }
        }
    }
}

@Composable
fun JellyCastMainApp(viewModel: JellyRemoteViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackStatus by viewModel.playbackStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // Handle Android system back button properly across screens
    BackHandler(enabled = uiState.currentScreen != Screen.SETUP && uiState.currentScreen != Screen.LIBRARIES) {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepVioletBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HeaderBar(
                currentScreen = uiState.currentScreen,
                connectedDevice = uiState.connectedDevice,
                isPlaybackActive = uiState.isPlaybackActive,
                isLoggedIn = uiState.isLoggedIn,
                onBackClick = { viewModel.navigateBack() },
                onCastPillClick = { viewModel.openDeviceDialog() },
                onOpenRemoteClick = { viewModel.openRemote() },
                onLogoutClick = { viewModel.logout() }
            )
        },
        bottomBar = {
            // Persistent Mini Player bar when casting media in the background
            if (uiState.currentScreen != Screen.REMOTE_CONTROL && uiState.currentScreen != Screen.SETUP && uiState.isPlaybackActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    MiniPlayerBar(
                        status = playbackStatus,
                        targetDevice = uiState.connectedDevice,
                        serverUrl = uiState.serverUrl,
                        token = uiState.accessToken,
                        onBarClick = { viewModel.openRemote() },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onStopPlayback = { viewModel.stopPlayback() }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepVioletBg)
        ) {
            AnimatedContent(
                targetState = uiState.currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.SETUP -> {
                        SetupScreen(
                            serverUrl = uiState.serverUrl,
                            username = uiState.username,
                            isAuthenticating = uiState.isAuthenticating,
                            authError = uiState.authError,
                            onServerUrlChange = { viewModel.updateServerUrl(it) },
                            onUsernameChange = { viewModel.updateUsername(it) },
                            onConnectClick = { pass -> viewModel.login(pass) },
                            onDiscoverClick = { viewModel.openServerDiscovery() }
                        )
                    }

                    Screen.LIBRARIES -> {
                        LibrariesScreen(
                            libraries = uiState.libraries,
                            isLoading = uiState.isLoadingLibraries,
                            connectedDevice = uiState.connectedDevice,
                            serverUrl = uiState.serverUrl,
                            onSelectLibrary = { lib -> viewModel.selectLibrary(lib) },
                            onRefresh = { viewModel.loadLibraries() },
                            onOpenCastScanner = { viewModel.openDeviceDialog() }
                        )
                    }

                    Screen.MEDIA_LIST -> {
                        val lib = uiState.selectedLibrary
                        if (lib != null) {
                            MediaListScreen(
                                library = lib,
                                items = uiState.mediaItems,
                                isLoading = uiState.isLoadingMedia,
                                searchQuery = uiState.searchQuery,
                                serverUrl = uiState.serverUrl,
                                token = uiState.accessToken,
                                onSearchChange = { viewModel.updateSearchQuery(it) },
                                onSelectItem = { item -> viewModel.selectMediaItem(item) }
                            )
                        }
                    }

                    Screen.SEASONS -> {
                        val series = uiState.selectedSeries
                        if (series != null) {
                            SeasonsScreen(
                                series = series,
                                seasons = uiState.seasons,
                                isLoading = uiState.isLoadingSeasons,
                                serverUrl = uiState.serverUrl,
                                token = uiState.accessToken,
                                onSelectSeason = { season -> viewModel.selectSeason(season) }
                            )
                        }
                    }

                    Screen.EPISODES -> {
                        val series = uiState.selectedSeries
                        val season = uiState.selectedSeason
                        if (series != null && season != null) {
                            EpisodesScreen(
                                series = series,
                                season = season,
                                episodes = uiState.episodes,
                                isLoading = uiState.isLoadingEpisodes,
                                serverUrl = uiState.serverUrl,
                                token = uiState.accessToken,
                                onSelectEpisode = { ep -> viewModel.selectMediaItem(ep) }
                            )
                        }
                    }

                    Screen.MEDIA_DETAIL -> {
                        val item = uiState.selectedMediaItem
                        if (item != null) {
                            MediaDetailScreen(
                                item = item,
                                connectedDevice = uiState.connectedDevice,
                                serverUrl = uiState.serverUrl,
                                token = uiState.accessToken,
                                audioTranscodeMode = uiState.audioTranscodeMode,
                                selectedAudioTrackIndex = uiState.selectedAudioTrackIndex,
                                selectedSubtitleTrackIndex = uiState.selectedSubtitleTrackIndex,
                                burnSubtitles = uiState.burnSubtitles,
                                onAudioModeChange = { mode -> viewModel.setAudioTranscodeMode(mode) },
                                onAudioTrackChange = { track -> viewModel.setSelectedAudioTrack(track) },
                                onSubtitleTrackChange = { track -> viewModel.setSelectedSubtitleTrack(track) },
                                onBurnSubtitlesChange = { burn -> viewModel.setBurnSubtitles(burn) },
                                onPlayClick = { viewModel.playMedia(item) },
                                onOpenCastScanner = { viewModel.openDeviceDialog() }
                            )
                        }
                    }

                    Screen.REMOTE_CONTROL -> {
                        RemoteControlScreen(
                            status = playbackStatus,
                            targetDevice = uiState.connectedDevice,
                            serverUrl = uiState.serverUrl,
                            token = uiState.accessToken,
                            audioTranscodeMode = uiState.audioTranscodeMode,
                            selectedAudioTrackIndex = uiState.selectedAudioTrackIndex,
                            selectedSubtitleTrackIndex = uiState.selectedSubtitleTrackIndex,
                            burnSubtitles = uiState.burnSubtitles,
                            onAudioModeChange = { mode -> viewModel.setAudioTranscodeMode(mode) },
                            onAudioTrackChange = { track -> viewModel.setSelectedAudioTrack(track) },
                            onSubtitleTrackChange = { track -> viewModel.setSelectedSubtitleTrack(track) },
                            onBurnSubtitlesChange = { burn -> viewModel.setBurnSubtitles(burn) },
                            onApplyStreamSettings = { viewModel.applyPlaybackSettings() },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkip = { sec -> viewModel.skip(sec) },
                            onSeekTo = { sec -> viewModel.seekTo(sec) },
                            onStop = { viewModel.stopPlayback() },
                            onSetVolume = { vol -> viewModel.setVolume(vol) },
                            onAdjustVolume = { delta -> viewModel.adjustVolume(delta) },
                            onToggleMute = { viewModel.toggleMute() },
                            onSendRemoteKey = { key -> viewModel.sendRemoteNavigation(key) },
                            onOpenCastScanner = { viewModel.openDeviceDialog() }
                        )
                    }
                }
            }
        }
    }

    // DLNA Device Scanner Dialog
    CastDeviceDialog(
        isOpen = uiState.showDeviceDialog,
        isScanning = uiState.isScanningDevices,
        devices = uiState.discoveredDevices,
        connectedDevice = uiState.connectedDevice,
        onDismiss = { viewModel.closeDeviceDialog() },
        onSelectDevice = { dev -> viewModel.connectDlnaDevice(dev) },
        onDisconnect = { viewModel.disconnectDlnaDevice() },
        onRescan = { viewModel.scanDlnaDevices() },
        onAddManualDevice = { name, ip, port -> viewModel.addManualDlnaDevice(name, ip, port) }
    )

    // Jellyfin Server Discovery Dialog
    ServerDiscoveryDialog(
        isOpen = uiState.showServerDiscoveryDialog,
        isScanning = uiState.isDiscoveringServers,
        servers = uiState.discoveredServers,
        currentServerUrl = uiState.serverUrl,
        onDismiss = { viewModel.closeServerDiscovery() },
        onSelectServer = { server -> viewModel.selectDiscoveredServer(server) },
        onRescan = { viewModel.discoverServers() }
    )
}
