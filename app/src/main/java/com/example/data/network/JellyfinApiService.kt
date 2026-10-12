package com.example.data.network

import com.example.data.model.JellyfinAuthRequest
import com.example.data.model.JellyfinAuthResponse
import com.example.data.model.JellyfinItemsResponse
import com.example.data.model.JellyfinViewsResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Retrofit service interface defining endpoints for the Jellyfin REST API.
 */
interface JellyfinApiService {

    /**
     * Authenticate by username and password.
     */
    @Headers(
        "X-Emby-Authorization: MediaBrowser Client=\"JellyCast\", Device=\"Android\", DeviceId=\"JellyCastAndroid-01\", Version=\"1.0.0\"",
        "Authorization: MediaBrowser Client=\"JellyCast\", Device=\"Android\", DeviceId=\"JellyCastAndroid-01\", Version=\"1.0.0\"",
        "Content-Type: application/json; charset=utf-8",
        "Accept: application/json"
    )
    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Body request: JellyfinAuthRequest
    ): Response<JellyfinAuthResponse>

    /**
     * Retrieve the user's media libraries (Views / User Folders) by user ID.
     */
    @GET("Users/{userId}/Views")
    suspend fun getUserViews(
        @Path("userId") userId: String,
        @Header("Authorization") authorization: String,
        @Header("X-Emby-Authorization") xEmbyAuthorization: String,
        @Header("X-Emby-Token") xEmbyToken: String,
        @Query("api_key") apiKey: String
    ): Response<JellyfinViewsResponse>

    /**
     * Direct endpoint to fetch user views without requiring userId in the URL path.
     */
    @GET("UserViews")
    suspend fun getUserViewsDirect(
        @Header("Authorization") authorization: String,
        @Header("X-Emby-Authorization") xEmbyAuthorization: String,
        @Header("X-Emby-Token") xEmbyToken: String,
        @Query("api_key") apiKey: String
    ): Response<JellyfinViewsResponse>

    /**
     * Fetch media items within a library using user ID.
     */
    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Query("parentId") parentId: String? = null,
        @Query("includeItemTypes") includeItemTypes: String? = null,
        @Query("fields") fields: String = DEFAULT_FIELDS,
        @Query("sortBy") sortBy: String = "SortName",
        @Query("recursive") recursive: Boolean = true,
        @Header("Authorization") authorization: String,
        @Header("X-Emby-Authorization") xEmbyAuthorization: String,
        @Header("X-Emby-Token") xEmbyToken: String,
        @Query("api_key") apiKey: String
    ): Response<JellyfinItemsResponse>

    /**
     * Direct endpoint to fetch items by parentId without userId.
     */
    @GET("Items")
    suspend fun getItemsDirect(
        @Query("parentId") parentId: String? = null,
        @Query("includeItemTypes") includeItemTypes: String? = null,
        @Query("fields") fields: String = DEFAULT_FIELDS,
        @Query("sortBy") sortBy: String = "SortName",
        @Query("recursive") recursive: Boolean = true,
        @Header("Authorization") authorization: String,
        @Header("X-Emby-Authorization") xEmbyAuthorization: String,
        @Header("X-Emby-Token") xEmbyToken: String,
        @Query("api_key") apiKey: String
    ): Response<JellyfinItemsResponse>

    /**
     * Fetch seasons for a given Series.
     */
    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasons(
        @Path("seriesId") seriesId: String,
        @Query("userId") userId: String? = null,
        @Query("fields") fields: String = DEFAULT_FIELDS,
        @Header("Authorization") authorization: String,
        @Header("X-Emby-Authorization") xEmbyAuthorization: String,
        @Header("X-Emby-Token") xEmbyToken: String,
        @Query("api_key") apiKey: String
    ): Response<JellyfinItemsResponse>

    /**
     * Fetch episodes for a given Series and Season.
     */
    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodes(
        @Path("seriesId") seriesId: String,
        @Query("seasonId") seasonId: String? = null,
        @Query("userId") userId: String? = null,
        @Query("fields") fields: String = DEFAULT_FIELDS,
        @Header("Authorization") authorization: String,
        @Header("X-Emby-Authorization") xEmbyAuthorization: String,
        @Header("X-Emby-Token") xEmbyToken: String,
        @Query("api_key") apiKey: String
    ): Response<JellyfinItemsResponse>

    companion object {
        const val CLIENT_NAME = "JellyCast"
        const val DEVICE_NAME = "Android"
        const val DEVICE_ID = "JellyCastAndroid-01"
        const val VERSION = "1.0.0"

        const val DEFAULT_AUTH_HEADER =
            "MediaBrowser Client=\"$CLIENT_NAME\", Device=\"$DEVICE_NAME\", DeviceId=\"$DEVICE_ID\", Version=\"$VERSION\""

        const val DEFAULT_FIELDS =
            "Overview,PrimaryImageAspectRatio,Genres,CommunityRating,PremiereDate,RunTimeTicks,ProductionYear,MediaSources,IndexNumber,ParentIndexNumber,SeriesName,SeriesId,SeasonId,SeasonName"

        fun buildAuthHeader(token: String? = null): String {
            return if (!token.isNullOrBlank()) {
                "MediaBrowser Client=\"$CLIENT_NAME\", Device=\"$DEVICE_NAME\", DeviceId=\"$DEVICE_ID\", Version=\"$VERSION\", Token=\"$token\""
            } else {
                DEFAULT_AUTH_HEADER
            }
        }

        /**
         * Factory function to instantiate [JellyfinApiService] for a target base URL using Retrofit.
         */
        fun create(
            baseUrl: String,
            customClient: OkHttpClient? = null
        ): JellyfinApiService {
            val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            val okHttpClient = customClient ?: OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()
                    val builder = original.newBuilder()
                        .header("Accept", "application/json")

                    // Fallback client identification headers if not already attached with a Token
                    if (original.header("X-Emby-Authorization") == null) {
                        builder.header("X-Emby-Authorization", DEFAULT_AUTH_HEADER)
                    }
                    if (original.header("Authorization") == null) {
                        builder.header("Authorization", DEFAULT_AUTH_HEADER)
                    }

                    chain.proceed(builder.build())
                }
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(JellyfinApiService::class.java)
        }
    }
}
