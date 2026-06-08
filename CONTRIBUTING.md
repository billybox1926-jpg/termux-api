# Contributing to Termux:API

Thank you for your interest in contributing! This document will help you get started.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Architecture](#project-architecture)
- [Code Style](#code-style)
- [How to Add a New API](#how-to-add-a-new-api)
- [How to Fix a Bug](#how-to-fix-a-bug)
- [Submitting Changes](#submitting-changes)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Useful Resources](#useful-resources)

---

## Getting Started

1. **Fork** this repository on GitHub
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/termux-api.git
   cd termux-api
   ```
3. **Set up the upstream remote:**
   ```bash
   git remote add upstream https://github.com/termux/termux-api.git
   ```
4. **Create a branch** for your work:
   ```bash
   git checkout -b feature/my-new-api
   ```

---

## Development Setup

### Requirements

- Android Studio (latest stable)
- Android SDK with compileSdk 35
- Java 11
- Gradle 8.7.3+ (included via wrapper)
- A physical Android device or emulator (Android 7.0+ / API 24+)

### Opening the Project

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to the cloned `termux-api` directory
4. Wait for Gradle sync to complete

### Building

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Clean build
./gradlew clean
```

### Important Note on Signing

The Termux:API app **must be signed with the same key as the main Termux app** for API permissions to work. The debug build uses a test key (`app/testkey_untrusted.jks`) which means it will only work alongside a debug build of the main Termux app signed with the same key.

For personal testing, install both the Termux debug APK and the Termux:API debug APK.

---

## Project Architecture

### Communication Flow

```
Termux CLI script (termux-api-package)
    |
    | creates two Unix domain sockets (input + output)
    | passes socket addresses via `am broadcast`
    v
TermuxApiReceiver (BroadcastReceiver)
    |
    | reads `api_method` extra from Intent
    | routes to the matching API class
    v
API class (e.g., TorchAPI.java)
    |
    | performs the privileged Android operation
    | writes result to output socket
    v
ResultReturner
    |
    | returns JSON (or binary) data through the socket
    v
Termux CLI script prints result to stdout
```

### Key Components

| Component | File | Purpose |
|-----------|------|---------|
| **Entry Point** | `TermuxApiReceiver.java` | Receives broadcasts, routes to API classes |
| **Socket Listener** | `SocketListener.java` | Accepts socket connections (alternative to broadcasts) |
| **Result Returner** | `ResultReturner.java` | Handles output socket writing and threading |
| **Constants** | `TermuxAPIConstants.java` | Receiver name, URI authorities |
| **App Class** | `TermuxAPIApplication.java` | Application initialization |
| **KeepAlive Service** | `KeepAliveService.java` | Foreground service for long-running operations |
| **Permission Activity** | `TermuxApiPermissionActivity.java` | Runtime permission requests |
| **Settings** | `settings/` | App preferences UI |

### ResultWriter Types

When implementing an API, choose the appropriate `ResultReturner` base class:

| Type | Use When | Example |
|------|----------|---------|
| `ResultJsonWriter` | Returning structured data | Battery status, sensor data |
| `ResultWriter` | Simple text output | Camera info |
| `WithInput` | Client sends data via input socket | Clipboard set, media playback |
| `WithStringInput` | Client sends a text string | Toast text, notification content |
| `BinaryOutput` | Returning raw binary data | Camera photos, audio recordings |
| `WithAncillaryFd` | Sending file descriptors | USB operations |

---

## Code Style

- **Language:** Java only (no Kotlin)
- **Indentation:** 4 spaces
- **Package structure:** Follow existing patterns
- **Logging:** Use `Logger.logDebug()`, `Logger.logError()`, `Logger.logStackTraceWithMessage()` from `com.termux.shared.logger`
- **Error handling:** Never let exceptions escape from a BroadcastReceiver — always catch and log
- **Permissions:** Request dangerous permissions at runtime using `TermuxApiPermissionActivity.checkAndRequestPermissions()`

---

## How to Add a New API

### Step 1: Create the API Class

Create a new file in `app/src/main/java/com/termux/api/apis/`, e.g., `MyNewAPI.java`:

```java
package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;

public class MyNewAPI {

    static void onReceive(TermuxApiReceiver receiver, Context context, Intent intent) {
        ResultReturner.returnData(receiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                out.beginObject();
                out.name("result").value("success");
                out.name("data").value("your data here");
                out.endObject();
            }
        });
    }
}
```

### Step 2: Register in TermuxApiReceiver

Add your import and a new case in `TermuxApiReceiver.java`:

```java
import com.termux.api.apis.MyNewAPI;

// In doWork() switch statement:
case "MyNewAPI":
    if (TermuxApiPermissionActivity.checkAndRequestPermissions(context, intent, Manifest.permission.SOME_PERMISSION)) {
        MyNewAPI.onReceive(this, context, intent);
    }
    break;
```

### Step 3: Add Permissions to AndroidManifest.xml

If your API needs special permissions, add them to `app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.SOME_PERMISSION" />
```

### Step 4: Create the Client Script

Create a corresponding CLI script in the [termux-api-package](https://github.com/termux/termux-api-package) repository. This is the script users will run from the terminal (e.g., `termux-my-new-api`).

### Step 5: Test

1. Build the debug APK
2. Install on your device alongside the matching Termux build
3. Run your client script and verify the output

---

## How to Fix a Bug

1. **Reproduce** the issue reliably on a device
2. **Find** the relevant API class in `apis/`
3. **Identify** the root cause (check logs via `logcat`)
4. **Fix** the code — keep the change minimal and focused
5. **Test** that the fix works and doesn't break other APIs
6. **Submit** a PR with a clear description of the bug and fix

---

## Submitting Changes

1. **Push** your branch to your fork:
   ```bash
   git push origin feature/my-new-api
   ```
2. **Open a Pull Request** against the upstream `master` branch
3. **Fill out the PR template** with:
   - What you changed and why
   - How you tested it
   - Any breaking changes or deprecations
4. **Respond** to review feedback promptly

### PR Checklist

- [ ] Code compiles without errors
- [ ] Code follows the existing style
- [ ] Tested on a real device
- [ ] Permissions handled correctly
- [ ] Error cases are handled
- [ ] Documentation updated (README, comments)

---

## Commit Message Guidelines

Use clear, descriptive commit messages:

```
Add: new MyNewAPI endpoint for accessing XYZ feature

Fix: crash in TorchAPI when camera is in use by another app

Refactor: simplify permission checks in TermuxApiReceiver

Docs: update README with new API examples
```

Prefix with:
- `Add:` — new feature or API
- `Fix:` — bug fix
- `Refactor:` — code restructuring, no behavior change
- `Docs:` — documentation only
- `Test:` — adding or updating tests

---

## Useful Resources

- [Termux Wiki](https://wiki.termux.com/)
- [Termux App Repository](https://github.com/termux/termux-app)
- [Termux:API Package (client scripts)](https://github.com/termux/termux-api-package)
- [Termux Libraries (termux-shared)](https://github.com/termux/termux-app/wiki/Termux-Libraries)
- [Android BroadcastReceiver Docs](https://developer.android.com/reference/android/content/BroadcastReceiver)
- [Android LocalSocket Docs](https://developer.android.com/reference/android/net/LocalSocket)
- [Gitter Chat](https://gitter.im/termux/termux)
