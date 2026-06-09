# Termux:API

[![Build status](https://github.com/termux/termux-api/workflows/Build/badge.svg)](https://github.com/termux/termux-api/actions)
[![Join the chat at https://gitter.im/termux/termux](https://badges.gitter.im/termux/termux.svg)](https://gitter.im/termux/termux)
[![License: GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

Termux:API is an Android app that exposes Android system APIs to Termux — letting you access device hardware and OS features directly from the command line, shell scripts, and programs.

Latest version: **v0.53.0**

---

## Table of Contents

- [What It Does](#what-it-does)
- [Installation](#installation)
- [Prerequisites](#prerequisites)
- [Available APIs](#available-apis)
- [How It Works](#how-it-works)
- [Building From Source](#building-from-source)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

---

## What It Does

Termux:API bridges the gap between Termux's Linux-like terminal environment and the Android operating system. Without it, Termux scripts cannot access device hardware or privileged OS features. With it, you can:

- Read sensors (accelerometer, gyroscope, light, proximity, etc.)
- Take photos and read camera info
- Get GPS location
- Send and read SMS
- Access call logs and contacts
- Control the flashlight/torch
- Show notifications, toasts, and dialogs
- Control volume, brightness, and wallpaper
- Play and record audio
- Access the clipboard
- Scan NFC tags
- Interact with USB devices
- Query battery status
- Vibrate the device
- Run jobs with JobScheduler
- Access the Android Keystore
- Use fingerprint authentication
- Share files and content
- Download files
- Access storage via SAF (Storage Access Framework)
- Scan media files
- Convert speech to text and text to speech
- Control WiFi (scan, connect, get info)
- Make phone calls
- Send infrared signals

All of this is accessible from simple command-line scripts inside Termux.

---

## Installation

### F-Droid (Recommended)

Install from [F-Droid](https://f-droid.org/en/packages/com.termux.api/).

### Debug Builds (Latest Features)

Per-commit debug builds are available from [GitHub Actions](https://github.com/termux/termux-api/actions/workflows/github_action_build.yml?query=branch%3Amaster+event%3Apush).

> **Note:** Signature keys differ between F-Droid and debug builds. Before switching installation sources, you must uninstall the Termux app and all installed plugins. See the [Termux app installation guide](https://github.com/termux/termux-app#Installation) for details.

---

## Prerequisites

- **Termux app** must be installed (same signing key is required for permissions to work)
- **Android 7.0 (API 24)** or higher
- The `termux-api` package installed inside Termux:

```bash
pkg install termux-api
```

---

## Available APIs

The following API endpoints are available. Each corresponds to a client script installed with the `termux-api` package.

| API | Description |
|-----|-------------|
| `termux-audio` | Get audio info and manage audio |
| `termux-battery-status` | Query battery status (level, health, temperature, etc.) |
| `termux-brightness` | Set screen brightness |
| `termux-call-log` | Read call history |
| `termux-camera-info` | Get camera device information |
| `termux-camera-photo` | Take a photo and save it |
| `termux-clipboard-get` / `termux-clipboard-set` | Read/write clipboard |
| `termux-contact-list` | List device contacts |
| `termux-dialog` | Show input dialogs (text, confirm, checkbox, etc.) |
| `termux-download` | Download files via the download manager |
| `termux-fingerprint` | Use fingerprint authentication |
| `termux-infrared-transmit` | Send infrared signals |
| `termux-job-scheduler` | Schedule background jobs |
| `termux-keystore` | Access the Android Keystore |
| `termux-location` | Get device location (GPS/network) |
| `termux-media-player` | Play/pause/seek media files |
| `termux-media-scan` | Trigger media scanner on files |
| `termux-mic-record` | Record audio from the microphone |
| `termux-nfc` | Read/write NFC tags |
| `termux-notification` | Show, edit, and remove notifications |
| `termux-notification-list` | List active notifications |
| `termux-saf` | Access files via Storage Access Framework |
| `termux-sensor` | Read device sensors in real time |
| `termux-share` | Share files or text |
| `termux-sms-inbox` | Read SMS messages |
| `termux-sms-send` | Send SMS messages |
| `termux-speech-to-text` | Convert speech to text |
| `termux-storage-get` | Pick a file from storage |
| `termux-telephony-call` | Initiate phone calls |
| `termux-telephony-cellinfo` | Get cellular network info |
| `termux-telephony-deviceinfo` | Get telephony device info |
| `termux-text-to-speech` | Convert text to speech |
| `termux-toast` | Show a toast message |
| `termux-torch` | Toggle the flashlight |
| `termux-usb` | Interact with USB devices |
| `termux-vibrate` | Vibrate the device |
| `termux-volume` | Get/set volume levels |
| `termux-wallpaper` | Set the wallpaper |
| `termux-wifi-connectioninfo` | Get current WiFi connection info |
| `termux-wifi-scaninfo` | Scan for WiFi networks |
| `termux-wifi-enable` | Enable/disable WiFi |

### Quick Examples

```bash
# Get battery status
termux-battery-status

# Take a photo with the back camera
termux-camera-photo -c 0 /sdcard/photo.jpg

# Get GPS location
termux-location

# Show a notification
termux-notification --title "Hello" --content "From Termux!" --id 1

# Toggle flashlight on
termux-torch on

# Read accelerometer sensor data
termux-sensor -s accelerometer -n 5

# Send an SMS
termux-sms-send -n +1234567890 "Hello from Termux"

# Show a toast
termux-toast "Hello, World!"

# Vibrate for 1 second
termux-vibrate -d 1000

# Get current WiFi info
termux-wifi-connectioninfo
```

---

## How It Works

The communication between Termux and the Termux:API app happens through Android's broadcast mechanism and Unix domain sockets:

1. The `termux-api` client binary (from the `termux-api` package) creates **two Unix domain sockets** — one for input, one for output.
2. It passes the socket addresses to `TermuxApiReceiver` (a broadcast receiver in the Android app) via:

   ```
   /system/bin/am broadcast ${BROADCAST_RECEIVER} --es socket_input ${INPUT_SOCKET} --es socket_output ${OUTPUT_SOCKET}
   ```

3. The input socket forwards stdin from the client to the relevant API handler class.
4. The output socket sends the API response back to the client's stdout.

This design means the client scripts never need to run as root — the Android app handles all privileged operations and returns results through the socket pair.

---

## Building From Source

### Requirements

- Android SDK (compileSdk 35, minSdk 24, targetSdk 28)
- Java 11
- Gradle 8.7.3+

### Build Steps

```bash
# Clone the repo
git clone https://github.com/termux/termux-api.git
cd termux-api

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

The output APK will be at `app/build/outputs/apk/`.

### Important Notes

- The app **must be signed with the same key as the main Termux app** for API permissions to work. The debug build uses `app/testkey_untrusted.jks`.
- The `termux-shared` library dependency is pulled from JitPack. If you need to modify it, see the [Termux Libraries wiki](https://github.com/termux/termux-app/wiki/Termux-Libraries).

---

## Project Structure

```
termux-api/
├── app/
│   ├── build.gradle              # App-level Gradle config
│   ├── testkey_untrusted.jks     # Debug signing key
│   └── src/main/
│       ├── java/com/termux/api/
│       │   ├── TermuxAPIApplication.java       # Application class
│       │   ├── TermuxApiReceiver.java          # Broadcast receiver (entry point)
│       │   ├── TermuxAPIConstants.java         # Shared constants
│       │   ├── KeepAliveService.java           # Foreground service for background work
│       │   ├── SocketListener.java             # Socket server for API communication
│       │   ├── apis/                           # Individual API implementations
│       │   │   ├── AudioAPI.java
│       │   │   ├── BatteryStatusAPI.java
│       │   │   ├── BrightnessAPI.java
│       │   │   ├── CallLogAPI.java
│       │   │   ├── CameraInfoAPI.java
│       │   │   ├── CameraPhotoAPI.java
│       │   │   ├── ClipboardAPI.java
│       │   │   ├── ContactListAPI.java
│       │   │   ├── DialogAPI.java
│       │   │   ├── DownloadAPI.java
│       │   │   ├── FingerprintAPI.java
│       │   │   ├── InfraredAPI.java
│       │   │   ├── JobSchedulerAPI.java
│       │   │   ├── KeystoreAPI.java
│       │   │   ├── LocationAPI.java
│       │   │   ├── MediaPlayerAPI.java
│       │   │   ├── MediaScannerAPI.java
│       │   │   ├── MicRecorderAPI.java
│       │   │   ├── NfcAPI.java
│       │   │   ├── NotificationAPI.java
│       │   │   ├── NotificationListAPI.java
│       │   │   ├── SAFAPI.java
│       │   │   ├── SensorAPI.java
│       │   │   ├── ShareAPI.java
│       │   │   ├── SmsInboxAPI.java
│       │   │   ├── SmsSendAPI.java
│       │   │   ├── SpeechToTextAPI.java
│       │   │   ├── StorageGetAPI.java
│       │   │   ├── TelephonyAPI.java
│       │   │   ├── TextToSpeechAPI.java
│       │   │   ├── ToastAPI.java
│       │   │   ├── TorchAPI.java
│       │   │   ├── UsbAPI.java
│       │   │   ├── VibrateAPI.java
│       │   │   ├── VolumeAPI.java
│       │   │   ├── WallpaperAPI.java
│       │   │   └── WifiAPI.java
│       │   ├── activities/                      # UI activities
│       │   │   ├── TermuxAPIMainActivity.java
│       │   │   └── TermuxApiPermissionActivity.java
│       │   ├── settings/                       # Settings/preferences
│       │   │   └── ...
│       │   └── util/                           # Shared utilities
│       │       ├── JsonUtils.java
│       │       ├── PendingIntentUtils.java
│       │       ├── PluginUtils.java
│       │       ├── ResultReturner.java
│       │       └── ViewUtils.java
│       └── AndroidManifest.xml
├── build.gradle                  # Project-level Gradle config
├── settings.gradle               # Project settings
├── gradle.properties             # SDK versions and Gradle options
├── gradlew / gradlew.bat         # Gradle wrapper
├── README.md
└── SECURITY.md
```

---

## Contributing

Contributions are welcome! Here's how to get started:

1. **Fork** the repository
2. **Clone** your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/termux-api.git
   ```
3. **Create a branch** for your feature or fix:
   ```bash
   git checkout -b my-feature
   ```
4. **Make your changes** and test them
5. **Commit** with a clear message:
   ```bash
   git commit -m "Add: description of your change"
   ```
6. **Push** to your fork:
   ```bash
   git push origin my-feature
   ```
7. **Open a Pull Request** against the `master` branch

### Guidelines

- Follow the existing code style (Java, no Kotlin)
- Keep changes focused — one feature/fix per PR
- Test your changes on a real device if possible
- Update documentation if you add or change API behavior
- Be respectful and constructive in discussions

### Adding a New API

To add a new API endpoint:

1. Create a new class in `app/src/main/java/com/termux/api/apis/` (e.g., `MyNewAPI.java`)
2. Register it in `TermuxApiReceiver.java` as a new case
3. Add any required permissions to `AndroidManifest.xml`
4. Create a client script in the [termux-api-package](https://github.com/termux/termux-api-package) repo

---

## License

This project is released under the [GNU General Public License v3.0](http://www.gnu.org/licenses/gpl-3.0.en.html).

See [LICENSE](LICENSE) for the full text.
