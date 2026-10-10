#!/usr/bin/env python3
"""
VirtualHit Server Daemon
Virtual Hitbox Controller bridge for Android via USB (ADB socket forward/reverse).
Supports Linux (evdev/uinput) and Windows (ViGEmBus via vgamepad).
"""

import sys
import os
import time
import socket
import struct
import subprocess
import threading
import argparse
import platform

# Protocol Constants
MAGIC_HEADER = b'VH'  # 0x5648
PROTOCOL_VERSION = 1
CMD_INPUT_UPDATE = 1
CMD_PING = 2
CMD_SOCD_CHANGE = 3

DEFAULT_PORT = 8888

# Button Bitmask Constants
BTN_BIT_UP       = 1 << 0
BTN_BIT_DOWN     = 1 << 1
BTN_BIT_LEFT     = 1 << 2
BTN_BIT_RIGHT    = 1 << 3

BTN_BIT_LP       = 1 << 4   # Light Punch (West / Square / X)
BTN_BIT_MP       = 1 << 5   # Medium Punch (North / Triangle / Y)
BTN_BIT_HP       = 1 << 6   # Heavy Punch (R1 / RB)
BTN_BIT_AP       = 1 << 7   # Extra Punch (L1 / LB)

BTN_BIT_LK       = 1 << 8   # Light Kick (South / Cross / A)
BTN_BIT_MK       = 1 << 9   # Medium Kick (East / Circle / B)
BTN_BIT_HK       = 1 << 10  # Heavy Kick (R2 / RT)
BTN_BIT_AK       = 1 << 11  # Extra Kick (L2 / LT)

BTN_BIT_SELECT   = 1 << 12  # Select / Coin / Back
BTN_BIT_START    = 1 << 13  # Start / Options
BTN_BIT_HOME     = 1 << 14  # Home / Guide / PS
BTN_BIT_TOUCHPAD = 1 << 15  # Turbo Mode / Aux
BTN_BIT_L3       = 1 << 16  # Left Stick Press (L3)
BTN_BIT_R3       = 1 << 17  # Right Stick Press (R3)

# SOCD Modes
SOCD_NEUTRAL = 0     # L+R = Neutral, D+U = Neutral
SOCD_UP_PRIORITY = 1 # L+R = Neutral, D+U = Up (Classic Hitbox)
SOCD_LAST_WIN = 2    # Last input overrides previous input

# ─────────────────────────────────────────────────────────────
# Detect host OS and load the appropriate gamepad backend
# ─────────────────────────────────────────────────────────────
HOST_OS = platform.system()  # 'Linux', 'Windows', or 'Darwin'


class VirtualGamepadLinux:
    """Linux backend: creates a virtual gamepad via uinput/evdev."""

    def __init__(self):
        try:
            import evdev
            from evdev import UInput, ecodes as e
            self._e = e
        except ImportError:
            print("[!] Error: 'evdev' Python module is missing.")
            print("[!] Please run: pip install evdev")
            sys.exit(1)

        e = self._e
        cap = {
            e.EV_KEY: [
                e.BTN_SOUTH,     # LK / A
                e.BTN_EAST,      # MK / B
                e.BTN_NORTH,     # MP / Y
                e.BTN_WEST,      # LP / X
                e.BTN_TL,        # AP / LB
                e.BTN_TR,        # HP / RB
                e.BTN_TL2,       # AK / LT
                e.BTN_TR2,       # HK / RT
                e.BTN_SELECT,    # Select
                e.BTN_START,     # Start
                e.BTN_MODE,      # Home / Guide
                e.BTN_THUMBL,    # L3
                e.BTN_THUMBR,    # R3
                e.BTN_DPAD_UP,
                e.BTN_DPAD_DOWN,
                e.BTN_DPAD_LEFT,
                e.BTN_DPAD_RIGHT,
            ],
            e.EV_ABS: [
                (e.ABS_X,    evdev.AbsInfo(value=0, min=-32767, max=32767, fuzz=16, flat=128, resolution=0)),
                (e.ABS_Y,    evdev.AbsInfo(value=0, min=-32767, max=32767, fuzz=16, flat=128, resolution=0)),
                (e.ABS_RX,   evdev.AbsInfo(value=0, min=-32767, max=32767, fuzz=16, flat=128, resolution=0)),
                (e.ABS_RY,   evdev.AbsInfo(value=0, min=-32767, max=32767, fuzz=16, flat=128, resolution=0)),
                (e.ABS_HAT0X,evdev.AbsInfo(value=0, min=-1, max=1, fuzz=0, flat=0, resolution=0)),
                (e.ABS_HAT0Y,evdev.AbsInfo(value=0, min=-1, max=1, fuzz=0, flat=0, resolution=0)),
                (e.ABS_Z,    evdev.AbsInfo(value=0, min=0, max=255, fuzz=0, flat=0, resolution=0)),
                (e.ABS_RZ,   evdev.AbsInfo(value=0, min=0, max=255, fuzz=0, flat=0, resolution=0)),
            ]
        }

        try:
            import evdev
            self.ui = evdev.UInput(cap, name="VirtualHit Arcade Controller", vendor=0x1234, product=0x5678, version=1)
            print("[+] Virtual Gamepad created via uinput!")
        except PermissionError:
            print("[!] Permission denied accessing /dev/uinput.")
            print("[!] Run: sudo chmod 666 /dev/uinput  OR  use setup_linux.sh")
            sys.exit(1)

    def update_inputs(self, up, down, left, right, mask, lx=0, ly=0, rx=0, ry=0):
        e = self._e
        self.ui.write(e.EV_ABS, e.ABS_X, lx)
        self.ui.write(e.EV_ABS, e.ABS_Y, ly)
        self.ui.write(e.EV_ABS, e.ABS_RX, rx)
        self.ui.write(e.EV_ABS, e.ABS_RY, ry)

        hat_x = (-1 if left else 0) + (1 if right else 0)
        hat_y = (-1 if up else 0) + (1 if down else 0)
        self.ui.write(e.EV_ABS, e.ABS_HAT0X, hat_x)
        self.ui.write(e.EV_ABS, e.ABS_HAT0Y, hat_y)

        self.ui.write(e.EV_KEY, e.BTN_DPAD_UP,    1 if up    else 0)
        self.ui.write(e.EV_KEY, e.BTN_DPAD_DOWN,  1 if down  else 0)
        self.ui.write(e.EV_KEY, e.BTN_DPAD_LEFT,  1 if left  else 0)
        self.ui.write(e.EV_KEY, e.BTN_DPAD_RIGHT, 1 if right else 0)

        self.ui.write(e.EV_KEY, e.BTN_WEST,  1 if (mask & BTN_BIT_LP) else 0)
        self.ui.write(e.EV_KEY, e.BTN_NORTH, 1 if (mask & BTN_BIT_MP) else 0)
        self.ui.write(e.EV_KEY, e.BTN_TR,    1 if (mask & BTN_BIT_HP) else 0)
        self.ui.write(e.EV_KEY, e.BTN_TL,    1 if (mask & BTN_BIT_AP) else 0)
        self.ui.write(e.EV_KEY, e.BTN_SOUTH, 1 if (mask & BTN_BIT_LK) else 0)
        self.ui.write(e.EV_KEY, e.BTN_EAST,  1 if (mask & BTN_BIT_MK) else 0)
        self.ui.write(e.EV_KEY, e.BTN_TR2,   1 if (mask & BTN_BIT_HK) else 0)
        self.ui.write(e.EV_KEY, e.BTN_TL2,   1 if (mask & BTN_BIT_AK) else 0)

        self.ui.write(e.EV_ABS, e.ABS_Z,  255 if (mask & BTN_BIT_AK) else 0)
        self.ui.write(e.EV_ABS, e.ABS_RZ, 255 if (mask & BTN_BIT_HK) else 0)

        self.ui.write(e.EV_KEY, e.BTN_SELECT, 1 if (mask & BTN_BIT_SELECT) else 0)
        self.ui.write(e.EV_KEY, e.BTN_START,  1 if (mask & BTN_BIT_START)  else 0)
        self.ui.write(e.EV_KEY, e.BTN_MODE,   1 if (mask & BTN_BIT_HOME)   else 0)
        self.ui.write(e.EV_KEY, e.BTN_THUMBL, 1 if (mask & BTN_BIT_L3)     else 0)
        self.ui.write(e.EV_KEY, e.BTN_THUMBR, 1 if (mask & BTN_BIT_R3)     else 0)

        self.ui.syn()

    def close(self):
        self.ui.close()


class VirtualGamepadWindows:
    """Windows backend: creates a virtual Xbox 360 controller via ViGEmBus (vgamepad)."""

    def __init__(self):
        try:
            import vgamepad as vg
            self._vg = vg
        except ImportError:
            print("[!] Error: 'vgamepad' Python module is missing.")
            print("[!] Please run: pip install vgamepad")
            print("[!] Also install the ViGEmBus driver from:")
            print("    https://github.com/ViGEm/ViGEmBus/releases/latest")
            sys.exit(1)

        try:
            self.gamepad = self._vg.VX360Gamepad()
            print("[+] Virtual Xbox 360 Gamepad created via ViGEmBus!")
        except Exception as ex:
            print(f"[!] Failed to create ViGEm virtual gamepad: {ex}")
            print("[!] Make sure the ViGEmBus driver is installed.")
            sys.exit(1)

    def update_inputs(self, up, down, left, right, mask, lx=0, ly=0, rx=0, ry=0):
        vg = self._vg
        gp = self.gamepad

        # Reset all buttons first to avoid stale presses
        gp.reset()

        # D-Pad
        dpad = vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP    if up    else 0
        dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN  if down  else 0
        dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT  if left  else 0
        dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT if right else 0

        # Action buttons
        if mask & BTN_BIT_LK: dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_A
        if mask & BTN_BIT_MK: dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_B
        if mask & BTN_BIT_LP: dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_X
        if mask & BTN_BIT_MP: dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_Y

        # Bumpers
        if mask & BTN_BIT_AP: dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER
        if mask & BTN_BIT_HP: dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER

        # Menu
        if mask & BTN_BIT_SELECT: dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_BACK
        if mask & BTN_BIT_START:  dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_START
        if mask & BTN_BIT_HOME:   dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_GUIDE

        # Stick clicks
        if mask & BTN_BIT_L3: dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_THUMB
        if mask & BTN_BIT_R3: dpad |= vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_THUMB

        gp.press_button(button=dpad)

        # Analog triggers (0..255 range)
        gp.left_trigger(value=255 if (mask & BTN_BIT_AK) else 0)
        gp.right_trigger(value=255 if (mask & BTN_BIT_HK) else 0)

        # Analog sticks — vgamepad expects -32768..32767
        gp.left_joystick(x_value=lx, y_value=-ly)   # Invert Y (vgamepad: up = positive)
        gp.right_joystick(x_value=rx, y_value=-ry)

        gp.update()

    def close(self):
        try:
            self.gamepad.reset()
            self.gamepad.update()
        except Exception:
            pass


def create_gamepad():
    """Factory: return the appropriate gamepad backend for the current OS."""
    if HOST_OS == "Linux":
        print("[*] Detected OS: Linux — using evdev/uinput backend.")
        return VirtualGamepadLinux()
    elif HOST_OS == "Windows":
        print("[*] Detected OS: Windows — using ViGEmBus/vgamepad backend.")
        return VirtualGamepadWindows()
    else:
        print(f"[!] Unsupported OS: {HOST_OS}. Only Linux and Windows are supported.")
        sys.exit(1)


class SOCDCleaner:
    def __init__(self, mode=SOCD_NEUTRAL):
        self.mode = mode
        self.last_lr = 0  # 1=left, 2=right
        self.last_ud = 0  # 1=up, 2=down

    def clean(self, raw_up, raw_down, raw_left, raw_right):
        up, down, left, right = raw_up, raw_down, raw_left, raw_right

        if raw_left and not raw_right: self.last_lr = 1
        elif raw_right and not raw_left: self.last_lr = 2
        if raw_up and not raw_down: self.last_ud = 1
        elif raw_down and not raw_up: self.last_ud = 2

        if self.mode == SOCD_NEUTRAL:
            if raw_left and raw_right: left = right = False
            if raw_up and raw_down:   up = down = False

        elif self.mode == SOCD_UP_PRIORITY:
            if raw_left and raw_right: left = right = False
            if raw_up and raw_down:   up = True; down = False

        elif self.mode == SOCD_LAST_WIN:
            if raw_left and raw_right:
                if self.last_lr == 1: right = False
                elif self.last_lr == 2: left = False
            if raw_up and raw_down:
                if self.last_ud == 1: down = False
                elif self.last_ud == 2: up = False

        return up, down, left, right


def setup_adb_forwarding(port):
    print("[*] Checking connected Android devices via ADB...")
    try:
        res = subprocess.run(["adb", "devices"], capture_output=True, text=True, check=True)
        lines = [line.strip() for line in res.stdout.strip().split('\n')[1:] if line.strip()]
        devices = [line.split()[0] for line in lines if 'device' in line]

        if not devices:
            print("[!] Warning: No ADB devices detected!")
            print("    Make sure USB Debugging is ON and phone is plugged via USB.")
            print("    Will still listen on port", port)
            return False

        print(f"[+] Found Android device: {devices[0]}")
        print(f"[*] Setting up 'adb reverse tcp:{port} tcp:{port}'...")
        rev_res = subprocess.run(["adb", "reverse", f"tcp:{port}", f"tcp:{port}"], capture_output=True, text=True)
        if rev_res.returncode == 0:
            print(f"[+] ADB Reverse tunnel established on tcp:{port}!")
            return True
        else:
            print(f"[*] 'adb reverse' not supported, trying 'adb forward'...")
            fwd_res = subprocess.run(["adb", "forward", f"tcp:{port}", f"tcp:{port}"], capture_output=True, text=True)
            if fwd_res.returncode == 0:
                print(f"[+] ADB Forward tunnel established on tcp:{port}!")
                return True
            else:
                print(f"[!] ADB forwarding failed: {fwd_res.stderr.strip()}")
                return False
    except Exception as err:
        print(f"[!] ADB invocation error: {err}")
        return False


def render_dashboard(client_ip, packets_cnt, pps, last_mask, up, down, left, right, socd_mode, lx=0, ly=0, rx=0, ry=0):
    def b(bit, name):
        return f"\033[1;32m[{name}]\033[0m" if (last_mask & bit) else f"\033[90m[{name}]\033[0m"

    dir_u = "\033[1;36m[▲]\033[0m" if up    else "\033[90m[▲]\033[0m"
    dir_d = "\033[1;36m[▼]\033[0m" if down  else "\033[90m[▼]\033[0m"
    dir_l = "\033[1;36m[◄]\033[0m" if left  else "\033[90m[◄]\033[0m"
    dir_r = "\033[1;36m[►]\033[0m" if right else "\033[90m[►]\033[0m"

    socd_str = "NEUTRAL" if socd_mode == SOCD_NEUTRAL else ("UP-PRIORITY" if socd_mode == SOCD_UP_PRIORITY else "LAST-WIN")

    status = (
        f"\r\033[K"
        f"Client: {client_ip} | PPS: {pps:4d} | Packets: {packets_cnt} | SOCD: {socd_str}\n\033[K"
        f"DIRECTIONS: {dir_l} {dir_d} {dir_r} {dir_u}  |  "
        f"PUNCH: {b(BTN_BIT_LP,'LP')} {b(BTN_BIT_MP,'MP')} {b(BTN_BIT_HP,'HP')} {b(BTN_BIT_AP,'AP')}  |  "
        f"KICK: {b(BTN_BIT_LK,'LK')} {b(BTN_BIT_MK,'MK')} {b(BTN_BIT_HK,'HK')} {b(BTN_BIT_AK,'AK')}  |  "
        f"STICKS: L({lx:+06d},{ly:+06d}) R({rx:+06d},{ry:+06d})"
        f"\033[A"
    )
    sys.stdout.write(status)
    sys.stdout.flush()


def main():
    parser = argparse.ArgumentParser(description="VirtualHit Gamepad Daemon (Linux/Windows)")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help="Server port (default: 8888)")
    parser.add_argument("--socd", type=int, default=0, choices=[0, 1, 2], help="SOCD Mode (0=Neutral, 1=Up Priority, 2=Last Win)")
    parser.add_argument("--no-adb", action="store_true", help="Skip ADB automatic port setup")
    args = parser.parse_args()

    print("==========================================================")
    print("        VirtualHit Server Daemon (USB Hitbox Server)      ")
    print(f"        Platform: {HOST_OS}                              ")
    print("==========================================================")

    if not args.no_adb:
        setup_adb_forwarding(args.port)

    gamepad = create_gamepad()
    cleaner = SOCDCleaner(mode=args.socd)

    server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        server_sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
    except AttributeError:
        pass

    server_sock.bind(("0.0.0.0", args.port))
    server_sock.listen(1)

    print(f"[+] Server listening on 0.0.0.0:{args.port}...")
    print("[*] Waiting for VirtualHit Android app connection...")

    try:
        while True:
            client_sock, client_addr = server_sock.accept()
            try:
                client_sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            except AttributeError:
                pass

            print(f"\n[+] Connected to VirtualHit client: {client_addr[0]}:{client_addr[1]}")

            packets_cnt = 0
            pps = 0
            last_pps_time = time.time()
            pps_counter = 0

            buffer = bytearray()
            # 24-byte Fixed Packet Format:
            # Header(2B: 'VH') | Ver(1B) | Cmd(1B) | Seq(4B uint) | Mask(4B uint) | LX(2B) | LY(2B) | RX(2B) | RY(2B) | Timestamp(4B)
            PACKET_SIZE = 24
            pkt_struct = struct.Struct(">2sBBIIhhhhI")

            try:
                while True:
                    data = client_sock.recv(1024)
                    if not data:
                        print("\n[-] Client disconnected.")
                        break

                    buffer.extend(data)

                    while len(buffer) >= PACKET_SIZE:
                        if buffer[0:2] != MAGIC_HEADER:
                            del buffer[0]
                            continue

                        hdr, ver, cmd, seq, mask, lx, ly, rx, ry, ts = pkt_struct.unpack(buffer[:PACKET_SIZE])
                        del buffer[:PACKET_SIZE]

                        packets_cnt += 1
                        pps_counter += 1

                        if cmd == CMD_INPUT_UPDATE:
                            raw_up    = bool(mask & BTN_BIT_UP)
                            raw_down  = bool(mask & BTN_BIT_DOWN)
                            raw_left  = bool(mask & BTN_BIT_LEFT)
                            raw_right = bool(mask & BTN_BIT_RIGHT)

                            up, down, left, right = cleaner.clean(raw_up, raw_down, raw_left, raw_right)
                            gamepad.update_inputs(up, down, left, right, mask, lx, ly, rx, ry)

                            now = time.time()
                            if now - last_pps_time >= 1.0:
                                pps = pps_counter
                                pps_counter = 0
                                last_pps_time = now

                            render_dashboard(client_addr[0], packets_cnt, pps, mask, up, down, left, right, cleaner.mode, lx, ly, rx, ry)

                        elif cmd == CMD_SOCD_CHANGE:
                            cleaner.mode = mask & 0x03
                            print(f"\n[*] SOCD mode changed to {cleaner.mode}")

            except (ConnectionResetError, BrokenPipeError):
                print("\n[-] Client connection reset.")
            finally:
                client_sock.close()

    except KeyboardInterrupt:
        print("\n[*] Shutting down VirtualHit Server...")
    finally:
        server_sock.close()
        gamepad.close()
        print("[+] Gamepad device cleaned up. Bye!")


if __name__ == "__main__":
    main()
