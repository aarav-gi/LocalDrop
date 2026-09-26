# LocalDrop

Offline, router-free, internet-free local file transfer for Android, built
per the attached project plan. The sender runs this app; receivers need
only a normal Wi-Fi connection and a browser — no app install required on
their side.

## Opening the project

1. Unzip this project.
2. Open the root folder in **Android Studio (Koala or newer recommended)**.
3. Let Android Studio sync Gradle. It will auto-generate the Gradle
   wrapper JAR if you don't have `gradlew`/`gradle-wrapper.jar` locally —
   these binary files aren't included since they were generated outside a
   network-connected environment. If Android Studio doesn't offer to
   create it, run **File ▸ New ▸ ... ▸ Gradle Wrapper** or
   `gradle wrapper --gradle-version 8.7` from a machine with Gradle
   installed.
4. Build & run on a physical device (the emulator's virtual Wi-Fi/hotspot
   support is unreliable for this kind of test — use two real phones).

**Minimum SDK:** 26 (Android 8.0) — required for `LocalOnlyHotspot`.

## What's implemented (Plan phases 1–9)

- Dynamic local IP/interface discovery — **never** hard-codes
  `192.168.43.1` (`core/network/NetworkInterfaceDetector.kt`)
- Local connectivity: reuses an existing Wi-Fi connection when available,
  otherwise falls back to the documented `WifiManager.startLocalOnlyHotspot`
  API — no root, no undocumented APIs (`hotspot/`)
- Embedded HTTP server (NanoHTTPD) with `/`, `/api/session`, `/api/files`,
  `/api/download/{token}`, `/api/health` (`server/LocalHttpServer.kt`)
- HTTP Range support for resumable downloads (`server/RangeHeader.kt`,
  `server/FileStreamer.kt`)
- Buffered streaming straight from `content://` URIs — large files are
  never loaded fully into RAM (`server/ProgressReportingInputStream.kt`)
- Per-file, per-session cryptographically random tokens; no raw filesystem
  paths are ever exposed over HTTP (`core/security/`)
- Simple per-IP rate limiting and request logging
- QR code generation of the runtime-discovered connection URL (ZXing)
- Live per-receiver transfer tracking with real measured speed/ETA
  (`server/TransferManager.kt`) — the UI never claims a guaranteed speed
- Foreground service so sharing survives the app leaving the foreground,
  with a live notification showing receiver count and speed
- Jetpack Compose UI: file picker (Storage Access Framework, multi-select),
  Start/Stop Sharing, QR + copy-link, live transfer progress list
- Responsive receiver web page (`assets/web/index.html`) — no install
  needed on the receiving device

## Known follow-ups (not yet implemented)

These are called out explicitly rather than silently skipped:

- **SHA-256 verification**: `core/utils/HashUtils.kt` computes hashes
  off-thread, but it isn't wired into the download flow/UI yet (Plan §16)
- **Transfer history (Room)**: sessions currently live only in memory for
  the current sharing session; persistent history (Plan §11/Phase 11) is
  not yet implemented
- **Folder sharing, upload support, device discovery**: explicitly listed
  in the plan as "future version" — not in this MVP
- **Automated test suite**: the 25 manual test scenarios and security
  tests in the plan (§29–30) haven't been scripted; recommend running
  through them manually across the device/OS matrix before any release
- **R8/ProGuard review & signed release build**: `proguard-rules.pro` has
  starter keep rules only — verify a release build actually works with
  minification on real devices before shipping (Phase 13)
- Multi-range (`Range: bytes=0-100,200-300`) requests are simplified to
  serve only the first requested range, which covers the standard
  single-connection resume case but not fully spec-compliant multipart
  ranges

## Security notes

- Files are addressed only by opaque per-file tokens
  (`/api/download/{token}?s={sessionToken}`) — never by path, so there is
  nothing to path-traverse.
- Sessions expire after 30 minutes of inactivity
  (`AppConstants.SESSION_TIMEOUT_MILLIS`) and are fully invalidated on
  STOP.
- All tokens are generated with `SecureRandom` (`TokenGenerator.kt`).

## Architecture

```
UI (Compose) → MainViewModel → SharingForegroundService
                                     ├── WifiNetworkManager (hotspot/Wi-Fi)
                                     ├── ServerController → LocalHttpServer
                                     ├── SessionManager (tokens/expiry)
                                     └── TransferManager (progress/speed)
```
