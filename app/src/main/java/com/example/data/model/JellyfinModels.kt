package com.example.data.model

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class JellyfinAuthRequest(
    @Json(name = "Username") val username: String,
    @Json(name = "Pw") val pw: String,
    @Json(name = "Password") val password: String = pw
)

@JsonClass(generateAdapter = false)
data class JellyfinUserDto(
    @Json(name = "Id") val id: String? = null,
    @Json(name = "Name") val name: String? = null,
    @Json(name = "ServerId") val serverId: String? = null
)

@JsonClass(generateAdapter = false)
data class JellyfinSessionInfoDto(
    @Json(name = "UserId") val userId: String? = null,
    @Json(name = "UserName") val userName: String? = null
)

@JsonClass(generateAdapter = false)
data class JellyfinAuthResponse(
    @Json(name = "AccessToken") val accessToken: String?,
    @Json(name = "ServerId") val serverId: String?,
    @Json(name = "User") val user: JellyfinUserDto?,
    @Json(name = "SessionInfo") val sessionInfo: JellyfinSessionInfoDto? = null
)

@JsonClass(generateAdapter = false)
data class JellyfinViewsResponse(
    @Json(name = "Items") val items: List<JellyfinLibraryItemDto>?,
    @Json(name = "TotalRecordCount") val totalRecordCount: Int? = null
)

@JsonClass(generateAdapter = false)
data class JellyfinLibraryItemDto(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String,
    @Json(name = "CollectionType") val collectionType: String? = null,
    @Json(name = "Type") val type: String? = null
)

@JsonClass(generateAdapter = false)
data class JellyfinItemsResponse(
    @Json(name = "Items") val items: List<JellyfinMediaItemDto>?,
    @Json(name = "TotalRecordCount") val totalRecordCount: Int? = null
)

@JsonClass(generateAdapter = false)
data class JellyfinMediaStreamDto(
    @Json(name = "Index") val index: Int,
    @Json(name = "Type") val type: String, // "Audio", "Subtitle", "Video"
    @Json(name = "Codec") val codec: String? = null,
    @Json(name = "Language") val language: String? = null,
    @Json(name = "DisplayTitle") val displayTitle: String? = null,
    @Json(name = "Title") val title: String? = null,
    @Json(name = "IsDefault") val isDefault: Boolean? = false,
    @Json(name = "IsForced") val isForced: Boolean? = false,
    @Json(name = "Channels") val channels: Int? = null
)

@JsonClass(generateAdapter = false)
data class JellyfinMediaSourceDto(
    @Json(name = "Id") val id: String? = null,
    @Json(name = "MediaStreams") val mediaStreams: List<JellyfinMediaStreamDto>? = null
)

@JsonClass(generateAdapter = false)
data class JellyfinMediaItemDto(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String,
    @Json(name = "Overview") val overview: String? = null,
    @Json(name = "Type") val type: String? = "Movie",
    @Json(name = "ProductionYear") val productionYear: Int? = null,
    @Json(name = "PremiereDate") val premiereDate: String? = null,
    @Json(name = "CommunityRating") val communityRating: Float? = null,
    @Json(name = "RunTimeTicks") val runTimeTicks: Long? = null,
    @Json(name = "Genres") val genres: List<String>? = null,
    @Json(name = "SeriesName") val seriesName: String? = null,
    @Json(name = "SeriesId") val seriesId: String? = null,
    @Json(name = "SeasonId") val seasonId: String? = null,
    @Json(name = "SeasonName") val seasonName: String? = null,
    @Json(name = "EpisodeTitle") val episodeTitle: String? = null,
    @Json(name = "IndexNumber") val indexNumber: Int? = null,
    @Json(name = "ParentIndexNumber") val parentIndexNumber: Int? = null,
    @Json(name = "PrimaryImageAspectRatio") val primaryImageAspectRatio: Float? = null,
    @Json(name = "MediaStreams") val mediaStreams: List<JellyfinMediaStreamDto>? = null,
    @Json(name = "MediaSources") val mediaSources: List<JellyfinMediaSourceDto>? = null
)

enum class AudioTranscodeMode(
    val displayName: String,
    val codecParam: String,
    val container: String,
    val description: String
) {
    AAC("AAC (MP4)", "aac", "mp4", "Transcodes audio to AAC 2.0 inside MP4. Universal for smart TVs."),
    AAC_TS("AAC (MPEG-TS)", "aac", "ts", "Transcodes to AAC 2.0 inside MPEG-TS. Fixes 'Unsupported' on AV receivers."),
    AC3("AC3 5.1", "ac3", "mp4", "Transcodes to Dolby Digital 5.1 (Best for surround receivers)."),
    DIRECT("Original (Direct)", "copy", "mp4", "Streams original audio & video untouched (Zero CPU usage)."),
    MP3("MP3 Stereo", "mp3", "mp4", "Transcodes to MP3 2.0 (For legacy UPnP players).")
}

@Immutable
data class MediaTrackInfo(
    val index: Int,
    val title: String,
    val codec: String?,
    val language: String?,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
    val channels: Int? = null
)

@Immutable
data class DiscoveredJellyfinServer(
    val name: String,
    val address: String, // e.g. "http://192.168.1.100:8096"
    val id: String? = null
)

@Immutable
data class JellyfinLibrary(
    val id: String,
    val name: String,
    val collectionType: String,
    val iconName: String = "folder"
)

@Immutable
data class JellyfinMediaItem(
    val id: String,
    val name: String,
    val overview: String,
    val type: String,
    val productionYear: Int?,
    val communityRating: Float?,
    val runTimeTicks: Long?,
    val genres: List<String>,
    val seriesName: String? = null,
    val seriesId: String? = null,
    val seasonId: String? = null,
    val seasonName: String? = null,
    val indexNumber: Int? = null,
    val parentIndexNumber: Int? = null,
    val customPosterUrl: String? = null,
    val customBackdropUrl: String? = null,
    val customStreamUrl: String? = null,
    val audioTracks: List<MediaTrackInfo> = emptyList(),
    val subtitleTracks: List<MediaTrackInfo> = emptyList()
) {
    val durationSeconds: Long
        get() = (runTimeTicks ?: 0L) / 10_000_000L

    val formattedDuration: String
        get() {
            val totalSec = durationSeconds
            if (totalSec <= 0) return "Unknown"
            val hours = totalSec / 3600
            val minutes = (totalSec % 3600) / 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }

    /**
     * Fast, downscaled thumbnail for high-FPS grid scrolling.
     */
    fun getThumbnailPosterUrl(serverUrl: String, token: String? = null): String {
        if (!customPosterUrl.isNullOrEmpty()) return customPosterUrl
        val cleanServer = serverUrl.trimEnd('/')
        return if (token != null) {
            "$cleanServer/Items/$id/Images/Primary?quality=75&maxWidth=260&api_key=$token"
        } else {
            "$cleanServer/Items/$id/Images/Primary?quality=75&maxWidth=260"
        }
    }

    fun getPosterUrl(serverUrl: String, token: String? = null): String {
        if (!customPosterUrl.isNullOrEmpty()) return customPosterUrl
        val cleanServer = serverUrl.trimEnd('/')
        return if (token != null) {
            "$cleanServer/Items/$id/Images/Primary?quality=90&maxWidth=400&api_key=$token"
        } else {
            "$cleanServer/Items/$id/Images/Primary?quality=90&maxWidth=400"
        }
    }

    fun getBackdropUrl(serverUrl: String, token: String? = null): String {
        if (!customBackdropUrl.isNullOrEmpty()) return customBackdropUrl
        val cleanServer = serverUrl.trimEnd('/')
        return if (token != null) {
            "$cleanServer/Items/$id/Images/Backdrop?quality=85&maxWidth=1000&api_key=$token"
        } else {
            "$cleanServer/Items/$id/Images/Backdrop?quality=85&maxWidth=1000"
        }
    }

    fun getDirectStreamUrl(serverUrl: String, token: String? = null): String {
        if (!customStreamUrl.isNullOrEmpty()) return customStreamUrl
        val cleanServer = serverUrl.trimEnd('/')
        return if (type.equals("Audio", ignoreCase = true)) {
            "$cleanServer/Audio/$id/stream.mp3?static=true&api_key=${token ?: ""}"
        } else {
            "$cleanServer/Videos/$id/stream.mp4?static=true&api_key=${token ?: ""}"
        }
    }

    /**
     * Builds playback stream URL with audio transcoding (e.g. AC3 -> AAC) and subtitle burn-in.
     */
    fun getPlaybackStreamUrl(
        serverUrl: String,
        token: String? = null,
        audioMode: AudioTranscodeMode = AudioTranscodeMode.AAC,
        selectedSubtitleIndex: Int? = null,
        burnSubtitles: Boolean = true,
        selectedAudioIndex: Int? = null
    ): String {
        if (!customStreamUrl.isNullOrEmpty()) return customStreamUrl
        val cleanServer = serverUrl.trimEnd('/')
        val apiKeyParam = if (!token.isNullOrBlank()) "&api_key=$token" else ""

        if (type.equals("Audio", ignoreCase = true)) {
            return "$cleanServer/Audio/$id/stream.mp3?static=true$apiKeyParam"
        }

        val hasSubtitleBurn = selectedSubtitleIndex != null && burnSubtitles
        val isDirectPlay = audioMode == AudioTranscodeMode.DIRECT && !hasSubtitleBurn && selectedAudioIndex == null

        if (isDirectPlay) {
            return "$cleanServer/Videos/$id/stream.mp4?static=true&MediaSourceId=$id$apiKeyParam"
        }

        // Determine streaming container: "ts" for MPEG-TS mode, "mp4" for MP4 modes
        val containerExt = if (audioMode == AudioTranscodeMode.AAC_TS) "ts" else "mp4"
        val urlBuilder = java.lang.StringBuilder("$cleanServer/Videos/$id/stream.$containerExt?static=false")

        // Mandatory parameters for Jellyfin track selection & session management
        urlBuilder.append("&MediaSourceId=$id")
        urlBuilder.append("&DeviceId=JellyCastAndroid")
        urlBuilder.append("&PlaySessionId=${System.currentTimeMillis()}")

        // Audio configuration
        when (audioMode) {
            AudioTranscodeMode.AAC, AudioTranscodeMode.AAC_TS -> {
                urlBuilder.append("&AudioCodec=aac&TranscodingMaxAudioChannels=2&AudioBitRate=256000&allowAudioStreamCopy=false")
            }
            AudioTranscodeMode.AC3 -> {
                urlBuilder.append("&AudioCodec=ac3&TranscodingMaxAudioChannels=6&AudioBitRate=384000&allowAudioStreamCopy=false")
            }
            AudioTranscodeMode.MP3 -> {
                urlBuilder.append("&AudioCodec=mp3&TranscodingMaxAudioChannels=2&AudioBitRate=256000&allowAudioStreamCopy=false")
            }
            AudioTranscodeMode.DIRECT -> {
                urlBuilder.append("&AudioCodec=copy&allowAudioStreamCopy=true")
            }
        }

        // Target audio stream index (requires MediaSourceId to take effect in Jellyfin)
        if (selectedAudioIndex != null) {
            urlBuilder.append("&AudioStreamIndex=$selectedAudioIndex")
        }

        // Video & Subtitle configuration
        if (hasSubtitleBurn) {
            // Re-encode video frames to burn subtitles
            urlBuilder.append("&SubtitleMethod=Encode&SubtitleStreamIndex=$selectedSubtitleIndex&VideoCodec=h264&allowVideoStreamCopy=false")
        } else {
            // Stream-copy original video frames without re-encoding (zero CPU overhead)
            urlBuilder.append("&VideoCodec=copy&allowVideoStreamCopy=true&enableAutoStreamCopy=true")
        }

        urlBuilder.append(apiKeyParam)
        return urlBuilder.toString()
    }
}

