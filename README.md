# JellyCast 🎬📡

A modern, high-performance Android remote control and casting client designed to browse your **Jellyfin** media server and cast media directly to any **DLNA / UPnP** Smart TV, streaming stick, or media renderer on your local network.

---

> ### 🤖 AI Disclosure
> **This project was created and developed with the assistance of AI** using **Google AI Studio** and Google's Gemini coding models. Architecture, UI design, DLNA/UPnP network protocols, and caching logic were generated and iteratively refined through AI-assisted software engineering.

---

## ✨ Features

- **Direct Jellyfin Integration**: Connects to your self-hosted Jellyfin server (HTTP/HTTPS) using secure token authentication.
- **DLNA / UPnP Streaming**: Built-in SSDP discovery and UPnP AVTransport / RenderingControl implementation:
  - Cast movies, series, and episodes directly to DLNA renderers (Smart TVs, AV receivers, game consoles, etc.).
  - Automatic filtering to ignore router gateways and the server itself, showing only actual media renderers.
- **Full Playback & Volume Control**:
  - Play, pause, stop, and seek (+30s / -10s or interactive slider).
  - Real-time hardware volume control on the target DLNA renderer.
  - Directional navigation D-pad and media control interface.
- **Dual-Tier High-Performance Caching**:
  - **L1 In-Memory & L2 Disk Cache**: Fast JSON serialization for libraries, collections, and movie/series metadata.
  - **Instant Browsing**: Cached libraries and series load instantly upon app startup, updating silently in the background.
- **Polished Material 3 UI**:
  - Modern dark-themed interface built 100% in Jetpack Compose.
  - Fluid animations, poster art caching with Coil, Mini-Player bar, and responsive layouts.
- **Automated CI/CD**:
  - GitHub Actions workflow (`.github/workflows/build-apk.yml`) builds and publishes the APK automatically on push or release tags without requiring manual setup.

---

## 📱 Screenshots & Architecture

```
JellyCast Architecture
├── UI Layer (Jetpack Compose + Material 3)
│   ├── Screens: Setup, Libraries, MediaList, MediaDetail, RemoteControl
│   └── Components: MiniPlayerBar, HeaderBar, CastDeviceDialog
├── Presentation Layer
│   └── JellyRemoteViewModel (StateFlow & Coroutines)
├── Domain & Cache Layer
│   └── MediaCacheManager (L1 RAM + L2 Disk Cache)
└── Data & Network Layer
    ├── JellyfinApiService & AuthRepository (Retrofit + Moshi)
    └── DlnaController (SSDP Multicast Discovery, SOAP AVTransport & RenderingControl)
```

---

## 🚀 Getting Started

### Prerequisites

- Android 8.0 (API Level 26) or higher.
- A running [Jellyfin Media Server](https://jellyfin.org/) accessible on your local network or via domain.
- DLNA / UPnP enabled on your Smart TV or media player.

### Download & Install

You can download the pre-compiled APK directly from the **[Releases](../../releases)** tab of this repository:
1. Go to the latest release (e.g., `v1.4`).
2. Download **`JellyCast-1.4.apk`** (or `JellyCast.apk` for app managers like Obtanium).
3. Install the APK on your Android device (ensure "Install from unknown sources" is enabled in settings).

For release notes and version history, see **[CHANGELOG.md](CHANGELOG.md)**.

### 🔄 Automatic Updates via Obtanium

JellyCast releases are 100% compatible with [Obtanium](https://github.com/ImranR98/Obtanium) for seamless automatic background updates:

1. Open **Obtanium** on your Android device.
2. Tap **Add App** and paste your GitHub repository URL:
   `https://github.com/<your-username>/<your-repo>`
3. Tap **Add** (no complex regex needed; Obtanium will automatically find `JellyCast-<version>.apk` or `JellyCast.apk`).
4. **Certificate Guarantee**: All GitHub Actions builds utilize a consistent, deterministic signing certificate stored in repository configuration, preventing "signing certificate mismatch" errors across updates.

---

## 🛠️ Building from Source

### Requirements
- **JDK 17**
- **Android SDK** (API 34)
- **Gradle 8.7+**

### Build Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/<your-username>/<your-repo>.git
   cd <your-repo>
   ```

2. **Prepare Environment**:
   ```bash
   cp .env.example .env
   ```

3. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   The compiled APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## ⚙️ CI/CD Workflow

This repository includes a fully configured GitHub Actions workflow (`.github/workflows/build-apk.yml`):
- Runs on `ubuntu-24.04` using modern **Node.js 24-native actions**:
  - `actions/checkout@v7`
  - `actions/setup-java@v6`
  - `actions/upload-artifact@v6`
  - `softprops/action-gh-release@v3`
- Automatically compiles the Android project on pushes to `main` and publishes `JellyCast.apk` directly under Releases.

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
