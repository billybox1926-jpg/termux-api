# Termux:API Reference

Complete reference for all available API endpoints, their parameters, return values, and required permissions.

---

## Table of Contents

- [Audio](#audio)
- [Battery Status](#battery-status)
- [Brightness](#brightness)
- [Call Log](#call-log)
- [Camera Info](#camera-info)
- [Camera Photo](#camera-photo)
- [Clipboard](#clipboard)
- [Contact List](#contact-list)
- [Dialog](#dialog)
- [Download](#download)
- [Fingerprint](#fingerprint)
- [Infrared](#infrared)
- [Job Scheduler](#job-scheduler)
- [Keystore](#keystore)
- [Location](#location)
- [Media Player](#media-player)
- [Media Scanner](#media-scanner)
- [Mic Recorder](#mic-recorder)
- [NFC](#nfc)
- [Notification](#notification)
- [Notification List](#notification-list)
- [Notification Remove](#notification-remove)
- [SAF (Storage Access Framework)](#saf-storage-access-framework)
- [Sensor](#sensor)
- [Share](#share)
- [SMS Inbox](#sms-inbox)
- [SMS Send](#sms-send)
- [Speech to Text](#speech-to-text)
- [Storage Get](#storage-get)
- [Telephony](#telephony)
- [Text to Speech](#text-to-speech)
- [Toast](#toast)
- [Torch](#torch)
- [USB](#usb)
- [Vibrate](#vibrate)
- [Volume](#volume)
- [Wallpaper](#wallpaper)
- [WiFi](#wifi)

---

## Audio

**API Method:** `AudioInfo`

Get audio information from the device.

**Permissions:** None

**Example:**
```bash
termux-audio
```

---

## Battery Status

**API Method:** `BatteryStatus`

Returns detailed battery information as JSON.

**Permissions:** None

**Returns:**

| Field | Type | Description |
|-------|------|-------------|
| `present` | boolean | Whether a battery is present |
| `technology` | string | Battery technology (e.g., "Li-ion") |
| `health` | string | `GOOD`, `DEAD`, `COLD`, `OVERHEAT`, `OVER_VOLTAGE`, `UNKNOWN`, `UNSPECIFIED_FAILURE` |
| `plugged` | string | `UNPLUGGED`, `PLUGGED_AC`, `PLUGGED_USB`, `PLUGGED_WIRELESS`, `PLUGGED_DOCK` |
| `status` | string | `CHARGING`, `DISCHARGING`, `FULL`, `NOT_CHARGING`, `UNKNOWN` |
| `temperature` | number | Battery temperature in Celsius (1 decimal place) |
| `voltage` | integer | Battery voltage in mV |
| `current` | integer | Instantaneous current in microamperes |
| `current_average` | integer | Average current in microamperes |
| `percentage` | integer | Battery percentage |
| `level` | integer | Raw battery level |
| `scale` | integer | Raw battery scale |
| `charge_counter` | integer | Battery charge in microampere-hours |
| `energy` | long | Battery energy in nanowatt-hours |
| `cycle` | integer | Charge cycle count (Android 14+) |

**Example:**
```bash
termux-battery-status
```

**Sample Output:**
```json
{
  "present": true,
  "technology": "Li-ion",
  "health": "GOOD",
  "plugged": "PLUGGED_AC",
  "status": "CHARGING",
  "temperature": 32.5,
  "voltage": 4200,
  "current": 150000,
  "current_average": 120000,
  "percentage": 85,
  "level": 85,
  "scale": 100,
  "charge_counter": 3500000,
  "energy": 15000000000,
  "cycle": 42
}
```

---

## Brightness

**API Method:** `Brightness`

Set the screen brightness.

**Permissions:** `WRITE_SETTINGS` (must be granted manually in system settings)

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `brightness` | integer | Brightness value (0-255) |

**Example:**
```bash
termux-brightness 128
```

---

## Call Log

**API Method:** `CallLog`

Read the device call history.

**Permissions:** `READ_CALL_LOG`

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit` | integer | 50 | Maximum number of entries |
| `offset` | integer | 0 | Offset for pagination |

**Example:**
```bash
termux-call-log -l 10
```

---

## Camera Info

**API Method:** `CameraInfo`

Get information about device cameras.

**Permissions:** None

**Example:**
```bash
termux-camera-info
```

**Sample Output:**
```
Camera: 0
  Facing: back
  Orientation: 90
Camera: 1
  Facing: front
  Orientation: 270
```

---

## Camera Photo

**API Method:** `CameraPhoto`

Take a photo and save it to a file.

**Permissions:** `CAMERA`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `camera` | integer | Camera ID (0 = back, 1 = front) |
| `file` | string | Output file path |

**Example:**
```bash
termux-camera-photo -c 0 /sdcard/photo.jpg
```

---

## Clipboard

**API Method:** `Clipboard`

Get or set the clipboard contents.

**Permissions:** None

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `text` | string | Text to set (omit to get current clipboard) |

**Example:**
```bash
# Get clipboard
termux-clipboard-get

# Set clipboard
termux-clipboard-set "Hello, World!"
```

---

## Contact List

**API Method:** `ContactList`

List all contacts on the device.

**Permissions:** `READ_CONTACTS`

**Example:**
```bash
termux-contact-list
```

---

## Dialog

**API Method:** `Dialog`

Show an input dialog to the user.

**Permissions:** None

**Dialog Types:**

| Type | Description |
|------|-------------|
| `text` | Text input field |
| `confirm` | Yes/No confirmation |
| `checkbox` | Multiple selection |
| `radio` | Single selection from options |
| `sheet` | Bottom sheet selection |
| `time` | Time picker |
| `date` | Date picker |
| `speech` | Voice input |

**Example:**
```bash
termux-dialog text -t "Enter your name" -i "Default value"
termux-dialog confirm -t "Are you sure?"
termux-dialog checkbox -t "Pick items" -v "Option 1,Option 2,Option 3"
```

---

## Download

**API Method:** `Download`

Download a file using Android's download manager.

**Permissions:** None

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `url` | string | URL to download |
| `title` | string | Download notification title |
| `description` | string | Download notification description |
| `path` | string | Destination file path |

**Example:**
```bash
termux-download -t "My File" -d "Downloading..." -p /sdcard/file.zip https://example.com/file.zip
```

---

## Fingerprint

**API Method:** `Fingerprint`

Use fingerprint authentication.

**Permissions:** None (uses system fingerprint dialog)

**Example:**
```bash
termux-fingerprint
```

---

## Infrared

**API Methods:** `InfraredFrequencies`, `InfraredTransmit`

Send infrared signals.

**Permissions:** `TRANSMIT_IR`

**Example:**
```bash
# Get supported carrier frequencies
termux-infrared-frequencies

# Transmit IR pattern
termux-infrared-transmit -f 38000 -p "100,200,100,200"
```

---

## Job Scheduler

**API Method:** `JobScheduler`

Schedule background jobs.

**Permissions:** None

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `script` | string | Script to execute |
| `period_ms` | integer | Period in milliseconds |
| `persisted` | boolean | Survive reboot |
| `battery_not_low` | boolean | Only run when battery is not low |
| `network` | string | Network type required (`any`, `unmetered`) |

**Example:**
```bash
termux-job-scheduler -s /sdcard/myscript.sh --period-ms 3600000 --persisted
```

---

## Keystore

**API Method:** `Keystore`

Access the Android Keystore for cryptographic operations.

**Permissions:** None

**Operations:**

| Operation | Description |
|-----------|-------------|
| `generate` | Generate a new key |
| `delete` | Delete a key |
| `list` | List stored keys |
| `sign` | Sign data |
| `verify` | Verify a signature |

**Example:**
```bash
termux-keystore generate -a my_key -s 256
termux-keystore sign -a my_key -d "data to sign"
```

---

## Location

**API Method:** `Location`

Get the device location.

**Permissions:** `ACCESS_FINE_LOCATION`

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `provider` | string | `gps` | Location provider (`gps`, `network`, `passive`) |
| `request` | string | `once` | Request type (`once`, `last`, `updates`) |

**Example:**
```bash
termux-location
termux-location -p network
termux-location -r last
```

**Sample Output:**
```json
{
  "latitude": 37.7749,
  "longitude": -122.4194,
  "altitude": 10.5,
  "accuracy": 5.0,
  "bearing": 45.0,
  "speed": 1.2,
  "elapsedMs": 100,
  "provider": "gps"
}
```

---

## Media Player

**API Method:** `MediaPlayer`

Control media playback.

**Permissions:** None

**Commands:**

| Command | Description |
|---------|-------------|
| `play` | Start playback |
| `pause` | Pause playback |
| `stop` | Stop playback |
| `next` | Skip to next track |
| `prev` | Skip to previous track |
| `seek` | Seek to position |

**Example:**
```bash
termux-media-player play /sdcard/music.mp3
termux-media-player pause
```

---

## Media Scanner

**API Method:** `MediaScanner`

Trigger the Android media scanner on files.

**Permissions:** None

**Example:**
```bash
termux-media-scan /sdcard/new_photo.jpg
```

---

## Mic Recorder

**API Method:** `MicRecorder`

Record audio from the microphone.

**Permissions:** `RECORD_AUDIO`

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `file` | string | auto | Output file path |
| `limit` | integer | 0 (unlimited) | Recording limit in seconds |
| `encoder` | string | `aac` | Audio encoder |
| `bitrate` | integer | 128000 | Bitrate in bps |
| `samplerate` | integer | 44100 | Sample rate in Hz |

**Example:**
```bash
termux-mic-record /sdcard/recording.m4a
termux-mic-record -l 10 /sdcard/short_recording.m4a
```

---

## NFC

**API Method:** `Nfc`

Read and write NFC tags.

**Permissions:** None (NFC is handled at the system level)

**Example:**
```bash
termux-nfc
```

---

## Notification

**API Method:** `Notification`, `NotificationChannel`, `NotificationReply`

Show rich notifications.

**Permissions:** None

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `title` | string | Notification title |
| `content` | string | Notification body text |
| `id` | string | Unique notification ID |
| `priority` | string | `min`, `low`, `default`, `high`, `max` |
| `led-color` | string | LED color (hex, e.g., `FF0000`) |
| `led-on` | integer | LED on time (ms) |
| `led-off` | integer | LED off time (ms) |
| `vibrate` | string | Vibration pattern (comma-separated ms) |
| `sound` | boolean | Play notification sound |
| `ongoing` | boolean | Make notification persistent |
| `alert-once` | boolean | Only alert on first show |
| `action` | string | Command to run on tap |
| `button_text_1` | string | Text for action button 1 |
| `button_action_1` | string | Command for action button 1 |
| `image-path` | string | Path to large image |
| `icon` | string | Small icon name |
| `group` | string | Notification group key |
| `channel` | string | Notification channel ID |
| `type` | string | `media` for media-style notification |

**Example:**
```bash
termux-notification --title "Hello" --content "From Termux!" --id 1
termux-notification --title "Alert" --content "Important!" --priority high --sound true
termux-notification --title "Download" --content "Complete" --action "termux-toast Done!"
```

---

## Notification List

**API Method:** `NotificationList`

List active notifications.

**Permissions:** Notification Access (must be enabled in system settings)

**Example:**
```bash
termux-notification-list
```

---

## Notification Remove

**API Method:** `NotificationRemove`

Remove a notification by ID.

**Permissions:** None

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | string | Notification ID to remove |

**Example:**
```bash
termux-notification-remove --id 1
```

---

## SAF (Storage Access Framework)

**API Method:** `SAF`

Access files through Android's Storage Access Framework.

**Permissions:** None

**Example:**
```bash
termux-saf-ls /sdcard
termux-saf-cat /sdcard/Documents/file.txt
termux-saf-touch /sdcard/newfile.txt
```

---

## Sensor

**API Method:** `Sensor`

Read device sensors.

**Permissions:** None

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `sensors` | string | all | Comma-separated sensor names |
| `all` | boolean | false | Listen to all sensors |
| `delay` | integer | 1000 | Delay between readings (ms) |
| `limit` | integer | unlimited | Number of readings before stopping |

**Commands:**

| Command | Description |
|---------|-------------|
| `list` | List all available sensors |
| `sensors` | Start listening to sensors |
| `cleanup` | Stop listening and clean up |

**Example:**
```bash
# List all sensors
termux-sensor -c list

# Read accelerometer 5 times
termux-sensor -s accelerometer -n 5

# Read all sensors continuously
termux-sensor -s all -d 500
```

**Sample Output:**
```json
{
  "BMI160 accelerometer": {
    "values": [0.12, 0.45, 9.81]
  }
}
```

---

## Share

**API Method:** `Share`

Share files or text.

**Permissions:** None

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `title` | string | Share dialog title |
| `content` | string | Text content to share |
| `file` | string | File path to share |
| `action` | string | `send`, `view`, `edit` |

**Example:**
```bash
termux-share -t "Check this out" /sdcard/photo.jpg
termux-share -a send -c "Hello from Termux!"
```

---

## SMS Inbox

**API Method:** `SmsInbox`

Read SMS messages from the inbox.

**Permissions:** `READ_SMS`, `READ_CONTACTS`

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit` | integer | 10 | Maximum messages |
| `offset` | integer | 0 | Offset for pagination |
| `type` | string | `inbox` | Folder type |

**Example:**
```bash
termux-sms-inbox -l 5
```

---

## SMS Send

**API Method:** `SmsSend`

Send an SMS message.

**Permissions:** `SEND_SMS`, `READ_PHONE_STATE`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `number` | string | Recipient phone number |
| `text` | string | Message body |

**Example:**
```bash
termux-sms-send -n +123****7890 "Hello from Termux"
```

---

## Speech to Text

**API Method:** `SpeechToText`

Convert speech to text using the microphone.

**Permissions:** `RECORD_AUDIO`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `language` | string | Language code (e.g., `en-US`) |
| `prompt` | string | Prompt to display |
| `max_results` | integer | Maximum results |

**Example:**
```bash
termux-speech-to-text
```

---

## Storage Get

**API Method:** `StorageGet`

Pick a file from storage using the system file picker.

**Permissions:** None

**Example:**
```bash
termux-storage-get
```

---

## Telephony

**API Methods:** `TelephonyCall`, `TelephonyCellInfo`, `TelephonyDeviceInfo`

Access telephony features.

**Permissions:**

| Method | Permission |
|--------|------------|
| `TelephonyCall` | `CALL_PHONE` |
| `TelephonyCellInfo` | `ACCESS_COARSE_LOCATION` |
| `TelephonyDeviceInfo` | `READ_PHONE_STATE` |

**Example:**
```bash
# Make a phone call
termux-telephony-call -n +123****7890

# Get cell tower info
termux-telephony-cellinfo

# Get device info (IMEI, etc.)
termux-telephony-deviceinfo
```

---

## Text to Speech

**API Method:** `TextToSpeech`

Convert text to speech.

**Permissions:** None

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `text` | string | (required) | Text to speak |
| `engine` | string | default | TTS engine |
| `language` | string | en | Language code |
| `pitch` | number | 1.0 | Voice pitch (0.5-2.0) |
| `rate` | number | 1.0 | Speech rate (0.5-2.0) |
| `stream` | string | music | Audio stream |
| `queue` | boolean | false | Queue mode |

**Example:**
```bash
termux-tts-speak "Hello, World!"
termux-tts-speak -l es "Hola, Mundo!"
```

---

## Toast

**API Method:** `Toast`

Show a toast message.

**Permissions:** None

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `text` | string | (required) | Toast text |
| `background` | string | `grey` | Background color |
| `color` | string | `white` | Text color |
| `position` | string | `middle` | `top`, `middle`, `bottom` |
| `short` | boolean | false | Short duration |

**Example:**
```bash
termux-toast "Hello, World!"
termux-toast -s "Quick message"
termux-toast -b blue -c yellow "Colored toast"
```

---

## Torch

**API Method:** `Torch`

Toggle the flashlight.

**Permissions:** None (CameraManager handles this internally)

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `enabled` | boolean | `true` = on, `false` = off |

**Example:**
```bash
termux-torch on
termux-torch off
```

---

## USB

**API Method:** `Usb`

Interact with USB devices.

**Permissions:** None

**Example:**
```bash
termux-usb list
```

---

## Vibrate

**API Method:** `Vibrate`

Vibrate the device.

**Permissions:** `VIBRATE`

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `duration` | integer | 1000 | Duration in milliseconds |
| `pattern` | string | (none) | Vibration pattern (comma-separated ms) |

**Example:**
```bash
termux-vibrate -d 1000
termux-vibrate -p "100,200,100,200"
```

---

## Volume

**API Method:** `Volume`

Get or set volume levels.

**Permissions:** None

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `stream` | string | `alarm`, `music`, `notification`, `ring`, `system`, `call` |
| `volume` | integer | Volume level to set |

**Example:**
```bash
# Get all volumes
termux-volume

# Set music volume
termux-volume music 10
```

---

## Wallpaper

**API Method:** `Wallpaper`

Set the device wallpaper.

**Permissions:** `SET_WALLPAPER`

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `file` | string | Path to image file |
| `lock` | boolean | Set as lock screen wallpaper |

**Example:**
```bash
termux-wallpaper -f /sdcard/wallpaper.jpg
```

---

## WiFi

**API Methods:** `WifiConnectionInfo`, `WifiScanInfo`, `WifiEnable`

Control and query WiFi.

**Permissions:**

| Method | Permission |
|--------|------------|
| `WifiConnectionInfo` | None |
| `WifiScanInfo` | `ACCESS_FINE_LOCATION` |
| `WifiEnable` | None |

**Example:**
```bash
# Get current connection info
termux-wifi-connectioninfo

# Scan for networks
termux-wifi-scaninfo

# Enable/disable WiFi
termux-wifi-enable true
termux-wifi-enable false
```

**Sample Output (connection info):**
```json
{
  "bssid": "aa:bb:cc:dd:ee:ff",
  "frequency_mhz": 2412,
  "ip": "192.168.1.100",
  "link_speed_mbps": 72,
  "mac_address": "02:00:00:00:00:00",
  "network_id": 1,
  "rssi": -45,
  "ssid": "MyWiFi",
  "ssid_hidden": false,
  "supplicant_state": "COMPLETED"
}
```
