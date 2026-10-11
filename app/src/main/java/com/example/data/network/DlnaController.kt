package com.example.data.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.DlnaDevice
import com.example.data.model.JellyfinMediaItem
import com.example.data.model.PositionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.TimeUnit

class DlnaController(private val context: Context) {
    private val tag = "DlnaController"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun discoverLocalDevices(timeoutMs: Int = 3500): List<DlnaDevice> = withContext(Dispatchers.IO) {
        val discoveredList = mutableListOf<DlnaDevice>()
        val seenLocations = mutableSetOf<String>()

        var multicastLock: WifiManager.MulticastLock? = null
        var socket: DatagramSocket? = null

        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("jellycast_ssdp")
            multicastLock?.setReferenceCounted(true)
            multicastLock?.acquire()

            socket = DatagramSocket()
            socket.soTimeout = 800

            val ssdpAddress = InetAddress.getByName("239.255.255.250")
            val ssdpPort = 1900

            // Search targets: AVTransport, MediaRenderer, rootdevice
            val searchTargets = listOf(
                "urn:schemas-upnp-org:service:AVTransport:1",
                "urn:schemas-upnp-org:device:MediaRenderer:1",
                "upnp:rootdevice",
                "ssdp:all"
            )

            for (st in searchTargets) {
                val query = "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 3\r\n" +
                        "ST: $st\r\n\r\n"

                val bytes = query.toByteArray()
                val packet = DatagramPacket(bytes, bytes.size, ssdpAddress, ssdpPort)
                socket.send(packet)
            }

            val endTime = System.currentTimeMillis() + timeoutMs
            val buffer = ByteArray(4096)

            while (System.currentTimeMillis() < endTime) {
                try {
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)

                    val location = parseHeader(response, "LOCATION")
                    if (!location.isNullOrBlank() && seenLocations.add(location)) {
                        val device = fetchDeviceDescription(location)
                        if (device != null) {
                            discoveredList.add(device)
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    if (System.currentTimeMillis() >= endTime) break
                } catch (e: Exception) {
                    Log.w(tag, "Packet receive error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Discovery error: ${e.message}", e)
        } finally {
            try {
                socket?.close()
                multicastLock?.let { if (it.isHeld) it.release() }
            } catch (ignored: Exception) {}
        }

        discoveredList
    }

    private fun parseHeader(response: String, header: String): String? {
        val lines = response.lines()
        for (line in lines) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2 && parts[0].trim().equals(header, ignoreCase = true)) {
                return parts[1].trim()
            }
        }
        return null
    }

    suspend fun fetchDeviceDescription(locationUrl: String): DlnaDevice? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(locationUrl).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val xml = response.body?.string() ?: return@withContext null

            val urlObj = URL(locationUrl)
            val portPart = if (urlObj.port != -1) ":${urlObj.port}" else ""
            val baseUrl = "${urlObj.protocol}://${urlObj.host}$portPart"
            val host = urlObj.host

            var friendlyName = "DLNA Media Renderer"
            var modelName: String? = null
            var manufacturer: String? = null
            var udn = "uuid:" + host.replace(".", "_")
            var avTransportControlUrl: String? = null
            var renderingControlUrl: String? = null

            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var currentTag = ""
            var insideService = false
            var currentServiceType = ""
            var currentControlUrl = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag.equals("service", ignoreCase = true)) {
                            insideService = true
                            currentServiceType = ""
                            currentControlUrl = ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            if (insideService) {
                                if (currentTag.equals("serviceType", ignoreCase = true)) {
                                    currentServiceType = text
                                } else if (currentTag.equals("controlURL", ignoreCase = true)) {
                                    currentControlUrl = text
                                }
                            } else {
                                when (currentTag.lowercase()) {
                                    "friendlyname" -> friendlyName = text
                                    "modelname" -> modelName = text
                                    "manufacturer" -> manufacturer = text
                                    "udn" -> udn = text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("service", ignoreCase = true)) {
                            insideService = false
                            if (currentServiceType.contains("AVTransport", ignoreCase = true)) {
                                avTransportControlUrl = resolveUrl(baseUrl, currentControlUrl)
                            } else if (currentServiceType.contains("RenderingControl", ignoreCase = true)) {
                                renderingControlUrl = resolveUrl(baseUrl, currentControlUrl)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            // Fallback URLs if not explicitly specified in service descriptor
            if (avTransportControlUrl == null) {
                avTransportControlUrl = "$baseUrl/upnp/control/AVTransport1"
            }
            if (renderingControlUrl == null) {
                renderingControlUrl = "$baseUrl/upnp/control/RenderingControl1"
            }

            DlnaDevice(
                id = udn,
                friendlyName = friendlyName,
                modelName = modelName,
                manufacturer = manufacturer,
                location = locationUrl,
                host = host,
                avTransportControlUrl = avTransportControlUrl,
                renderingControlUrl = renderingControlUrl
            )
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse XML from $locationUrl: ${e.message}")
            null
        }
    }

    suspend fun probeManualDevice(name: String, ip: String, port: Int): DlnaDevice = withContext(Dispatchers.IO) {
        val cleanIp = ip.trim()
        val candidatePaths = listOf(
            "/description.xml",
            "/device-desc.xml",
            "/ssdp/device-desc.xml",
            "/dial/dd.xml",
            "/dmr/SamsungMRDesc.xml",
            "/upnp/desc.xml"
        )

        for (path in candidatePaths) {
            val url = "http://$cleanIp:$port$path"
            val fetched = fetchDeviceDescription(url)
            if (fetched != null) {
                return@withContext if (name.isNotBlank()) fetched.copy(friendlyName = name) else fetched
            }
        }

        // Direct fallback device with standard UPnP endpoints
        DlnaDevice(
            id = "manual:$cleanIp:$port",
            friendlyName = name.ifBlank { "DLNA TV ($cleanIp)" },
            modelName = "DLNA Media Renderer",
            manufacturer = "UPnP Device",
            location = "http://$cleanIp:$port/description.xml",
            host = cleanIp,
            avTransportControlUrl = "http://$cleanIp:$port/upnp/control/AVTransport1",
            renderingControlUrl = "http://$cleanIp:$port/upnp/control/RenderingControl1"
        )
    }

    private fun resolveUrl(baseUrl: String, relativeOrAbsolute: String): String {
        return if (relativeOrAbsolute.startsWith("http://") || relativeOrAbsolute.startsWith("https://")) {
            relativeOrAbsolute
        } else {
            val cleanBase = baseUrl.trimEnd('/')
            val cleanPath = if (relativeOrAbsolute.startsWith("/")) relativeOrAbsolute else "/$relativeOrAbsolute"
            "$cleanBase$cleanPath"
        }
    }

    suspend fun setAvTransportUri(
        device: DlnaDevice,
        mediaUrl: String,
        mediaItem: JellyfinMediaItem
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val didlLite = buildDidlMetadata(mediaItem, mediaUrl)
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                  <CurrentURI><![CDATA[$mediaUrl]]></CurrentURI>
                  <CurrentURIMetaData><![CDATA[$didlLite]]></CurrentURIMetaData>
                </u:SetAVTransportURI>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        sendSoapAction(
            device.avTransportControlUrl,
            "urn:schemas-upnp-org:service:AVTransport:1",
            "SetAVTransportURI",
            body
        )
    }

    suspend fun play(device: DlnaDevice): Result<Boolean> = withContext(Dispatchers.IO) {
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:Play xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                  <Speed>1</Speed>
                </u:Play>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        sendSoapAction(device.avTransportControlUrl, "urn:schemas-upnp-org:service:AVTransport:1", "Play", body)
    }

    suspend fun pause(device: DlnaDevice): Result<Boolean> = withContext(Dispatchers.IO) {
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:Pause xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                </u:Pause>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        sendSoapAction(device.avTransportControlUrl, "urn:schemas-upnp-org:service:AVTransport:1", "Pause", body)
    }

    suspend fun stop(device: DlnaDevice): Result<Boolean> = withContext(Dispatchers.IO) {
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:Stop xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                </u:Stop>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        sendSoapAction(device.avTransportControlUrl, "urn:schemas-upnp-org:service:AVTransport:1", "Stop", body)
    }

    suspend fun seek(device: DlnaDevice, targetSeconds: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        val targetTime = formatToRelTime(targetSeconds)
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:Seek xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                  <Unit>REL_TIME</Unit>
                  <Target>$targetTime</Target>
                </u:Seek>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        sendSoapAction(device.avTransportControlUrl, "urn:schemas-upnp-org:service:AVTransport:1", "Seek", body)
    }

    suspend fun getPositionInfo(device: DlnaDevice): PositionInfo? = withContext(Dispatchers.IO) {
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:GetPositionInfo xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                  <InstanceID>0</InstanceID>
                </u:GetPositionInfo>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val response = executeSoapCall(device.avTransportControlUrl, "urn:schemas-upnp-org:service:AVTransport:1", "GetPositionInfo", body)
        if (response != null) {
            val relTime = parseXmlTagValue(response, "RelTime")
            val trackDuration = parseXmlTagValue(response, "TrackDuration")
            if (relTime != null && trackDuration != null) {
                val relSec = parseTimeToSeconds(relTime)
                val durSec = parseTimeToSeconds(trackDuration)
                if (durSec > 0 || relSec > 0) {
                    return@withContext PositionInfo(trackDurationSeconds = durSec, relTimeSeconds = relSec)
                }
            }
        }
        null
    }

    suspend fun setVolume(device: DlnaDevice, desiredVolume: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        val controlUrl = device.renderingControlUrl ?: "${device.host}/upnp/control/RenderingControl1"
        val clamped = desiredVolume.coerceIn(0, 100)
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:SetVolume xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
                  <InstanceID>0</InstanceID>
                  <Channel>Master</Channel>
                  <DesiredVolume>$clamped</DesiredVolume>
                </u:SetVolume>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        sendSoapAction(controlUrl, "urn:schemas-upnp-org:service:RenderingControl:1", "SetVolume", body)
    }

    suspend fun getVolume(device: DlnaDevice): Int? = withContext(Dispatchers.IO) {
        val controlUrl = device.renderingControlUrl ?: return@withContext null
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:GetVolume xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
                  <InstanceID>0</InstanceID>
                  <Channel>Master</Channel>
                </u:GetVolume>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val response = executeSoapCall(controlUrl, "urn:schemas-upnp-org:service:RenderingControl:1", "GetVolume", body)
        if (response != null) {
            val volumeStr = parseXmlTagValue(response, "CurrentVolume")
            volumeStr?.toIntOrNull()
        } else {
            null
        }
    }

    suspend fun setMute(device: DlnaDevice, mute: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        val controlUrl = device.renderingControlUrl ?: return@withContext Result.failure(Exception("Device has no RenderingControl"))
        val desiredMute = if (mute) "1" else "0"
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:SetMute xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
                  <InstanceID>0</InstanceID>
                  <Channel>Master</Channel>
                  <DesiredMute>$desiredMute</DesiredMute>
                </u:SetMute>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        sendSoapAction(controlUrl, "urn:schemas-upnp-org:service:RenderingControl:1", "SetMute", body)
    }

    suspend fun getMute(device: DlnaDevice): Boolean? = withContext(Dispatchers.IO) {
        val controlUrl = device.renderingControlUrl ?: return@withContext null
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
              <s:Body>
                <u:GetMute xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
                  <InstanceID>0</InstanceID>
                  <Channel>Master</Channel>
                </u:GetMute>
              </s:Body>
            </s:Envelope>
        """.trimIndent()

        val response = executeSoapCall(controlUrl, "urn:schemas-upnp-org:service:RenderingControl:1", "GetMute", body)
        if (response != null) {
            val muteStr = parseXmlTagValue(response, "CurrentMute")
            muteStr == "1" || muteStr.equals("true", ignoreCase = true)
        } else {
            null
        }
    }

    private suspend fun sendSoapAction(
        controlUrl: String,
        serviceType: String,
        action: String,
        soapBody: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val responseString = executeSoapCall(controlUrl, serviceType, action, soapBody)
            if (responseString != null) {
                Result.success(true)
            } else {
                Result.failure(Exception("SOAP $action failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeSoapCall(
        controlUrl: String,
        serviceType: String,
        action: String,
        soapBody: String
    ): String? {
        return try {
            val mediaType = "text/xml; charset=\"utf-8\"".toMediaType()
            val request = Request.Builder()
                .url(controlUrl)
                .header("SOAPAction", "\"$serviceType#$action\"")
                .header("Content-Type", "text/xml; charset=\"utf-8\"")
                .post(soapBody.toRequestBody(mediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseXmlTagValue(xml: String, tagName: String): String? {
        return try {
            val regex = "<$tagName[^>]*>(.*?)</$tagName>".toRegex(RegexOption.IGNORE_CASE)
            regex.find(xml)?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTimeToSeconds(timeStr: String): Long {
        val parts = timeStr.trim().split(":")
        return when (parts.size) {
            3 -> {
                val hrs = parts[0].toLongOrNull() ?: 0L
                val mins = parts[1].toLongOrNull() ?: 0L
                val secs = parts[2].substringBefore(".").toLongOrNull() ?: 0L
                hrs * 3600 + mins * 60 + secs
            }
            2 -> {
                val mins = parts[0].toLongOrNull() ?: 0L
                val secs = parts[1].substringBefore(".").toLongOrNull() ?: 0L
                mins * 60 + secs
            }
            else -> 0L
        }
    }

    private fun formatToRelTime(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hrs, mins, secs)
    }

    private fun buildDidlMetadata(item: JellyfinMediaItem, mediaUrl: String): String {
        val isAudio = item.type.equals("Audio", ignoreCase = true)
        val upnpClass = if (isAudio) "object.item.audioItem.musicTrack" else "object.item.videoItem.movie"
        val mimeType = if (isAudio) "audio/mp3" else "video/mp4"

        return """
            <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">
              <item id="1" parentID="0" restricted="1">
                <dc:title>${item.name.replace("<", "&lt;").replace(">", "&gt;")}</dc:title>
                <upnp:class>$upnpClass</upnp:class>
                <res protocolInfo="http-get:*:$mimeType:*">$mediaUrl</res>
              </item>
            </DIDL-Lite>
        """.trimIndent()
    }
}
