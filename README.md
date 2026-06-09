# Termux:API

[![Debug APK CI](https://img.shields.io/github/actions/workflow/status/billybox1926-jpg/termux-api/android-debug.yml?branch=workbench-api-updates&label=debug%20APK)](https://github.com/billybox1926-jpg/termux-api/actions/workflows/android-debug.yml?query=branch%3Aworkbench-api-updates)
[![License: GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

Termux:API is the Android app side of the Termux API stack. It exposes selected Android system features to Termux commands, shell scripts, and programs.

The command-line wrappers live in the companion package repository: [`termux-api-package`](https://github.com/termux/termux-api-package).

Current upstream app version: **0.53.0**

---

## What this app does

Termux itself runs as a terminal app. Android device features such as sensors, notifications, storage pickers, cameras, and telephony are owned by the Android framework, so Termux needs an installed Android app to bridge those APIs safely.

This app receives requests from the `termux-api` command helper, runs the requested Android API operation, and returns results to the terminal process.

Common API areas include:

| Area | Examples |
| --- | --- |
| Device state | battery, sensors, location, telephony info |
| Media | camera, audio info, microphone recording, media scan |
| User interaction | toast, dialog, notification, clipboard |
| Storage and sharing | SAF, storage picker, share, download manager |
| System controls | brightness, volume, torch, vibration, wallpaper |
| Connectivity | Wi-Fi info, Wi-Fi scan, USB, NFC |
| Text and speech | speech-to-text, text-to-speech |

---

## Install and use

Install both pieces of the stack:

1. The **Termux:API app** from the same source/signing family as your Termux app.
2. The **`termux-api` package** inside Termux.

Inside Termux:

```sh
pkg install termux-api
```

Then run commands such as:

```sh
termux-battery-status
termux-toast "Hello from Termux"
termux-location
termux-notification --title "Termux" --content "API bridge is working"
```

### Signing and source compatibility

Termux plugins depend on Android package identity and signing relationships. Do not casually mix F-Droid, GitHub debug, Play Store, or locally signed builds.

If you switch installation sources, uninstall the related Termux apps/plugins first unless you intentionally know the packages and signing keys are compatible. See the [Termux app installation guide](https://github.com/termux/termux-app#Installation) for the current upstream guidance.

---

## Current fork additions

This fork is carrying workbench changes before they are split into clean upstream pull requests.

| Area                      | Status                        | Notes                                                                                                         |
| ------------------------- | ----------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Keyboard API              | App-side implementation added | Adds `KeyboardShow`, `KeyboardHide`, and `KeyboardVisible` app methods for matching package wrappers.         |
| SAF picker                | App-side implementation added | Adds a file-picker flow intended for a `termux-saf-picker` wrapper.                                           |
| Media notifications       | Behavior fixed                | Media notifications can expose whichever media actions are provided instead of requiring the full action set. |
| Antivirus false positives | Documentation added           | Explains official build sources, debug signing, and useful report details.                                    |

Keyboard visibility is best-effort on Android. IME state depends on the focused window, keyboard implementation, Android version, and whether an app currently owns an editable view.

---

## How requests flow

The command-line package provides small shell wrappers plus the `termux-api` helper binary. A typical call follows this path:

```text
Termux shell command
  -> wrapper script
  -> termux-api helper
  -> Android broadcast / socket bridge
  -> TermuxApiReceiver
  -> API implementation class
  -> stdout / stderr response back to Termux
```

The app performs Android-side work; the Termux command receives plain terminal output. No root access is required.

---

## Build from source

### Requirements

* Android SDK with `compileSdkVersion=35`
* Android 7.0 / API 24 minimum runtime
* Java 11-compatible toolchain
* Gradle wrapper from this repository

### Build

Linux/macOS/Termux-style shell:

```sh
./gradlew clean :app:assembleDebug --no-daemon --console=plain
```

Windows PowerShell:

```powershell
.\gradlew.bat clean :app:assembleDebug --no-daemon --console=plain
```

APK outputs are written under:

```text
app/build/outputs/apk/
```

### Debug builds

Debug APKs are signed with the repository test key and are not drop-in replacements for every installed Termux stack. Treat debug builds as test artifacts unless you are also controlling the matching Termux app and package helper target.

---

## Project layout

```text
termux-api/
├── app/
│   ├── build.gradle
│   ├── testkey_untrusted.jks
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/termux/api/
│           ├── TermuxApiReceiver.java
│           ├── apis/
│           ├── activities/
│           ├── settings/
│           └── util/
├── build.gradle
├── gradle.properties
├── gradlew / gradlew.bat
├── README.md
└── SECURITY.md
```

Most feature work happens in one of three places:

| Change                              | File area                                |
| ----------------------------------- | ---------------------------------------- |
| New Android API behavior            | `app/src/main/java/com/termux/api/apis/` |
| Request dispatch                    | `TermuxApiReceiver.java`                 |
| Permissions / activities / services | `app/src/main/AndroidManifest.xml`       |

Command wrappers belong in the companion `termux-api-package` repository.

---

## Development notes

When adding or changing an API:

1. Add or update the Android API implementation.
2. Register the method in `TermuxApiReceiver`.
3. Add any required manifest permissions or internal activities.
4. Add or update the matching wrapper in `termux-api-package`.
5. Test with a matched app/package stack on a real Android device.
6. Document behavior, permissions, and known Android limitations.

Keep PRs focused. A clean upstream-ready change should explain the user-visible behavior, include the package-side wrapper when needed, and avoid bundling unrelated cleanup.

---

## Security

Security reporting guidance is in [`SECURITY.md`](SECURITY.md). For general Termux security policy, see [https://termux.dev/security](https://termux.dev/security).

---

## License

This project is released under the [GNU General Public License v3.0](http://www.gnu.org/licenses/gpl-3.0.en.html). See [`LICENSE`](LICENSE) for the full text.
