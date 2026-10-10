#!/bin/bash
set -e

echo "=========================================================="
echo "         VirtualHit Linux Server Setup Script             "
echo "=========================================================="

echo "[1/3] Installing python dependency 'evdev'..."
pip3 install -r requirements.txt || pip install -r requirements.txt

echo "[2/3] Setting up udev permission rules for /dev/uinput..."
if [ "$EUID" -ne 0 ]; then
    echo "[!] sudo privileges required to install udev rules and add user to 'input' group."
    sudo cp 99-virtualhit.rules /etc/udev/rules.d/
    sudo usermod -aG input $USER
    sudo udevadm control --reload-rules && sudo udevadm trigger
    sudo modprobe uinput || true
else
    cp 99-virtualhit.rules /etc/udev/rules.d/
    usermod -aG input $USER
    udevadm control --reload-rules && udevadm trigger
    modprobe uinput || true
fi

chmod +x virtualhit_server.py

echo "=========================================================="
echo "[+] Setup completed successfully!"
echo "Note: You may need to log out and log back in for the 'input' group to take effect,"
echo "      or simply run the server with: ./virtualhit_server.py (or sudo ./virtualhit_server.py)"
echo "=========================================================="
