# Changelog

All notable changes to the **JellyCast** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.4] - 2026-10-26

### Added
- **Audio Track Selector**:
  - Live Audio Track selection dropdown in both `RemoteControlScreen` and `MediaDetailScreen`.
  - Shows audio stream title, language, codec (e.g. `AAC`, `AC3`, `EAC3`, `DTS`, `TrueHD`), and channel count (Stereo, 5.1, 7.1).
  - Active audio track status pill on the playback bar.
- **MPEG-TS Live Transcoding Mode (`AAC (MPEG-TS)`)**:
  - Added `.ts` container streaming option to bypass AV receiver and Smart TV incompatibilities with live progressive MP4 containers.
- **Dynamic DLNA DIDL-Lite MIME Type Detection**:
  - Automatically advertises `video/mp2t` for MPEG-TS, `video/x-matroska` for MKV, and `video/mp4` for MP4, ensuring DLNA renderers correctly accept stream metadata.
- **Seamless Position Preservation**:
  - Automatically captures the current playback timestamp and resumes smoothly when changing audio tracks or transcode profiles.
- **One-Tap Jellyfin Server Auto-Discovery**:
  - Auto-discovers Jellyfin instances on the local network via UDP broadcast (port 7359) and subnet probing so users don't need to manually type IP addresses.
- **Dynamic Versioned APK Naming**:
  - Release APKs are now dynamically named after the version: `JellyCast-1.4.apk`, `JellyCast-1.5.apk`, etc.

### Fixed
- **Audio Transcoding No-Sound & "Unsupported Format" Issue**:
  - Fixed FFmpeg query parameters for Jellyfin live streaming (`VideoCodec=copy`, `allowVideoStreamCopy=true`, `enableAutoStreamCopy=true`, `allowAudioStreamCopy=false`, downmixing to 2-channel 256k AAC).
  - Resolved receiver playback failure and video-playing-without-sound bugs.
- **Multi-Track Audio Silence on Secondary Tracks**:
  - Fixed missing `MediaSourceId`, `DeviceId`, and `PlaySessionId` in stream URLs. Jellyfin now correctly locates the underlying media source and extracts alternate audio tracks (commentary, secondary languages, surround soundtracks) with full audio output.

---

## [1.3] - 2026-10-26

### Added
- **Server Discovery Engine**:
  - Implemented UDP broadcast socket listener on port 7359.
  - Added parallel network subnet scan fallback for servers running with broadcast disabled.
- **Stream Options UI**:
  - Added Audio Transcoding & Subtitle dialog access on the remote screen.

---

## [1.2] - 2026-10-26

### Changed
- **Ultra-Smooth 60/120 FPS UI Optimization**:
  - Decoupled playback position polling flow from screen recomposition.
  - Eliminated high-frequency recomposition stutters across MediaList and Detail screens.
  - Configured Coil with memory-efficient `RGB_565` bitmap decoding and lightweight caching.
- **Full TV Series Hierarchy**:
  - Complete navigation tree: Libraries -> Series -> Seasons -> Episodes with episode metadata and thumbnails.
- **Obtanium & GitHub Releases Compatibility**:
  - Configured stable keystore reuse and automated GitHub Actions release workflow.

---

## [1.1] - 2026-10-26

### Added
- **Dual-Tier High-Performance Caching**:
  - Fast in-memory (L1) and persistent disk (L2) JSON caching for libraries, seasons, and episodes.
  - Instant catalog loading with background cache refreshing.
- **Episode & Movie Detail Views**:
  - Backdrop banners, overview synopsis, runtime, genres, and community ratings.

---

## [1.0] - 2026-10-26

### Added
- **Initial Release**:
  - Direct connection to Jellyfin server with user credentials and token authentication.
  - DLNA / UPnP SSDP device discovery with smart filtering for media renderers.
  - SOAP AVTransport playback control (Play, Pause, Stop, Seek +30s / -10s).
  - SOAP RenderingControl volume adjustment and mute toggling.
  - Modern Jetpack Compose Material 3 dark-mode user interface.
