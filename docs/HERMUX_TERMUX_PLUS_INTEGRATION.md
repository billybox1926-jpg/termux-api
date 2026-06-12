# Hermux / Termux Plus Integration Lane

This note maps what should be imported from `billybox1926-jpg/Hermux` and `billybox1926-jpg/Termux-Plus` into the Termux:API app workbench, and what should stay as package-side or Termux-side tooling.

## Source repos

| Repo | Role | Import posture |
| --- | --- | --- |
| `billybox1926-jpg/Hermux` | Termux-first field-node cockpit and operator command layer. | Do not copy wholesale into the Android app. Treat as operator UX, docs, and workflow source. |
| `billybox1926-jpg/Termux-Plus` | Bash toolkit for health checks, bus messaging, receipts, inbox processing, and node workflows. | Keep as Termux userspace tooling. Port only Android-facing primitives into Termux:API when scripts need platform access. |
| `billybox1926-jpg/termux-api-package` | Native helper and wrapper package. | Wrapper commands belong here. This is the correct home for shell entrypoints. |
| `billybox1926-jpg/termux-api` | Android API provider app. | Android framework operations, permissions, activities, services, receivers, and socket-backed API implementations belong here. |

## Why Hermux / Termux Plus work in some builds

Hermux and Termux Plus run inside Termux's Linux-like userspace. They use stable shell assumptions such as:

```text
$HOME
$PREFIX
/proc
pgrep
awk
jq
bash
python
Termux storage paths
Termux package binaries
```

That is why their health checks, process checks, workspace checks, receipts, and operator menus work without Android app code.

The Termux:API app does not run in that same environment. It is an Android app process with Android lifecycle, permissions, package identity, signing, exported component rules, and socket listener state. Code that works as a Termux shell script can fail in the app if it assumes a shell, a Termux prefix, a writable `$HOME`, or a running foreground process.

## Correct import boundary

### Keep in Hermux / Termux Plus

- Menus and cockpit UX.
- Receipts and operator logs.
- Workspace checks.
- Inbox/outbox processing.
- Shell-only health checks.
- STOP-file safety gates.
- Bash wrappers that only inspect Termux userspace.

### Keep in termux-api-package

- Public shell commands.
- Native helper targeting.
- Wrapper argument parsing.
- Side-by-side debug target selection.
- CMake options for app package, receiver component, and socket address.

### Port into Termux:API app

Only import behavior that requires Android framework access, such as:

- Android permission checks.
- App process / socket listener status.
- Battery optimization state.
- Notification listener state.
- Accessibility service state.
- Storage access framework flows.
- App package manager queries that need Android context.
- Intent-driven activities or services.
- APIs that need `Context`, `Activity`, `Service`, `BroadcastReceiver`, or Android system services.

## Proposed Android API surface

Use small API methods instead of embedding cockpit scripts in the app.

| API method | App-side purpose | Package wrapper |
| --- | --- | --- |
| `ApiStatus` | Return app package, version, process, socket address, and listener readiness. | `termux-api-status` |
| `ApiPermissions` | Return important permission/appop states in JSON. | `termux-api-permissions` |
| `ApiPrewarm` | Start or keep alive the API app process without launching UI. | `termux-api-start` / fixed debug-aware wrapper |
| `ApiDebugTarget` | Report active package identity, receiver component, and expected socket address. | `termux-api-debug-target` |
| `NodeHealthAndroid` | Return Android-side health details not available from Bash alone. | `termux-node-health-android` |

Hermux and Termux Plus can then call these wrappers and combine Android-side facts with their existing Termux-side health checks.

## Current likely failure split

When behavior works in Hermux / Termux Plus but not in the app stack, check this order:

1. **Package identity** — release target `com.termux.api` vs debug target `com.termux.api.debug`.
2. **Receiver component** — wrapper helper points to the same package installed on the device.
3. **Socket address** — helper and app agree on `com.termux.api://listen` or `com.termux.api.debug://listen`.
4. **App process** — `TermuxAPIApplication.onCreate()` has run and `SocketListener` is alive.
5. **Signing / UID relationship** — normal shared-UID assumptions do not apply to standalone debug APKs.
6. **Android exported rules** — Termux shell cannot start non-exported app components directly.
7. **Permission state** — runtime permissions and appops are granted for the app package under test.
8. **Shell assumptions** — Android app code cannot assume Termux `$HOME`, `$PREFIX`, `jq`, `bash`, or `/sdcard` access.

## Immediate work lane

Do not import every shell script into the app. Instead:

1. Add `ApiStatus` to the app.
2. Add a package wrapper that targets the same app/package/socket as the installed APK.
3. Use Hermux / Termux Plus to call that wrapper as part of their health checks.
4. Compare passing builds against failing builds using the status JSON.

The first useful proof should answer:

```text
Which package is installed?
Which package did the helper target?
Which socket address did the helper target?
Is the app process alive?
Did SocketListener start?
Did the API method return over the socket?
```

That turns the current mystery from “some builds work” into a concrete mismatch report.

## Upstream hygiene

Any import from Hermux or Termux Plus should be split into narrow changes:

1. App-side Android primitive.
2. Package wrapper.
3. Hermux / Termux Plus caller update.
4. Runtime proof from a matched app/package stack.

Avoid moving private operator workflows, receipts, device inventories, or personal logs into the Android app repository.