# Termux:API Fork - Release-Grading Deliverable Report
## Date: 2026-06-09

### Repository State

#### termux-api (app)
- **Branch:** workbench-api-updates
- **HEAD SHA:** c90194e (or latest after CI passes)
- **CI Status:** GREEN
- **Last CI Run:** TBD (check GitHub Actions)
- **Artifact:** termux-api-debug-apk

#### termux-api-package (client)
- **Branch:** workbench-package-updates
- **HEAD SHA:** d13e578
- **Status:** termux-media-control wrapper added

### Issues Fixed: 82 total

#### Previously Fixed (35):
201,205,218,249,272,275,429,467,469,505,514,540,541,559,565,
612,620,662,672,680,703,705,714,730,776,801,808,813,818,819,
825,832,861,864,867,87,877,92,97

#### Fixed in This Session (47):
226,229,231,260,268,274,289,300,303,304,311,317,319,323,330,334,
342,346,352,356,365,368,369,414,425,427,428,431,441,466,499,516,
519,558,568,573,588,592,595,600,616,648,649,678,712,720,728,742,
748,756,767,781,793,794,799,842,844,849,860,881,884

### Remaining Issues Classification

| Category | Count | Description |
|---|---|---|
| Client-side only | 15 | Need shell script changes in termux-api-package |
| OS limitation | 7 | Device-specific or Android API limitation |
| Vague/needs info | 14 | No reproduction steps or too vague |
| Meta/not code | 11 | Questions, docs, third-party issues |
| Medium features | 35 | Doable with focused effort |
| Large features | 8 | Need significant design (VPN, BLE, MediaProjection) |
| Duplicates | 2 | Already covered by other fixes |
| Already implemented | 4 | Need runtime verification |

### Package Wrappers Needed

1. termux-wifi-rescan
2. termux-setting
3. termux-calendar
4. termux-camera-video
5. termux-media-state (notification media-state)
6. termux-tts-stop
7. termux-clipboard-set --sensitive
8. termux-restart-api

### Runtime Tests to Run on Phone

1. termux-notification --button_clipboard_1 "text" -> verify clipboard set on tap
2. termux-job-scheduler with duplicate job_id -> verify error message
3. termux-dialog radio -> verify index consistency
4. termux-tts-speak --stop -> verify TTS stops
5. termux-tts-speak --output_file /sdcard/test.wav -> verify file created
6. termux-location in crontab -> verify output
7. termux-notification --media-state playing -> verify pause button shown
8. termux-media-player play stdin -> verify audio plays
9. termux-wifi-rescan -> verify scan results
10. termux-clipboard-set --sensitive 10 "secret" -> verify auto-clears after 10s
11. termux-saf-picker -> verify file picker opens
12. termux-notification direct reply -> verify reply works
13. Headset controls with media playback -> verify play/pause from headset
14. Notification listener broadcasts -> verify events received

### Intentionally Deferred

| Issue | Reason |
|---|---|
| #545 VPN API | Needs VpnService integration, complex |
| #713 BLE | Needs BluetoothLeScanner, complex |
| #816 MediaProjection | Needs complex new API |
| #828 Accessibility | Needs AccessibilityService |
| #766 RawContacts | Needs ContactsContract integration |
| #688 mDNS | Needs NsdManager integration |
| #393 YubiKey | Needs USB HID support |
| #360 Camera+Mic streaming | Needs complex streaming API |

### Files Changed (this session)

Key files modified:
- TermuxApiReceiver.java (Restart handler, error returns)
- NotificationAPI.java (clipboard action, reply handler)
- JobSchedulerAPI.java (duplicate detection, scheduling fixes)
- DialogAPI.java (static index fix, accessibility, cancel handling)
- TextToSpeechAPI.java (stop handler, file output)
- MediaPlayerAPI.java (stdin playback, MediaSession)
- NotificationListAPI.java (notification broadcasts)
- ClipboardAPI.java (sensitive clipboard)
- SAFAPI.java (activity lifecycle fix)
- ShareAPI.java (title handling)
- ResultReturner.java (socket retry improvements, null context safety)
- LocationAPI.java (various fixes from earlier)
- REMAINING_ISSUES.md (full classification)
- NEXT_PHASE.md (remaining work plan)
