package com.example.data.model

enum class TransportState {
    IDLE,
    CONNECTING,
    BUFFERING,
    PLAYING,
    PAUSED,
    STOPPED,
    ERROR
}

data class PositionInfo(
    val trackDurationSeconds: Long,
    val relTimeSeconds: Long
)

data class DlnaDevice(
    val id: String,
    val friendlyName: String,
    val modelName: String? = null,
    val manufacturer: String? = null,
    val location: String,
    val host: String,
    val avTransportControlUrl: String,
    val renderingControlUrl: String? = null,
    val iconUrl: String? = null
) {
    val displaySubtitle: String
        get() = when {
            !modelName.isNullOrBlank() -> "$modelName ($host)"
            !manufacturer.isNullOrBlank() -> "$manufacturer ($host)"
            else -> host
        }
}

data class DlnaPlaybackStatus(
    val state: TransportState = TransportState.IDLE,
    val currentItem: JellyfinMediaItem? = null,
    val positionSeconds: Long = 0L,
    val durationSeconds: Long = 0L,
    val volume: Int = 50,
    val isMuted: Boolean = false,
    val errorMessage: String? = null,
    val speed: String = "1"
) {
    val progress: Float
        get() = if (durationSeconds > 0) {
            (positionSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val formattedPosition: String
        get() = formatTime(positionSeconds)

    val formattedDuration: String
        get() = formatTime(durationSeconds)

    companion object {
        fun formatTime(totalSeconds: Long): String {
            if (totalSeconds <= 0) return "00:00"
            val hrs = totalSeconds / 3600
            val mins = (totalSeconds % 3600) / 60
            val secs = totalSeconds % 60
            return if (hrs > 0) {
                String.format("%02d:%02d:%02d", hrs, mins, secs)
            } else {
                String.format("%02d:%02d", mins, secs)
            }
        }
    }
}
