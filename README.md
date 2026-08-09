# LyfStack.Agent.Android

**LyfStack.Agent.Android** is the official Android companion agent for **LyfStack** (personal life & activity management), designed as the mobile equivalent of `LyfStack.Agent.Windows`.

It runs as a persistent background service on Android, tracks active application usage sessions, stores aggregated sessions locally in Room (SQLite), and synchronizes them to the LyfStack cloud via HTTPS POST API or outbound WebSocket control commands.

---

## Features

- **Background Usage Tracking**: Samples foreground applications every 5 seconds using `UsageStatsManager` and aggregates them into clean `UsageSession` records (ID, applicationName, processName, startedAt, endedAt, activeDurationSeconds, idleDurationSeconds, lastState, isOpen, category).
- **Auto & Manual Categorization**: Auto-classifies packages into standard categories (`Work`, `Browser`, `Games`, `Entertainment`, `Communication`, `System`, `Other`), with support for custom package-to-category overrides and package ignore lists.
- **Teal / Slate Brand Aesthetic**: Designed with modern Jetpack Compose and Material 3 utilizing LyfStack brand colors (`#0F766E` teal accent, `#0F172A` slate header gradient, `#F3F5F7` page background).
- **4 Primary Navigation Tabs**:
  1. **Activity**: Real-time tracking status, quick stats, summary range selector (Today, Week, Month, Year, All), metrics grid (Active, Idle, Tracked, Focus %), Top Apps, By Category breakdown, and manual sync action.
  2. **History**: Filterable activity session history with CSV and JSON data export.
  3. **Device**: Detailed hardware, RAM, CPU, OS, Agent version, Device UUID, and live WebSocket connection state monitor.
  4. **Settings**: Sync endpoint URL, auto-sync intervals, WebSocket control toggle/url/token, package ignore list editor, and category overrides manager.

---

## Required Permissions

To function accurately in the background, the agent requires the following Android permissions:

1. **Usage Access (`PACKAGE_USAGE_STATS`)**:
   - Required to identify the active foreground app.
   - Deep-link button available on the Activity and Settings screens to open `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
2. **Foreground Service (`FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`)**:
   - Displays a ongoing persistent notification ("LyfStack Agent (Android)") to guarantee continuous background execution without OS process killing.
3. **Post Notifications (`POST_NOTIFICATIONS`)**:
   - Required on Android 13+ (API 33+) for the foreground tracking notification.
4. **Unrestricted Battery / Ignore Battery Optimizations (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)**:
   - Prevents Doze mode from deferring sampling ticks during long idle periods.

---

## Sync & WebSocket Protocol Architecture

The sync model mirrors `LyfStack.Agent.Windows`:

### A. Outbound HTTPS POST Sync (Data Transfer)
- **Endpoint**: Configurable in Settings (default: `POST /api/v1/device-activity/sync?range=since_last`)
- **Headers**: `Content-Type: application/json`
- **Request Body Format (camelCase JSON)**:
```json
{
  "source": "LyfStack.Agent.Android",
  "deviceId": "3f8b1c4e-5a2d-4b9e-8c3a-1d7e9f2a4b6c",
  "device": "Google Pixel 8",
  "platform": "android",
  "exportedAt": "2026-08-09T00:30:00Z",
  "aggregation": "usage_sessions",
  "sync": {
    "range": "since_last",
    "from": null,
    "to": null,
    "pendingOnly": true
  },
  "sessionCount": 1,
  "sessions": [
    {
      "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "applicationName": "Chrome",
      "processName": "com.android.chrome",
      "processId": null,
      "startedAt": "2026-08-09T00:00:00Z",
      "endedAt": "2026-08-09T00:15:00Z",
      "activeDurationSeconds": 900,
      "idleDurationSeconds": 0,
      "lastState": "Active",
      "isOpen": false,
      "category": "Browser"
    }
  ]
}
```

### B. Outbound WebSocket Connection (Control Layer)
- **Endpoint**: Configurable in Settings (default: `wss://api.lyfstack.app/device-connection?deviceId=...&platform=android`)
- **Hello Frame**: Sent on connection:
  `{ "type": "HELLO", "deviceId": "...", "device": "...", "platform": "android", "agentVersion": "1.0.0" }`
- **Server Messages Handled**:
  - `SYNC_NOW`: Triggers HTTPS POST sync -> responds with `{ "type": "SYNC_RESULT", "success": true, "sessionCount": N }`
  - `PING`: Responds with `{ "type": "PONG" }`
  - `PAUSE` / `RESUME`: Toggles tracking -> responds with `{ "type": "STATUS", "isTrackingActive": boolean }`

---

## Comparison: Android vs Windows Agent

| Feature | LyfStack.Agent.Windows | LyfStack.Agent.Android |
| :--- | :--- | :--- |
| **Idle Detection** | Win32 `GetLastInputInfo()` mouse/keyboard hardware hook | Android `PowerManager.isInteractive`, `KeyguardManager.isKeyguardLocked` & `UsageStats` gaps |
| **App Identification** | Process Executable Name (`chrome.exe`) + Window Title | Package Name (`com.android.chrome`) + Package Label ("Chrome") |
| **Background Model** | Windows System Tray / Service | Android Foreground Service + Persistent Notification + WorkManager |
| **Protocol & Payload** | LyfStack JSON Schema v1 | Exact identical LyfStack JSON Schema v1 (`source: "LyfStack.Agent.Android"`) |

---

## How to Build & Run

1. Open project in **Android Studio**.
2. Build and install APK on device or emulator (Min SDK 26+ / Android 8.0+).
3. On launch, tap **"Grant"** on the top notification banner to enable **Usage Access**.
4. Configure your custom HTTPS sync endpoint or WebSocket URL in **Settings** tab.
