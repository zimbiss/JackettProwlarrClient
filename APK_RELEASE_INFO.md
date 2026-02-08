# APK Release Information

## Latest Build: app-debug.apk

**Date**: February 8, 2026  
**Version**: 1.0 (versionCode 1)  
**Size**: 7.4 MB  
**Type**: Debug APK  
**Min Android**: API 26 (Android 8.0)  
**Target Android**: API 34  

### Build Details
- Built from latest source code in repository
- Includes all features from JackettProwlarrClient
- Debug build with full logging enabled

### Installation
1. Download `app-debug.apk` from repository root
2. Enable "Install from Unknown Sources" on Android device
3. Transfer APK to device and install
4. Grant necessary permissions when prompted

### Features Included
- Torznab API integration (Jackett/Prowlarr)
- 61+ built-in torrent providers
- 200+ tracker integration
- Video search with 10+ platforms
- Custom site support
- Tor/.onion site access
- qBittorrent integration
- Download history tracking

### Configuration
Update hardcoded credentials in MainActivity.kt for your setup:
- Jackett: http://192.168.1.175:9117
- Prowlarr: http://192.168.1.175:9696

Or use built-in providers without external server configuration.
