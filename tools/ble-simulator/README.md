# TVS BLE Device Simulator

Pretends to be your motorcycle's BLE cluster so you can test the app's keyless
flow **without the real bike**. It advertises as a TVS device and implements the
`0x40 → 0xF2 → 0x50` challenge-response exactly like the firmware.

## Install
```bash
pip install -r requirements.txt
```

## Run (reads keys from ../../secrets.properties)
```bash
python tvs_sim.py --vin MD626EG56S1P31415
```
You should see it start advertising as **TVSBT_SIM**.

## Test with the app
1. On the phone, open **SmartXonnect+** → **"Connect to my bike directly"**.
2. In the scan list, tap **TVSBT_SIM** (flagged ✓ as a vehicle by name).
3. Press **Find Vehicle** (or Lock/Unlock/Trunk).
4. Watch this console:
   ```
   [RX raw] ...        <- the app's 0x40 (XOR-wrapped)
   [0x40 ] INITIATE cmd=FIND_VEHICLE ...
   [0xF2 ] CHALLENGE <16 random bytes>
   [TX -> ] ...        <- challenge sent to the app
   [RX raw] ...        <- the app's 0x50 response
   [0x50 ] RESPONSE recv=.. expect=..
   ✅ AUTHENTICATED — executing: FIND_VEHICLE
   ```
   A ✅ means the app encrypted the challenge correctly with your AES key — the
   whole keyless chain works. The app's in-app **BLE Debug Console** shows the
   mirror image of these frames.

## How it verifies
On `0x50`, the app sends `AES-CTR(challenge)` sliced at a random `start/len`.
The simulator recomputes `AES-CTR(challenge)` with your AES key and checks the
received slice against `expected[start:start+len]`. Match ⇒ authenticated.

## Requirements / caveats
- A Bluetooth adapter that supports the **peripheral/advertiser role**.
  - **Windows:** most built-in Intel/Realtek adapters work via WinRT. Some USB
    dongles don't support peripheral mode — if advertising fails, that's why.
  - **Linux:** works via BlueZ (`sudo` may be needed).
  - **macOS:** works via CoreBluetooth.
- The phone and this PC must both have Bluetooth on and be in range.
- The device MAC shown in the app's scan list is this PC's Bluetooth MAC.

## If your adapter can't advertise
Use a **second Android phone** as the peripheral instead (most phones support
BLE advertising), or an **ESP32**. Ask and I can port this state machine to
either — the protocol logic is identical.
