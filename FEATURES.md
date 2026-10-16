# SmartXonnect+ — Feature roadmap (vs. original TVS Connect)

Reverse-engineered inventory of the original app's features and where our rebuild stands.
Legend: ✅ built (real backend) · 🟡 built but local-only (not wired to backend) · ❌ not built.

> Scale note: the original app is ~180 Activities / 346 Fragments, re-implemented per vehicle
> platform (N109/N112/N251/N360/U368/U399/U400/U408/U445/U449/U458/U507/U532…). Treat these as
> a **platform matrix over a shared feature set**, not 346 features. Full parity is a multi-month effort.

---

## Core (what we have)
| Feature | Status | Hook |
|---|---|---|
| Phone + OTP login | ✅ | REST `v3/UserLogin/Loginv3`, `v3/userlogin/verifyloginotp` |
| Vehicle list / garage (basic) | ✅ | REST `Vehicle/GetVahicleList` |
| Live telemetry | ✅ | GraphQL `GetTvsmLatestDeviceData` |
| Charging (battery SOC/status) | ✅ | GraphQL `GetBatteriesLatestData` |
| Ride history | ✅ | REST `Travel/GetTravelList` |
| Keyless BLE (find/lock/unlock/trunk) | ✅ | AES challenge-response `0x40→0xF2→0x50` |
| BLE scan + pair + proximity auto-connect | ✅ | — |
| Crash-alert config + emergency contacts | 🟡 | GraphQL `addEmergencyContacts` / REST multipart (wire writes) |
| Geo/Time fence + overspeed | 🟡 | GraphQL `setTimeFence` / `setOverspeedThreshold` |
| Share vehicle (secondary riders) | 🟡 | GraphQL `addSecondaryUser` / `removeUserVehicleAssociation` |
| Vehicle settings (immobilizer/theft/low-batt…) | 🟡 | GraphQL `SetVehicleSettings` |
| Profile | ✅ | login session |

---

## ❌ BLE cluster push-controls (separate protocol: `[0x5A/0x5B][opcode][payload][0xFF]`, N360 AES-encrypts)
This is a different BLE channel from keyless — plain push frames, **no challenge**, but **model-specific**.
| Control | Opcode | Notes |
|---|---|---|
| **Screen brightness / illumination** | `{0x5A,0xF1}` byte[2] | N360: 0–5 ×2; U-series raw. `sendIllumination()` |
| Ride mode | `{0x5A,0xF1}` byte[3] | same frame |
| Day/night + speedo-mode toggle | `{0x5A,0xF1}` | `setDay` / `setSpeedoModeChange` |
| Theme / personalization | `{0x5A,0x50}`, `{0x5B,0x7E}` | user theme, DOB |
| Turn-by-turn nav mirroring | `{0x5A,0x4E}`, `{0x5A,0x49}`, `{0x5B,0x50}` | distance/direction to cluster |
| Media/music (state/vol/title/album/artist) | `{0x5B,0x60/0x61/0x62/0x65}` | full stack on N360, U400 |
| Caller / SMS / app-notif counts | `{0x5B,0x43}` / `{0x5B,0x53}` / `{0x5B,0x9C}` | WhatsApp/FB/Insta counts |
| Phone status (signal/battery/time/missed) | `{0x5B,0x4A}` | cyclic; also carries time-sync |
| Rider name / current location | `{0x5B,0x52}` / `{0x5B,0x5C}` | |
| Weather + AQI | `{0x5B,0x5D}` | |
| News / Cricket / Football scrolls | `{0x5B,0x96}` / `{0x5B,0x81}` / `{0x5B,0x91}` | |
| Accessory control | `{0x5B,0x57}` | U399/U400 |
| Low-fuel / high-speed alert text | `{0x5B,0x4C}` | |
| Service-reminder request | prefix `"S"` | |
| Cluster wallpaper | — | custom wallpaper upload to cluster |
| Cluster screenshot | inbound byte[17] | cluster-triggered, phone executes |

---

## ❌ Navigation, maps & location
Turn-by-turn nav (HERE **and** Mappls stacks), tour multi-day planning, nearby POI/petrol search,
**find-my-vehicle / last-parked location**, auto-reroute, non-IoT phone-GPS tracking.

## ❌ Rides / Tours / Track
Per-model ride detail + analytics, **lap timing / track mode / replay coach** (premium Apache),
riding patterns & tips, tours (multi-day, saved, tour mode), **share-ride image/map/theme editor**.

## ❌ Safety suite
Crash/fall UI flows (have config), **SOS button**, nearest hospital + ambulance, **Roadside Assistance (RSA)**,
ICE (in-case-of-emergency) documents on cluster.

## ❌ Geofence / anti-theft (writes)
Geofence create/edit/delete + breach alerts, time-fence CRUD, **anti-theft/immobilizer** commands
(`TheftInput`), overspeed config — currently local-only in our app.

## ❌ EV charger management
Home charging cradle + portable charger: onboard by IMEI, telemetry, settings/commands
(`GetHomechargerLatestData`, `SetHomeChargerSettings`, portable equivalents).

## ❌ Accessories & wearables
Smart helmet, **Garmin & Noise smartwatch** companions, MAC-ID activation, accessory management.

## ❌ Service & documents
Service booking + history + records + reminders, dealer/service locator, warranty status, DIY maintenance,
mileage calculator, test-ride booking, insurance/PUC, **DigiDoc** (RC/license/insurance sync to cluster),
CSI/NPS feedback surveys.

## ❌ Community / rewards (KOGO platform)
Full social network: feed, discussions, events, ride stories, groups, gallery; **challenges & moments & trips**
(KOGO SDK); badges, rewards, coupons, referrals, merchandise store.

## ❌ Settings & smart controls
Display/brightness, theme, units, **DND + auto-reply SMS**, **follow-me headlamp**, auto-TSL, auto-connect/launch,
incognito mode, **TVS Intelligo AI auto-settings**, HUD mirroring config, overspeed alert popup.

## ❌ Voice / media / notifications
Voice assistant (TVS Intelligo / Alexa / KOGO Pro), music control, call accept/reject rules,
notification relay + WhatsApp + quick-reply SMS to cluster.

## ❌ Other
TPMS (front/rear pressure + sensor battery + history), raw data-parameter diagnostics, OTA/firmware
(cluster software update), help/FAQ, know-your-bike / user manual, address book.

---

## Suggested build order (simplest → hardest)
1. **Cluster brightness + ride-mode + day/night** — one `{0x5A,0xF1}` frame; immediate, visible win.
2. Wire the 🟡 features to real mutations (settings, geofence, contacts, share).
3. Cluster notifications/media/caller mirroring (`0x5B` frames).
4. Find-my-vehicle / last-parked + a single map stack (Mappls).
5. EV charger management, TPMS, service suite.
6. Tours/track mode, community/KOGO, voice — large, later.

> Caveat: cluster frames are model-specific and some are AES-encrypted (N360); each needs verification
> against the real bike over BLE.
