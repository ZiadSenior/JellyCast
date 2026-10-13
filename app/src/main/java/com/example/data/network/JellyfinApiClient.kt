package com.example.data.network

import com.example.data.model.JellyfinAuthRequest
import com.example.data.model.JellyfinAuthResponse
import com.example.data.model.JellyfinLibrary
import com.example.data.model.JellyfinMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-level Jellyfin client wrapping [JellyfinApiService] calls into safe Kotlin [Result] types.
 */
class JellyfinApiClient {

    private fun getService(serverUrl: String): JellyfinApiService {
        val cleanUrl = serverUrl.trim().trimEnd('/')
        val normalizedUrl = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            "http://$cleanUrl"
        } else {
            cleanUrl
        }
        return JellyfinApiService.create(normalizedUrl)
    }

    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String
    ): Result<JellyfinAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val service = getService(serverUrl)
            val request = JellyfinAuthRequest(
                username = username.trim(),
                pw = password,
                password = password
            )
            val response = service.authenticateByName(request = request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Server returned empty auth body"))
                }
            } else {
                val errorBody = try {
                    response.errorBody()?.string()?.trim() ?: ""
                } catch (ignored: Exception) {
                    ""
                }

                val failureMessage = when {
                    response.code() == 401 || errorBody.contains("Invalid username or password", ignoreCase = true) ->
                        "Invalid username or password. Please verify your credentials."
                    response.code() == 400 && errorBody.isNotBlank() ->
                        "Server error (HTTP 400): $errorBody"
                    response.code() == 400 ->
                        "Bad Request (HTTP 400). Please check your server URL and credentials."
                    errorBody.isNotBlank() && errorBody.length < 250 ->
                        "Authentication error (${response.code()}): $errorBody"
                    else ->
                        "Authentication error (HTTP ${response.code()}): ${response.message()}"
                }
                Result.failure(Exception(failureMessage))
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Unable to connect to Jellyfin server"
            Result.failure(Exception(msg))
        }
    }

    suspend fun getLibraries(
        serverUrl: String,
        userId: String,
        token: String
    ): Result<List<JellyfinLibrary>> = withContext(Dispatchers.IO) {
        try {
            val service = getService(serverUrl)
            val authHeader = JellyfinApiService.buildAuthHeader(token)

            // Try standard Users/{userId}/Views if userId is valid, else fallback to direct UserViews
            val response = if (userId.isNotBlank()) {
                val primaryResponse = service.getUserViews(
                    userId = userId,
                    authorization = authHeader,
                    xEmbyAuthorization = authHeader,
                    xEmbyToken = token,
                    apiKey = token
                )
                if (primaryResponse.isSuccessful) {
                    primaryResponse
                } else {
                    // Fallback to direct UserViews if path-based call failed
                    service.getUserViewsDirect(
                        authorization = authHeader,
                        xEmbyAuthorization = authHeader,
                        xEmbyToken = token,
                        apiKey = token
                    )
                }
            } else {
                service.getUserViewsDirect(
                    authorization = authHeader,
                    xEmbyAuthorization = authHeader,
                    xEmbyToken = token,
                    apiKey = token
                )
            }

            if (response.isSuccessful) {
                val items = response.body()?.items?.map { dto ->
                    JellyfinLibrary(
                        id = dto.id,
                        name = dto.name,
                        collectionType = dto.collectionType ?: "folder",
                        iconName = when (dto.collectionType?.lowercase()) {
                            "movies" -> "movie"
                            "tvshows" -> "tv"
                            "music" -> "music"
                            "homevideos" -> "videocam"
                            else -> "folder"
                        }
                    )
                } ?: emptyList()
                Result.success(items)
            } else {
                val errBody = try { response.errorBody()?.string() ?: "" } catch (e: Exception) { "" }
                val msg = if (errBody.isNotBlank() && errBody.length < 200) errBody else "HTTP ${response.code()}: ${response.message()}"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItemsInLibrary(
        serverUrl: String,
        userId: String,
        token: String,
        parentId: String,
        collectionType: String? = null
    ): Result<List<JellyfinMediaItem>> = withContext(Dispatchers.IO) {
        try {
            val service = getService(serverUrl)
            val authHeader = JellyfinApiService.buildAuthHeader(token)

            // For tvshows libraries, we fetch Series and Movies so shows appear at the top level
            // rather than raw mixed episodes
            val itemTypes = when (collectionType?.lowercase()) {
                "tvshows" -> "Series"
                "movies" -> "Movie"
                else -> null
            }

            val response = if (userId.isNotBlank()) {
                val primary = service.getItems(
                    userId = userId,
                    parentId = parentId,
                    includeItemTypes = itemTypes,
                    recursive = true,
                    authorization = authHeader,
                    xEmbyAuthorization = authHeader,
                    xEmbyToken = token,
                    apiKey = token
                )
                if (primary.isSuccessful) primary else {
                    service.getItemsDirect(
                        parentId = parentId,
                        includeItemTypes = itemTypes,
                        recursive = true,
                        authorization = authHeader,
                        xEmbyAuthorization = authHeader,
                        xEmbyToken = token,
                        apiKey = token
                    )
                }
            } else {
                service.getItemsDirect(
                    parentId = parentId,
                    includeItemTypes = itemTypes,
                    recursive = true,
                    authorization = authHeader,
                    xEmbyAuthorization = authHeader,
                    xEmbyToken = token,
                    apiKey = token
                )
            }

            if (response.isSuccessful) {
                val items = response.body()?.items?.map { dto ->
                    mapDtoToMediaItem(dto)
                } ?: emptyList()
                Result.success(items)
            } else {
                val errBody = try { response.errorBody()?.string() ?: "" } catch (e: Exception) { "" }
                val msg = if (errBody.isNotBlank() && errBody.length < 200) errBody else "HTTP ${response.code()}: ${response.message()}"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeasons(
        serverUrl: String,
        userId: String,
        token: String,
        seriesId: String
    ): Result<List<JellyfinMediaItem>> = withContext(Dispatchers.IO) {
        try {
            val service = getService(serverUrl)
            val authHeader = JellyfinApiService.buildAuthHeader(token)

            val response = service.getSeasons(
                seriesId = seriesId,
                userId = userId.takeIf { it.isNotBlank() },
                authorization = authHeader,
                xEmbyAuthorization = authHeader,
                xEmbyToken = token,
                apiKey = token
            )

            if (response.isSuccessful) {
                val items = response.body()?.items?.map { dto ->
                    mapDtoToMediaItem(
                        dto = dto,
                        defaultType = "Season",
                        overrideSeriesId = seriesId,
                        overrideSeasonId = dto.id,
                        overrideSeasonName = dto.name
                    )
                } ?: emptyList()
                Result.success(items)
            } else {
                val errBody = try { response.errorBody()?.string() ?: "" } catch (e: Exception) { "" }
                Result.failure(Exception(if (errBody.isNotBlank()) errBody else "HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEpisodes(
        serverUrl: String,
        userId: String,
        token: String,
        seriesId: String,
        seasonId: String? = null
    ): Result<List<JellyfinMediaItem>> = withContext(Dispatchers.IO) {
        try {
            val service = getService(serverUrl)
            val authHeader = JellyfinApiService.buildAuthHeader(token)

            val response = service.getEpisodes(
                seriesId = seriesId,
                seasonId = seasonId,
                userId = userId.takeIf { it.isNotBlank() },
                authorization = authHeader,
                xEmbyAuthorization = authHeader,
                xEmbyToken = token,
                apiKey = token
            )

            if (response.isSuccessful) {
                val items = response.body()?.items?.map { dto ->
                    mapDtoToMediaItem(
                        dto = dto,
                        defaultType = "Episode",
                        overrideSeriesId = seriesId,
                        overrideSeasonId = seasonId ?: dto.seasonId
                    )
                } ?: emptyList()
                Result.success(items)
            } else {
                val errBody = try { response.errorBody()?.string() ?: "" } catch (e: Exception) { "" }
                Result.failure(Exception(if (errBody.isNotBlank()) errBody else "HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItemDetails(
        serverUrl: String,
        userId: String,
        token: String,
        itemId: String
    ): Result<JellyfinMediaItem> = withContext(Dispatchers.IO) {
        try {
            val service = getService(serverUrl)
            val authHeader = JellyfinApiService.buildAuthHeader(token)

            val response = if (userId.isNotBlank()) {
                service.getItem(
                    userId = userId,
                    itemId = itemId,
                    authorization = authHeader,
                    xEmbyAuthorization = authHeader,
                    xEmbyToken = token,
                    apiKey = token
                )
            } else {
                service.getItemDirect(
                    itemId = itemId,
                    authorization = authHeader,
                    xEmbyAuthorization = authHeader,
                    xEmbyToken = token,
                    apiKey = token
                )
            }

            if (response.isSuccessful && response.body() != null) {
                Result.success(mapDtoToMediaItem(response.body()!!))
            } else {
                val errBody = try { response.errorBody()?.string() ?: "" } catch (e: Exception) { "" }
                Result.failure(Exception(if (errBody.isNotBlank()) errBody else "HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapDtoToMediaItem(
        dto: com.example.data.model.JellyfinMediaItemDto,
        defaultType: String? = null,
        overrideSeriesId: String? = null,
        overrideSeasonId: String? = null,
        overrideSeasonName: String? = null
    ): JellyfinMediaItem {
        val (audioTracks, subtitleTracks) = extractMediaTracks(dto)

        return JellyfinMediaItem(
            id = dto.id,
            name = dto.name,
            overview = dto.overview ?: if (defaultType == "Season") "" else "No overview available.",
            type = defaultType ?: dto.type ?: "Movie",
            productionYear = dto.productionYear,
            communityRating = dto.communityRating,
            runTimeTicks = dto.runTimeTicks,
            genres = dto.genres ?: emptyList(),
            seriesName = dto.seriesName,
            seriesId = overrideSeriesId ?: dto.seriesId,
            seasonId = overrideSeasonId ?: dto.seasonId,
            seasonName = overrideSeasonName ?: dto.seasonName,
            indexNumber = dto.indexNumber,
            parentIndexNumber = dto.parentIndexNumber,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks
        )
    }

    private fun extractMediaTracks(
        dto: com.example.data.model.JellyfinMediaItemDto
    ): Pair<List<com.example.data.model.MediaTrackInfo>, List<com.example.data.model.MediaTrackInfo>> {
        val streams = dto.mediaStreams
            ?: dto.mediaSources?.firstOrNull()?.mediaStreams
            ?: emptyList()

        val audioTracks = mutableListOf<com.example.data.model.MediaTrackInfo>()
        val subtitleTracks = mutableListOf<com.example.data.model.MediaTrackInfo>()

        for (s in streams) {
            val lang = s.language?.lowercase()
            val codec = s.codec?.lowercase()
            val channels = s.channels
            val channelStr = when (channels) {
                1 -> "Mono"
                2 -> "Stereo"
                6 -> "5.1"
                8 -> "7.1"
                else -> ""
            }

            if (s.type.equals("Audio", ignoreCase = true)) {
                val title = s.displayTitle ?: s.title ?: run {
                    val parts = listOfNotNull(
                        lang?.uppercase(),
                        codec?.uppercase(),
                        channelStr.takeIf { it.isNotBlank() }
                    )
                    if (parts.isNotEmpty()) parts.joinToString(" • ") else "Audio Track #${s.index}"
                }
                audioTracks.add(
                    com.example.data.model.MediaTrackInfo(
                        index = s.index,
                        title = title,
                        codec = codec,
                        language = lang,
                        isDefault = s.isDefault == true,
                        isForced = s.isForced == true,
                        channels = channels
                    )
                )
            } else if (s.type.equals("Subtitle", ignoreCase = true)) {
                val title = s.displayTitle ?: s.title ?: run {
                    val parts = listOfNotNull(
                        lang?.uppercase(),
                        codec?.uppercase(),
                        if (s.isForced == true) "Forced" else null
                    )
                    if (parts.isNotEmpty()) parts.joinToString(" • ") else "Subtitle #${s.index}"
                }
                subtitleTracks.add(
                    com.example.data.model.MediaTrackInfo(
                        index = s.index,
                        title = title,
                        codec = codec,
                        language = lang,
                        isDefault = s.isDefault == true,
                        isForced = s.isForced == true,
                        channels = null
                    )
                )
            }
        }

        return Pair(audioTracks, subtitleTracks)
    }
}

