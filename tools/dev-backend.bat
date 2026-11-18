@echo off
title Idlix Backend API Server (Development Mode)
cd /d "%~dp0..\Backend API"

for /f "tokens=*" %%a in ('node -e "const os = require('os'); const nets = os.networkInterfaces(); for (const name of Object.keys(nets)) { for (const net of nets[name]) { if (net.family === 'IPv4' && !net.internal && !net.address.startsWith('169.254')) { console.log(net.address); process.exit(0); } } } console.log('127.0.0.1');"') do set "LAN_IP=%%a"

echo ======================================================================
echo             IDLIX BACKEND API SERVER (DEVELOPMENT MODE)
echo ======================================================================
echo  Direktori      : %CD%
echo  Akses Laptop   : http://localhost:5376
echo  Akses TV / HP  : http://%LAN_IP%:5376
echo ======================================================================
echo  CATATAN:
echo  1. Pastikan Laptop dan Android TV / HP terhubung ke Wi-Fi yang sama.
echo  2. Masukkan URL 'http://%LAN_IP%:5376' pada aplikasi Android TV / HP.
echo ======================================================================
echo Sedang menyalakan server dalam mode development...
echo.

npm.cmd run dev
pause
