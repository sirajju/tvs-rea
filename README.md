# SmartXonnect+ 🏍️⚡

A redesigned, modern companion app for **your own** TVS SmartXonnect motorcycle —
Kotlin + Jetpack Compose, with the keyless-entry BLE protocol, **auto-connect on
approach**, and **proximity auto-actions** (walk up to your bike → it connects and
fires *Find Vehicle* automatically).

> This is an interoperability/companion client for a vehicle **you own**, built from
> your own decompiled app + your own reverse-engineered protocol. All device-specific
> secrets live in `secrets.properties` (gitignored) — nothing is hardcoded in source.

---

## 1. Fill in your secrets (required)

Copy the template and fill in the values you already extracted from your bike/app:

```bash
cp secrets.properties.example secrets.properties
```

| Key | What it is |
|-----|-----------|
| `AES_KEY_HEX` | 16-byte AES key (`KeylessEntryUtils.AES_KEY_BYTE_ARRAY`) as hex |
| `XOR_KEY_BYTE` | XOR layer byte, decimal (`0xEB` = `235`) |
| `SEED_FIND_VEHICLE` / `SEED_LOCK` / `SEED_UNLOCK` / `SEED_OPEN_TRUNK` | 16-byte command seeds, hex |
| `UUID_WRITE` / `UUID_NOTIFY` | GATT characteristic UUIDs (defaults match TVSBT/U388) |
| `BOOTSTRAP_FRAMES` | *(optional)* comma-separated 20-byte hex preamble frames |
| `BASE_URL_ICE` / `BASE_URL_EV` / `GRAPHQL_URL` | Cloud endpoints (real TVS prod defaults) |

The bike's **BLE MAC** is **not** a build secret — it comes from your account's
vehicle list after login (`VehicleRecord.BleMacId`). For offline testing, dev mock
mode supplies a sample MAC you can change.

## 2. Build & run

Open the folder in **Android Studio** (Koala/Ladybug+) and hit Run, or from CLI:

```bash
./gradlew :app:assembleDebug        # build
./gradlew :app:installDebug         # install on a connected device
```

- **Min SDK 26, target 35.** Requires a **physical device** — BLE does not work on emulators.
- Grant Bluetooth + Location + Notifications when prompted (needed for scan/connect/proximity).

## 3. The actual flow (not a simulation)

```
Splash ─▶ Login (phone)  ─▶  POST v3/UserLogin/Loginv3         (real OTP to your phone)
              │
              ▼
          OTP screen      ─▶  POST v3/userlogin/verifyloginotp  (ICE+EV tokens + profile)
              │                 tokens stored in EncryptedSharedPreferences (Keystore)
              ▼
          Dashboard
              ├─ GET Vehicle/GetVahicleList  → your vehicle(s) + BLE MAC
              ├─ Keyless control (Find / Lock / Unlock / Trunk)
              └─ Smart Proximity (foreground service, RSSI hysteresis)
```

**Dev mock mode** (debug builds only): toggle on the login screen to run the whole
UI offline with canned data — no network, any number/OTP.

## 4. Keyless protocol (the heart)

`ble/KeylessProtocol.kt` is a 1:1 Kotlin port of your Python PoC:

1. Enable CCCD notifications on the NOTIFY characteristic.
2. Send **`0x40` INITIATE_REQUEST** with the command seed (XOR-wrapped 20-byte frame).
3. Receive the bike's **`0xF2` CHALLENGE** notification (XOR-decrypted, bytes `[2..18)`).
4. **AES/CTR/NoPadding** encrypt the 16-byte challenge (IV = challenge).
5. Send **`0x50` SEND_RESPONSE** with the ciphertext → bike verifies and acts.

Frame layout (before XOR): `[9A F1][pktType][start][len][00][seed_slice…][pad][FF]`,
random `start`/`len` per `BluetoothUtil.getLength`. XOR pass preserves bytes 0–1 and
the trailing `FF`, XORs the middle with `XOR_KEY_BYTE`.

`ble/BleKeylessManager.kt` runs this over Android `BluetoothGatt` with a coroutine
that suspends on the challenge (8 s timeout) then sends the response. Every TX/RX
frame is surfaced in the in-app **BLE Debug Console** for protocol verification.

## 5. Smart Proximity (new)

`proximity/ProximityService.kt` is a low-power foreground service that scans for your
paired MAC and smooths RSSI:

- **Arrive** (smoothed RSSI ≥ `rssiEnter`, default −65 dBm) → auto-connect, then fire
  the configured approach action (default **Find Vehicle**), debounced 30 s.
- **Leave** (RSSI ≤ `rssiExit`, default −85 dBm) → optional auto-lock.

Thresholds and behaviour are user-configurable (`SettingsStore`, DataStore).

## 6. Architecture

```
ui/            Compose screens (login, otp, dashboard, feature scaffolds) + theme
ble/           KeylessProtocol (pure) + BleKeylessManager (GATT) + models
proximity/     ProximityService (foreground, RSSI auto-connect/auto-action)
data/remote/   Retrofit TvsApi + DTOs (real endpoints)
data/local/    TokenStore (encrypted) + SettingsStore (DataStore)
data/repo/     TvsRepository (login→otp→profile→vehicles, mock toggle)
di/            Hilt modules
```
Hilt for DI · Retrofit/Moshi/OkHttp · DataStore · Security-Crypto · Accompanist permissions.

## 7. Feature status (core-first build)

| Feature | Status |
|---------|--------|
| Phone + OTP login (real endpoints) | ✅ implemented |
| **Nearby-device scan + pairing list** (pick your bike, like the old app) | ✅ implemented |
| **Direct connect without sign-in** (BLE-only mode) | ✅ implemented |
| **Bluetooth-off prompt on open** (system enable dialog) | ✅ implemented |
| Profile + vehicle list | ✅ implemented |
| BLE keyless: Find / Lock / Unlock / Trunk | ✅ implemented (challenge-response) |
| Auto-connect on approach | ✅ implemented |
| Proximity auto-action (auto Find) + auto-lock on leave | ✅ implemented |
| Encrypted session storage + auto-login | ✅ implemented |
| BLE debug console | ✅ implemented |
| Live telemetry, Crash Alert, Geo/Time-fence, Rides, Charging, Share | 🧩 screens scaffolded & wired into nav; data layer is the next phase |

The scaffolded features map directly onto the original app's backends (p360 GraphQL
`GetTvsmLatestDeviceData` / `setTimeFence` / `addSecondaryUser`, REST ride/telemetry,
crash accelerometer+activity-recognition fusion). They are the next build phase.

## 8. Security notes

- Secrets are build-time only (`secrets.properties`), never committed.
- Session tokens are stored with `EncryptedSharedPreferences` (Keystore-backed) — the
  original app wrote them to plaintext SharedPreferences and logcat; this does not.
- Keyless commands require physical BLE proximity + your AES key — same trust model as
  the OEM app, for your own vehicle.
```
