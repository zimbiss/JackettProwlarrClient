# JackettProwlarrClient

[![Build APK](https://github.com/zimbiss/JackettProwlarrClient/actions/workflows/build-apk.yml/badge.svg)](https://github.com/zimbiss/JackettProwlarrClient/actions/workflows/build-apk.yml)
[![GitHub release](https://img.shields.io/github/v/release/zimbiss/JackettProwlarrClient?include_prereleases)](https://github.com/zimbiss/JackettProwlarrClient/releases)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A powerful Android BitTorrent client that uses the Torznab API to search and download torrents from Jackett and Prowlarr indexers, with 61+ built-in providers and **200+ super fast tracker integration**.

## 📥 Download APK

> **🆕 First time using GitHub Actions?** Check out the **[📦 Complete APK Download Guide](GITHUB_ACTIONS_GUIDE.md)** for step-by-step instructions!

### 🚀 Latest Build (Always Up-to-Date)
**[📦 Download Latest Debug APK](https://nightly.link/zimbiss/JackettProwlarrClient/workflows/build-apk/main/JackettProwlarr-debug-v1.0.zip)**

Automatically built from the latest commit on the main branch.

### Alternative Download Methods

#### Method 1: GitHub Actions (Most Recent)
1. Go to the [Actions tab](https://github.com/zimbiss/JackettProwlarrClient/actions/workflows/build-apk.yml)
2. Click on the latest successful workflow run (green checkmark ✅)
3. Scroll down to "Artifacts" section
4. Download the APK artifact (available for 90 days)
5. Extract the ZIP file and install the APK

#### Method 2: GitHub Releases (Stable Versions)
1. Go to the [Releases page](https://github.com/zimbiss/JackettProwlarrClient/releases)
2. Download the latest APK from the release assets
3. Install directly on your Android device

#### Method 3: Build from Source
```bash
git clone https://github.com/zimbiss/JackettProwlarrClient.git
cd JackettProwlarrClient
./gradlew assembleDebug
# APK will be in: app/build/outputs/apk/debug/app-debug.apk
```

### 📱 Installation Instructions
1. **Enable Unknown Sources**: Settings → Security → Install from Unknown Sources
2. **Download APK**: Choose one of the methods above
3. **Install**: Open the APK file and tap "Install"
4. **Grant Permissions**: Allow necessary permissions when prompted
5. **Configure**: Set up Jackett/Prowlarr URLs in Settings (or use built-in providers)

### ⚠️ Important Notes
- **Debug APK**: Recommended for testing, includes detailed logs
- **Release APK**: Optimized but unsigned (requires signing for production)
- **Minimum Android Version**: Android 8.0 (API 26) or higher
- **Build Frequency**: APK is rebuilt automatically on every commit to main branch

## Features

### 🔍 **Multi-Source Search**
- **Torznab API Integration**: Connect to Jackett and Prowlarr servers
- **61+ Built-in Providers**: Pre-configured torrent indexers including:
  - 13 Public Indexers (1337x, The Pirate Bay, RARBG, YTS, Nyaa, etc.)
  - 8 Private Trackers (IPTorrents, TorrentLeech, PassThePopcorn, etc.)
  - 16 Adult Content Sites (Empornium, Sukebei, JAVTorrent, etc.)
  - 8 International Sites (Rutracker, Torrent9, BTDigg, etc.)
  - 16 Specialized DHT/Meta Search Engines
- **Custom URL Support**: Add your own clearnet or .onion torrent sites
- **Mass Import**: Bulk import all indexers from Jackett/Prowlarr with one click

### 📊 **Advanced Torrent Details**
- **Health Status**: Visual health indicators (Excellent, Good, Fair, Poor, Dead)
- **Seeder/Leecher Count**: Real-time peer information with color-coded display
- **Size Information**: Human-readable file sizes (GB, MB, KB)
- **Indexer Source**: See which indexer provided each result
- **Category Information**: Torrent categories when available
- **Publication Date**: When the torrent was uploaded
- **Magnet Link Support**: Direct magnet link handling

### 🎯 **Smart Search**
- **Keyword Extraction**: Automatically searches for individual keywords
- **Duplicate Removal**: Intelligent deduplication of results
- **Relevance Sorting**: Results sorted by seeder count
- **Multi-Source Aggregation**: Search across all enabled sources simultaneously

### 📥 **Multi-Client Download Support**
- **qBittorrent Integration**: Send torrents directly to qBittorrent
- **Automatic Client Detection**: Finds installed torrent clients:
  - µTorrent
  - BitTorrent
  - LibreTorrent
  - Flud
  - Transdroid
  - Deluge
  - FrostWire
- **Magnet Link Support**: Opens magnet links in compatible apps
- **Download History**: Track all downloaded torrents with timestamps

### 🛠️ **Configuration**
- **Built-in Providers**: Load 61+ pre-configured providers with one click
- **Custom Sites**: Add custom torrent sites with CSS selector configuration
- **Tor Support**: Access .onion sites via Orbot integration
- **Indexer Management**: Enable/disable individual indexers
- **qBittorrent Setup**: Configure remote qBittorrent server
- **Connection Monitoring**: Real-time connection status
- **Mass Import**: Import all Jackett/Prowlarr indexers at once

## Hardcoded API Credentials

For development/testing, the app has hardcoded Jackett/Prowlarr credentials:

- **Jackett**: `http://192.168.1.175:9117` (API Key: `sfbizvj42r5h41a2aojb2t29zouqgd3s`)
- **Prowlarr**: `http://192.168.1.175:9696` (API Key: `11e5676f4c3444479cea3671a6c0c55b`)

Change these in [MainActivity.kt](app/src/main/java/com/zim/jackettprowler/MainActivity.kt) for your local setup.

## Technical Implementation

### Torznab Service Layer
The app includes a comprehensive `TorznabService` class that handles all API communication:

```kotlin
val service = TorznabService(baseUrl, apiKey)

// Simple search
val results = service.search("ubuntu", SearchType.SEARCH)

// TV search with metadata
val tvResults = service.searchTv("Breaking Bad", season = 1, episode = 1)

// Movie search
val movieResults = service.searchMovie("Inception", imdbId = "tt1375666")

// Check capabilities
val caps = service.getCapabilities()
```

### Enhanced Data Model
`TorrentResult` includes comprehensive torrent information:
- Title, size, seeders, leechers, peers
- Download URL, magnet URL, info hash
- Indexer source, category, publication date
- IMDB ID support
- Health calculation and status indicators

### Download History
Powered by `DownloadHistoryManager`:
- JSON-based persistent storage
- Last 100 downloads tracked
- Search and filter capabilities
- Timestamp tracking

## Setup

1. **Configure Jackett/Prowlarr**
   - Update `JACKETT_BASE_URL` and `JACKETT_API_KEY` in MainActivity.kt
   - Update `PROWLARR_BASE_URL` and `PROWLARR_API_KEY` in MainActivity.kt

2. **Optional: Configure qBittorrent**
   - Go to Settings in the app
   - Enable qBittorrent integration
   - Enter your qBittorrent Web UI URL, username, and password

3. **Build and Install**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Requirements

- Android 8.0 (API 26) or higher
- Active Jackett or Prowlarr server
- Network connectivity to indexer servers

## Architecture

- **Language**: Kotlin
- **UI**: Material Design with ViewBinding
- **Networking**: OkHttp
- **Async**: Kotlin Coroutines
- **XML Parsing**: Android XmlPullParser
- **JSON**: Gson

## Torznab API Compliance

This app implements the Torznab specification:
- RSS/XML feed parsing
- Torznab namespace attributes (`torznab:attr`)
- All standard search parameters
- Capabilities endpoint (`t=caps`)
- Enclosure support for direct downloads

## Screenshots

The app displays:
- Torrent title (up to 3 lines)
- File size with unit
- Health status (color-coded)
- Seeders (↑ green) and Leechers (↓ orange)
- Source indexer name
- Optional category badge

## Future Enhancements

- [ ] Custom search filters (category, size range, min seeders)
- [ ] Save favorite searches
- [ ] RSS feed subscriptions
- [ ] Automatic download scheduling
- [ ] VPN integration check
- [ ] Advanced sorting options
- [ ] Dark mode

## License

This project is open source. Use responsibly and respect copyright laws in your jurisdiction.
