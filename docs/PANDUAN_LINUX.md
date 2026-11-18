# 🐧 Panduan Instalasi & Deployment IDLIX Player di Linux

Panduan ini menjelaskan cara memasang dan menjalankan **IDLIX Backend Media Gateway & Streaming Server** di berbagai jenis distribusi Linux, baik pada **komputer desktop/laptop**, **Mini PC / TV Box (Armbian/Raspberry Pi)**, maupun **Server VPS (Cloud)**.

---

## 📋 Daftar Isi
1. [Distribusi Linux yang Didukung](#1-distribusi-linux-yang-didukung)
2. [Metode 1: Instalasi Otomatis (Skrip Universal)](#2-metode-1-instalasi-otomatis-skrip-universal)
3. [Metode 2: Menggunakan Docker & Docker Compose (Paling Direkomendasikan)](#3-metode-2-menggunakan-docker--docker-compose-paling-direkomendasikan)
4. [Metode 3: Berjalan 24/7 di VPS / Server (Systemd Service)](#4-metode-3-berjalan-247-di-vps--server-systemd-service)
5. [Metode 4: Berjalan via PM2 Process Manager](#5-metode-4-berjalan-via-pm2-process-manager)
6. [Pengaturan Firewall (Port 5376)](#6-pengaturan-firewall-port-5376)
7. [Menghubungkan Aplikasi TV & HP](#7-menghubungkan-aplikasi-tv--hp)
8. [Troubleshooting Linux](#8-troubleshooting-linux)

---

## 1. Distribusi Linux yang Didukung

Backend IDLIX dirancang untuk portabilitas tinggi di seluruh ekosistem Linux:

| Keluarga Distro | Distribusi yang Didukung | Package Manager | Status |
| :--- | :--- | :---: | :---: |
| **Debian / Ubuntu** | Ubuntu 20.04+, Debian 11+, Linux Mint, Pop!_OS, Armbian, Raspberry Pi OS | `apt` | ![Supported](https://img.shields.io/badge/Status-Verified-success) |
| **RHEL / Fedora** | Fedora 38+, CentOS Stream 9, Rocky Linux 9, AlmaLinux 9, RHEL 8/9 | `dnf` / `yum` | ![Supported](https://img.shields.io/badge/Status-Verified-success) |
| **Arch Linux** | Arch Linux, Manjaro, EndeavourOS | `pacman` | ![Supported](https://img.shields.io/badge/Status-Verified-success) |
| **Alpine Linux** | Alpine Linux v3.18+ (Container / Edge Host) | `apk` | ![Supported](https://img.shields.io/badge/Status-Verified-success) |
| **SUSE** | openSUSE Leap 15+, Tumbleweed, SLES | `zypper` | ![Supported](https://img.shields.io/badge/Status-Verified-success) |
| **Container** | Segala distro yang memiliki Docker Engine | `docker` | ![Supported](https://img.shields.io/badge/Status-Universal-brightgreen) |

---

## 2. Metode 1: Instalasi Otomatis (Skrip Universal)

Skrip [`install-linux.sh`](../install-linux.sh) secara otomatis mendeteksi package manager Linux Anda, menginstall Node.js 20+ LTS, memasang dependensi Chromium & virtual display Xvfb, menyiapkan konfigurasi `.env`, dan mengompilasi TypeScript.

### Langkah-Langkah:
```bash
# 1. Masuk ke direktori repository
cd /path/ke/IDLIX

# 2. Berikan izin eksekusi pada skrip
chmod +x install-linux.sh run-server.sh

# 3. Jalankan skrip installer otomatis
./install-linux.sh
```

### Menjalankan Server:
Setelah instalasi selesai, Anda dapat menjalankan server kapan saja dengan:
```bash
./run-server.sh
```

Terminal akan menampilkan IP lokal yang dapat Anda gunakan di Android TV dan HP:
```text
======================================================================
 Akses Komputer (Dashboard) : http://localhost:5376
 Akses TV / HP (Jaringan)   : http://192.168.1.150:5376
======================================================================
```

---

## 3. Metode 2: Menggunakan Docker & Docker Compose (Paling Direkomendasikan)

Jika server atau laptop Anda telah memiliki **Docker** dan **Docker Compose**, metode ini adalah yang paling bersih dan universal karena seluruh pustaka sistem dan lingkungan browser terisolasi dalam container.

### Menjalankan:
```bash
# Di direktori utama repository:
docker compose up -d
```

### Memeriksa Status & Log:
```bash
# Melihat log aktif
docker compose logs -f

# Memeriksa status container
docker compose ps
```

### Mematikan Server:
```bash
docker compose down
```

> 💡 *Catatan:* File konfigurasi [`.env`](../Backend%20API/.env.example) akan otomatis dibaca oleh Docker Compose jika Anda ingin mengubah port atau URL upstream.

---

## 4. Metode 3: Berjalan 24/7 di VPS / Server (Systemd Service)

Untuk server VPS atau Mini PC rumahan yang menyala terus-menerus dan membutuhkan server IDLIX menyala otomatis saat booting:

### 1. Salin File Service Template
```bash
sudo cp tools/idlix-backend.service /etc/systemd/system/idlix-backend.service
```

### 2. Sesuaikan Path Direktori
Buka file service tersebut:
```bash
sudo nano /etc/systemd/system/idlix-backend.service
```
Pastikan `WorkingDirectory` menunjuk ke path absolut folder `Backend API` di server Anda, contoh:
```ini
WorkingDirectory=/var/www/IDLIX/Backend API
ExecStart=/usr/bin/node dist/server.js
```

### 3. Aktifkan & Jalankan Service
```bash
# Reload systemd configuration
sudo systemctl daemon-reload

# Aktifkan agar jalan otomatis saat boot
sudo systemctl enable idlix-backend

# Jalankan service sekarang
sudo systemctl start idlix-backend
```

### 4. Perintah Manajemen Service
```bash
# Cek status berjalan
sudo systemctl status idlix-backend

# Lihat log real-time
sudo journalctl -u idlix-backend -f

# Restart service
sudo systemctl restart idlix-backend

# Hentikan service
sudo systemctl stop idlix-backend
```

---

## 5. Metode 4: Berjalan via PM2 Process Manager

Jika Anda terbiasa menggunakan **PM2** di Linux / NodeJS:

```bash
# 1. Pasang PM2 secara global jika belum ada
sudo npm install -g pm2

# 2. Masuk ke folder Backend API
cd "Backend API"

# 3. Jalankan melalui file konfigurasi ecosystem yang sudah disediakan
pm2 start ecosystem.config.cjs

# 4. Simpan agar berjalan saat server reboot
pm2 save
pm2 startup
```

---

## 6. Pengaturan Firewall (Port 5376)

Agar perangkat Android TV dan HP di jaringan lokal atau internet dapat terhubung ke server backend, pastikan port `5376` diizinkan pada firewall server Anda:

### Ubuntu / Debian (UFW):
```bash
sudo ufw allow 5376/tcp comment 'IDLIX Backend Gateway'
sudo ufw reload
```

### CentOS / RHEL / Rocky / Fedora (Firewalld):
```bash
sudo firewall-cmd --add-port=5376/tcp --permanent
sudo firewall-cmd --reload
```

### Iptables:
```bash
sudo iptables -A INPUT -p tcp --dport 5376 -j ACCEPT
```

---

## 7. Menghubungkan Aplikasi TV & HP

1. **Jaringan Lokal (Wi-Fi Rumah / Kantor)**:
   - Pastikan perangkat TV/HP terhubung ke router Wi-Fi yang sama dengan komputer/server.
   - Buka aplikasi **IDLIX di Android TV / HP**.
   - Masuk ke menu **Pengaturan (Settings)** ⚙️.
   - Klik **"Auto-Detect Server"** (aplikasi akan mendeteksi server di subnet secara otomatis) atau masukkan IP manual seperti `http://192.168.1.150:5376`.

2. **Server Cloud / VPS Publik**:
   - Jika backend dijalankan di VPS dengan IP publik atau domain (contoh: `http://vps-anda.com:5376`), cukup masukkan alamat tersebut ke menu **Pengaturan -> Alamat Server** di aplikasi Android TV atau HP Anda.

---

## 8. Troubleshooting Linux

### Q1: Error "Cannot find module 'xvfb'" atau gagal membuka virtual display?
**Solusi**: Pastikan paket `xvfb` sudah terpasang di sistem operasi Anda:
* Ubuntu/Debian: `sudo apt install -y xvfb`
* CentOS/RHEL: `sudo dnf install -y xorg-x11-server-Xvfb`
* Arch Linux: `sudo pacman -S xorg-server-xvfb`
* Alpine: `sudo apk add xvfb`

### Q2: Error "error while loading shared libraries: libnss3.so"?
**Solusi**: Pasang pustaka Chromium runtime yang diperlukan:
* Ubuntu/Debian: `sudo apt install -y libnss3 libatk1.0-0 libcups2 libdrm2 libgbm1 libasound2`
* CentOS/RHEL: `sudo dnf install -y nss cups-libs libdrm mesa-libgbm alsa-lib`

### Q3: Port 5376 sudah digunakan aplikasi lain?
**Solusi**:
Jalankan skrip `./run-server.sh` (ia akan otomatis menghentikan proses lama di port 5376), atau Anda dapat mengganti port di file `.env`:
```ini
PORT=5380
```
Lalu hubungkan aplikasi TV/HP ke port yang baru (`http://IP_SERVER:5380`).
