# Next Phase Checklist
## Remaining Actionable Work

### Phase A: Package/Client-Side Wrappers (termux-api-package repo)

These new app-side APIs need shell script wrappers:

- [ ] `termux-wifi-rescan` -> calls `WifiRescan` api_method
- [ ] `termux-setting` -> calls `Setting` api_method with action=get/put/dark_mode/display_info
- [ ] `termux-calendar` -> calls `Calendar` api_method with action=add/list/delete
- [ ] `termux-camera-video` -> calls `CameraVideo` api_method with action=start/stop
- [ ] `termux-media-state` -> pass media-state extra to NotificationAPI
- [ ] `termux-tts-stop` -> calls TextToSpeech with stop=true extra
- [ ] `termux-clipboard-set --sensitive` -> pass sensitive=true extra
- [ ] `termux-restart-api` -> calls Restart api_method

Already work automatically (no wrapper needed):
- Location getTime() -> already in termux-location JSON output
- Telephony device_name/android_id -> already in termux-telephony-deviceinfo
- SMS/calllog subscription_id -> already in respective JSON outputs
- MediaControl play/pause/next/prev -> already in termux-media-control

### Phase B: Medium Features (app-side, larger effort)

High value, doable:
- [ ] #462 Dialog enhancements (multi-spinner, date range, etc.)
- [ ] #498 saf-realpath / saf-realname
- [ ] #282 Cron-like job scheduling (AlarmManager integration)
- [ ] #305 AlarmManager API
- [ ] #380 List and launch apps

### Phase C: Large Features (need significant design)

- [ ] #531 MPRIS (MediaSession integration)
- [ ] #545 VPN API (VpnService)
- [ ] #816 MediaProjection + Input control
- [ ] #828 Accessibility API
- [ ] #713 BLE (BluetoothLeScanner)
- [ ] #766 RawContacts read/write

### Phase D: Runtime Verification Needed

- [ ] #874 termux-saf-picker (app + package both exist)
- [ ] #538 Direct reply to notifications (NotificationReply case exists)
- [ ] #356 TTS output to file (synthesizeToFile)
- [ ] #516 Media headset controls (MediaSession)
- [ ] #860 Notification listener broadcasts
