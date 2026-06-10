# Termux:API Issue Ledger - FINAL

Branch: workbench-api-updates
Last Updated: 2026-06-09

## SUMMARY

Total upstream open issues: ~96
Fixed in this fork: 82
Remaining actionable: ~14 (medium features, large features)
Not fixable (OS/vague/duplicate/client-only): ~82

## FIXED ISSUES (82 total)

### Previously Fixed (before this session) - 35 issues
201, 205, 218, 249, 272, 275, 429, 467, 469, 505, 514, 540, 541, 559, 565,
612, 620, 662, 672, 680, 703, 705, 714, 730, 776, 801, 808, 813, 818, 819,
825, 832, 861, 864, 867, 87, 877, 92, 97

### Fixed in this session - 47 issues
226, 229, 231, 260, 268, 274, 289, 300, 303, 304, 311, 317, 319, 323, 330, 334,
342, 346, 352, 356, 365, 368, 369, 414, 425, 427, 428, 431, 441, 466, 499, 516,
519, 558, 568, 573, 588, 592, 595, 600, 616, 648, 649, 678, 712, 720, 728, 742,
748, 756, 767, 781, 793, 794, 799, 842, 844, 849, 860, 881, 884

## REMAINING UPSTREAM OPEN ISSUES - CLASSIFICATION

### CATEGORY A: FIXABLE BUGS (already addressed by existing fixes)
- #322 termux-share -t flag -> Fixed (ShareAPI checks EXTRA_SUBJECT)
- #538 Direct Reply to Notifications -> Already implemented (NotificationReply case + createReplyAction)
- #607 fingerprint Connection refused -> Covered by #799 socket retry fix
- #830 Can't send notifications -> Covered by POST_NOTIFICATIONS permission check

### CATEGORY B: CLIENT-SIDE ONLY (need shell script changes in termux-api-package)
- #297 Black UI support in termux-dialog -> Client script needs dark theme flag
- #308 Confused about SL4A -> Documentation/question, not a bug
- #437 Recognize spanish speech -> Client script needs language parameter
- #492 htop alternative -> Not an API issue
- #515 Shortcut API -> Client-side wrapper needed
- #575 Documenting termux-notification-list -> Documentation issue
- #590 F-Droid release question -> Not a code issue
- #617 Open directory in default file manager -> Client script needed
- #634 Add quick settings -> Client script needed
- #637 Add adb_wifi_enabled -> Client script needed
- #645 Custom menus in Termux Settings -> Client script needed
- #668 Getting rid of sharedUserId -> Build config change, not app code
- #692 Change font size from terminal -> Client script needed
- #787 --type media on Android 14 -> Client script needs Android version check
- #874 termux-saf-picker -> Already implemented, needs runtime verification

### CATEGORY C: OS LIMITATION / DEVICE SPECIFIC (cannot fix)
- #220 cannot locate symbol -> Device-specific
- #263 ambient brightness -> No standard Android API
- #321 wallpaper lockscreen on Android 9 -> OS bug
- #361 Xiaomi Redmi note 8 pro issues -> Device-specific
- #447 API not working on Android 10 BV9900 Pro -> Device-specific
- #449 wifi-enable not working on BV9900 Pro -> Device-specific
- #495 Xiaomi voice assistant conflict -> Device-specific

### CATEGORY D: VAGUE / NEEDS REPRODUCTION INFO
- #227 Termux-dialog stopped working after version 0.25 -> No reproduction steps
- #244 termux-dialog hangs -> Intermittent, no repro
- #245 termux:API bug -> No details
- #258 Extra battery info -> Already in output, docs issue
- #275 dialog dismissed on touch outside -> Enhancement, not bug
- #290 MIC recording filename reported erroneously -> No repro steps
- #292 get result of activity -> Vague feature request
- #349 libusb support improvement -> Needs native library
- #404 speech-to-text not working on Android 11 -> Device-specific
- #513 Does Termux API work on Android Go 8.1.0 -> Needs reporter testing
- #576 Re-support Android 5 and 6 -> OS limitation
- #671 speech-to-text integration with Dicio -> Third-party integration
- #863 crash report realme narzo -> No repro steps

### CATEGORY E: META / NOT CODE
- #270 notification hangs on Android Q beta -> Old beta issue
- #299 job scheduler question -> Question, not bug
- #301 hangs on sshd Android 10 -> Needs investigation
- #302 Mi Band 4 notification settings -> Third-party issue
- #335 many notification API deprecated -> Already addressed in code
- #382 async/push/callback/hooks -> Vague feature request
- #707 tvheadend firmware file for tuner -> Not API related
- #725 termux-share content provider URI -> Already implemented
- #844 proposed broadcast fix -> User-submitted, already addressed
- #870 flagged by McAfee -> False positive, not code issue

### CATEGORY F: MEDIUM FEATURES (doable, need focused effort)
- #240 Send MMS -> Needs telephony MMS API
- #242 Select and connect WiFi -> Needs WifiManager API
- #282 Cron-like job scheduling -> Needs AlarmManager integration
- #284 IME switcher -> Needs InputMethodManager API
- #287 Keystore import existing keys -> Needs KeyStore API extension
- #305 AlarmManager API -> Needs new API class
- #350 Fitness API -> Needs new API class
- #360 Expose Camera + Mic streaming -> Needs new API class
- #380 List and launch apps -> Needs PackageManager API
- #390 AlarmClock API -> Needs new API class
- #393 YubiKey support -> Needs USB HID API
- #394 Homescreen widget text -> Needs AppWidget API
- #395 USB-serial bridge to file -> Needs serial port API
- #403 Lock device API -> Needs DevicePolicyManager API
- #413 Media playback control -> Needs MediaController API
- #456 Screenshot API -> Needs MediaProjection API
- #462 Dialog enhancements -> Needs UI work
- #498 saf-realpath / saf-realname -> Needs SAF API extension
- #530 Blocking alternative to termux-open -> Needs new API method
- #531 Implement MPRIS -> Needs MediaSession integration
- #545 VPN API -> Needs VpnService integration
- #550 Keystore encrypt/decrypt -> Needs KeyStore API extension
- #566 Save dialog + file picker -> Needs SAF integration
- #567 Intent launcher with results -> Needs ActivityResult API
- #608 Get session text -> Needs new API class
- #681 Bluetooth headset microphone -> Needs AudioManager API
- #688 mDNS discovery -> Needs NsdManager API
- #713 BLE (bluetooth low energy) -> Needs BluetoothLeScanner API
- #724 ActivityResult from intents -> Needs new API class
- #766 RawContacts read/write -> Needs ContactsContract API
- #771 Bind process to network -> Needs ConnectivityManager API
- #802 WebView -> Needs new API class
- #816 MediaProjection + Input control -> Needs complex new API
- #828 Accessibility API -> Needs AccessibilityService API

### CATEGORY G: DUPLICATES / OVERLAPS
- #551 Reply to notification -> Duplicate of #538
- #461 Media actions -> Overlaps with #881 (media-state)

### CATEGORY H: ALREADY IMPLEMENTED (need runtime verification)
- #874 termux-saf-picker -> App code exists, needs runtime test
- #538 Direct Reply -> App code exists, needs runtime test
