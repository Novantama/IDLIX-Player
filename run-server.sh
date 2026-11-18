#!/usr/bin/env bash
# ==============================================================================
# IDLIX Backend API Server Runner (Linux)
# Platform Streaming Native Android TV & Mobile
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -d "$SCRIPT_DIR/Backend API" ]; then
    BACKEND_DIR="$SCRIPT_DIR/Backend API"
elif [ -f "$SCRIPT_DIR/package.json" ]; then
    BACKEND_DIR="$SCRIPT_DIR"
else
    echo "[ERROR] Direktori Backend API tidak ditemukan!"
    exit 1
fi

cd "$BACKEND_DIR"

echo "======================================================================"
echo "                     IDLIX BACKEND API SERVER"
echo "            Platform Streaming Native Android TV & Mobile"
echo "======================================================================"

# 1. Periksa ketersediaan Node.js
if ! command -v node >/dev/null 2>&1; then
    echo "[ERROR] Node.js belum terpasang di sistem Linux Anda!"
    echo "Silakan jalankan skrip instalasi otomatis terlebih dahulu:"
    echo "    ./install-linux.sh"
    exit 1
fi

# 2. Periksa ketersediaan port 5376
echo "[1/3] Memeriksa ketersediaan port 5376..."
OCCUPIED_PID=""
if command -v lsof >/dev/null 2>&1; then
    OCCUPIED_PID=$(lsof -ti :5376 2>/dev/null || true)
elif command -v fuser >/dev/null 2>&1; then
    OCCUPIED_PID=$(fuser 5376/tcp 2>/dev/null | tr -d ' ' || true)
fi

if [ -n "$OCCUPIED_PID" ]; then
    echo "     Port 5376 sedang digunakan PID $OCCUPIED_PID, menghentikan proses lama..."
    kill -9 $OCCUPIED_PID 2>/dev/null || true
    sleep 1
fi

# 3. Pastikan file .env tersedia
if [ ! -f .env ] && [ -f .env.example ]; then
    echo "[INFO] Menyiapkan file konfigurasi lokal .env..."
    cp .env.example .env
fi

# 4. Pastikan node_modules terpasang
if [ ! -d "node_modules" ]; then
    echo "[2/3] Menginstall dependensi: npm install..."
    npm install
fi

# 5. Pastikan kode terkompilasi
if [ ! -f "dist/server.js" ]; then
    echo "[2/3] Mengompilasi TypeScript: npm run build..."
    npm run build
fi

# 6. Deteksi Alamat IP Jaringan Lokal (LAN)
LAN_IP=$(node -e "const os = require('os'); const nets = os.networkInterfaces(); for (const name of Object.keys(nets)) { for (const net of nets[name] || []) { if (net.family === 'IPv4' && !net.internal && !net.address.startsWith('169.254')) { console.log(net.address); process.exit(0); } } } console.log('127.0.0.1');" 2>/dev/null || hostname -I 2>/dev/null | awk '{print $1}' || echo "127.0.0.1")

echo ""
echo "======================================================================"
echo " Akses Komputer (Dashboard) : http://localhost:5376"
echo " Akses TV / HP (Jaringan)   : http://${LAN_IP}:5376"
echo "======================================================================"
echo " PETUNJUK KONEKSI:"
echo " 1. Pastikan Komputer dan Android TV / HP terhubung ke Wi-Fi yang sama."
echo " 2. Buka aplikasi di TV/HP -> Pengaturan -> Klik 'Auto-Detect Server'."
echo " 3. Atau masukkan alamat http://${LAN_IP}:5376 secara manual."
echo "======================================================================"
echo "[3/3] Menyalakan server IDLIX..."
echo ""

exec node dist/server.js
