package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AudioTranscodeMode
import com.example.data.model.DlnaDevice
import com.example.data.model.DlnaPlaybackStatus
import com.example.data.model.MediaTrackInfo
import com.example.data.model.TransportState
import com.example.ui.theme.CastCyan
import com.example.ui.theme.CastGreenActive
import com.example.ui.theme.DeepVioletBg
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.JellyViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun RemoteControlScreen(
    status: DlnaPlaybackStatus,
    targetDevice: DlnaDevice?,
    serverUrl: String,
    token: String?,
    audioTranscodeMode: AudioTranscodeMode = AudioTranscodeMode.AAC,
    selectedAudioTrackIndex: Int? = null,
    selectedSubtitleTrackIndex: Int? = null,
    burnSubtitles: Boolean = true,
    onAudioModeChange: (AudioTranscodeMode) -> Unit = {},
    onAudioTrackChange: (Int?) -> Unit = {},
    onSubtitleTrackChange: (Int?) -> Unit = {},
    onBurnSubtitlesChange: (Boolean) -> Unit = {},
    onApplyStreamSettings: () -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onSkip: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onStop: () -> Unit,
    onSetVolume: (Int) -> Unit,
    onAdjustVolume: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onSendRemoteKey: (String) -> Unit,
    onOpenCastScanner: () -> Unit
) {
    val item = status.currentItem
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderTempPosition by remember { mutableFloatStateOf(0f) }
    var showStreamSettingsDialog by remember { mutableStateOf(false) }
    var showSubtitleDropdown by remember { mutableStateOf(false) }
    var showAudioDropdown by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeepVioletBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Target Device Live Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceElevated, RoundedCornerShape(16.dp))
                    .clickable { onOpenCastScanner() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("remote_target_device_header")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CastGreenActive.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CastConnected,
                                contentDescription = null,
                                tint = CastGreenActive,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = targetDevice?.friendlyName ?: "DLNA Player",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Active UPnP AVTransport Stream",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Live Playback State Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (status.state) {
                                    TransportState.PLAYING -> CastGreenActive.copy(alpha = 0.2f)
                                    TransportState.PAUSED -> WarningAmber.copy(alpha = 0.2f)
                                    TransportState.BUFFERING -> CastCyan.copy(alpha = 0.2f)
                                    else -> SurfaceElevated
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = status.state.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (status.state) {
                                TransportState.PLAYING -> CastGreenActive
                                TransportState.PAUSED -> WarningAmber
                                TransportState.BUFFERING -> CastCyan
                                else -> TextMuted
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Media Banner / Poster
            if (item != null) {
                val posterUrl = item.getPosterUrl(serverUrl, token)
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 155.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceDark)
                        .border(2.dp, SurfaceElevated, RoundedCornerShape(14.dp))
                ) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = item.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.seriesName != null) {
                    Text(
                        text = item.seriesName,
                        fontSize = 12.sp,
                        color = JellyViolet,
                        textAlign = TextAlign.Center
                    )
                }

                // Live Stream Transcode & Subtitles Status Pill Row
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .border(1.dp, SurfaceElevated, RoundedCornerShape(8.dp))
                            .clickable { showStreamSettingsDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("remote_audio_format_pill")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = CastCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = audioTranscodeMode.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CastCyan
                            )
                        }
                    }

                    // Audio Track Pill if multi-track
                    val activeTrack = item?.audioTracks?.find { it.index == selectedAudioTrackIndex }
                        ?: item?.audioTracks?.firstOrNull()
                    if ((item?.audioTracks?.size ?: 0) > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDark)
                                .border(1.dp, SurfaceElevated, RoundedCornerShape(8.dp))
                                .clickable { showStreamSettingsDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("remote_audio_track_pill")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = CastCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeTrack?.language?.uppercase() ?: "Track #${activeTrack?.index ?: 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CastCyan
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .border(1.dp, SurfaceElevated, RoundedCornerShape(8.dp))
                            .clickable { showStreamSettingsDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("remote_subtitle_pill")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = null,
                                tint = if (selectedSubtitleTrackIndex != null) CastGreenActive else TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedSubtitleTrackIndex != null) "Subs: Burned" else "Subs: Off",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedSubtitleTrackIndex != null) CastGreenActive else TextMuted
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated)
                            .clickable { showStreamSettingsDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("remote_open_stream_settings_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Options",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Stream Settings Modal Dialog
            if (showStreamSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showStreamSettingsDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = CastCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DLNA Stream Settings",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Audio Transcode Format",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Text(
                                text = audioTranscodeMode.description,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AudioTranscodeMode.values().forEach { mode ->
                                    val isSelected = audioTranscodeMode == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onAudioModeChange(mode) },
                                        label = {
                                            Text(
                                                text = mode.displayName,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = SurfaceVariantDark,
                                            labelColor = TextSecondary,
                                            selectedContainerColor = CastCyan.copy(alpha = 0.2f),
                                            selectedLabelColor = CastCyan,
                                            selectedLeadingIconColor = CastCyan
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = SurfaceElevated,
                                            selectedBorderColor = CastCyan
                                        )
                                    )
                                }
                            }

                            // Audio Track Selector
                            val audioTracks = item?.audioTracks ?: emptyList()
                            if (audioTracks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SurfaceElevated))
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Audio Track (${audioTracks.size} available)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Jellyfin transcodes this track to ${audioTranscodeMode.displayName} so your receiver plays it with sound.",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val currentAudio = audioTracks.find { it.index == selectedAudioTrackIndex }
                                    ?: audioTracks.firstOrNull()

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceVariantDark)
                                            .clickable { showAudioDropdown = true }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Audiotrack,
                                                contentDescription = null,
                                                tint = CastCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = currentAudio?.title ?: "Default Track",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${currentAudio?.codec?.uppercase() ?: "AUDIO"}${if (currentAudio?.language != null) " • ${currentAudio.language.uppercase()}" else ""}${if (currentAudio?.channels != null) " • ${if (currentAudio.channels == 6) "5.1" else if (currentAudio.channels == 2) "Stereo" else "${currentAudio.channels}ch"}" else ""}",
                                                    fontSize = 10.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Change",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CastCyan
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showAudioDropdown,
                                        onDismissRequest = { showAudioDropdown = false }
                                    ) {
                                        audioTracks.forEach { track ->
                                            val isSelected = (selectedAudioTrackIndex == track.index) ||
                                                (selectedAudioTrackIndex == null && track.isDefault)
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = track.title + if (track.isDefault) " (Default)" else "",
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) CastCyan else TextPrimary
                                                        )
                                                        Text(
                                                            text = "Track #${track.index} • ${track.codec?.uppercase() ?: "AUDIO"}${if (track.language != null) " • ${track.language.uppercase()}" else ""}",
                                                            fontSize = 10.sp,
                                                            color = TextMuted
                                                        )
                                                    }
                                                },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = CastCyan,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else null,
                                                onClick = {
                                                    onAudioTrackChange(track.index)
                                                    showAudioDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SurfaceElevated))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Subtitles (Burned into Video)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Text(
                                text = "Renders text directly into video stream for DLNA.",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val currentSub = item?.subtitleTracks?.find { it.index == selectedSubtitleTrackIndex }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceVariantDark)
                                        .clickable { showSubtitleDropdown = true }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = currentSub?.title ?: if (selectedSubtitleTrackIndex == null) "Off (No Subtitles)" else "Subtitle #${selectedSubtitleTrackIndex}",
                                        fontSize = 11.sp,
                                        color = if (selectedSubtitleTrackIndex != null) CastCyan else TextSecondary
                                    )
                                    Text(
                                        text = "Change",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CastCyan
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSubtitleDropdown,
                                    onDismissRequest = { showSubtitleDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Off (No Subtitles)", fontSize = 11.sp) },
                                        onClick = {
                                            onSubtitleTrackChange(null)
                                            showSubtitleDropdown = false
                                        }
                                    )
                                    item?.subtitleTracks?.forEach { track ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = track.title + if (track.isDefault) " (Default)" else "",
                                                    fontSize = 11.sp
                                                )
                                            },
                                            onClick = {
                                                onSubtitleTrackChange(track.index)
                                                showSubtitleDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (selectedSubtitleTrackIndex != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Burn subtitles into video",
                                        fontSize = 11.sp,
                                        color = TextPrimary
                                    )
                                    Switch(
                                        checked = burnSubtitles,
                                        onCheckedChange = { onBurnSubtitlesChange(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DeepVioletBg,
                                            checkedTrackColor = CastGreenActive,
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = SurfaceElevated
                                        )
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showStreamSettingsDialog = false
                                onApplyStreamSettings()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CastCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Apply to TV", color = DeepVioletBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showStreamSettingsDialog = false },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                        }
                    },
                    containerColor = SurfaceDark
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Timeline & Scrubber
            val durationSec = status.durationSeconds.coerceAtLeast(1L)
            val currentPos = if (isDraggingSlider) {
                (sliderTempPosition * durationSec).toLong()
            } else {
                status.positionSeconds
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (isDraggingSlider) sliderTempPosition else status.progress,
                    onValueChange = {
                        isDraggingSlider = true
                        sliderTempPosition = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        val targetSeconds = (sliderTempPosition * durationSec).toLong()
                        onSeekTo(targetSeconds)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("remote_seek_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = CastCyan,
                        activeTrackColor = CastCyan,
                        inactiveTrackColor = SurfaceVariantDark
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = DlnaPlaybackStatus.formatTime(currentPos),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = status.formattedDuration,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transport Control Cluster: Skip Back 10, Play/Pause, Skip Fwd 30, Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Replay 10s
                IconButton(
                    onClick = { onSkip(-10L) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceElevated, CircleShape)
                        .testTag("remote_skip_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Main Play / Pause Button
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(JellyViolet, CastCyan)
                            )
                        )
                        .testTag("remote_play_pause_button")
                ) {
                    if (status.state == TransportState.BUFFERING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = DeepVioletBg,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (status.state == TransportState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (status.state == TransportState.PLAYING) "Pause" else "Play",
                            tint = DeepVioletBg,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Forward 30s
                IconButton(
                    onClick = { onSkip(30L) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceElevated, CircleShape)
                        .testTag("remote_skip_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward30,
                        contentDescription = "Forward 30 seconds",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Stop Button
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceElevated, CircleShape)
                        .testTag("remote_stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = ErrorRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Dedicated, Prominent Volume Section in Playing Page
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceElevated, RoundedCornerShape(20.dp))
                    .padding(16.dp)
                    .testTag("remote_volume_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header row: Speaker icon, label, and volume % badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val speakerIcon = when {
                                status.isMuted -> Icons.Default.VolumeOff
                                status.volume == 0 -> Icons.Default.VolumeMute
                                status.volume <= 50 -> Icons.Default.VolumeDown
                                else -> Icons.Default.VolumeUp
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (status.isMuted) ErrorRed.copy(alpha = 0.2f) else CastCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = speakerIcon,
                                    contentDescription = "Volume",
                                    tint = if (status.isMuted) ErrorRed else CastCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "DLNA Volume",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = targetDevice?.friendlyName ?: "Renderer Control",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Volume Percentage Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (status.isMuted) ErrorRed.copy(alpha = 0.2f) else CastCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (status.isMuted) "MUTED" else "${status.volume}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (status.isMuted) ErrorRed else CastCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stepper row with [-], Slider, and [+] buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volume Down Button (-5%)
                        IconButton(
                            onClick = { onAdjustVolume(-5) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceElevated)
                                .testTag("remote_volume_down_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Volume",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // High precision volume slider
                        Slider(
                            value = if (status.isMuted) 0f else status.volume.toFloat(),
                            onValueChange = { onSetVolume(it.toInt()) },
                            valueRange = 0f..100f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("remote_volume_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = CastCyan,
                                activeTrackColor = CastCyan,
                                inactiveTrackColor = SurfaceVariantDark
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Volume Up Button (+5%)
                        IconButton(
                            onClick = { onAdjustVolume(5) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceElevated)
                                .testTag("remote_volume_up_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Volume",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Volume quick preset chips and Mute toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (status.isMuted) ErrorRed else SurfaceElevated)
                                .clickable { onToggleMute() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("remote_mute_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (status.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeMute,
                                    contentDescription = null,
                                    tint = if (status.isMuted) Color.White else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (status.isMuted) "Unmute" else "Mute",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (status.isMuted) Color.White else TextSecondary
                                )
                            }
                        }

                        // Presets: 25%, 50%, 75%, 100%
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(25, 50, 75, 100).forEach { preset ->
                                val isSelected = !status.isMuted && status.volume == preset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CastCyan else SurfaceVariantDark)
                                        .clickable { onSetVolume(preset) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("volume_preset_$preset")
                                ) {
                                    Text(
                                        text = "$preset%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) DeepVioletBg else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tactile D-Pad Navigation Cluster (TV Remote Controller)
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(2.dp, SurfaceElevated, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Up
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onSendRemoteKey("UP") }
                        .testTag("dpad_up"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = TextPrimary)
                }

                // Down
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onSendRemoteKey("DOWN") }
                        .testTag("dpad_down"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = TextPrimary)
                }

                // Left
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onSendRemoteKey("LEFT") }
                        .testTag("dpad_left"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = TextPrimary)
                }

                // Right
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onSendRemoteKey("RIGHT") }
                        .testTag("dpad_right"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = TextPrimary)
                }

                // Center OK / Enter
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(JellyViolet)
                        .clickable { onSendRemoteKey("OK") }
                        .testTag("dpad_ok"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OK",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = DeepVioletBg
                    )
                }
            }
        }
    }
}
