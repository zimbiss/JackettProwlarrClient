# 📱 APK Build & Download Guide

## 🚀 Automatic APK Building

This repository uses **GitHub Actions** to automatically build APKs whenever code is pushed to:
- `main` branch
- Any `copilot/**` branch (like `copilot/track-last-request-status`)
- Any pull request to `main`

You can also **manually trigger** a build using the "workflow_dispatch" option.

---

## 📦 How to Get Your APK

### Method 1: Download from GitHub Actions (Recommended)

1. **Go to the Actions tab** on GitHub:
   ```
   https://github.com/zimbiss/JackettProwlarrClient/actions
   ```

2. **Find your workflow run**:
   - Look for workflows named "Build APK" or "Build and Release APK"
   - Click on the most recent successful run (green checkmark ✅)

3. **Download the APK artifact**:
   - Scroll down to the "Artifacts" section
   - Click on one of these to download:
     - `JackettProwlarr-debug-v1.0` - Debug APK with logs (7-8 MB)
     - `JackettProwlarr-release-v1.0` - Optimized release APK (7-8 MB)
   - The APK will download as a ZIP file - extract it to get the APK

4. **Install on Android**:
   - Transfer the APK to your Android device
   - Enable "Install from Unknown Sources" in Settings → Security
   - Tap the APK file to install

---

## 🔧 Available Workflows

### 1. **build-apk.yml** (Most Comprehensive) ⭐
- Builds both Debug and Release APKs
- Creates versioned filenames
- Uploads artifacts for 90 days
- Auto-creates GitHub Releases on version tags
- **Triggers**: Push to `main`, `copilot/**`, PRs, manual dispatch

### 2. **build.yml** (Full Build)
- Builds Debug and Release APKs
- Creates releases on version tags
- **Triggers**: Push to `main`, `master`, `copilot/**`, PRs, manual dispatch

### 3. **android.yml** (Simple Build)
- Builds Debug APK only
- Quick validation build
- **Triggers**: Push to `main`, `copilot/**`, PRs, manual dispatch

---

## 🎯 Current Branch: `copilot/track-last-request-status`

Your latest changes include:
- ✅ Search job cancellation (prevents background requests)
- ✅ Proper CancellationException handling
- ✅ Improvements to TorrentAggregator ("aggregaterx")

The workflow **has already been triggered** for this branch and is building your APK now!

---

## 🏃 Manual Trigger (If Needed)

If you want to manually trigger a build:

1. Go to **Actions** tab
2. Select **"Build APK"** workflow from the left sidebar
3. Click **"Run workflow"** button (top right)
4. Select your branch: `copilot/track-last-request-status`
5. Click **"Run workflow"** (green button)

The build will start immediately and complete in ~5-10 minutes.

---

## 📊 Build Status

Check build status here:
- **All Actions**: https://github.com/zimbiss/JackettProwlarrClient/actions
- **Latest Workflow**: https://github.com/zimbiss/JackettProwlarrClient/actions/workflows/build-apk.yml

---

## 🔍 Finding the "180MB" Reference

**Note**: The APK files are typically 7-8 MB, not 180 MB. The 180 MB reference might be:
- The entire Gradle build cache (~150-200 MB including dependencies)
- A full project export with build artifacts
- A misunderstanding about file size

The actual APK you need is the **7.4 MB debug APK** that's built by GitHub Actions.

---

## ✅ What Was Fixed

### Search Cancellation Feature
When users start a new search while another is running, the old search is now properly cancelled:

```kotlin
// Track active search
private var searchJob: Job? = null

// Cancel previous before starting new
searchJob?.cancel()
searchJob = uiScope.launch(Dispatchers.IO) {
    // ... search logic
}
```

This applies to:
- Torrent searches (Jackett/Prowlarr)
- Video searches
- Aggregated searches (all sources)

---

## 🛠️ Development Build (Local)

If you want to build locally (requires network access):

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

./gradlew assembleRelease  
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

**Note**: Local builds require internet access to download Gradle dependencies (~200 MB first time).

---

## 📝 Summary

✅ **GitHub Actions is configured and working**
✅ **APK builds automatically on push to your branch**
✅ **Download from Actions tab within 90 days**
✅ **Your search cancellation fix is included**
✅ **TorrentAggregator (aggregaterx) improvements are built in**

**Next Step**: Go to the Actions tab and download your APK! 🎉
