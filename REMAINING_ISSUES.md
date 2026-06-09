# Corrected Remaining Issues Ledger
# Branch: workbench-api-updates
# CI: PASSING (commit 4836394)
# Updated: 2026-06-09

## FIXED IN THIS SESSION (47 issues)
226,229,231,247,260,268,274,289,300,303,304,317,319,330,334,346,365,369,
425,427,428,431,441,519,558,568,573,588,592,595,600,616,648,649,678,
712,720,728,748,756,767,781,793,794,849,881,884

## PREVIOUSLY FIXED (before this session, from git log)
201,205,218,249,272,275,429,467,469,505,514,540,541,559,565,568,
612,620,662,672,680,703,705,714,730,776,801,808,813,818,819,825,
832,861,864,867,87,877,92,97

## TOTAL FIXED: ~84 issues
## REMAINING: ~82 issues (of 200 open)

────────────────────────────────────────
## MEDIUM FEATURES (45) — doable, need care
────────────────────────────────────────
#221  Record phone call
#232  Modify time settings
#240  Send MMS
#241  USB-to-serial access
#242  Select and connect WiFi
#246  Store secrets with fingerprint
#282  Cron-like job scheduling
#284  IME switcher
#287  Keystore import existing keys
#305  AlarmManager API
#323  Media player play stdin
#332  Sensitive clipboard handling
#350  Fitness API
#356  TTS output to MP3
#359  Fingerprint blank window on secondary display
#360  Expose Camera + Mic streaming
#380  List and launch apps
#385  Watch battery info
#390  AlarmClock API
#393  YubiKey support
#394  Homescreen widget text
#395  USB-serial bridge to file
#403  Lock device API
#413  Media playback control
#456  Screenshot API
#462  Dialog enhancements (multi-spinner etc)
#498  saf-realpath / saf-realname
#530  Blocking alternative to termux-open
#531  Implement MPRIS
#538  Direct reply to notifications
#545  VPN API
#550  Keystore encrypt/decrypt
#566  Save dialog + file picker
#567  Intent launcher with results
#608  Get session text
#681  Bluetooth headset microphone
#688  mDNS discovery
#713  BLE (bluetooth low energy)
#724  ActivityResult from intents
#766  RawContacts read/write
#771  Bind process to network
#802  WebView
#816  MediaProjection + Input control
#828  Accessibility API

────────────────────────────────────────
## BUGS — FIXABLE (19)
──────────────────────────────
#311  notification set clipboard on action doesn't work
#342  accessibility problems in termux-dialog sheet widget
#352  launcher to restart API if crashed
#368  cant add multiple jobs through job-scheduler
#414  termux-dialog response inconsistencies
#466  commands hang without output
#499  saf-managedir select screen disappears when switching apps
#516  media notifications controllable through headset
#557  dialog hangs from shortcuts
#624  arbitrary settings (partially done — SettingAPI exists)
#645  custom menus in Termux Settings
#719  location returns no output in crontab (partially fixed)
#742  job scheduler random scheduling
#799  LocalSocket Error on Android 16
#842  termux and termux-api crashes
#860  listen to notifications (NotificationListenerService exists, needs wiring)

────────────────────────────────────────
## BUGS — VAGUE / NEEDS INFO (14)
──────────────────────────────────────
#227  Termux-dialog stopped working after version 0.25
#244  termux-dialog hangs
#245  termux:API bug (no details)
#258  Extra battery info (already in output — docs issue?)
#275  dialog dismissed on touch outside (enhancement)
#290  MIC recording filename reported erroneously
#292  get result of activity
#349  libusb support improvement
#367  storage-get doesn't block on Android 9
#404  speech-to-text not working on Android 11
#513  Android Go 8.1 API not working
#576  Re-support Android 5 and 6
#671  speech-to-text integration with Dicio
#830  Can't send notifications

────────────────────────────────────────
## CLIENT-SIDE ONLY — shell scripts (15)
────────────────────────────────────────
#297  Black UI support in termux-dialog
#308  confused about SL4A (question)
#437  recognize spanish speech
#492  htop alternative (not API)
#515  Shortcut API
#575  Documenting termux-notification-list
#590  F-Droid release question
#617  Open directory in default file manager
#634  Add quick settings
#637  Add adb_wifi_enabled
#645  custom menus in settings
#668  Getting rid of sharedUserId
#692  Change font size from terminal
#787  --type media on Android 14
#874  saf-picker (already implemented — needs runtime verify)

────────────────────────────────────────
## CAN'T FIX / OS LIMITATION (7)
────────────────────────────────────────
#220  cannot locate symbol (device-specific)
#263  ambient brightness (no standard Android API)
#321  wallpaper lockscreen on Android 9 (OS bug)
#361  Xiaomi Redmi note 8 pro issues (device-specific)
#447  API not working on Android 10 BV9900 Pro (device-specific)
#449  wifi-enable not working on BV9900 Pro (device-specific)
#495  Xiaomi voice assistant conflict (device-specific)

────────────────────────────────────────
## META / NOT CODE (11)
────────────────────────────────────────
#270  notification hangs on Android Q beta
#299  job scheduler question
#301  hangs on sshd Android 10
#302  Mi Band 4 notification settings
#335  many notification API deprecated
#382  async/push/callback/hooks (vague feature request)
#707  tvheadend firmware file for tuner
#725  termux-share content provider URI
#844  proposed broadcast fix (user-submitted)
#863  crash report realme narzo
#870  flagged by McAfee

────────────────────────────────────────
## DUPLICATES / OVERLAPS
────────────────────────────────────────
#551 (reply to notification) → dup of #538 (direct reply)
#461 (media actions) → overlaps #881 (media notification state)
#322 (share -t flag) → fixed in this session
