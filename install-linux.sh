#!/usr/bin/env bash
# ==============================================================================
# IDLIX Backend API - Universal Linux Installer
# Mendukung: Ubuntu/Debian/Mint, Fedora/CentOS/RHEL/Rocky/Alma, Arch/Manjaro, Alpine, openSUSE
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

echo "======================================================================"
echo "          IDLIX BACKEND API - PEMASANGAN OTOMATIS LINUX"
echo "======================================================================"

# 1. Deteksi Hak Akses (Root / Sudo)
SUDO=""
if [ "$(id -u)" -ne 0 ]; then
    if command -v sudo >/dev/null 2>&1; then
        SUDO="sudo"
    else
        echo "[PERINGATAN] Script tidak dijalankan sebagai root dan 'sudo' tidak ditemukan."
        echo "Pastikan dependensi sistem sudah terpasang."
    fi
fi

# 2. Deteksi Distribusi Linux & Package Manager
echo "[1/4] Mendeteksi distribusi Linux & package manager..."
PKG_MANAGER=""
if command -v apt-get >/dev/null 2>&1; then
    PKG_MANAGER="apt"
elif command -v dnf >/dev/null 2>&1; then
    PKG_MANAGER="dnf"
elif command -v yum >/dev/null 2>&1; then
    PKG_MANAGER="yum"
elif command -v pacman >/dev/null 2>&1; then
    PKG_MANAGER="pacman"
elif command -v apk >/dev/null 2>&1; then
    PKG_MANAGER="apk"
elif command -v zypper >/dev/null 2>&1; then
    PKG_MANAGER="zypper"
fi

if [ -z "$PKG_MANAGER" ]; then
    echo "[PERINGATAN] Package manager tidak dikenali secara otomatis."
    echo "Silakan pastikan Node.js (>=20), Chromium, dan Xvfb telah terpasang secara manual."
else
    echo "      Package manager terdeteksi: $PKG_MANAGER"
fi

# 3. Pemasangan Node.js & Dependensi Sistem
echo "[2/4] Memeriksa & memasang Node.js serta pustaka Chromium / Xvfb..."

NEED_NODE_INSTALL=true
if command -v node >/dev/null 2>&1; then
    NODE_MAJOR=$(node -v | sed 's/v//' | cut -d. -f1)
    if [ "$NODE_MAJOR" -ge 20 ]; then
        echo "      Node.js $(node -v) sudah terpasang dan memenuhi syarat."
        NEED_NODE_INSTALL=false
    else
        echo "      Node.js versi lama terdeteksi ($(node -v)). Diperlukan Node.js >= 20."
    fi
fi

case "$PKG_MANAGER" in
    apt)
        $SUDO apt-get update -y
        $SUDO apt-get install -y curl ca-certificates gnupg git
        if [ "$NEED_NODE_INSTALL" = true ]; then
            echo "      Memasang Node.js 20 LTS dari NodeSource..."
            curl -fsSL https://deb.nodesource.com/setup_20.x | $SUDO -E bash -
            $SUDO apt-get install -y nodejs
        fi
        echo "      Memasang Xvfb & pustaka Chromium..."
        $SUDO apt-get install -y \
            xvfb \
            fonts-liberation \
            fonts-noto-color-emoji \
            ca-certificates \
            libasound2 \
            libatk-bridge2.0-0 \
            libatk1.0-0 \
            libc6 \
            libcairo2 \
            libcups2 \
            libdbus-1-3 \
            libdrm2 \
            libgbm1 \
            libglib2.0-0 \
            libgtk-3-0 \
            libnspr4 \
            libnss3 \
            libpango-1.0-0 \
            libx11-6 \
            libxcb1 \
            libxcomposite1 \
            libxdamage1 \
            libxext6 \
            libxfixes3 \
            libxrandr2 \
            libxss1 || true
        # Coba pasang paket browser chromium
        $SUDO apt-get install -y chromium-browser 2>/dev/null || $SUDO apt-get install -y chromium 2>/dev/null || true
        ;;

    dnf|yum)
        $SUDO $PKG_MANAGER install -y curl ca-certificates git
        # Pasang EPEL jika di CentOS / RHEL / Rocky / Alma
        $SUDO $PKG_MANAGER install -y epel-release 2>/dev/null || true
        if [ "$NEED_NODE_INSTALL" = true ]; then
            echo "      Memasang Node.js 20 LTS..."
            curl -fsSL https://rpm.nodesource.com/setup_20.x | $SUDO bash -
            $SUDO $PKG_MANAGER install -y nodejs
        fi
        echo "      Memasang Xvfb & Chromium..."
        $SUDO $PKG_MANAGER install -y xorg-x11-server-Xvfb chromium fontconfig freetype liberation-fonts ca-certificates || true
        ;;

    pacman)
        $SUDO pacman -Sy --noconfirm
        $SUDO pacman -S --noconfirm --needed curl ca-certificates git
        if [ "$NEED_NODE_INSTALL" = true ]; then
            echo "      Memasang Node.js & npm..."
            $SUDO pacman -S --noconfirm --needed nodejs npm
        fi
        echo "      Memasang Xvfb & Chromium..."
        $SUDO pacman -S --noconfirm --needed xorg-server-xvfb chromium fontconfig
        ;;

    apk)
        $SUDO apk update
        $SUDO apk add --no-cache curl ca-certificates git
        if [ "$NEED_NODE_INSTALL" = true ]; then
            echo "      Memasang Node.js & npm..."
            $SUDO apk add --no-cache nodejs npm
        fi
        echo "      Memasang Xvfb & Chromium..."
        $SUDO apk add --no-cache xvfb chromium nss freetype harfbuzz fontconfig
        ;;

    zypper)
        $SUDO zypper refresh
        $SUDO zypper in -y curl ca-certificates git
        if [ "$NEED_NODE_INSTALL" = true ]; then
            echo "      Memasang Node.js & npm..."
            $SUDO zypper in -y nodejs20 npm20 || $SUDO zypper in -y nodejs npm
        fi
        echo "      Memasang Xvfb & Chromium..."
        $SUDO zypper in -y xorg-x11-server-extra chromium fontconfig || true
        ;;
esac

# 4. Siapkan File Konfigurasi .env
echo "[3/4] Menyiapkan file konfigurasi lingkungan..."
if [ ! -f "$BACKEND_DIR/.env" ] && [ -f "$BACKEND_DIR/.env.example" ]; then
    cp "$BACKEND_DIR/.env.example" "$BACKEND_DIR/.env"
    echo "      Berhasil membuat '$BACKEND_DIR/.env' dari template."
fi

if [ -d "$SCRIPT_DIR/Backend API" ] && [ ! -f "$SCRIPT_DIR/.env" ] && [ -f "$SCRIPT_DIR/.env.example" ]; then
    cp "$SCRIPT_DIR/.env.example" "$SCRIPT_DIR/.env"
fi

# 5. Pasang Dependensi Node.js & Kompilasi TypeScript
echo "[4/4] Menginstall paket npm & mengompilasi TypeScript..."
cd "$BACKEND_DIR"
npm install
npm run build

# Jadikan skrip runner dapat dieksekusi
if [ -f "$SCRIPT_DIR/run-server.sh" ]; then
    chmod +x "$SCRIPT_DIR/run-server.sh"
fi
if [ -f "$SCRIPT_DIR/install-linux.sh" ]; then
    chmod +x "$SCRIPT_DIR/install-linux.sh"
fi

echo ""
echo "======================================================================"
echo "          PEMASANGAN BERHASIL DILAKUKAN DENGAN SUKSES! 🎉"
echo "======================================================================"
echo "Pilihan cara menjalankan IDLIX Backend Server:"
echo ""
echo "1. Jalankan langsung di terminal:"
echo "   ./run-server.sh"
echo ""
echo "2. Jalankan otomatis 24/7 di latar belakang (Systemd Service):"
echo "   sudo cp tools/idlix-backend.service /etc/systemd/system/"
echo "   sudo systemctl daemon-reload"
echo "   sudo systemctl enable --now idlix-backend"
echo ""
echo "3. Jalankan melalui Docker Container (Universal):"
echo "   docker compose up -d"
echo ""
echo "4. Jalankan via Process Manager 2 (PM2):"
echo "   sudo npm install -g pm2"
echo "   pm2 start 'Backend API/ecosystem.config.cjs'"
echo "   pm2 save && pm2 startup"
echo "======================================================================"
