#!/usr/bin/env python3
"""
TVS SmartXonnect — BLE device SIMULATOR (peripheral / GATT server)
==================================================================

Pretends to be your motorcycle's BLE cluster so you can test the app
end-to-end WITHOUT the real bike. It advertises as a TVS device, exposes
the write (0x5352) and notify (0x5354) characteristics, and implements the
exact keyless handshake the app speaks:

    app  --0x40 INITIATE (seed slice)-->  sim
    sim  --0xF2 CHALLENGE (16 random bytes)-->  app
    app  --0x50 RESPONSE  (AES-CTR slice)-->  sim
    sim   verifies AES-CTR(challenge) and "executes" the command

Secrets (AES key, seeds, XOR byte, UUIDs) are read from the project's
gitignored  secrets.properties  — nothing is hardcoded here.

Runs on Windows / macOS / Linux via `bless` (WinRT / CoreBluetooth / BlueZ).
Requires a Bluetooth adapter that supports the PERIPHERAL role.

    pip install -r requirements.txt
    python tvs_sim.py --vin MD626EG56S1P31415

Then on the phone: open the app -> "Connect to my bike directly" ->
tap "TVSBT_SIM" in the scan list -> press Find Vehicle. Watch this console.
"""

import argparse
import asyncio
import os
import secrets as pysecrets
import sys
from pathlib import Path

try:
    from Crypto.Cipher import AES
except ImportError:
    sys.exit("Missing pycryptodome. Run: pip install -r requirements.txt")

try:
    from bless import (
        BlessServer,
        GATTCharacteristicProperties,
        GATTAttributePermissions,
    )
except ImportError:
    sys.exit("Missing bless. Run: pip install -r requirements.txt")


# --- Config loading (from secrets.properties) --------------------------------

def load_properties(path: Path) -> dict:
    props = {}
    if not path.exists():
        sys.exit(f"secrets.properties not found at {path}\n"
                 f"Copy secrets.properties.example -> secrets.properties and fill it in.")
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        props[k.strip()] = v.strip()
    return props


def hx(s: str) -> bytes:
    return bytes.fromhex(s.replace(":", "").replace(" ", ""))


# --- Protocol primitives (mirror the app / PoC exactly) ----------------------

class Protocol:
    def __init__(self, cfg: dict):
        self.aes_key = hx(cfg["AES_KEY_HEX"])
        self.xor = int(cfg.get("XOR_KEY_BYTE", "235")) & 0xFF
        self.seeds = {
            "FIND_VEHICLE": hx(cfg["SEED_FIND_VEHICLE"]),
            "LOCK":         hx(cfg["SEED_LOCK"]),
            "UNLOCK":       hx(cfg["SEED_UNLOCK"]),
            "OPEN_TRUNK":   hx(cfg["SEED_OPEN_TRUNK"]),
        }

    def xor_frame(self, raw: bytes) -> bytes:
        """Symmetric XOR layer: preserve bytes 0,1 and the trailing 0xFF."""
        if len(raw) < 3:
            return raw
        out = bytearray(raw)
        for i in range(2, len(raw) - 1):
            out[i] ^= self.xor
        out[-1] = 0xFF
        return bytes(out)

    def aes_ctr(self, challenge: bytes) -> bytes:
        iv = (challenge + b"\x00" * 16)[:16]
        return AES.new(self.aes_key, AES.MODE_CTR, nonce=b"", initial_value=iv).encrypt(iv)

    def build_challenge_frame(self, challenge: bytes) -> bytes:
        """[9A F2][challenge*16][0x27][FF] then XOR-wrapped (what the bike sends)."""
        raw = bytes([0x9A, 0xF2]) + challenge[:16] + bytes([0x27, 0xFF])
        return self.xor_frame(raw)

    def build_ack_frame(self, ok: bool) -> bytes:
        raw = bytes([0x9A, 0xF1, 0x50, 0x01 if ok else 0x00]) + bytes(15) + bytes([0xFF])
        return self.xor_frame(raw[:19] + bytes([0xFF]))

    def identify_command(self, start: int, slice_: bytes):
        for name, seed in self.seeds.items():
            if 0 <= start <= len(seed) and seed[start:start + len(slice_)] == slice_:
                return name
        return None


# --- Simulator state machine -------------------------------------------------

class Simulator:
    def __init__(self, proto: Protocol, service_uuid, notify_uuid, write_uuid, vin):
        self.p = proto
        self.service_uuid = service_uuid
        self.notify_uuid = notify_uuid
        self.write_uuid = write_uuid
        self.vin = vin
        self.server: BlessServer | None = None
        self.challenge: bytes | None = None
        self.pending_cmd: str | None = None

    def log(self, tag, msg):
        print(f"  [{tag:<9}] {msg}")

    # bless calls this synchronously when the phone writes to 0x5352
    def on_write(self, characteristic, value, **kwargs):
        raw = bytes(value)
        if str(characteristic.uuid).lower() != str(self.write_uuid).lower():
            return
        print(f"\n[RX raw] {raw.hex()}")
        dec = self.p.xor_frame(raw)
        print(f"[RX dec] {dec.hex()}")
        if len(dec) < 7:
            return
        # Correct byte order (verified from smali / v2 PoC):
        #   [2]=nonce  [3]=length  [4]=0x00  [5]=cmd_byte  [6..]=seed slice
        nonce = dec[2]
        length = dec[3]
        cmd_byte = dec[5]
        slice_ = dec[6:6 + length]

        if cmd_byte == 0x40:
            cmd = self.p.identify_command(nonce, slice_)
            self.pending_cmd = cmd
            self.challenge = pysecrets.token_bytes(16)
            self.log("0x40", f"INITIATE  cmd={cmd or '??'}  (nonce={nonce} len={length} slice={slice_.hex()})")
            self.log("0xF2", f"CHALLENGE {self.challenge.hex()}")
            self._notify(self.p.build_challenge_frame(self.challenge))

        elif cmd_byte == 0x50:
            if self.challenge is None:
                self.log("0x50", "RESPONSE received but no active challenge — ignoring")
                return
            expected = self.p.aes_ctr(self.challenge)
            exp_slice = expected[nonce:nonce + length]
            ok = (exp_slice == slice_)
            self.log("0x50", f"RESPONSE  recv={slice_.hex()}  expect[{nonce}:{nonce+length}]={exp_slice.hex()}")
            if ok:
                print(f"  ✅ AUTHENTICATED — executing: {self.pending_cmd or 'COMMAND'}  (VIN {self.vin})")
            else:
                print(f"  ❌ AUTH FAILED (slice mismatch) for {self.pending_cmd or 'COMMAND'}")
            self._notify(self.p.build_ack_frame(ok))
            self.challenge = None
            self.pending_cmd = None
        else:
            self.log("other", f"cmd=0x{cmd_byte:02x} (bootstrap / non-keyless) — ignored")

    def on_read(self, characteristic, **kwargs):
        return characteristic.value

    def _notify(self, frame: bytes):
        if not self.server:
            return
        try:
            ch = self.server.get_characteristic(self.notify_uuid)
            ch.value = bytearray(frame)
            self.server.update_value(self.service_uuid, self.notify_uuid)
            print(f"[TX ->  ] {frame.hex()}")
        except Exception as e:
            print(f"[TX ERR ] {e}")

    async def run(self, name: str):
        self.server = BlessServer(name=name)
        self.server.read_request_func = self.on_read
        self.server.write_request_func = self.on_write

        await self.server.add_new_service(self.service_uuid)
        await self.server.add_new_characteristic(
            self.service_uuid, self.write_uuid,
            GATTCharacteristicProperties.write | GATTCharacteristicProperties.write_without_response,
            None, GATTAttributePermissions.writeable,
        )
        await self.server.add_new_characteristic(
            self.service_uuid, self.notify_uuid,
            GATTCharacteristicProperties.notify | GATTCharacteristicProperties.read,
            bytearray(b"\x00"), GATTAttributePermissions.readable,
        )
        await self.server.start()
        print("=" * 64)
        print(f"  TVS BLE simulator running as '{name}'")
        print(f"  VIN           : {self.vin}")
        print(f"  Service       : {self.service_uuid}")
        print(f"  Write  (RX)   : {self.write_uuid}")
        print(f"  Notify (TX)   : {self.notify_uuid}")
        print(f"  Commands      : {', '.join(self.p.seeds.keys())}")
        print("=" * 64)
        print("Advertising… connect from the app and send a keyless command.")
        print("Press Ctrl+C to stop.\n")
        try:
            while True:
                await asyncio.sleep(1)
        except asyncio.CancelledError:
            pass
        finally:
            await self.server.stop()


# --- Entry point -------------------------------------------------------------

def main():
    default_secrets = Path(__file__).resolve().parents[2] / "secrets.properties"
    ap = argparse.ArgumentParser(description="TVS SmartXonnect BLE device simulator")
    ap.add_argument("--secrets", default=str(default_secrets),
                    help="Path to secrets.properties (default: project root)")
    ap.add_argument("--vin", default="SIMULATED-VIN",
                    help="VIN to report (e.g. MD626EG56S1P31415)")
    ap.add_argument("--name", default="TVSBT_SIM",
                    help="Advertised BLE device name (keep 'TVS' so the app flags it)")
    ap.add_argument("--service", default="00005350-0000-1000-8000-00805f9b34fb",
                    help="GATT service UUID to host the characteristics under")
    args = ap.parse_args()

    cfg = load_properties(Path(args.secrets))
    proto = Protocol(cfg)
    write_uuid = cfg.get("UUID_WRITE", "00005352-0000-1000-8000-00805f9b34fb")
    notify_uuid = cfg.get("UUID_NOTIFY", "00005354-0000-1000-8000-00805f9b34fb")

    sim = Simulator(proto, args.service, notify_uuid, write_uuid, args.vin)
    try:
        asyncio.run(sim.run(args.name))
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    main()
