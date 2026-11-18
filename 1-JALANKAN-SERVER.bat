@echo off
title IDLIX Backend API Server
cd /d "%~dp0Backend API"

echo ======================================================================
echo                      IDLIX BACKEND API SERVER
echo             Platform Streaming Native Android TV ^& Mobile
echo ======================================================================

where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Node.js belum terpasang di komputer Anda!
    echo Silakan download dan install Node.js dari https://nodejs.org/
    pause
    exit /b 1
)

echo [1/3] Memeriksa ketersediaan port 5376...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":5376" ^| findstr "LISTENING"') do (
    echo     Port 5376 sedang digunakan PID %%a, mematikan proses lama...
    taskkill /F /PID %%a >nul 2>nul
)

if not exist .env (
    if exist .env.example (
        echo [INFO] Menyiapkan file konfigurasi lokal .env...
        copy .env.example .env >nul
    )
)

if not exist node_modules (
    echo [2/3] Menginstall dependensi: npm install...
    call npm.cmd install
)

echo [2/3] Mengompilasi TypeScript: npm run build...
call npm.cmd run build

for /f "tokens=*" %%a in ('node -e "const os = require('os'); const nets = os.networkInterfaces(); for (const name of Object.keys(nets)) { for (const net of nets[name]) { if (net.family === 'IPv4' && !net.internal && !net.address.startsWith('169.254')) { console.log(net.address); process.exit(0); } } } console.log('127.0.0.1');"') do set "LAN_IP=%%a"

echo.
echo ======================================================================
echo  Akses Laptop (Dashboard) : http://localhost:5376
echo  Akses TV / HP            : http://%LAN_IP%:5376
echo ======================================================================
echo  PETUNJUK KONEKSI:
echo  1. Pastikan Laptop dan Android TV / HP terhubung ke Wi-Fi yang sama.
echo  2. Buka aplikasi di TV/HP -^> Pengaturan -^> Klik "Auto-Detect Server".
echo  3. Browser akan otomatis membuka Dashboard di http://localhost:5376
echo ======================================================================
echo [3/3] Menyalakan server...
echo.

node dist/server.js
pause
