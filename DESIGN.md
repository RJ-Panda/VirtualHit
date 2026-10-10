# VirtualHit — Technical & Architecture Design Specification 🕹️📐

> **Document Version**: 1.4.0  
> **Source of Truth**: Codebase (`/android` and `/server`)

---

## 🎯 1. Codebase Purpose & Core Mission

### The Problem
Physical leverless arcade controllers (Hitbox, Snackbox Micro, Haute42, Victrix FS-12) have become the premier input standard for competitive fighting games (*Street Fighter 6*, *Tekken 8*, *Guilty Gear Strive*, *Mortal Kombat 1*) due to instantaneous directional actuation and precision. However, entry-level hardware costs between **$150 and $400 USD**, posing a steep barrier to entry.

Conversely, off-the-shelf mobile touchscreen controller apps suffer from critical flaws:
- **30–80 ms Input Latency**: Wireless Wi-Fi socket jitter and Bluetooth stack overhead introduce unpredictable frame delays (in games where a single frame is 16.6ms).
- **Incompatible or Illegal SOCD Cleaning**: Simultaneous inputs either cancel unpredictably or allow tournament-illegal states (e.g., simultaneous Left+Right inputs giving unfair block advantages).
- **Clunky Touch Ergonomics**: Generic virtual gamepads use simple floating buttons that do not allow rapid button gliding, pianoing, or simultaneous opposing button combinations.

### The VirtualHit Solution
**VirtualHit** turns any modern Android device (**Android 7.1.1 Nougat / API 25+**) into a tournament-grade, sub-millisecond leverless fightstick and gamepad controller tethered via USB:
- **Sub-Millisecond Wire Transport**: Leverages Android Debug Bridge (ADB) TCP reverse socket tunneling (`adb reverse tcp:8888 tcp:8888`) with `TCP_NODELAY` and a 1024-byte send buffer to stream directly through the physical USB cable.
- **Hardware & Software SOCD Parity**: Enforces tournament-standard Simultaneous Opposite Cardinal Direction cleaning on both the Android client and the host daemon to strictly comply with Capcom Pro Tour and global fighting game community rules.
- **Dynamic Touch Architecture**: Single-touch pointer isolation for directional discs, independent multi-touch zone detection for action clusters (enabling opposing combinations like X+B or A+Y), and gliding touch execution.
- **Unified Cross-Platform Daemon**: A single Python daemon auto-switches kernel-level backends between Linux (`evdev`/`/dev/uinput`) and Windows (`ViGEmBus`/`vgamepad` virtual Xbox 360 controller).

---

## 📂 2. Critical Files & Component Directory

```
VirtualHit/
├── android/
│   └── app/src/main/java/com/virtualhit/app/
│       ├── MainActivity.java        # Lifecycle, fullscreen immersion & persistence
│       ├── HitboxView.java          # Custom hardware Canvas & multi-touch engine
│       ├── HitboxButton.java        # Element data model, geometry & zone math
│       ├── SOCDCleaner.java         # Directional cleaning engine (SF6 / Capcom standard)
│       ├── SocketClient.java        # TCP_NODELAY binary framing client (24-byte packets)
│       └── SettingsSidePanel.java   # Configuration drawer & profile manager
└── server/
    ├── virtualhit_server.py         # Cross-platform daemon (Linux evdev + Windows ViGEmBus)
    ├── setup_linux.sh               # Linux udev & group automated provisioner
    ├── 99-virtualhit.rules          # udev rule granting /dev/uinput access to group 'input'
    ├── requirements.txt             # Linux dependencies (evdev>=1.6.0)
    └── requirements_windows.txt     # Windows dependencies (vgamepad>=0.1.0)
```

### Detailed Component Roles

#### Host Server (`/server`)
- [`virtualhit_server.py`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/server/virtualhit_server.py):
  - Auto-detects runtime OS using `platform.system()`.
  - Sets up ADB reverse tunnel (`adb reverse tcp:8888 tcp:8888`) with automated fallback to `adb forward`.
  - Unpacks fixed 24-byte binary network packets into sequence numbers, button bitmasks, stick coordinates, and timestamps.
  - Implements `VirtualGamepadLinux`: creates a virtual gamepad via `/dev/uinput` and `evdev` mapping `BTN_SOUTH`, `BTN_EAST`, `BTN_NORTH`, `BTN_WEST`, bumpers, triggers, hat axes, and analog joystick axes.
  - Implements `VirtualGamepadWindows`: creates a virtual Xbox 360 controller via `vgamepad` (ViGEmBus driver) translating bitmasks into `XUSB_BUTTON` constants and inverted Y axes.
  - Renders live ANSI terminal dashboard showing Client IP, Packets Per Second (PPS), packet count, SOCD mode, button states, and stick values.
- [`setup_linux.sh`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/server/setup_linux.sh):
  - Automates installation of dependencies, installs `99-virtualhit.rules` into `/etc/udev/rules.d/`, appends the current user to the `input` group, triggers `udevadm`, and loads the `uinput` kernel module.
- [`99-virtualhit.rules`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/server/99-virtualhit.rules):
  - Grants read/write permissions to `/dev/uinput` for non-root users in the `input` group (`KERNEL=="uinput", GROUP="input", MODE="0660"`).

#### Android Client (`/android/app/src/main/java/com/virtualhit/app`)
- [`HitboxView.java`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/android/app/src/main/java/com/virtualhit/app/HitboxView.java):
  - Custom hardware-accelerated `View` handling rendering and multi-touch dispatch.
  - Manages pointer states: gliding for regular buttons, pointer lock for joysticks and D-Pad discs, and multi-touch zone tracking for action discs.
  - Houses the Turbo Mode pulse engine (`Handler` running at 1–20 Hz) triggered by holding `TM`.
  - Computes edge glow effects using dynamic linear gradients and hue cycling.
  - Implements layout edit mode: dragging, resizing, adding, duplicating, and deleting elements.
- [`HitboxButton.java`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/android/app/src/main/java/com/virtualhit/app/HitboxButton.java):
  - Core data structure containing position (`cx`, `cy`), bounding box, radius, `Shape` (`CIRCLE`, `SQUARE`), `DiscType` (`DPAD`, `ABXY`), and bitmask flags.
  - Calculates 8-directional disc zones using angular trigonometry (`Math.atan2`) in Circle mode and 3×3 cell math in Square mode, with a 20% center radial deadband.
  - Serializes and deserializes button state to/from JSON.
- [`SOCDCleaner.java`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/android/app/src/main/java/com/virtualhit/app/SOCDCleaner.java):
  - Hardware-standard SOCD cleaner. Implements:
    - `MODE_NEUTRAL` (0): Left + Right = Neutral, Down + Up = Neutral (Capcom / SF6 standard).
    - `MODE_UP_PRIORITY` (1): Left + Right = Neutral, Down + Up = Up (Classic Hitbox standard).
    - `MODE_LAST_WIN` (2): Most recent directional input overrides the prior direction.
- [`SocketClient.java`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/android/app/src/main/java/com/virtualhit/app/SocketClient.java):
  - Manages low-latency TCP communication over `127.0.0.1:8888`.
  - Configures socket with `setTcpNoDelay(true)` to bypass Nagle's algorithm.
  - Employs a single-threaded `ExecutorService` to pack and flush 24-byte big-endian binary frames without blocking the main UI thread.
- [`SettingsSidePanel.java`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/android/app/src/main/java/com/virtualhit/app/SettingsSidePanel.java):
  - Modular configuration drawer built in code with dark theme surfaces.
  - Exposes connection inputs (host IP & port), HSV background and edge light pickers, 3-slot layout profile save/load, button picker, haptic feedback sliders (1–50ms), and Turbo speed slider (1–20 Hz).
- [`MainActivity.java`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/android/app/src/main/java/com/virtualhit/app/MainActivity.java):
  - Enforces `FLAG_KEEP_SCREEN_ON` and `SYSTEM_UI_FLAG_IMMERSIVE_STICKY` for distraction-free full-screen gameplay.
  - Reads and writes persistent configurations via `SharedPreferences` (`VirtualHitPrefs`).

---

## 📡 3. 24-Byte Binary Network Protocol Specification

To prevent serialization latency, garbage collection overhead, and packet fragmentation, inputs are encoded into fixed **24-byte big-endian binary packets**:

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|   Magic 'V'   |   Magic 'H'   |  Version (1)  |   Cmd (1/2/3) |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       Sequence Number (uint32)                |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       Button Bitmask (uint32)                 |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|        LX (-32767..32767)     |        LY (-32767..32767)     |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|        RX (-32767..32767)     |        RY (-32767..32767)     |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       Timestamp (uint32 ms)                   |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### Struct Layout
- **Python Struct Format**: `>2sBBIIhhhhI`
- **Total Packet Size**: 24 bytes

| Field | Offset | Type | Description |
|---|---|---|---|
| `magic` | 0 | `char[2]` | Fixed identifier bytes `b'VH'` (`0x56`, `0x48`) |
| `version` | 2 | `uint8` | Protocol version (`0x01`) |
| `command` | 3 | `uint8` | `1` = `INPUT_UPDATE`, `2` = `PING`, `3` = `SOCD_CHANGE` |
| `seq` | 4 | `uint32` | Monotonically incrementing packet sequence number |
| `mask` | 8 | `uint32` | 32-bit active button bitmask (see table below) |
| `lx` | 12 | `int16` | Left stick horizontal axis (`-32767` to `32767`, `0` = neutral) |
| `ly` | 14 | `int16` | Left stick vertical axis (`-32767` to `32767`, `0` = neutral) |
| `rx` | 16 | `int16` | Right stick horizontal axis (`-32767` to `32767`, `0` = neutral) |
| `ry` | 18 | `int16` | Right stick vertical axis (`-32767` to `32767`, `0` = neutral) |
| `timestamp` | 20 | `uint32` | Client timestamp in milliseconds (`currentTimeMillis & 0xFFFFFFFF`) |

### Button Bitmask Reference Table

| Constant Name | Shift | Bitmask (Hex) | Label | Xbox 360 Equivalent | Linux evdev Target |
|---|---|---|---|---|---|
| `BTN_BIT_UP` | `1 << 0` | `0x00000001` | `▲` | D-Pad Up | `BTN_DPAD_UP` / `ABS_HAT0Y -1` |
| `BTN_BIT_DOWN` | `1 << 1` | `0x00000002` | `▼` | D-Pad Down | `BTN_DPAD_DOWN` / `ABS_HAT0Y +1` |
| `BTN_BIT_LEFT` | `1 << 2` | `0x00000004` | `◄` | D-Pad Left | `BTN_DPAD_LEFT` / `ABS_HAT0X -1` |
| `BTN_BIT_RIGHT` | `1 << 3` | `0x00000008` | `►` | D-Pad Right | `BTN_DPAD_RIGHT` / `ABS_HAT0X +1` |
| `BTN_BIT_LP` | `1 << 4` | `0x00000010` | `X` | Button X (West) | `BTN_WEST` |
| `BTN_BIT_MP` | `1 << 5` | `0x00000020` | `Y` | Button Y (North) | `BTN_NORTH` |
| `BTN_BIT_HP` | `1 << 6` | `0x00000040` | `RB` | Right Bumper | `BTN_TR` |
| `BTN_BIT_AP` | `1 << 7` | `0x00000080` | `LB` | Left Bumper | `BTN_TL` |
| `BTN_BIT_LK` | `1 << 8` | `0x00000100` | `A` | Button A (South) | `BTN_SOUTH` |
| `BTN_BIT_MK` | `1 << 9` | `0x00000200` | `B` | Button B (East) | `BTN_EAST` |
| `BTN_BIT_HK` | `1 << 10` | `0x00000400` | `RT` | Right Trigger (255) | `BTN_TR2` / `ABS_RZ 255` |
| `BTN_BIT_AK` | `1 << 11` | `0x00000800` | `LT` | Left Trigger (255) | `BTN_TL2` / `ABS_Z 255` |
| `BTN_BIT_SELECT` | `1 << 12` | `0x00001000` | `SEL` | Back / Select | `BTN_SELECT` |
| `BTN_BIT_START` | `1 << 13` | `0x00002000` | `STA` | Start | `BTN_START` |
| `BTN_BIT_HOME` | `1 << 14` | `0x00004000` | `HOME` | Guide / Home | `BTN_MODE` |
| `BTN_BIT_TOUCHPAD`| `1 << 15` | `0x00008000` | `TM` | Turbo Modifier | Handled internally in app |
| `BTN_BIT_L3` | `1 << 16` | `0x00010000` | `L3` | Left Thumb Click | `BTN_THUMBL` |
| `BTN_BIT_R3` | `1 << 17` | `0x00020000` | `R3` | Right Thumb Click | `BTN_THUMBR` |

---

## 🎨 4. Visual Identity & Design System Tokens

All styling tokens are implemented programmatically to avoid XML inflation overhead:

### Color Palette
- **Canvas Default Background**: `#1C1D26`
- **Settings Modal Surface**: `#373B4B`
- **Section Panel Background**: `#353759`
- **Section Header Fill**: `#4D5A83`
- **Primary Teal Accent**: `#00B099` (used for active outlines, D-Pad press, connection pills)
- **Directional Buttons**: Normal `#1E293B` | Pressed `#00B099`
- **Jump Thumb Button**: Normal `#334155` | Pressed `#00FF88` (Vibrant Emerald)
- **Auxiliary Menu Buttons**: Normal `#151D2A` | Pressed `#FFB300` (Amber Gold)
- **Xbox Official Action Palette**:
  - **A (LK)**: Normal `#005F00` | Pressed `#00FF00`
  - **B (MK)**: Normal `#5F0000` | Pressed `#FF0000`
  - **X (LP)**: Normal `#00005F` | Pressed `#0000FF`
  - **Y (MP)**: Normal `#5F5F00` | Pressed `#FFFF00`
  - **Bumpers (LB/RB)**: Normal `#2D2436` | Pressed `#FF0055`
  - **Triggers (LT/RT)**: Normal `#1E2E40` | Pressed `#00B0FF`
- **Disc Pad Palettes**:
  - **DPad Disc**: Normal `#1E293B` | Pressed `#00B099`
  - **ABXY Pad**: Normal `#2D2436` | Pressed `#00E676`

### Typography & Formatting
- **Main Button Labels**: Android System Bold, centered, White (`#FFFFFF`).
- **Sub-Labels**: System Sans, centered, Slate Grey (`#94A3B8`).
- **Cardinal Disc Labels**: Directional arrows (`◄`, `▼`, `►`, `▲`) and action letters (`A`, `B`, `X`, `Y`) rendered only on cardinal wedges; diagonal sectors remain clean and unlabelled.

### Geometric Sizing Rules
- **Base Action Button Radius**: `Math.min(w, h) * 0.082f`
- **Directional Button Radius**: `baseRadius * 1.05f`
- **Jump Button Radius**: `baseRadius * 1.35f`
- **Disc Pad Default Radius**: `Math.min(w, h) * 0.16f`
- **Disc Center Deadzone**: `radius * 0.20f` (inner radial deadband)
- **Analog Joystick Motion Limit**: `radius * 0.85f` (clamped knob travel)
- **Haptic Vibration Feedback**: `1ms` to `50ms` (default: `12ms`)
- **Turbo Frequency Engine**: `1 Hz` to `20 Hz` (default: `5 Hz`)

---

## 🧭 5. Project Goals & Technical Roadmap

1. **Sub-Millisecond Wired Latency Target**: Maintain consistent `< 1ms` end-to-end packet delivery across USB on both Linux and Windows.
2. **Capcom Pro Tour Compliance**: Adhere to official tournament SOCD resolution rules with zero bypass opportunities.
3. **Ergonomic Accessibility**: Allow players of all hand geometries and physical needs to design custom, zero-fatigue button layouts.
4. **Planned Enhancements**:
   - Layout import and export via camera-scannable QR codes and JSON clipboard export.
   - Low-jitter UDP transport mode for wireless setups.
   - macOS virtual controller daemon using IOKit / virtual HID drivers.

---
---

# 🌐 Español (Especificación de Diseño)

# VirtualHit — Especificación de Diseño Técnico y Arquitectura 🕹️📐

> **Versión del Documento**: 1.4.0  
> **Fuente de la Verdad**: Código fuente (`/android` y `/server`)

---

## 🎯 1. Propósito del Proyecto y Misión Central

### El Problema
Los controles arcade leverless físicos (Hitbox, Snackbox Micro, Haute42, Victrix FS-12) son el estándar indiscutible de ejecución para juegos de pelea competitivos (*Street Fighter 6*, *Tekken 8*, *Guilty Gear Strive*, *Mortal Kombat 1*). Sin embargo, el costo de entrada oscila entre **$150 y $400 USD**, lo que representa una barrera económica significativa.

Por otra parte, las aplicaciones móviles tradicionales de control táctil presentan defectos graves:
- **Latencia de 30 a 80 ms**: La fluctuación de paquetes Wi-Fi y la sobrecarga de Bluetooth añaden retrasos impredecibles en juegos donde cada cuadro dura 16.6 ms.
- **Limpieza SOCD Deficiente o Ilegal**: Las entradas simultáneas se cancelan de forma errática o generan combinaciones ilegales en torneos.
- **Ergonomía Táctil Rígida**: Los controles virtuales convencionales no permiten el deslizamiento rápido de dedos, ejecuciones simultáneas opuestas ni configuraciones adaptables.

### La Solución de VirtualHit
**VirtualHit** convierte cualquier teléfono Android moderno (**Android 7.1.1 Nougat / API 25+**) en un control leverless y gamepad de nivel competitivo con latencia submilisegundo conectado por USB:
- **Transmisión Submilisegundo por Cable**: Utiliza túneles TCP inversos de ADB (`adb reverse tcp:8888 tcp:8888`) con la bandera `TCP_NODELAY` y un búfer de envío de 1024 bytes directamente por el cable USB físico.
- **Limpieza SOCD Legal para Torneos**: Limpieza de direcciones cardinales opuestas simultáneas tanto en el cliente Android como en el daemon del host para cumplir con las normas de Capcom Pro Tour y la FGC.
- **Arquitectura Táctil Adaptable**: Bloqueo de puntero para discos de dirección, detección multi-toque independiente para el disco de acción (permitiendo combinaciones opuestas como X+B o A+Y) y deslizamiento fluido de dedos.
- **Daemon Multiplataforma Unificado**: Servidor en Python con detección automática de sistema operativo que conmuta controladores a nivel de kernel entre Linux (`evdev`/`/dev/uinput`) y Windows (`ViGEmBus`/`vgamepad` simulando un control de Xbox 360).

---

## 📂 2. Directorio de Archivos Críticos

### Servidor Anfitrión (`/server`)
- [`virtualhit_server.py`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/server/virtualhit_server.py): Detecta el sistema operativo, establece el túnel ADB, desempaqueta tramas binarias de 24 bytes y despacha entradas a través de `VirtualGamepadLinux` (`evdev`/`uinput`) o `VirtualGamepadWindows` (`vgamepad`/ViGEmBus). Muestra un panel interactivo ANSI en terminal.
- [`setup_linux.sh`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/server/setup_linux.sh): Script que automatiza la configuración de dependencias, permisos udev, grupo `input` y módulo `uinput` en Linux.
- [`99-virtualhit.rules`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/server/99-virtualhit.rules): Regla udev que otorga permisos de lectura/escritura a `/dev/uinput` para integrantes del grupo `input`.
- [`requirements.txt`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/server/requirements.txt) & [`requirements_windows.txt`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/server/requirements_windows.txt): Dependencias específicas de cada plataforma (`evdev` en Linux, `vgamepad` en Windows).

### Cliente Android (`/android/app/src/main/java/com/virtualhit/app`)
- [`HitboxView.java`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/android/app/src/main/java/com/virtualhit/app/HitboxView.java): Lienzo gráfico acelerado por hardware que gestiona renderizado, deslizamiento táctil, aislamiento de punteros, motor de ráfaga Turbo (1–20 Hz), brillos perimetrales y edición interactiva de layouts.
- [`HitboxButton.java`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/android/app/src/main/java/com/virtualhit/app/HitboxButton.java): Modelo de datos con geometría, trigonometría para discos circulares de 8 direcciones, cuadrícula 3×3, zona muerta central del 20% y persistencia JSON.
- [`SOCDCleaner.java`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/android/app/src/main/java/com/virtualhit/app/SOCDCleaner.java): Limpieza SOCD estándar: `MODE_NEUTRAL` (0: I+D=Neutral, A+B=Neutral), `MODE_UP_PRIORITY` (1: I+D=Neutral, A+B=Arriba) y `MODE_LAST_WIN` (2: última entrada gana).
- [`SocketClient.java`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/android/app/src/main/java/com/virtualhit/app/SocketClient.java): Cliente de red TCP con `TCP_NODELAY` y envío asíncrono en tramas de 24 bytes mediante un `ExecutorService` dedicado.
- [`SettingsSidePanel.java`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/android/app/src/main/java/com/virtualhit/app/SettingsSidePanel.java): Panel modal de ajustes con selectores de IP/puerto, sliders HSV, gestión de 3 slots de layouts, selector de botones y ajuste háptico (1–50ms).
- [`MainActivity.java`](file:///home/rj_panda/.gemini/antigravity/scratch/VirtualHit/android/app/src/main/java/com/virtualhit/app/MainActivity.java): Mantiene la pantalla encendida (`FLAG_KEEP_SCREEN_ON`), activa inmersión completa y guarda ajustes en `SharedPreferences` (`VirtualHitPrefs`).

---

## 📡 3. Especificación del Protocolo Binario (24 Bytes)

Las tramas se envían en paquetes binarios fijos de **24 bytes en Big-Endian** (`>2sBBIIhhhhI` en Python):
- **Cabecera (`2B`)**: Bytes ASCII `VH` (`0x56`, `0x48`).
- **Versión (`1B`)**: `0x01`.
- **Comando (`1B`)**: `1` (Actualización de Entrada), `2` (Ping), `3` (Cambio SOCD).
- **Secuencia (`4B`)**: Entero de 32 bits sin signo incremental.
- **Máscara de Botones (`4B`)**: Campo de bits de 32 bits con los botones activos.
- **Joysticks LX, LY, RX, RY (`8B`)**: Cuatro enteros de 16 bits con signo (`-32767` a `32767`).
- **Timestamp (`4B`)**: Marca de tiempo en milisegundos del cliente.

---

## 🎨 4. Identidad Visual y Tokens de Diseño

- **Fondo Predeterminado del Lienzo**: `#1C1D26`
- **Superficie de Ventana Modal**: `#373B4B`
- **Fondo de Tarjetas de Sección**: `#353759`
- **Cabecera de Sección**: `#4D5A83`
- **Acento Primario Cian**: `#00B099`
- **Botón de Salto Presionado**: `#00FF88` (Verde Esmeralda)
- **Botones de Menú Presionados**: `#FFB300` (Oro Ámbar)
- **Colores Oficiales Xbox**:
  - **A**: Normal `#005F00` | Presionado `#00FF00`
  - **B**: Normal `#5F0000` | Presionado `#FF0000`
  - **X**: Normal `#00005F` | Presionado `#0000FF`
  - **Y**: Normal `#5F5F00` | Presionado `#FFFF00`
  - **Bumpers (LB/RB)**: Normal `#2D2436` | Presionado `#FF0055`
  - **Gatillos (LT/RT)**: Normal `#1E2E40` | Presionado `#00B0FF`
- **Dimensiones**: Radio base de botón = `min(w, h) * 0.082f`, radio de disco = `0.16f`, zona muerta de disco = `20%`, recorrido de palanca analógica = `85%`.
