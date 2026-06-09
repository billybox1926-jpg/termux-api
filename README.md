# Termux:API Fork Workbench

[![Debug APK CI](https://img.shields.io/github/actions/workflow/status/billybox1926-jpg/termux-api/android-debug.yml?branch=workbench-api-updates&label=debug%20APK&logo=githubactions&logoColor=white)](https://github.com/billybox1926-jpg/termux-api/actions/workflows/android-debug.yml?query=branch%3Aworkbench-api-updates)
[![Workbench branch](https://img.shields.io/badge/branch-workbench--api--updates-0969da?logo=git&logoColor=white)](https://github.com/billybox1926-jpg/termux-api/tree/workbench-api-updates)
[![Companion package](https://img.shields.io/badge/package-termux--api--package-2ea44f?logo=gnubash&logoColor=white)](https://github.com/billybox1926-jpg/termux-api-package)
[![License: GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

This repository is a focused fork of **Termux:API**, the Android app that lets Termux commands talk to Android system APIs. It is maintained as a CI-first workbench for upstream-friendly fixes, Android runtime experiments, and package/app integration work.

> This is not an official Termux release channel. Debug APKs and workbench builds are test artifacts. Install them only when you understand the matching Termux app, Termux:API app, package helper, Android package name, and signing relationship involved.

---

## What this app does

Termux runs as a terminal app. Android device features such as sensors, notifications, storage pickers, cameras, media controls, and telephony are owned by the Android framework. Termux needs an installed Android app to bridge those APIs safely.

A normal API call flows through the companion command package and returns plain terminal output:

```text
Termux shell command
  -> wrapper script
  -> termux-api native helper
  -> socket / broadcast bridge
  -> TermuxApiReceiver
  -> Android API implementation
  -> stdout / stderr response back to Termux
```

No root access is required for normal supported APIs. Android permissions, package identity, and signing compatibility still matter.

---

## Current fork focus

This fork carries changes on workbench branches before they are split into clean upstream pull requests.

| Area | Status | Notes |
| --- | --- | --- |
| Keyboard API | App-side implementation added | Adds `KeyboardShow`, `KeyboardHide`, and `KeyboardVisible` for matching package wrappers. |
| SAF picker | App-side implementation added | Adds a file-picker flow intended for a `termux-saf-picker` wrapper. |
| Media notifications | Behavior fixed | Media notifications can expose whichever media actions are provided instead of requiring the full action set. |
| Debug runtime lane | In progress | Side-by-side `com.termux.api.debug` testing is used to avoid casually replacing the installed release plugin. |
| Antivirus false positives | Documentation added | Explains official build sources, debug signing, and useful report details. |

Keyboard visibility is best-effort on Android. IME state depends on focus, keyboard implementation, Android version, and whether an app currently owns an editable view.

---

## Repository model

| Repo | Purpose | Active lane |
| --- | --- | --- |
| [`termux-api`](https://github.com/billybox1926-jpg/termux-api) | Android app-side API implementation | `workbench-api-updates` |
| [`termux-api-package`](https://github.com/billybox1926-jpg/termux-api-package) | Termux command wrappers and native helper | `workbench-package-updates` |

Most features need both sides:

1. Android implementation in this repo.
2. Method dispatch in `TermuxApiReceiver`.
3. Manifest permissions, activities, services, or providers when needed.
4. Matching wrapper/helper support in `termux-api-package`.
5. Real-device runtime proof with a matched app/package stack.

---

## Install and use

For normal daily usage, install official Termux-family packages from one compatible source and then install the command package inside Termux:

```sh
pkg install termux-api
```

Example commands:

```sh
termux-battery-status
termux-toast "Hello from Termux"
termux-location
termux-notification --title "Termux" --content "API bridge is working"
```

### Signing and source compatibility

Termux plugins depend on Android package identity and signing relationships. Do not casually mix F-Droid, GitHub debug, Play Store, or locally signed builds.

If you switch installation sources, uninstall the related Termux apps/plugins first unless you intentionally know the packages and signing keys are compatible. See the [Termux app installation guide](https://github.com/termux/termux-app#installation) for upstream guidance.

---

## Debug APK workflow

This fork uses GitHub Actions as the build authority for debug APKs. Local Windows builds are useful for quick inspection, but CI is the green/red gate.

The debug workflow builds from `workbench-api-updates` and uploads an APK artifact with a commit-stamped filename, for example:

```text
termux-api-app_0.53.0+<short-sha>.github.debug.apk
```

Side-by-side debug testing targets:

```text
package:  com.termux.api.debug
receiver: com.termux.api.debug/com.termux.api.TermuxApiReceiver
socket:   com.termux.api.debug://listen
```

Raw `adb shell am broadcast` is useful only to prove receiver delivery. Full API proof should go through a matching `termux-api` helper because `ResultReturner` expects socket extras created by the native helper.

---

## Build from source

### Requirements

* Android SDK with `compileSdkVersion=35`
* Android 7.0 / API 24 minimum runtime
* Java 11-compatible toolchain
* Gradle wrapper from this repository

### Linux, macOS, or Termux-style shell

```sh
./gradlew clean :app:assembleDebug --no-daemon --console=plain
```

### Windows PowerShell

```powershell
.\gradlew.bat clean :app:assembleDebug --no-daemon --console=plain
```

APK outputs are written under:

```text
app/build/outputs/apk/
```

Debug APKs are signed with the repository test key. Treat them as test artifacts unless you also control the matching Termux app and package helper target.

---

## Project layout

```text
termux-api/
├── .github/
│   ├── workflows/
│   │   └── android-debug.yml
│   └── PULL_REQUEST_TEMPLATE.md
├── app/
│   ├── build.gradle
│   ├── testkey_untrusted.jks
│   └── src/
│       ├── debug/
│       │   └── AndroidManifest.xml
│       └── main/
│           ├── AndroidManifest.xml
│           └── java/com/termux/api/
│               ├── TermuxApiReceiver.java
│               ├── SocketListener.java
│               ├── apis/
│               ├── activities/
│               ├── settings/
│               └── util/
├── build.gradle
├── gradle.properties
├── gradlew / gradlew.bat
├── README.md
└── SECURITY.md
```

Most app-side changes land in one of these areas:

| Change | File area |
| --- | --- |
| New Android API behavior | `app/src/main/java/com/termux/api/apis/` |
| Request dispatch | `app/src/main/java/com/termux/api/TermuxApiReceiver.java` |
| Socket-backed request path | `app/src/main/java/com/termux/api/SocketListener.java` |
| Permissions / activities / services | `app/src/main/AndroidManifest.xml` |
| Debug-only package behavior | `app/src/debug/` |

Command wrappers and native helper targeting belong in the companion `termux-api-package` repository.

---

## Development standards

A good change should be small, inspectable, and runtime-aware.

Before a change is considered ready for upstream review:

* CI must pass on the relevant workbench branch.
* Runtime behavior should be tested on a real Android device when the change touches Android APIs.
* App-side changes that require wrappers should include package-side support.
* Signing/package identity risks must be called out when debug builds are involved.
* Logs should show the first real compiler/runtime error, not surrounding noise.
* Security-sensitive or permission-sensitive changes should explain why the permission is needed.

Keep PRs focused. A clean upstream-ready change should explain the user-visible behavior, include the package-side wrapper when needed, and avoid bundling unrelated cleanup.

---

## Runtime proof checklist

Use this checklist when validating an API change:

```text
[ ] Correct branch and commit are installed
[ ] APK package name is expected
[ ] Matching command helper targets the same package/component/socket
[ ] Receiver delivery is proven
[ ] Socket-backed helper call returns terminal output
[ ] Android permission prompts or appops are documented
[ ] Logcat is checked for crashes or permission denials
[ ] Rollback path is known before replacing any release package
```

---

## Security

Security reporting guidance is in [`SECURITY.md`](SECURITY.md). For general Termux security policy, see [https://termux.dev/security](https://termux.dev/security).

Please do not report suspected security vulnerabilities through public issues unless the Termux security policy explicitly directs you to do so.

---

## License

This project is released under the [GNU General Public License v3.0](http://www.gnu.org/licenses/gpl-3.0.en.html). See [`LICENSE`](LICENSE) for the full text.
