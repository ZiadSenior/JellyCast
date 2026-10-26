package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import com.example.data.model.AudioTranscodeMode
import com.example.data.model.DlnaDevice
import com.example.data.model.JellyfinMediaItem
import com.example.data.model.MediaTrackInfo
import com.example.ui.theme.CastCyan
import com.example.ui.theme.CastGreenActive
import com.example.ui.theme.DeepVioletBg
import com.example.ui.theme.JellyViolet
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun MediaDetailScreen(
    item: JellyfinMediaItem,
    connectedDevice: DlnaDevice?,
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
    onPlayClick: () -> Unit,
    onOpenCastScanner: () -> Unit
) {
    var showSubtitleDropdown by remember { mutableStateOf(false) }
    var showAudioDropdown by remember { mutableStateOf(false) }

    val backdropUrl = item.getBackdropUrl(serverUrl, token)
    val posterUrl = item.getPosterUrl(serverUrl, token)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeepVioletBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // Backdrop Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient scrims
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    DeepVioletBg.copy(alpha = 0.5f),
                                    DeepVioletBg
                                )
                            )
                        )
                )

                // Poster overlapping
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp)
                        .size(width = 96.dp, height = 138.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(2.dp, SurfaceElevated, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Title & Meta Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                val showSubtitle = when {
                    item.seriesName != null && item.seasonName != null && item.indexNumber != null ->
                        "${item.seriesName} • ${item.seasonName} • Episode ${item.indexNumber}"
                    item.seriesName != null && item.indexNumber != null ->
                        "${item.seriesName} • Episode ${item.indexNumber}"
                    item.seriesName != null ->
                        item.seriesName
                    item.seasonName != null ->
                        item.seasonName
                    else -> null
                }

                if (showSubtitle != null) {
                    Text(
                        text = showSubtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = JellyViolet
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata chips: Year, Rating, Duration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.productionYear != null) {
                        MetaBadge(text = "${item.productionYear}")
                    }

                    if (item.communityRating != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(WarningAmber.copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = String.format("%.1f", item.communityRating),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                            }
                        }
                    }

                    if (item.durationSeconds > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceElevated)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = item.formattedDuration,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    MetaBadge(text = item.type)
                }

                if (item.genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item.genres.take(4).forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceVariantDark)
                                    .border(1.dp, SurfaceElevated, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = genre,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CastCyan
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // DLNA Streaming & Transcode Settings Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("dlna_stream_options_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceDark
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = CastCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DLNA Stream Options",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CastCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = audioTranscodeMode.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CastCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Audio Transcode Mode Selector
                        Text(
                            text = "Audio Format",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = audioTranscodeMode.description,
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AudioTranscodeMode.values().forEach { mode ->
                                val isSelected = audioTranscodeMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onAudioModeChange(mode) },
                                    label = {
                                        Text(
                                            text = mode.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
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
                                    ),
                                    modifier = Modifier.testTag("audio_mode_chip_${mode.name.lowercase()}")
                                )
                            }
                        }

                        // Audio Track Selector if multiple audio tracks
                        if (item.audioTracks.size > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val currentAudioTrack = item.audioTracks.find { it.index == selectedAudioTrackIndex }
                                ?: item.audioTracks.firstOrNull()

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceVariantDark)
                                        .clickable { showAudioDropdown = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Audio: ${currentAudioTrack?.title ?: "Default"}",
                                            fontSize = 11.sp,
                                            color = TextPrimary
                                        )
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
                                    item.audioTracks.forEach { track ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = track.title + if (track.isDefault) " (Default)" else "",
                                                    fontSize = 12.sp
                                                )
                                            },
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

                        // Subtitle Section (Burn-in for DLNA)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = null,
                                    tint = CastCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Subtitles",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                            }
                            if (selectedSubtitleTrackIndex != null && burnSubtitles) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CastGreenActive.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Burned into Video",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CastGreenActive
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "TVs cannot parse external subtitles over DLNA. Jellyfin will burn them into the video frames.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle Track Picker
                        val currentSubTrack = item.subtitleTracks.find { it.index == selectedSubtitleTrackIndex }
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
                                    text = currentSubTrack?.title ?: if (selectedSubtitleTrackIndex == null) "Off (No Subtitles)" else "Subtitle #${selectedSubtitleTrackIndex}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedSubtitleTrackIndex != null) CastCyan else TextSecondary
                                )
                                Text(
                                    text = "Select Track",
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
                                    text = { Text("Off (No Subtitles)", fontSize = 12.sp) },
                                    onClick = {
                                        onSubtitleTrackChange(null)
                                        showSubtitleDropdown = false
                                    }
                                )
                                item.subtitleTracks.forEach { track ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = track.title + if (track.isDefault) " (Default)" else "",
                                                fontSize = 12.sp
                                            )
                                        },
                                        onClick = {
                                            onSubtitleTrackChange(track.index)
                                            showSubtitleDropdown = false
                                        }
                                    )
                                }
                                if (item.subtitleTracks.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No embedded subtitles found", fontSize = 12.sp, color = TextMuted) },
                                        onClick = { showSubtitleDropdown = false }
                                    )
                                }
                            }
                        }

                        // Burn-in Toggle Switch (shown when a subtitle is chosen)
                        if (selectedSubtitleTrackIndex != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Burn into Video (DLNA)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Hardcodes subtitle onto video stream",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
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
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Play / Cast Button (Requirement 6 & 8)
                val isDlnaConnected = connectedDevice != null
                if (isDlnaConnected) {
                    Button(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("play_on_dlna_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CastGreenActive
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = DeepVioletBg,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play on ${connectedDevice?.friendlyName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DeepVioletBg
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Connected to ${connectedDevice?.friendlyName} • ",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "Switch Device",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CastCyan,
                            modifier = Modifier.clickable { onOpenCastScanner() }
                        )
                    }
                } else {
                    // Not connected warning & CTA button (Requirement 8)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(WarningAmber.copy(alpha = 0.12f))
                            .border(1.dp, WarningAmber.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DLNA Device Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = WarningAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect a local DLNA player (smart TV or AV receiver) before starting playback.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onOpenCastScanner,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("connect_dlna_before_play_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarningAmber
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CastConnected,
                                contentDescription = null,
                                tint = DeepVioletBg,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select DLNA Target to Play",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DeepVioletBg
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Plot & Overview
                Text(
                    text = "Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.overview,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // DLNA Stream Specs Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = CastCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "UPnP / DLNA Stream Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(label = "Format", value = if (item.type == "Audio") "MP3 / AAC Audio" else "MP4 / H.264 Video")
                        DetailRow(label = "Transport Protocol", value = "UPnP AVTransport 1.0 (SOAP)")
                        DetailRow(label = "Direct Stream URL", value = item.getDirectStreamUrl(serverUrl, token))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = TextMuted)
        Text(
            text = value,
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
