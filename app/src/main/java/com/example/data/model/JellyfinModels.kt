package com.example.data.model

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
    @Json(name = "EpisodeTitle") val episodeTitle: String? = null,
    @Json(name = "PrimaryImageAspectRatio") val primaryImageAspectRatio: Float? = null
)

data class JellyfinLibrary(
    val id: String,
    val name: String,
    val collectionType: String,
    val iconName: String = "folder"
)

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
    val customPosterUrl: String? = null,
    val customBackdropUrl: String? = null,
    val customStreamUrl: String? = null
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
}
