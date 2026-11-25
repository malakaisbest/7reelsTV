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

- **Leanback TV Launcher:** Appears natively on the Android TV home screen row with a custom 320x180dp vector banner (`@drawable/tv_banner`).
- **Immersive Fullscreen:** Automatically hides status and navigation bars with sticky immersive mode (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`).
- **Desktop Layout:** Sends a Linux Chrome Desktop User-Agent so the website renders in full widescreen 1080p rather than mobile view.
- **Three-Tier Ad & Popup Blocking:**
  - Silently drops all `window.open()` and `target="_blank"` popups.
  - Enforces domain locking to `7reels.cc` and trusted CDN/video hosts, blocking rogue redirects.
  - Network-level interception returning empty responses for known tracker and ad networks.
- **HTML5 Fullscreen Video:** Automatically expands video streams to the full display via `onShowCustomView` / `onHideCustomView`.
- **Integrated Exit Buttons:**
  - Injected directly into the top navigation bar (`Home`, `Movies`, `Series`, `My List`, `Exit`).
  - Injected directly next to the floating **Back** button inside the movie watch player (`/watch`).
- **Remote-Friendly Navigation:**
  - Standard D-Pad focus navigation.
  - **Virtual Mouse Cursor:** Long-press **OK** or press **MENU** to toggle an on-screen cursor for buttons without keyboard focus states.
  - **Instant Exit:** Hold the remote **BACK** button for 0.8s from any screen to close the app without traversing history.

---

## 📥 Installation

For detailed step-by-step instructions, see [INSTALLATION.md](INSTALLATION.md).

### Quick Install via ADB (Raspberry Pi 4 / Android TV):

1. Download `7ReelsTV-v1.0.0.apk` from [Releases](https://github.com/malakaisbest/7reelsTV/releases).
2. Connect to your TV over Wi-Fi:
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   adb install -r 7ReelsTV-v1.0.0.apk
   ```

---

## 🎮 Remote Controls Summary

| Button | Function |
| :--- | :--- |
| **D-Pad Direction Keys** | Move focus between elements |
| **D-Pad Center (OK)** | Select / Click focused item |
| **Long-Press OK (0.6s) / MENU** | Toggle Virtual Mouse Cursor overlay |
| **Back (Short Press)** | Exit fullscreen video $\rightarrow$ Go back in web history |
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
