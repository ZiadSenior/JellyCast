package com.example.data.cache

import android.content.Context
import com.example.data.model.JellyfinLibrary
import com.example.data.model.JellyfinMediaItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-performance dual-tier (Memory + Disk) cache for Jellyfin libraries and series/movie metadata.
 * Enables zero-latency navigation, instant poster/item loading, and reliable offline browsing capability.
 */
class MediaCacheManager(
    private val context: Context? = null,
    cacheDirectory: File? = null
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val libraryListType = Types.newParameterizedType(List::class.java, JellyfinLibrary::class.java)
    private val mediaListType = Types.newParameterizedType(List::class.java, JellyfinMediaItem::class.java)

    private val librariesAdapter = moshi.adapter<List<JellyfinLibrary>>(libraryListType)
    private val mediaItemsAdapter = moshi.adapter<List<JellyfinMediaItem>>(mediaListType)
    private val mediaItemAdapter = moshi.adapter(JellyfinMediaItem::class.java)

    // In-memory fast L1 cache
    @Volatile
    private var memoryLibraries: List<JellyfinLibrary>? = null
    private val memoryLibraryItems = mutableMapOf<String, List<JellyfinMediaItem>>()
    private val memoryIndividualItems = mutableMapOf<String, JellyfinMediaItem>()

    // Disk L2 cache directory
    private val cacheDir: File = (cacheDirectory ?: File(context?.cacheDir, "jelly_metadata")).apply { mkdirs() }

    suspend fun getCachedLibraries(): List<JellyfinLibrary>? = withContext(Dispatchers.IO) {
        // L1 Check
        memoryLibraries?.let { return@withContext it }

        // L2 Check
        val file = File(cacheDir, "libraries.json")
        if (file.exists() && file.length() > 0) {
            try {
                val json = file.readText()
                val list = librariesAdapter.fromJson(json)
                if (!list.isNullOrEmpty()) {
                    memoryLibraries = list
                    return@withContext list
                }
            } catch (e: Exception) {
                file.delete()
            }
        }
        null
    }

    suspend fun saveLibraries(libraries: List<JellyfinLibrary>) = withContext(Dispatchers.IO) {
        memoryLibraries = libraries
        try {
            val file = File(cacheDir, "libraries.json")
            file.writeText(librariesAdapter.toJson(libraries))
        } catch (ignored: Exception) {}
    }

    suspend fun getCachedMediaItems(libraryId: String): List<JellyfinMediaItem>? = withContext(Dispatchers.IO) {
        // L1 Check
        synchronized(memoryLibraryItems) {
            memoryLibraryItems[libraryId]?.let { return@withContext it }
        }

        // L2 Check
        val file = File(cacheDir, "items_$libraryId.json")
        if (file.exists() && file.length() > 0) {
            try {
                val json = file.readText()
                val list = mediaItemsAdapter.fromJson(json)
                if (!list.isNullOrEmpty()) {
                    synchronized(memoryLibraryItems) {
                        memoryLibraryItems[libraryId] = list
                        list.forEach { item -> memoryIndividualItems[item.id] = item }
                    }
                    return@withContext list
                }
            } catch (e: Exception) {
                file.delete()
            }
        }
        null
    }

    suspend fun saveMediaItems(libraryId: String, items: List<JellyfinMediaItem>) = withContext(Dispatchers.IO) {
        synchronized(memoryLibraryItems) {
            memoryLibraryItems[libraryId] = items
            items.forEach { item -> memoryIndividualItems[item.id] = item }
        }
        try {
            val file = File(cacheDir, "items_$libraryId.json")
            file.writeText(mediaItemsAdapter.toJson(items))
        } catch (ignored: Exception) {}
    }

    suspend fun getCachedMediaItem(itemId: String): JellyfinMediaItem? = withContext(Dispatchers.IO) {
        // L1 Check
        synchronized(memoryIndividualItems) {
            memoryIndividualItems[itemId]?.let { return@withContext it }
        }

        // Search all cached lists in memory
        synchronized(memoryLibraryItems) {
            for (list in memoryLibraryItems.values) {
                val match = list.firstOrNull { it.id == itemId }
                if (match != null) {
                    memoryIndividualItems[itemId] = match
                    return@withContext match
                }
            }
        }

        // L2 Check individual item file
        val file = File(cacheDir, "item_$itemId.json")
        if (file.exists() && file.length() > 0) {
            try {
                val item = mediaItemAdapter.fromJson(file.readText())
                if (item != null) {
                    synchronized(memoryIndividualItems) {
                        memoryIndividualItems[itemId] = item
                    }
                    return@withContext item
                }
            } catch (ignored: Exception) {}
        }
        null
    }

    suspend fun saveMediaItem(item: JellyfinMediaItem) = withContext(Dispatchers.IO) {
        synchronized(memoryIndividualItems) {
            memoryIndividualItems[item.id] = item
        }
        try {
            val file = File(cacheDir, "item_${item.id}.json")
            file.writeText(mediaItemAdapter.toJson(item))
        } catch (ignored: Exception) {}
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        memoryLibraries = null
        synchronized(memoryLibraryItems) {
            memoryLibraryItems.clear()
        }
        synchronized(memoryIndividualItems) {
            memoryIndividualItems.clear()
        }
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
