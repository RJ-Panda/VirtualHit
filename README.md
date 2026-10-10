# VirtualHit 🕹️📱

**VirtualHit** (v1.4.0) is a high-performance, ultra-low-latency Virtual Hitbox, Leverless Fightstick & Gamepad controller system for **Linux and Windows** fighting games, using any Android device (**Android 7.1.1 Nougat and newer**) tethered via USB.

> 📐 **Looking for the deep technical architecture, protocol specification, or design system tokens?** Check out the [DESIGN.md](DESIGN.md) specification document.

---

## 🌟 Features in v1.4.0

- 🕹️ **Analog Joysticks (Left Stick `LJ` & Right Stick `RJ`)**:
  - Full 360° analog joystick control with dynamic thumbstick knob motion, crosshair rendering, and snap-to-center physics.
  - Transmits 16-bit signed shorts (`-32767` to `32767`) over a 24-byte low-latency binary network protocol.
- 🎛️ **Disc Pad Elements (DPad & ABXY Pad)**:
  - **DPad Disc**: Pointer-locked single-touch 8-directional directional rose (like the Xbox Elite controller d-pad). Fires combined cardinal bitmasks on diagonal zones. Supports SOCD cleaning.
  - **ABXY Pad**: Multi-touch 8-directional action disc. Each finger independently activates its zone, allowing simultaneous opposing inputs (e.g. X+B or A+Y). No SOCD cleaning.
  - Both elements support **Circle (rose arc)** and **Square (3×3 grid)** visual modes, cardinal-only labels, and 3-segment diagonal lighting.
- ⚡ **Rapid-Fire Turbo Mode Engine**:
  - Hold `TM` button and tap any action button to toggle Turbo rapid-fire (`⚡` badge).
  - Configurable frequency slider (1–20 Hz) with global toggle.
- 🎮 **Xbox Controller Layout & Colors**:
  - Default Xbox action buttons: **A** (Green), **B** (Red), **X** (Blue), **Y** (Yellow), **LB**, **LT**, **RB**, **RT**, **HOME**, **TM**.
  - Recognized by Steam Input, Linux `evdev`, and Windows ViGEmBus as a virtual Xbox 360 controller.
- 🧹 **Hardware & Software SOCD Cleaning**:
  - **Neutral**: `L+R = Neutral`, `D+U = Neutral` (Capcom / SF6 standard).
  - **Up Priority**: `L+R = Neutral`, `D+U = Up` (Classic Hitbox standard).
  - **Last Input Priority**: Most recent input overrides previous direction.
- ⚡ **Sub-Millisecond Low Latency**: Tethered USB connection via ADB TCP reverse socket forwarding (`TCP_NODELAY`, 24-byte binary packet framing).
- 🌈 **RGB Edge Lighting & Haptics**: Customizable screen-edge RGB glow cycle, touch-responsive lighting, and adjustable haptic feedback (1–50ms).
- 💾 **Multi-Slot Layout Editor**: Save and load custom button configurations across 3 independent layout slots.
- 🖥️ **Cross-Platform Server**: Single `virtualhit_server.py` auto-detects the host OS and uses `evdev/uinput` on Linux or `ViGEmBus/vgamepad` on Windows.

---

## 🚀 Quick Start — Linux

### 1. Linux Host Server Setup

1. Install Python dependencies:
   ```bash
   cd server
   pip install -r requirements.txt
   ```
2. Set up `/dev/uinput` permissions (one-time):
   ```bash
   ./setup_linux.sh
   ```
3. Run the server:
   ```bash
   python3 virtualhit_server.py
   ```
   *The server automatically detects connected Android devices via ADB and establishes `adb reverse tcp:8888 tcp:8888`.*

---

### 2. Android App Setup (Linux)

1. Enable **USB Debugging** on your Android device (*Settings → Developer Options → USB Debugging*).
2. Connect your Android device to your Linux PC via USB.
3. Build & install the APK:
   ```bash
   cd android
   gradle assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
4. Launch **VirtualHit** on your phone.
   - The green dot on the top left confirms connection.

> **📡 Wireless option:** A USB cable is recommended for minimum latency, but Wi-Fi also works. Open the app's **Settings → Connection** and change the Host IP to the IP address of the PC running the server (e.g., `192.168.1.x`). No ADB tunnel is needed in this mode — just make sure both devices are on the same network and the server is reachable on port `8888`.

---

## 🪟 Quick Start — Windows

### 1. Windows Host Server Requirements

| Requirement | How to get it |
|---|---|
| **Python 3.10+** | [python.org/downloads](https://www.python.org/downloads/) |
| **ViGEmBus Driver** | [github.com/ViGEm/ViGEmBus/releases](https://github.com/ViGEm/ViGEmBus/releases/latest) — install `ViGEmBus_Setup_x64.msi` |
| **ADB (Platform Tools)** | [developer.android.com/tools/releases/platform-tools](https://developer.android.com/tools/releases/platform-tools) — extract and add to PATH |

### 2. Windows Host Server Setup

1. Install the **ViGEmBus** driver (required before running the server — it's a kernel-level virtual controller bus):
   - Download `ViGEmBus_Setup_x64.msi` from the link above and run it.
   - Reboot if prompted.

2. Install Python dependencies:
   ```cmd
   cd server
   pip install -r requirements_windows.txt
   ```

3. Run the server (no admin rights needed after ViGEmBus is installed):
   ```cmd
   python virtualhit_server.py
   ```
   The server will print `Detected OS: Windows — using ViGEmBus/vgamepad backend.` and begin listening.

---

### 3. Android App Setup (Windows)

> **Note:** On Windows, you need ADB Platform Tools installed and on your system PATH to use the USB tunnel. The server calls `adb reverse` automatically on startup.

1. Install **ADB Platform Tools**:
   - Download from the link in the requirements table above.
   - Extract the zip somewhere (e.g., `C:\adb\`).
   - Add that folder to your Windows **PATH** environment variable:
     - Search → *Edit the system environment variables* → *Environment Variables* → select *Path* → *Edit* → *New* → paste `C:\adb\`.

2. Enable **USB Debugging** on your Android device (*Settings → Developer Options → USB Debugging*).

3. Connect your Android device via USB. Open a terminal (`cmd` or PowerShell) and verify:
   ```cmd
   adb devices
   ```
   Your device should appear as `<serial>    device`.

4. Install the VirtualHit APK onto your device.

   **Option A — If you have the pre-built APK file:**
   ```cmd
   adb install VirtualHit.apk
   ```

   **Option B — Build from source (requires Android SDK / Gradle):**
   ```cmd
   cd android
   gradle assembleDebug
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

5. Launch **VirtualHit** on your phone. The server will automatically set up `adb reverse tcp:8888 tcp:8888`, so the app connects to `127.0.0.1:8888` over the USB cable.
   - The green dot confirms connection.

> **📡 Wireless option:** A USB cable is recommended for minimum latency, but Wi-Fi also works. Open the app's **Settings → Connection** and change the Host IP to the IP address of the PC running the server (e.g., `192.168.1.x`). No ADB tunnel is needed in this mode — just make sure both devices are on the same network and the server is reachable on port `8888`.

---

## 🎨 Controls & Customization

- **Settings Gear (Top Right)**: Tap the top-right corner to open the settings panel.
  - **Connection**: Host IP and Port (default `127.0.0.1:8888` for USB).
  - **Appearance**: Background HSV color picker, Edge Lighting mode, speed & custom RGB sliders.
  - **Layout Editor**: Interactive Edit Mode (drag & resize buttons), Add Button modal — includes `DPad Disc`, `ABXY Pad`, `L3`, `R3`, `Left Stick LJ`, `Right Stick RJ`.
  - **Turbo Settings**: Rapid-fire speed slider (1–20 Hz) and global Turbo toggle.
  - **SOCD Rules**: Toggle Neutral, Up Priority, or Last Input Priority.
  - **Haptics**: Toggle vibration feedback and intensity (1–50ms).

---

## 🏗️ Architecture Overview

```
+-------------------------------------------------------------+
|                     Android 7.1.1+ Phone                    |
|   +-----------------------------------------------------+   |
|   | HitboxView (Multi-touch Canvas, Disc Pads, Sticks)  |   |
|   +-----------------------------------------------------+   |
|   | SOCDCleaner (Neutral / Up Priority / Last Win)      |   |
|   +-----------------------------------------------------+   |
|   | SocketClient (TCP_NODELAY, 24-byte Binary Framing)  |   |
+------------------------------|------------------------------+
                               | USB Cable (ADB TCP Tunnel)
+------------------------------v------------------------------+
|              Linux PC  /  Windows PC                        |
|   +-----------------------------------------------------+   |
|   | ADB Reverse Tunnel Manager (adb reverse tcp:8888)   |   |
|   +-----------------------------------------------------+   |
|   | virtualhit_server.py — OS auto-detect               |   |
|   +-----------------------------------------------------+   |
|   | Linux:   /dev/uinput (evdev virtual gamepad)        |   |
|   | Windows: ViGEmBus (virtual Xbox 360 controller)     |   |
+-------------------------------------------------------------+
```

---
---

# 🌐 Español

**VirtualHit** (v1.4.0) es un sistema de control virtual tipo Hitbox / Leverless / Gamepad de alto rendimiento y latencia ultra baja para juegos de pelea en **Linux y Windows**, usando cualquier dispositivo Android (**Android 7.1.1 Nougat o superior**) conectado por USB.

> 📐 **¿Buscas la arquitectura técnica completa, especificación de protocolos o tokens de diseño?** Consulta el documento [DESIGN.md](DESIGN.md).

---

## 🌟 Características en v1.4.0

- 🕹️ **Joysticks Analógicos (Stick Izquierdo `LJ` y Derecho `RJ`)**:
  - Control analógico de 360° con animación de knob, retícula visual y física de regreso al centro.
  - Transmite valores de 16 bits con signo (`-32767` a `32767`) en un protocolo binario de 24 bytes.
- 🎛️ **Elementos Disc Pad (DPad y ABXY Pad)**:
  - **DPad Disc**: Un solo dedo, bloqueado al disco, 8 direcciones al estilo del control Elite de Xbox. Las zonas diagonales disparan combinaciones de bits cardinales y pasan por limpieza SOCD.
  - **ABXY Pad**: Multi-táctil, cada dedo activa su zona independientemente, permitiendo combinaciones simultáneas opuestas (ej. X+B o A+Y). Sin limpieza SOCD.
  - Ambos elementos soportan modo **Círculo (arco de rosa)** y **Cuadrado (cuadrícula 3×3)**, etiquetas solo en cardinales e iluminación de 3 segmentos en diagonales.
- ⚡ **Motor de Turbo Rapid-Fire**:
  - Mantén `TM` y toca cualquier botón de acción para activar el turbo (`⚡`).
  - Velocidad configurable (1–20 Hz) con interruptor global.
- 🎮 **Layout y Colores Estilo Xbox**:
  - Colores oficiales: **A** (Verde), **B** (Rojo), **X** (Azul), **Y** (Amarillo), **LB**, **LT**, **RB**, **RT**, **HOME**, **TM**.
  - Reconocido por Steam Input, `evdev` de Linux y ViGEmBus de Windows como controlador virtual Xbox 360.
- 🧹 **Limpieza SOCD**:
  - **Neutral**: `I+D = Neutral`, `A+B = Neutral` (estándar Capcom / SF6).
  - **Prioridad Arriba**: `I+D = Neutral`, `A+B = Arriba` (estándar Hitbox clásico).
  - **Última Entrada Gana**: El input más reciente reemplaza al anterior.
- ⚡ **Latencia Ultra Baja**: Conexión USB via ADB con reenvío TCP inverso (`TCP_NODELAY`, tramas de 24 bytes).
- 🌈 **Iluminación RGB e Hápticos**: Glow RGB en el borde de la pantalla, iluminación reactiva al toque, retroalimentación háptica ajustable (1–50ms).
- 💾 **Editor de Layout Multi-Ranura**: Guarda y carga configuraciones personalizadas en 3 ranuras independientes.
- 🖥️ **Servidor Multi-Plataforma**: `virtualhit_server.py` detecta el sistema operativo automáticamente y usa `evdev/uinput` en Linux o `ViGEmBus/vgamepad` en Windows.

---

## 🚀 Inicio Rápido — Linux

### 1. Configuración del Servidor en Linux

1. Instala las dependencias de Python:
   ```bash
   cd server
   pip install -r requirements.txt
   ```
2. Configura los permisos de `/dev/uinput` (una sola vez):
   ```bash
   ./setup_linux.sh
   ```
3. Ejecuta el servidor:
   ```bash
   python3 virtualhit_server.py
   ```
   *El servidor detecta automáticamente dispositivos Android conectados via ADB y establece `adb reverse tcp:8888 tcp:8888`.*

---

### 2. Instalación de la App en Android (Linux)

1. Activa **Depuración USB** en tu dispositivo (*Ajustes → Opciones de desarrollador → Depuración USB*).
2. Conecta tu dispositivo Android a tu PC Linux via USB.
3. Compila e instala el APK:
   ```bash
   cd android
   gradle assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
4. Abre **VirtualHit** en tu teléfono.
   - El punto verde en la esquina superior izquierda confirma la conexión.

> **📡 Opción inalámbrica:** Se recomienda el cable USB para latencia mínima, pero también funciona por Wi-Fi. Abre **Ajustes → Conexión** en la app y cambia la IP del servidor a la IP del PC donde corre el servidor (por ejemplo, `192.168.1.x`). En este modo no se necesita el túnel ADB — solo asegúrate de que ambos dispositivos estén en la misma red y el puerto `8888` sea accesible.

---

## 🪟 Inicio Rápido — Windows

### 1. Requisitos del Servidor en Windows

| Requisito | Cómo obtenerlo |
|---|---|
| **Python 3.10+** | [python.org/downloads](https://www.python.org/downloads/) |
| **Driver ViGEmBus** | [github.com/ViGEm/ViGEmBus/releases](https://github.com/ViGEm/ViGEmBus/releases/latest) — instala `ViGEmBus_Setup_x64.msi` |
| **ADB (Platform Tools)** | [developer.android.com/tools/releases/platform-tools](https://developer.android.com/tools/releases/platform-tools) — extrae el ZIP y agrégalo al PATH |

### 2. Configuración del Servidor en Windows

1. Instala el driver **ViGEmBus** (es el bus virtual de controladores a nivel de kernel, necesario antes de correr el servidor):
   - Descarga `ViGEmBus_Setup_x64.msi` desde el enlace de arriba y ejecútalo.
   - Reinicia si se te pide.

2. Instala las dependencias de Python:
   ```cmd
   cd server
   pip install -r requirements_windows.txt
   ```

3. Ejecuta el servidor (sin necesidad de administrador tras instalar ViGEmBus):
   ```cmd
   python virtualhit_server.py
   ```
   El servidor mostrará `Detected OS: Windows — using ViGEmBus/vgamepad backend.` y comenzará a escuchar.

---

### 3. Instalación de la App en Android (Windows)

> **Nota:** En Windows necesitas ADB Platform Tools instalado y en el PATH del sistema para usar el túnel USB. El servidor llama a `adb reverse` automáticamente al iniciar.

1. Instala **ADB Platform Tools**:
   - Descarga el ZIP desde el enlace en la tabla de requisitos.
   - Extráelo en algún lugar (por ejemplo, `C:\adb\`).
   - Agrega esa carpeta al PATH de Windows:
     - Busca → *Editar las variables de entorno del sistema* → *Variables de entorno* → selecciona *Path* → *Editar* → *Nuevo* → pega `C:\adb\`.

2. Activa **Depuración USB** en tu dispositivo (*Ajustes → Opciones de desarrollador → Depuración USB*).

3. Conecta tu dispositivo Android via USB. Abre una terminal (`cmd` o PowerShell) y verifica:
   ```cmd
   adb devices
   ```
   Tu dispositivo debe aparecer como `<serial>    device`.

4. Instala el APK de VirtualHit en tu dispositivo.

   **Opción A — Si ya tienes el archivo APK pre-compilado:**
   ```cmd
   adb install VirtualHit.apk
   ```

   **Opción B — Compilar desde el código fuente (requiere Android SDK / Gradle):**
   ```cmd
   cd android
   gradle assembleDebug
   adb install app\build\outputs\apk\debug\app-debug.apk
   ```

5. Abre **VirtualHit** en tu teléfono. El servidor configurará automáticamente `adb reverse tcp:8888 tcp:8888`, por lo que la app se conectará a `127.0.0.1:8888` por el cable USB.
   - El punto verde confirma la conexión.

> **📡 Opción inalámbrica:** Se recomienda el cable USB para latencia mínima, pero también funciona por Wi-Fi. Abre **Ajustes → Conexión** en la app y cambia la IP del servidor a la IP del PC donde corre el servidor (por ejemplo, `192.168.1.x`). En este modo no se necesita el túnel ADB — solo asegúrate de que ambos dispositivos estén en la misma red y el puerto `8888` sea accesible.

---

## 🎨 Controles y Personalización

- **Engrane de Configuración (Esquina Superior Derecha)**: Toca la esquina superior derecha para abrir el panel de ajustes.
  - **Conexión**: IP del servidor y puerto (predeterminado `127.0.0.1:8888` para USB).
  - **Apariencia**: Selector de color HSV del fondo, modo de iluminación de bordes, velocidad y sliders RGB personalizados.
  - **Editor de Layout**: Modo de edición interactivo (arrastra y redimensiona botones), modal para agregar botones — incluye `DPad Disc`, `ABXY Pad`, `L3`, `R3`, `LJ`, `RJ`.
  - **Configuración de Turbo**: Slider de velocidad (1–20 Hz) e interruptor global.
  - **Reglas SOCD**: Cambiar entre Neutral, Prioridad Arriba o Última Entrada Gana.
  - **Hápticos**: Activar retroalimentación de vibración e intensidad (1–50ms).

---

## 🏗️ Arquitectura General

```
+-------------------------------------------------------------+
|                   Teléfono Android 7.1.1+                   |
|   +-----------------------------------------------------+   |
|   | HitboxView (Lienzo multi-táctil, Disc Pads, Sticks) |   |
|   +-----------------------------------------------------+   |
|   | SOCDCleaner (Neutral / Prioridad Arriba / Último)   |   |
|   +-----------------------------------------------------+   |
|   | SocketClient (TCP_NODELAY, tramas binarias 24 bytes) |  |
+------------------------------|------------------------------+
                               | Cable USB (Túnel ADB TCP)
+------------------------------v------------------------------+
|              PC Linux  /  PC Windows                        |
|   +-----------------------------------------------------+   |
|   | Gestor de Túnel ADB (adb reverse tcp:8888)          |   |
|   +-----------------------------------------------------+   |
|   | virtualhit_server.py — detección automática de OS   |   |
|   +-----------------------------------------------------+   |
|   | Linux:   /dev/uinput (gamepad virtual evdev)        |   |
|   | Windows: ViGEmBus (control Xbox 360 virtual)        |   |
+-------------------------------------------------------------+
```
