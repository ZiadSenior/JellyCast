package com.example.data.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.DiscoveredJellyfinServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Discovers Jellyfin media servers on the local network using:
 * 1. Official Jellyfin UDP Broadcast Discovery on port 7359 ("Who is JellyfinServer?")
 * 2. Rapid HTTP probe on common local subnet addresses (/System/Info/Public on port 8096)
 */
class JellyfinServerDiscovery(private val context: Context) {
    private val tag = "JellyfinDiscovery"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(1200, TimeUnit.MILLISECONDS)
        .readTimeout(1500, TimeUnit.MILLISECONDS)
        .build()

    suspend fun discoverServers(timeoutMs: Int = 3000): List<DiscoveredJellyfinServer> = withContext(Dispatchers.IO) {
        val foundServers = mutableMapOf<String, DiscoveredJellyfinServer>()

        var multicastLock: WifiManager.MulticastLock? = null
        var socket: DatagramSocket? = null

        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("jellycast_discovery")
            multicastLock?.setReferenceCounted(true)
            multicastLock?.acquire()

            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 700

            // 1. Send Jellyfin UDP broadcast on port 7359
            val queryBytes = "Who is JellyfinServer?".toByteArray(Charsets.UTF_8)
            val broadcastTargets = mutableListOf<InetAddress>()

            try {
                broadcastTargets.add(InetAddress.getByName("255.255.255.255"))
            } catch (e: Exception) { /* ignore */ }

            // Add interface broadcast addresses
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    if (networkInterface.isLoopback || !networkInterface.isUp) continue
                    for (interfaceAddress in networkInterface.interfaceAddresses) {
                        val broadcast = interfaceAddress.broadcast
                        if (broadcast != null) {
                            broadcastTargets.add(broadcast)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to get interface broadcasts: ${e.message}")
            }

            for (target in broadcastTargets.distinct()) {
                try {
                    val packet = DatagramPacket(queryBytes, queryBytes.size, target, 7359)
                    socket.send(packet)
                } catch (e: Exception) {
                    Log.d(tag, "Failed sending broadcast to $target: ${e.message}")
                }
            }

            // Receive UDP responses
            val buffer = ByteArray(4096)
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(receivePacket)
                    val responseText = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8).trim()
                    Log.d(tag, "UDP discovery response from ${receivePacket.address.hostAddress}: $responseText")

                    val server = parseJellyfinResponse(responseText, receivePacket.address.hostAddress)
                    if (server != null) {
                        foundServers[server.address] = server
                    }
                } catch (e: SocketTimeoutException) {
                    // Small step timeout, continue checking until overall timeout elapsed
                } catch (e: Exception) {
                    Log.w(tag, "Receive error: ${e.message}")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Discovery error: ${e.message}", e)
        } finally {
            try { socket?.close() } catch (ignored: Exception) {}
            try { multicastLock?.release() } catch (ignored: Exception) {}
        }

        // 2. Complementary fast subnet probe on default port 8096 if UDP yielded few or no results
        if (foundServers.isEmpty()) {
            probeCommonLocalAddresses(foundServers)
        }

        foundServers.values.toList()
    }

    private fun parseJellyfinResponse(jsonStr: String, senderIp: String): DiscoveredJellyfinServer? {
        return try {
            val json = JSONObject(jsonStr)
            var address = json.optString("Address").takeIf { it.isNotBlank() }
                ?: json.optString("EndpointAddress").takeIf { it.isNotBlank() }
                ?: "http://$senderIp:8096"

            if (!address.startsWith("http://") && !address.startsWith("https://")) {
                address = "http://$address"
            }
            // Remove trailing slash
            address = address.trimEnd('/')

            val name = json.optString("Name").takeIf { it.isNotBlank() }
                ?: json.optString("ServerName").takeIf { it.isNotBlank() }
                ?: "Jellyfin Server ($senderIp)"
            val id = json.optString("Id").takeIf { it.isNotBlank() }

            DiscoveredJellyfinServer(name = name, address = address, id = id)
        } catch (e: Exception) {
            if (jsonStr.contains("http") || jsonStr.contains("8096")) {
                DiscoveredJellyfinServer(
                    name = "Jellyfin Server ($senderIp)",
                    address = if (jsonStr.startsWith("http")) jsonStr else "http://$senderIp:8096"
                )
            } else null
        }
    }

    private suspend fun probeCommonLocalAddresses(foundMap: MutableMap<String, DiscoveredJellyfinServer>) = coroutineScope {
        val candidates = mutableSetOf<String>()

        // Find device local IP prefix e.g. 192.168.1.
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        val parts = host.split(".")
                        if (parts.size == 4) {
                            val prefix = "${parts[0]}.${parts[1]}.${parts[2]}."
                            // Common server suffixes
                            listOf(1, 2, 3, 4, 10, 20, 50, 100, 101, 150, 200, parts[3].toIntOrNull() ?: 100).forEach { suffix ->
                                candidates.add("$prefix$suffix")
                            }
                        }
                    }
                }
            }
        } catch (ignored: Exception) {}

        // Fallback default candidates if no wifi interface detected
        if (candidates.isEmpty()) {
            listOf(
                "192.168.1.100", "192.168.1.2", "192.168.0.100", "192.168.0.2",
                "192.168.1.1", "192.168.0.1", "10.0.0.100", "10.0.0.2"
            ).forEach { candidates.add(it) }
        }

        val tasks = candidates.map { ip ->
            async {
                probeSingleAddress(ip)
            }
        }

        val results = tasks.awaitAll().filterNotNull()
        for (server in results) {
            foundMap[server.address] = server
        }
    }

    private fun probeSingleAddress(ip: String): DiscoveredJellyfinServer? {
        val testUrl = "http://$ip:8096/System/Info/Public"
        return try {
            val request = Request.Builder()
                .url(testUrl)
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val serverName = json.optString("ServerName").takeIf { it.isNotBlank() } ?: "Jellyfin Server ($ip)"
                    val serverId = json.optString("Id").takeIf { it.isNotBlank() }
                    DiscoveredJellyfinServer(
                        name = serverName,
                        address = "http://$ip:8096",
                        id = serverId
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
