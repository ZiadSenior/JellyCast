package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cache.MediaCacheManager
import com.example.data.model.DlnaDevice
import com.example.data.model.DlnaPlaybackStatus
import com.example.data.model.JellyfinLibrary
import com.example.data.model.JellyfinMediaItem
import com.example.data.model.TransportState
import com.example.data.network.DlnaController
import com.example.data.network.JellyfinApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import androidx.compose.runtime.Immutable
import com.example.data.model.AudioTranscodeMode
import com.example.data.model.DiscoveredJellyfinServer
import com.example.data.model.MediaTrackInfo
import com.example.data.network.JellyfinServerDiscovery

enum class Screen {
    SETUP,
    LIBRARIES,
    MEDIA_LIST,
    SEASONS,
    EPISODES,
    MEDIA_DETAIL,
    REMOTE_CONTROL
}

@Immutable
data class UiState(
    val currentScreen: Screen = Screen.SETUP,
    val serverUrl: String = "http://192.168.1.100:8096",
    val username: String = "",
    val accessToken: String? = null,
    val userId: String? = null,
    val isLoggedIn: Boolean = false,
    val isAuthenticating: Boolean = false,
    val authError: String? = null,

    val libraries: List<JellyfinLibrary> = emptyList(),
    val isLoadingLibraries: Boolean = false,
    val selectedLibrary: JellyfinLibrary? = null,

    val mediaItems: List<JellyfinMediaItem> = emptyList(),
    val isLoadingMedia: Boolean = false,
    val searchQuery: String = "",
    val selectedMediaItem: JellyfinMediaItem? = null,

    // Audio & Subtitle stream options
    val audioTranscodeMode: AudioTranscodeMode = AudioTranscodeMode.AAC,
    val selectedAudioTrackIndex: Int? = null,
    val selectedSubtitleTrackIndex: Int? = null,
    val burnSubtitles: Boolean = true,

    // Series hierarchy state
    val selectedSeries: JellyfinMediaItem? = null,
    val seasons: List<JellyfinMediaItem> = emptyList(),
    val isLoadingSeasons: Boolean = false,
    val selectedSeason: JellyfinMediaItem? = null,
    val episodes: List<JellyfinMediaItem> = emptyList(),
    val isLoadingEpisodes: Boolean = false,

    val isScanningDevices: Boolean = false,
    val discoveredDevices: List<DlnaDevice> = emptyList(),
    val connectedDevice: DlnaDevice? = null,
    val showDeviceDialog: Boolean = false,

    // Server Discovery state
    val isDiscoveringServers: Boolean = false,
    val discoveredServers: List<DiscoveredJellyfinServer> = emptyList(),
    val showServerDiscoveryDialog: Boolean = false,

    val isPlaybackActive: Boolean = false,
    val toastMessage: String? = null
)

class JellyRemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("jellycast_prefs", Context.MODE_PRIVATE)
    private val apiClient = JellyfinApiClient()
    private val dlnaController = DlnaController(application)
    private val cacheManager = MediaCacheManager(application)
    private val serverDiscovery = JellyfinServerDiscovery(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _playbackStatus = MutableStateFlow(DlnaPlaybackStatus())
    val playbackStatus: StateFlow<DlnaPlaybackStatus> = _playbackStatus.asStateFlow()

    private var playbackTickerJob: Job? = null

    init {
        loadSavedSession()
    }

    private fun loadSavedSession() {
        val savedServer = prefs.getString("server_url", "http://192.168.1.100:8096") ?: "http://192.168.1.100:8096"
        val savedUser = prefs.getString("username", "") ?: ""
        val savedToken = prefs.getString("token", null)
        val savedUserId = prefs.getString("user_id", null)
        val savedAudioModeStr = prefs.getString("audio_transcode_mode", AudioTranscodeMode.AAC.name) ?: AudioTranscodeMode.AAC.name
        val savedAudioMode = try { AudioTranscodeMode.valueOf(savedAudioModeStr) } catch (e: Exception) { AudioTranscodeMode.AAC }
        val savedBurnSubtitles = prefs.getBoolean("burn_subtitles", true)

        if (!savedToken.isNullOrEmpty() && !savedUserId.isNullOrEmpty()) {
            _uiState.update {
                it.copy(
                    serverUrl = savedServer,
                    username = savedUser,
                    accessToken = savedToken,
                    userId = savedUserId,
                    audioTranscodeMode = savedAudioMode,
                    burnSubtitles = savedBurnSubtitles,
                    isLoggedIn = true,
                    currentScreen = Screen.LIBRARIES
                )
            }
            viewModelScope.launch {
                val cached = cacheManager.getCachedLibraries()
                if (!cached.isNullOrEmpty()) {
                    _uiState.update { it.copy(libraries = cached) }
                }
            }
            loadLibraries()
        } else {
            _uiState.update {
                it.copy(
                    serverUrl = savedServer,
                    username = savedUser,
                    audioTranscodeMode = savedAudioMode,
                    burnSubtitles = savedBurnSubtitles
                )
            }
        }
    }

    fun setAudioTranscodeMode(mode: AudioTranscodeMode) {
        prefs.edit().putString("audio_transcode_mode", mode.name).apply()
        _uiState.update { it.copy(audioTranscodeMode = mode) }
    }

    fun setSelectedAudioTrack(index: Int?) {
        _uiState.update { it.copy(selectedAudioTrackIndex = index) }
    }

    fun setSelectedSubtitleTrack(index: Int?) {
        _uiState.update { it.copy(selectedSubtitleTrackIndex = index) }
    }

    fun setBurnSubtitles(burn: Boolean) {
        prefs.edit().putBoolean("burn_subtitles", burn).apply()
        _uiState.update { it.copy(burnSubtitles = burn) }
    }

    fun updateServerUrl(url: String) {
        _uiState.update { it.copy(serverUrl = url, authError = null) }
    }

    fun updateUsername(name: String) {
        _uiState.update { it.copy(username = name, authError = null) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openDeviceDialog() {
        _uiState.update { it.copy(showDeviceDialog = true) }
        scanDlnaDevices()
    }

    fun closeDeviceDialog() {
        _uiState.update { it.copy(showDeviceDialog = false) }
    }

    fun openServerDiscovery() {
        _uiState.update { it.copy(showServerDiscoveryDialog = true) }
        discoverServers()
    }

    fun closeServerDiscovery() {
        _uiState.update { it.copy(showServerDiscoveryDialog = false, isDiscoveringServers = false) }
    }

    fun discoverServers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscoveringServers = true) }
            val servers = serverDiscovery.discoverServers(timeoutMs = 2800)
            _uiState.update {
                it.copy(
                    isDiscoveringServers = false,
                    discoveredServers = servers
                )
            }
            if (servers.isNotEmpty()) {
                _uiState.update {
                    it.copy(toastMessage = "Found ${servers.size} Jellyfin server(s) on network!")
                }
            } else {
                _uiState.update {
                    it.copy(toastMessage = "No Jellyfin servers responded on local network.")
                }
            }
        }
    }

    fun selectDiscoveredServer(server: DiscoveredJellyfinServer) {
        _uiState.update {
            it.copy(
                serverUrl = server.address,
                showServerDiscoveryDialog = false,
                authError = null,
                toastMessage = "Selected: ${server.name} (${server.address})"
            )
        }
        prefs.edit().putString("server_url", server.address).apply()
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun login(password: String) {
        val state = _uiState.value
        val rawUrl = state.serverUrl.trim()
        if (rawUrl.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter Jellyfin Server URL") }
            return
        }
        val cleanUser = state.username.trim()
        if (cleanUser.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter your username") }
            return
        }

        val normalizedUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "http://$rawUrl"
        } else {
            rawUrl
        }.trimEnd('/')

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAuthenticating = true,
                    authError = null,
                    serverUrl = normalizedUrl,
                    username = cleanUser
                )
            }
            val result = apiClient.authenticate(normalizedUrl, cleanUser, password)
            result.onSuccess { response ->
                val token = response.accessToken ?: ""
                val userId = response.user?.id ?: response.sessionInfo?.userId ?: ""

                prefs.edit()
                    .putString("server_url", normalizedUrl)
                    .putString("username", cleanUser)
                    .putString("token", token)
                    .putString("user_id", userId)
                    .apply()

                _uiState.update {
                    it.copy(
                        accessToken = token,
                        userId = userId,
                        isLoggedIn = true,
                        isAuthenticating = false,
                        currentScreen = Screen.LIBRARIES,
                        toastMessage = "Connected to Jellyfin as ${response.user?.name ?: cleanUser}"
                    )
                }
                loadLibraries()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = error.localizedMessage ?: "Unable to connect to server"
                    )
                }
            }
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
        stopPlayback()
        viewModelScope.launch {
            cacheManager.clear()
        }
        _uiState.update {
            UiState(serverUrl = it.serverUrl, username = it.username)
        }
    }

    fun loadLibraries() {
        val state = _uiState.value
        val token = state.accessToken ?: return
        val userId = state.userId ?: ""

        viewModelScope.launch {
            // Instant L1/L2 cache read for instant response
            val cachedLibs = cacheManager.getCachedLibraries()
            if (!cachedLibs.isNullOrEmpty()) {
                _uiState.update { it.copy(libraries = cachedLibs, isLoadingLibraries = false) }
            } else {
                _uiState.update { it.copy(isLoadingLibraries = true) }
            }

            // Fresh background sync
            val res = apiClient.getLibraries(state.serverUrl, userId, token)
            res.onSuccess { libs ->
                cacheManager.saveLibraries(libs)
                _uiState.update { it.copy(libraries = libs, isLoadingLibraries = false) }
            }.onFailure { err ->
                if (cachedLibs.isNullOrEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingLibraries = false,
                            toastMessage = "Failed loading libraries: ${err.message}"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingLibraries = false) }
                }
            }
        }
    }

    fun selectLibrary(library: JellyfinLibrary) {
        _uiState.update {
            it.copy(
                selectedLibrary = library,
                currentScreen = Screen.MEDIA_LIST,
                searchQuery = "",
                selectedSeries = null,
                seasons = emptyList(),
                selectedSeason = null,
                episodes = emptyList()
            )
        }
        loadMediaItems(library)
    }

    private fun loadMediaItems(library: JellyfinLibrary) {
        val state = _uiState.value
        val token = state.accessToken ?: return
        val userId = state.userId ?: ""
        val libraryId = library.id

        viewModelScope.launch {
            // Instant L1/L2 cache check
            val cachedItems = cacheManager.getCachedMediaItems(libraryId)
            if (!cachedItems.isNullOrEmpty()) {
                _uiState.update { it.copy(mediaItems = cachedItems, isLoadingMedia = false) }
            } else {
                _uiState.update { it.copy(isLoadingMedia = true) }
            }

            // Fresh background fetch
            val res = apiClient.getItemsInLibrary(
                serverUrl = state.serverUrl,
                userId = userId,
                token = token,
                parentId = libraryId,
                collectionType = library.collectionType
            )
            res.onSuccess { items ->
                cacheManager.saveMediaItems(libraryId, items)
                _uiState.update { it.copy(mediaItems = items, isLoadingMedia = false) }
            }.onFailure { err ->
                if (cachedItems.isNullOrEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingMedia = false,
                            toastMessage = "Could not load media items: ${err.message}"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingMedia = false) }
                }
            }
        }
    }

    fun selectMediaItem(item: JellyfinMediaItem) {
        viewModelScope.launch {
            cacheManager.saveMediaItem(item)
        }
        if (item.type.equals("Series", ignoreCase = true)) {
            // User clicked on a TV Show -> drill down into its seasons
            selectSeries(item)
        } else if (item.type.equals("Season", ignoreCase = true)) {
            // User clicked on a Season -> drill down into its episodes
            selectSeason(item)
        } else {
            // Movie or Episode or other playable media -> view details
            val defaultAudio = item.audioTracks.firstOrNull { it.isDefault }?.index ?: item.audioTracks.firstOrNull()?.index
            val defaultSub = item.subtitleTracks.firstOrNull { it.isDefault }?.index
            _uiState.update {
                it.copy(
                    selectedMediaItem = item,
                    selectedAudioTrackIndex = defaultAudio,
                    selectedSubtitleTrackIndex = defaultSub,
                    currentScreen = Screen.MEDIA_DETAIL
                )
            }

            // Fetch enriched track metadata (audio codecs & subtitles) from server if not already present
            val state = _uiState.value
            val token = state.accessToken
            if (!token.isNullOrBlank() && (item.audioTracks.isEmpty() || item.subtitleTracks.isEmpty())) {
                viewModelScope.launch {
                    val detailsRes = apiClient.getItemDetails(state.serverUrl, state.userId ?: "", token, item.id)
                    detailsRes.onSuccess { detailedItem ->
                        _uiState.update { current ->
                            if (current.selectedMediaItem?.id == detailedItem.id) {
                                val audio = current.selectedAudioTrackIndex
                                    ?: detailedItem.audioTracks.firstOrNull { it.isDefault }?.index
                                    ?: detailedItem.audioTracks.firstOrNull()?.index
                                val sub = current.selectedSubtitleTrackIndex
                                    ?: detailedItem.subtitleTracks.firstOrNull { it.isDefault }?.index
                                current.copy(
                                    selectedMediaItem = detailedItem,
                                    selectedAudioTrackIndex = audio,
                                    selectedSubtitleTrackIndex = sub
                                )
                            } else current
                        }
                    }
                }
            }
        }
    }

    fun selectSeries(series: JellyfinMediaItem) {
        _uiState.update {
            it.copy(
                selectedSeries = series,
                currentScreen = Screen.SEASONS,
                seasons = emptyList(),
                selectedSeason = null,
                episodes = emptyList(),
                isLoadingSeasons = true
            )
        }
        loadSeasons(series.id)
    }

    fun selectSeason(season: JellyfinMediaItem) {
        val series = _uiState.value.selectedSeries
        _uiState.update {
            it.copy(
                selectedSeason = season,
                currentScreen = Screen.EPISODES,
                episodes = emptyList(),
                isLoadingEpisodes = true
            )
        }
        loadEpisodes(seriesId = series?.id ?: season.seriesId ?: season.id, seasonId = season.id)
    }

    fun loadSeasons(seriesId: String) {
        val state = _uiState.value
        val token = state.accessToken ?: return
        val userId = state.userId ?: ""

        viewModelScope.launch {
            val cacheKey = "seasons_$seriesId"
            val cached = cacheManager.getCachedMediaItems(cacheKey)
            if (!cached.isNullOrEmpty()) {
                _uiState.update { it.copy(seasons = cached, isLoadingSeasons = false) }
            } else {
                _uiState.update { it.copy(isLoadingSeasons = true) }
            }

            val res = apiClient.getSeasons(state.serverUrl, userId, token, seriesId)
            res.onSuccess { seasons ->
                cacheManager.saveMediaItems(cacheKey, seasons)
                _uiState.update { it.copy(seasons = seasons, isLoadingSeasons = false) }
            }.onFailure { err ->
                if (cached.isNullOrEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingSeasons = false,
                            toastMessage = "Could not load seasons: ${err.message}"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingSeasons = false) }
                }
            }
        }
    }

    fun loadEpisodes(seriesId: String, seasonId: String) {
        val state = _uiState.value
        val token = state.accessToken ?: return
        val userId = state.userId ?: ""

        viewModelScope.launch {
            val cacheKey = "episodes_${seriesId}_$seasonId"
            val cached = cacheManager.getCachedMediaItems(cacheKey)
            if (!cached.isNullOrEmpty()) {
                _uiState.update { it.copy(episodes = cached, isLoadingEpisodes = false) }
            } else {
                _uiState.update { it.copy(isLoadingEpisodes = true) }
            }

            val res = apiClient.getEpisodes(state.serverUrl, userId, token, seriesId, seasonId)
            res.onSuccess { episodes ->
                cacheManager.saveMediaItems(cacheKey, episodes)
                _uiState.update { it.copy(episodes = episodes, isLoadingEpisodes = false) }
            }.onFailure { err ->
                if (cached.isNullOrEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingEpisodes = false,
                            toastMessage = "Could not load episodes: ${err.message}"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingEpisodes = false) }
                }
            }
        }
    }

    fun navigateBack() {
        _uiState.update { current ->
            when (current.currentScreen) {
                Screen.SETUP -> current
                Screen.LIBRARIES -> current
                Screen.MEDIA_LIST -> current.copy(
                    currentScreen = Screen.LIBRARIES,
                    selectedLibrary = null
                )
                Screen.SEASONS -> current.copy(
                    currentScreen = Screen.MEDIA_LIST,
                    selectedSeries = null,
                    seasons = emptyList()
                )
                Screen.EPISODES -> current.copy(
                    currentScreen = Screen.SEASONS,
                    selectedSeason = null,
                    episodes = emptyList()
                )
                Screen.MEDIA_DETAIL -> {
                    if (current.selectedSeason != null) {
                        current.copy(currentScreen = Screen.EPISODES, selectedMediaItem = null)
                    } else if (current.selectedSeries != null) {
                        current.copy(currentScreen = Screen.SEASONS, selectedMediaItem = null)
                    } else {
                        current.copy(currentScreen = Screen.MEDIA_LIST, selectedMediaItem = null)
                    }
                }
                Screen.REMOTE_CONTROL -> {
                    if (current.selectedMediaItem != null) {
                        current.copy(currentScreen = Screen.MEDIA_DETAIL)
                    } else {
                        current.copy(currentScreen = Screen.LIBRARIES)
                    }
                }
            }
        }
    }

    fun openRemote() {
        _uiState.update { it.copy(currentScreen = Screen.REMOTE_CONTROL) }
    }

    fun scanDlnaDevices() {
        val serverHost = try {
            java.net.URL(_uiState.value.serverUrl).host
        } catch (e: Exception) {
            ""
        }
        val ignored = setOfNotNull(
            "192.168.1.1",
            "192.168.0.1",
            "10.0.0.1",
            serverHost.takeIf { it.isNotBlank() }
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isScanningDevices = true) }
            // Query local network SSDP for real DLNA renderers, ignoring router and Jellyfin server
            val realDevices = dlnaController.discoverLocalDevices(timeoutMs = 3500, ignoredHosts = ignored)
            
            _uiState.update {
                it.copy(
                    isScanningDevices = false,
                    discoveredDevices = realDevices.distinctBy { d -> d.id }
                )
            }
        }
    }

    fun addManualDlnaDevice(name: String, ip: String, port: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningDevices = true) }
            val realDevice = dlnaController.probeManualDevice(name, ip, port)
            _uiState.update {
                it.copy(
                    isScanningDevices = false,
                    discoveredDevices = (listOf(realDevice) + it.discoveredDevices).distinctBy { d -> d.id }
                )
            }
            connectDlnaDevice(realDevice)
        }
    }

    fun connectDlnaDevice(device: DlnaDevice) {
        _uiState.update {
            it.copy(
                connectedDevice = device,
                showDeviceDialog = false,
                toastMessage = "Connected to ${device.friendlyName}"
            )
        }

        // Query device's current volume and mute state
        viewModelScope.launch {
            val vol = dlnaController.getVolume(device)
            val mute = dlnaController.getMute(device)
            if (vol != null || mute != null) {
                _playbackStatus.update {
                    it.copy(
                        volume = vol ?: it.volume,
                        isMuted = mute ?: it.isMuted
                    )
                }
            }
        }
    }

    fun disconnectDlnaDevice() {
        val currentDevice = _uiState.value.connectedDevice
        stopPlayback()
        _uiState.update {
            it.copy(
                connectedDevice = null,
                toastMessage = "Disconnected from ${currentDevice?.friendlyName ?: "DLNA device"}"
            )
        }
    }

    fun playMedia(item: JellyfinMediaItem) {
        val targetDevice = _uiState.value.connectedDevice
        if (targetDevice == null) {
            _uiState.update {
                it.copy(
                    showDeviceDialog = true,
                    toastMessage = "Connect a DLNA device first before playing!"
                )
            }
            scanDlnaDevices()
            return
        }

        val state = _uiState.value
        val streamUrl = item.getPlaybackStreamUrl(
            serverUrl = state.serverUrl,
            token = state.accessToken,
            audioMode = state.audioTranscodeMode,
            selectedSubtitleIndex = state.selectedSubtitleTrackIndex,
            burnSubtitles = state.burnSubtitles,
            selectedAudioIndex = state.selectedAudioTrackIndex
        )
        val initialDuration = item.durationSeconds.takeIf { it > 0 } ?: 0L

        viewModelScope.launch {
            _playbackStatus.value = DlnaPlaybackStatus(
                state = TransportState.BUFFERING,
                currentItem = item,
                positionSeconds = 0L,
                durationSeconds = initialDuration,
                volume = _playbackStatus.value.volume,
                isMuted = _playbackStatus.value.isMuted
            )
            _uiState.update {
                it.copy(
                    currentScreen = Screen.REMOTE_CONTROL,
                    isPlaybackActive = true
                )
            }

            // Real UPnP AVTransport SetAVTransportURI & Play calls
            val setUriResult = dlnaController.setAvTransportUri(targetDevice, streamUrl, item)
            if (setUriResult.isFailure) {
                _uiState.update {
                    it.copy(
                        toastMessage = "Could not start casting: ${setUriResult.exceptionOrNull()?.message}",
                        isPlaybackActive = false
                    )
                }
                _playbackStatus.update { it.copy(state = TransportState.ERROR) }
                return@launch
            }
            delay(400)
            val playResult = dlnaController.play(targetDevice)
            if (playResult.isFailure) {
                Log.w("JellyRemoteViewModel", "Play command result: ${playResult.exceptionOrNull()?.message}")
            }

            _playbackStatus.update {
                it.copy(state = TransportState.PLAYING)
            }
            startPlaybackTicker()
        }
    }

    /**
     * Re-applies stream URL with updated audio transcode or subtitle burn settings to current playback.
     */
    fun applyPlaybackSettings() {
        val currentItem = _playbackStatus.value.currentItem ?: _uiState.value.selectedMediaItem
        val targetDevice = _uiState.value.connectedDevice
        if (currentItem != null && targetDevice != null) {
            val state = _uiState.value
            val streamUrl = currentItem.getPlaybackStreamUrl(
                serverUrl = state.serverUrl,
                token = state.accessToken,
                audioMode = state.audioTranscodeMode,
                selectedSubtitleIndex = state.selectedSubtitleTrackIndex,
                burnSubtitles = state.burnSubtitles,
                selectedAudioIndex = state.selectedAudioTrackIndex
            )
            viewModelScope.launch {
                val subNotice = if (state.selectedSubtitleTrackIndex != null && state.burnSubtitles) " with burned subtitles" else ""
                _uiState.update {
                    it.copy(
                        toastMessage = "Updating stream on ${targetDevice.friendlyName} (${state.audioTranscodeMode.displayName}$subNotice)..."
                    )
                }
                dlnaController.setAvTransportUri(targetDevice, streamUrl, currentItem)
                delay(350)
                dlnaController.play(targetDevice)
            }
        }
    }


    private fun startPlaybackTicker() {
        playbackTickerJob?.cancel()
        playbackTickerJob = viewModelScope.launch {
            var counter = 0
            while (isActive) {
                delay(1000)
                counter++
                val device = _uiState.value.connectedDevice
                val current = _playbackStatus.value

                // Every 3 seconds, sync live position info from the DLNA device
                if (device != null && counter % 3 == 0) {
                    val posInfo = dlnaController.getPositionInfo(device)
                    if (posInfo != null) {
                        _playbackStatus.update {
                            it.copy(
                                positionSeconds = posInfo.relTimeSeconds,
                                durationSeconds = if (posInfo.trackDurationSeconds > 0) posInfo.trackDurationSeconds else it.durationSeconds
                            )
                        }
                        continue
                    }
                }

                // Local continuous progress ticker
                if (current.state == TransportState.PLAYING) {
                    val nextPos = current.positionSeconds + 1
                    if (current.durationSeconds > 0 && nextPos >= current.durationSeconds) {
                        _playbackStatus.update {
                            it.copy(
                                state = TransportState.STOPPED,
                                positionSeconds = current.durationSeconds
                            )
                        }
                        _uiState.update { it.copy(isPlaybackActive = false) }
                        break
                    } else {
                        _playbackStatus.update {
                            it.copy(positionSeconds = nextPos)
                        }
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        val device = _uiState.value.connectedDevice ?: return
        val currentStatus = _playbackStatus.value

        viewModelScope.launch {
            if (currentStatus.state == TransportState.PLAYING) {
                dlnaController.pause(device)
                _playbackStatus.update { it.copy(state = TransportState.PAUSED) }
            } else {
                dlnaController.play(device)
                _playbackStatus.update { it.copy(state = TransportState.PLAYING) }
                startPlaybackTicker()
            }
        }
    }

    fun skip(seconds: Long) {
        val device = _uiState.value.connectedDevice ?: return
        val currentStatus = _playbackStatus.value
        val targetPos = (currentStatus.positionSeconds + seconds).coerceIn(0L, currentStatus.durationSeconds.coerceAtLeast(1L))

        viewModelScope.launch {
            dlnaController.seek(device, targetPos)
            _playbackStatus.update { it.copy(positionSeconds = targetPos) }
        }
    }

    fun seekTo(seconds: Long) {
        val device = _uiState.value.connectedDevice ?: return
        val currentStatus = _playbackStatus.value
        val targetPos = seconds.coerceIn(0L, currentStatus.durationSeconds.coerceAtLeast(1L))

        viewModelScope.launch {
            dlnaController.seek(device, targetPos)
            _playbackStatus.update { it.copy(positionSeconds = targetPos) }
        }
    }

    fun stopPlayback() {
        val device = _uiState.value.connectedDevice
        playbackTickerJob?.cancel()
        playbackTickerJob = null

        viewModelScope.launch {
            if (device != null) {
                dlnaController.stop(device)
            }
            _playbackStatus.update {
                it.copy(
                    state = TransportState.STOPPED,
                    positionSeconds = 0L
                )
            }
            _uiState.update { it.copy(isPlaybackActive = false) }
        }
    }

    fun setVolume(volumePercent: Int) {
        val device = _uiState.value.connectedDevice
        val clamped = volumePercent.coerceIn(0, 100)

        viewModelScope.launch {
            if (device != null) {
                dlnaController.setVolume(device, clamped)
            }
            _playbackStatus.update {
                it.copy(
                    volume = clamped,
                    isMuted = clamped == 0
                )
            }
        }
    }

    fun adjustVolume(delta: Int) {
        val currentVol = _playbackStatus.value.volume
        val newVol = (currentVol + delta).coerceIn(0, 100)
        setVolume(newVol)
    }

    fun toggleMute() {
        val device = _uiState.value.connectedDevice
        val currentMuted = _playbackStatus.value.isMuted
        val newMuted = !currentMuted

        viewModelScope.launch {
            if (device != null) {
                dlnaController.setMute(device, newMuted)
            }
            _playbackStatus.update {
                it.copy(isMuted = newMuted)
            }
        }
    }

    fun sendRemoteNavigation(direction: String) {
        val deviceName = _uiState.value.connectedDevice?.friendlyName ?: "DLNA Device"
        _uiState.update {
            it.copy(toastMessage = "Remote $direction sent to $deviceName")
        }
    }
}
