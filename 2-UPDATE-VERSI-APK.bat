@echo off
chcp 65001 >nul
cls
title IDLIX Player - Update Versi & Notifikasi Auto-Update
color 0B

echo ======================================================================
echo          IDLIX PLAYER - AUTO UPDATE PUBLISHER ^& NOTIFICATION
echo               Repository: claudiaaulerie/IDLIX-Player
echo ======================================================================
echo.
echo Panduan:
echo Setiap kali Anda memperbarui APK di folder APK_Installers atau ingin
echo menaikkan versi agar semua Android TV ^& HP otomatis memunculkan pop-up
echo notifikasi "Update Tersedia: Pasang Sekarang", jalankan script ini.
echo.
echo ======================================================================
echo Konfigurasi Saat Ini (releases.ini):
type releases.ini
echo ======================================================================
echo.

set /p TV_VER="Masukkan Versi Baru Android TV (kosongkan jika tidak diubah): "
set /p MOB_VER="Masukkan Versi Baru Mobile HP (kosongkan jika tidak diubah): "
set /p NOTE="Catatan Perubahan / Changelog (opsional): "

if "%NOTE%"=="" set NOTE=Pembaruan server IDLIX dan perbaikan performa.

powershell -Command ^
  "$ini = Get-Content 'releases.ini' -Raw;" ^
  "if ('%TV_VER%' -ne '') { $ini = $ini -replace '(?m)^Version=.*?(?=\r?\nLink=.*Android_TV)', 'Version=%TV_VER%'; }" ^
  "if ('%MOB_VER%' -ne '') { $ini = $ini -replace '(?m)^Version=.*?(?=\r?\nLink=.*Mobile_HP)', 'Version=%MOB_VER%'; }" ^
  "if ('%NOTE%' -ne '') { $ini = $ini -replace '(?m)^Changelog=.*', 'Changelog=%NOTE%'; }" ^
  "Set-Content -Path 'releases.ini' -Value $ini -NoNewline;"

echo.
echo [✓] releases.ini berhasil diperbarui!
echo.
echo Sedang melakukan sinkronisasi ke GitHub claudiaaulerie/IDLIX-Player...

set GIT_PATH="C:\Users\USER\AppData\Local\MinGit\cmd\git.exe"
if not exist %GIT_PATH% set GIT_PATH=git

%GIT_PATH% add releases.ini app-tv/build.gradle.kts app-mobile/build.gradle.kts APK_Installers/
%GIT_PATH% commit -m "chore(release): bump app version and update releases.ini"
%GIT_PATH% push origin main

echo.
echo ======================================================================
echo [BERHASIL] Pembaruan telah dipublikasikan ke GitHub dan Folder Lokal!
echo.
echo Ketika pengguna membuka aplikasi di TV atau HP:
echo  1. Aplikasi otomatis mendeteksi versi baru di repository Anda.
echo  2. Muncul notifikasi pop-up 'Pembaruan Tersedia: Pasang Sekarang'.
echo  3. Aplikasi mengunduh APK langsung dan siap dipasang dalam 1 klik!
echo ======================================================================
echo.
pause
