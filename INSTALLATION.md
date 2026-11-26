# Android TV Installation & Setup Guide

This guide explains how to install the **7Reels TV** application on any Android TV device, including **LineageOS Android TV on Raspberry Pi 4**, Google TV, Fire TV, and Android TV boxes.

---

## 📥 Download the APK

Always download the latest build from the [GitHub Releases](https://github.com/malakaisbest/7reelsTV/releases) page:
- **Direct APK:** `7ReelsTV-v1.1.0.apk`

---

## 🚀 Installation Methods

### Method 1: Wireless ADB Installation (Recommended for Raspberry Pi 4 / Developers)

If your TV or Raspberry Pi has **Network ADB Debugging** enabled:

1. **Enable Developer Options & Network ADB on your TV:**
   - Go to **Settings** > **Device Preferences** > **About**.
   - Tap **Build Number** 7 times until Developer Mode is unlocked.
   - Go back to **Developer Options** and turn on **Network ADB Debugging**.
   - Note your TV's IP address (e.g. `192.168.0.108`) under **Settings > Network & Internet**.

2. **Connect and Install from your PC / Terminal:**
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   adb install -r 7ReelsTV-v1.1.0.apk
   ```

3. **Launch the App:**
   ```bash
   adb shell am start -n com.malakaonline.tv7reals/.MainActivity
   ```
   Or find **7Reels TV** on your Android TV home row / apps screen.

---

### Method 2: Sideloading via USB Flash Drive

1. Download `7ReelsTV-v1.1.0.apk` onto a USB flash drive formatted in FAT32 or exFAT.
2. Plug the USB flash drive into your Android TV or Raspberry Pi 4.
3. On your TV, install a file manager from Google Play Store or F-Droid (e.g., **FX File Explorer**, **AnExplorer**, or **Total Commander**).
4. Allow your file manager permission to *"Install unknown apps"*:
   - **Settings** > **Apps** > **Special app access** > **Install unknown apps** > Enable your file manager.
5. Open your file manager, navigate to the USB drive, click `7ReelsTV-v1.1.0.apk`, and select **Install**.

---

### Method 3: Direct Download via "Downloader" App on Android TV

1. On your Android TV, open the Google Play Store and install the free **Downloader by AFTVnews** app.
2. In TV Settings > Security & Restrictions, enable **Install unknown apps** for the *Downloader* app.
3. Open *Downloader* and enter the direct download URL for the release APK from the GitHub releases page:
   ```text
   https://github.com/malakaisbest/7reelsTV/releases/latest/download/7ReelsTV-v1.1.0.apk
   ```
4. Once the download finishes, click **Install** when prompted.

---

## 🎮 TV Remote Navigation Guide

| Control | Action |
| :--- | :--- |
| **D-Pad Directional Arrows** | Navigate web elements and buttons |
| **D-Pad Center / OK** | Select / Click the focused item |
| **Long-Press OK (0.6s) or MENU key** | **Toggle Virtual Mouse Cursor** on/off |
| **D-Pad Arrows (in Mouse Mode)** | Move the high-contrast mouse pointer |
| **OK Button (in Mouse Mode)** | Simulates touch tap at the cursor position |
| **Back Button (Short Press)** | Exits fullscreen video $\rightarrow$ Navigates back in web history |
| **Back Button (Hold 0.8s)** | **Instant Exit** (closes app from any page immediately) |
| **On-Screen Exit Buttons** | Click the red **Exit** button in the top navbar or watch player |
