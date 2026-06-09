# Termux:API Architecture

This document describes the internal architecture of the Termux:API Android app — how it communicates with Termux, how requests are routed, and how results are returned.

---

## Table of Contents

- [Overview](#overview)
- [Communication Channels](#communication-channels)
  - [Broadcast + Socket Pair (Primary)](#broadcast--socket-pair-primary)
  - [Socket Listener (Alternative)](#socket-listener-alternative)
- [Request Lifecycle](#request-lifecycle)
- [Request Routing](#request-routing)
- [Result Return System](#result-return-system)
  - [ResultWriter Types](#resultwriter-types)
  - [Threading Model](#threading-model)
- [Permission Handling](#permission-handling)
- [Background Execution](#background-execution)
- [Data Formats](#data-formats)
- [Security Model](#security-model)

---

## Overview

Termux:API is an Android app that runs as a separate APK from the main Termux app. It acts as a privileged intermediary — it holds the Android permissions needed to access device hardware and OS features, and exposes those capabilities to Termux through IPC (inter-process communication).

```
┌─────────────────────────────────────────────────────────────┐
│  Termux App (com.termux)                                    │
│                                                             │
│  Shell script / termux-api CLI                              │
│      │                                                      │
│      │  Unix domain sockets (input + output)                │
│      │  am broadcast Intent                                 │
│      ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Termux:API App (com.termux.api)                     │   │
│  │                                                       │   │
│  │  TermuxApiReceiver                                    │   │
│  │      │                                                │   │
│  │      ▼                                                │   │
│  │  API Class (e.g., TorchAPI)                           │   │
│  │      │                                                │   │
│  │      ▼                                                │   │
│  │  ResultReturner ──► output socket ──► CLI stdout      │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## Communication Channels

### Broadcast + Socket Pair (Primary)

This is the standard communication method used by all client scripts.

1. The `termux-api` CLI binary (written in C, in the `termux-api-package` repo) creates two Unix domain sockets.
2. It constructs an Android `am broadcast` command with the socket addresses as extras:
   ```
   am broadcast com.termux.api/.TermuxApiReceiver \
     --es api_method "Torch" \
     --es socket_input "/data/data/com.termux/files/usr/tmp/termux-api.XXXXXX/0" \
     --es socket_output "/data/data/com.termux/files/usr/tmp/termux-api.XXXXXX/1"
   ```
3. The Android system delivers the broadcast to `TermuxApiReceiver.onReceive()`.
4. The receiver routes to the correct API class based on the `api_method` extra.
5. The API class uses `ResultReturner` to write results to the output socket.
6. The CLI binary reads the output socket and prints the result to stdout.

**Why sockets?** Android broadcasts have a size limit on Intent extras (~1MB). Sockets allow arbitrary-size data transfer in both directions.

### Socket Listener (Alternative)

`SocketListener.java` provides an alternative communication path using a `LocalServerSocket`. Instead of using `am broadcast`, a client can connect directly to the socket at `com.termux.api://listen`. The listener:

1. Accepts incoming connections
2. Verifies the peer UID matches the Termux app (security check)
3. Parses the command line from the socket
4. Constructs and sends an ordered broadcast to `TermuxApiReceiver`
5. Sends a null byte acknowledgment back to the client

This path is used for more complex interactions where the full `am broadcast` command line would be unwieldy.

---

## Request Lifecycle

Here is the complete lifecycle of a single API request:

```
1. User runs: termux-torch on
2. termux-api CLI binary:
   a. Creates temp directory with two socket files
   b. Executes am broadcast with socket paths as extras
3. Android system delivers broadcast to TermuxApiReceiver
4. TermuxApiReceiver.onReceive():
   a. Sets up logging
   b. Calls doWork()
5. doWork():
   a. Reads "api_method" extra ("Torch")
   b. Checks permissions if needed
   c. Routes to TorchAPI.onReceive()
6. TorchAPI:
   a. Gets CameraManager
   b. Sets torch mode on/off
   c. Uses ResultReturner to write JSON result
7. ResultReturner:
   a. Connects to output socket
   b. Writes JSON data
   c. Closes socket
   d. Calls asyncResult.finish()
8. termux-api CLI:
   a. Reads result from socket
   b. Prints to stdout
   c. Exits
```

---

## Request Routing

All requests flow through `TermuxApiReceiver.doWork()`, which uses a `switch` statement on the `api_method` Intent extra:

```java
String apiMethod = intent.getStringExtra("api_method");
switch (apiMethod) {
    case "Torch":
        TorchAPI.onReceive(this, context, intent);
        break;
    case "BatteryStatus":
        BatteryStatusAPI.onReceive(this, context, intent);
        break;
    // ... 40+ more cases
    default:
        Logger.logError(LOG_TAG, "Unrecognized api_method: " + apiMethod);
}
```

Each API class has a static `onReceive()` method that follows a consistent signature:

```java
static void onReceive(TermuxApiReceiver receiver, Context context, Intent intent)
```

Some APIs require input data from the client. Their `onReceive()` method reads from the input socket:

```java
static void onReceive(TermuxApiReceiver receiver, Context context, Intent intent) {
    ResultReturner.returnData(receiver, intent, new ResultReturner.WithStringInput() {
        @Override
        public void writeResult(PrintWriter out) throws Exception {
            // inputString contains data from the client
            String data = inputString;
            // process and write result...
        }
    });
}
```

---

## Result Return System

`ResultReturner` is the central mechanism for sending data back to the client. It handles:

- Socket connection management
- Threading (spawning worker threads for non-IntentService contexts)
- Error handling and logging
- Broadcast lifecycle management (`goAsync()` / `finish()`)

### ResultWriter Types

| Base Class | Use Case | Method to Override |
|------------|----------|-------------------|
| `ResultWriter` | Simple text output | `writeResult(PrintWriter out)` |
| `ResultJsonWriter` | JSON output (most common) | `writeJson(JsonWriter out)` |
| `WithInput` | Read data from input socket | `writeResult(PrintWriter out)` after `setInput()` |
| `WithStringInput` | Read a string from input socket | `writeResult(PrintWriter out)`, access `inputString` |
| `BinaryOutput` | Write raw binary data | `writeResult(OutputStream out)` |
| `WithAncillaryFd` | Send file descriptors over socket | `sendFd(PrintWriter out, int fd)` |

### Threading Model

`ResultReturner.returnData()` automatically decides whether to run synchronously or in a new thread:

- If the context is an `IntentService`, it runs synchronously (the service already provides a worker thread).
- Otherwise, it spawns a new `Thread` to avoid blocking the BroadcastReceiver's main thread.

This is determined by `shouldRunThreadForResultRunnable()`:

```java
public static boolean shouldRunThreadForResultRunnable(Object context) {
    return !(context instanceof IntentService);
}
```

---

## Permission Handling

Android requires runtime permissions for sensitive operations. Termux:API handles this through `TermuxApiPermissionActivity`:

```java
if (TermuxApiPermissionActivity.checkAndRequestPermissions(context, intent, Manifest.permission.CAMERA)) {
    CameraPhotoAPI.onReceive(this, context, intent);
}
```

The pattern is:
1. Check if the permission is granted
2. If not, launch `TermuxApiPermissionActivity` to request it
3. The API call is deferred — when the user grants permission, the operation proceeds

Some APIs require special permissions that can't be requested at runtime:
- **WRITE_SETTINGS** (BrightnessAPI): User must manually enable this in system settings
- **Notification Access** (NotificationListAPI): User must enable in notification listener settings

---

## Background Execution

For long-running operations (e.g., sensor monitoring, audio recording), the app uses `KeepAliveService` — a foreground service that prevents Android from killing the process. It shows a persistent notification while active.

`JobSchedulerAPI` allows scheduling background work that survives app restarts, using Android's `JobScheduler` framework.

---

## Data Formats

### JSON (Default)

Most APIs return JSON via `ResultJsonWriter`:

```json
{
  "result": "success",
  "data": {
    "level": 85,
    "status": "Charging"
  }
}
```

### Plain Text

Simple APIs may return plain text via `ResultWriter`:

```
Camera: 0
  Facing: back
  Orientation: 90
```

### Binary

Some APIs return raw binary data via `BinaryOutput`:
- `CameraPhotoAPI` — JPEG image bytes
- `MicRecorderAPI` — audio recording bytes

---

## Security Model

Termux:API enforces several security measures:

1. **Shared UID / Signing Key:** The API app must be signed with the same key as the main Termux app. This is an Android system requirement for certain permissions.

2. **UID Verification (SocketListener):** The socket listener verifies that incoming connections come from the Termux app's UID:
   ```java
   if (con.getPeerCredentials().getUid() != app.getApplicationInfo().uid) {
       continue; // reject connection
   }
   ```

3. **Filesystem Path Validation:** Socket paths must be under the Termux app's data directory:
   ```java
   if (!FileUtils.isPathInDirPaths(socketAddress, termuxAppDataDirectories, true)) {
       throw new RuntimeException("Socket address not under Termux data directories");
   }
   ```

4. **Runtime Permissions:** All dangerous permissions are requested at runtime, following Android best practices.

5. **No Exported Activities:** The app's activities are not exported, preventing other apps from launching them directly.
