# Next Phase Checklist
## Package/Client-Side Pairing + Small Bugs Only
## After CI passes on workbench-api-updates

### Rules:
- NO new app-side features
- NO upstream PRs
- NO touching master/main
- Small bugs only (1-2 files max per fix)
- Package wrappers for new APIs from this session

---

## Phase A: Package/Client-Side Wrappers (termux-api-package)

These new app-side APIs need shell script wrappers in the termux-api-package repo:

- [ ] `termux-wifi-rescan` → calls `WifiRescan` api_method
- [ ] `termux-setting` → calls `Setting` api_method with action=get/put/dark_mode/display_info
- [ ] `termux-calendar` → calls `Calendar` api_method with action=add/list/delete
- [ ] `termux-camera-video` → calls `CameraVideo` api_method with action=start/quit
- [ ] `termux-notification --media-state` → pass media-state extra to NotificationAPI

Already work automatically (no wrapper needed):
- Location getTime() — already in termux-location JSON output
- Telephony device_name/android_id — already in termux-telephony-deviceinfo
- SMS/calllog subscription_id — already in respective JSON outputs

---

## Phase B: Small Fixable Bugs (app-side, 1-2 files each)

Priority order (easiest first):

1. [ ] #311 — notification set clipboard on action
2. [ ] #352 — API restart launcher if crashed
3. [ ] #499 — saf-managedir select screen disappears
4. [ ] #557 — dialog hangs from shortcuts
5. [ ] #742 — job scheduler random scheduling
6. [ ] #860 — listen to notifications (wire up NotificationListenerService)

---

## Phase C: Medium Features (only after A+B are done)

High value, doable:
- [ ] #538 — direct reply to notifications
- [ ] #240 — send MMS
- [ ] #282 — cron-like job scheduling
- [ ] #462 — dialog enhancements

---

## Duplicates/Overlaps (mark as such, don't fix separately):
- #551 → dup of #538
- #461 → overlaps #881 (media-state)
- #322 → already fixed in this session

## Already Implemented (needs runtime verification):
- #874 — saf-picker (app + package both exist)
- #624 — arbitrary settings (SettingAPI exists)
