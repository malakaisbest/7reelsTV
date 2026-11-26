# 7Reels TV — Android TV WebView Browser

A lightweight, dedicated kiosk web browser application designed specifically for **Android TV** (targeting **LineageOS 23.2 on Raspberry Pi 4** connected to a 55" Full HD display).

---

## ⚠️ Disclaimer & Legal Notice

> **IMPORTANT NOTICE:**  
> The developer of this application has **no affiliation, association, authorization, endorsement, or responsibility** for the third-party website (`7reels.cc`), its servers, video providers, or content.  
> This project is solely an independent open-source **display application and TV interface** created to allow comfortable remote-control navigation on Android TV hardware. All copyrights, media, and trademarks belong to their respective owners.  
> See [DISCLAIMER.md](DISCLAIMER.md) for full terms.

---

## ✨ Features

- **Dual TV Modes:**
  - **Native Leanback Experience:** Smooth, remote-friendly dashboard with featured movies auto-rotation, Trending rows, and Movies / TV Series browse grids.
  - **WebView Browser Mode:** Full desktop kiosk browser mode for the direct website experience with integrated overlay exit & mode-switcher controls.
- **Movie Details & Media Hub:**
  - View full plot, genres, user rating, and release date.
  - Horizontally scrollable **Cast & Crew** actors list with headshots.
  - Grid-based **Streaming Server Selector** with server status badges.
  - TV Shows season & episode explorer.
  - Similar & Recommended Movies row.
- **Watch History & Continue Watching:**
  - Automatic watch timeline tracking for movies and TV episodes.
  - Dedicated **Continue Watching** shelf on the home screen with progress bar and remaining time (`1h 32m left`).
  - **Smart Resume Dialog:** Automatically prompts to resume playback right where you left off or start from the beginning.
- **Enhanced TV Remote Controls & Player Shortcuts:**
  - **D-Pad UP (▲):** Instantly triggers Subtitle/CC menu and focuses language selection.
  - **D-Pad Center (OK):** Direct Play / Pause toggle inside video player.
  - **D-Pad Left / Right (◄ / ►):** Instant 10-second Seek backward / forward with on-screen HUD.
  - **Remote Back Button:** Exits virtual cursor mode instantly without closing playback.
  - **Virtual Mouse Cursor:** Long-press OK or press MENU to toggle virtual cursor anytime.
- **Three-Tier Ad & Popup Blocking:**
  - Silently drops all `window.open()` and `target="_blank"` popups.
  - Enforces domain locking to trusted CDN/video hosts, blocking rogue redirects.
  - Network-level interception returning empty responses for known tracker and ad networks.

---

## 📥 Installation

For detailed step-by-step instructions, see [INSTALLATION.md](INSTALLATION.md).

### Quick Install via ADB (Raspberry Pi 4 / Android TV):

1. Download `7ReelsTV-v1.1.0.apk` from [Releases](https://github.com/malakaisbest/7reelsTV/releases).
2. Connect to your TV over Wi-Fi:
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   adb install -r 7ReelsTV-v1.1.0.apk
   ```

---

## 🎮 Remote Controls Summary

| Button | Function |
| :--- | :--- |
| **D-Pad Direction Keys** | Move focus between cards / grids |
| **D-Pad Center (OK)** | Select item / Play-Pause inside video stream |
| **D-Pad UP (▲)** | Auto-open Subtitles / CC menu during video playback |
| **D-Pad Left / Right (◄ / ►)** | Seek -10s / +10s during video playback |
| **Long-Press OK (0.6s) / MENU** | Toggle Virtual Mouse Cursor overlay |
| **Back (Short Press)** | Exit cursor mode $\rightarrow$ Exit video $\rightarrow$ Go back in history |
| **Back (Hold 0.8s)** | **Direct Exit App** (bypasses history stack) |
| **Back (Double Tap at Root)** | Exit confirmation guard |

---

## 🛠️ Building from Source

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (API 35, Build-Tools 34.0.0 or 35.0.0)

### Build APK
```bash
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
This project is open-source under the MIT License. See [LICENSE](LICENSE) for details.
