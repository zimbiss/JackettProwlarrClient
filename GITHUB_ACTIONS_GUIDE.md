# 📦 GitHub Actions APK Build Guide

## 🚀 Automatic APK Building

This repository is configured to **automatically build the APK** whenever you push code to GitHub. No manual building required!

### ✅ How It Works

1. **Push Your Code**: When you commit and push changes to any `copilot/**` branch (or `main`)
2. **GitHub Actions Triggers**: Three workflows automatically start building
3. **APK is Built**: The app is compiled in the cloud (takes ~5-10 minutes)
4. **Download Ready**: APK is available as an artifact for 90 days

### 📥 How to Download Your APK

#### Method 1: Direct from GitHub Actions (Recommended)

1. **Go to Actions Tab**: 
   - Visit: https://github.com/zimbiss/JackettProwlarrClient/actions

2. **Find Your Build**:
   - Look for the green checkmark ✅ next to your latest commit
   - Click on the workflow run (e.g., "Build APK")

3. **Download the Artifact**:
   - Scroll to the bottom of the page
   - Look for the "Artifacts" section
   - Click on **"JackettProwlarr-debug-v1.0"** to download
   - This downloads as a ZIP file

4. **Extract and Install**:
   - Unzip the downloaded file
   - You'll find `JackettProwlarr-v1.0-debug.apk`
   - Transfer to your Android device
   - Install the APK

#### Method 2: Using nightly.link (Direct Download)

For the **main** branch only:
- [📦 Download Latest Debug APK](https://nightly.link/zimbiss/JackettProwlarrClient/workflows/build-apk/main/JackettProwlarr-debug-v1.0.zip)

For **this branch** (`copilot/push-latest-codespace-changes`):
- [📦 Download Latest Debug APK](https://nightly.link/zimbiss/JackettProwlarrClient/workflows/build-apk/copilot/push-latest-codespace-changes/JackettProwlarr-debug-v1.0.zip)

### 🔄 Current Workflow Status

You can check if the build is running or completed here:
- **Build APK Workflow**: [![Build APK](https://github.com/zimbiss/JackettProwlarrClient/actions/workflows/build-apk.yml/badge.svg?branch=copilot/push-latest-codespace-changes)](https://github.com/zimbiss/JackettProwlarrClient/actions/workflows/build-apk.yml)

### 📱 Installation on Android

1. **Enable Unknown Sources** (if not already enabled):
   - Go to Settings → Security (or Apps)
   - Enable "Install from Unknown Sources" or "Install Unknown Apps"
   - Select your browser/file manager and allow installations

2. **Install the APK**:
   - Download the APK using one of the methods above
   - Open the APK file
   - Tap "Install"
   - Tap "Open" when installation completes

3. **Configure the App**:
   - Open JackettProwlarr Client
   - Go to Settings (⚙️ icon)
   - Configure your Jackett/Prowlarr server URLs
   - Or use the 60+ built-in providers

### 🛠️ Available Workflows

This repository has **3 workflows** that build APKs:

1. **android.yml** - Basic CI build
2. **build-apk.yml** - Advanced build with debug + release APKs ⭐ (Recommended)
3. **build.yml** - Build with versioning and releases

All workflows trigger on:
- ✅ Push to `main` branch
- ✅ Push to `copilot/**` branches (your current branch!)
- ✅ Pull requests to `main`
- ✅ Manual trigger (workflow_dispatch)

### 🎯 Quick Start for First-Time Users

If this is your first time using GitHub Actions to download an APK:

1. **Push this branch** (already done! ✅)
2. **Wait 5-10 minutes** for GitHub Actions to build
3. **Go to**: https://github.com/zimbiss/JackettProwlarrClient/actions
4. **Click** the top workflow run with a green ✅
5. **Scroll down** to "Artifacts"
6. **Download** the APK ZIP file
7. **Unzip** and install on your Android device

### ⚡ Pro Tips

- **Build Time**: APKs take ~5-10 minutes to build in GitHub Actions
- **Artifact Retention**: APKs are kept for 90 days
- **Multiple APKs**: Each workflow creates its own artifact (debug + release)
- **Email Notifications**: GitHub can email you when builds complete (check Settings → Notifications)
- **Manual Trigger**: You can manually trigger builds from the Actions tab using "Run workflow"

### 🐛 Troubleshooting

**Q: I don't see the Artifacts section?**
- A: Make sure the workflow has completed (green ✅ checkmark)
- A: You must be logged into GitHub to see artifacts

**Q: Build is failing?**
- A: Click on the failed job to see error logs
- A: Most common issue: Gradle dependency resolution (usually auto-fixes on retry)

**Q: Can't find my branch's build?**
- A: Use the branch filter in the Actions tab
- A: Select "copilot/push-latest-codespace-changes" from the dropdown

**Q: APK won't install on my phone?**
- A: Make sure you have Android 8.0 (API 26) or higher
- A: Enable "Install from Unknown Sources"
- A: Some phones require enabling installation per-app (check security settings)

### 📊 Build Information

- **Minimum Android Version**: 8.0 (API 26)
- **Target Android Version**: 14 (API 34)
- **Java Version**: 17
- **Gradle Version**: 8.2
- **Build Type**: Debug (includes logging) and Release (optimized, unsigned)

---

## 🎉 That's It!

Every time you push code, GitHub Actions automatically builds a fresh APK for you. No need to install Android Studio or build locally!

**Next Step**: Push this guide to trigger a build, then download your APK! 🚀
