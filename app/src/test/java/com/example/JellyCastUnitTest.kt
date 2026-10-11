package com.example

import com.example.data.model.DlnaDevice
import com.example.data.model.DlnaPlaybackStatus
import com.example.data.model.JellyfinMediaItem
import com.example.data.model.TransportState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JellyCastUnitTest {

    @Test
    fun testMediaItemDurationFormatting() {
        val item = JellyfinMediaItem(
            id = "test-1",
            name = "Test Movie",
            overview = "An overview",
            type = "Movie",
            productionYear = 2023,
            communityRating = 8.5f,
            runTimeTicks = 7260000000L, // 726 seconds -> 12 min 6 sec
            genres = listOf("Action")
        )

        assertEquals(726L, item.durationSeconds)
        assertEquals("12m", item.formattedDuration)

        val longItem = JellyfinMediaItem(
            id = "test-2",
            name = "Long Epic",
            overview = "Long movie",
            type = "Movie",
            productionYear = 2024,
            communityRating = 9.0f,
            runTimeTicks = 75000000000L, // 7500 seconds -> 2h 5m
            genres = listOf("Drama")
        )
        assertEquals("2h 5m", longItem.formattedDuration)
    }

    @Test
    fun testPlaybackStatusCalculations() {
        val status = DlnaPlaybackStatus(
            state = TransportState.PLAYING,
            positionSeconds = 125L,
            durationSeconds = 500L,
            volume = 65
        )

        assertEquals(0.25f, status.progress, 0.001f)
        assertEquals("02:05", status.formattedPosition)
        assertEquals("08:20", status.formattedDuration)
    }

    @Test
    fun testDlnaVolumeAdjustment() {
        val status = DlnaPlaybackStatus(
            state = TransportState.PLAYING,
            volume = 40
        )
        val steppedUp = (status.volume + 5).coerceIn(0, 100)
        assertEquals(45, steppedUp)

        val steppedDown = (status.volume - 5).coerceIn(0, 100)
        assertEquals(35, steppedDown)

        val clampedHigh = (98 + 5).coerceIn(0, 100)
        assertEquals(100, clampedHigh)

        val clampedLow = (2 - 5).coerceIn(0, 100)
        assertEquals(0, clampedLow)
    }

    @Test
    fun testDlnaDeviceProperties() {
        val device = DlnaDevice(
            id = "uuid:test-tv",
            friendlyName = "Living Room TV",
            modelName = "OLED55",
            manufacturer = "LG",
            location = "http://192.168.1.50:8008/desc.xml",
            host = "192.168.1.50",
            avTransportControlUrl = "http://192.168.1.50:8008/upnp/control/AVTransport1"
        )

        assertEquals("OLED55 (192.168.1.50)", device.displaySubtitle)
    }

    @Test
    fun testJellyfinApiServiceCreation() {
        val service = com.example.data.network.JellyfinApiService.create("http://192.168.1.100:8096")
        assertNotNull(service)
        assertTrue(com.example.data.network.JellyfinApiService.DEFAULT_AUTH_HEADER.contains("MediaBrowser"))

        val authWithToken = com.example.data.network.JellyfinApiService.buildAuthHeader("my_access_token_123")
        assertTrue(authWithToken.contains("Token=\"my_access_token_123\""))
        assertTrue(authWithToken.contains("Client=\"JellyCast\""))
    }

    @Test
    fun testMediaCacheManagerLibrariesAndItems() = kotlinx.coroutines.runBlocking {
        val tempDir = java.nio.file.Files.createTempDirectory("jelly_cache_test").toFile()
        try {
            val cacheManager = com.example.data.cache.MediaCacheManager(cacheDirectory = tempDir)

            // Test libraries caching
            val testLibraries = listOf(
                com.example.data.model.JellyfinLibrary(id = "lib-1", name = "Movies", collectionType = "movies"),
                com.example.data.model.JellyfinLibrary(id = "lib-2", name = "TV Shows", collectionType = "tvshows")
            )
            cacheManager.saveLibraries(testLibraries)

            val cachedLibs = cacheManager.getCachedLibraries()
            assertNotNull(cachedLibs)
            assertEquals(2, cachedLibs!!.size)
            assertEquals("Movies", cachedLibs[0].name)

            // Test series/movie items caching
            val testItems = listOf(
                com.example.data.model.JellyfinMediaItem(
                    id = "item-101",
                    name = "Interstellar",
                    overview = "Space exploration",
                    type = "Movie",
                    productionYear = 2014,
                    communityRating = 8.7f,
                    runTimeTicks = 10000000L,
                    genres = listOf("Sci-Fi", "Adventure")
                )
            )
            cacheManager.saveMediaItems("lib-1", testItems)

            val cachedItems = cacheManager.getCachedMediaItems("lib-1")
            assertNotNull(cachedItems)
            assertEquals(1, cachedItems!!.size)
            assertEquals("Interstellar", cachedItems[0].name)

            // Test individual item lookup
            val singleItem = cacheManager.getCachedMediaItem("item-101")
            assertNotNull(singleItem)
            assertEquals("Interstellar", singleItem!!.name)

            // Test cache clear
            cacheManager.clear()
            val afterClear = cacheManager.getCachedLibraries()
            org.junit.Assert.assertNull(afterClear)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
