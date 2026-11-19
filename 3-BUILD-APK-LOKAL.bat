@echo off
chcp 65001 >nul
cls
title IDLIX Player - Kompilasi APK Lokal
color 0A

echo ======================================================================
echo             IDLIX PLAYER - KOMPILASI APK LOKAL (GRADLE)
echo ======================================================================
echo.
echo Sedang menyiapkan environment Java 21 dan Android SDK...
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.13.102-hotspot"
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"

echo.
echo Memulai kompilasi APK Mobile dan Android TV...
call "%~dp0gradlew.bat" assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Kompilasi gagal! Periksa pesan error di atas.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Menyalin file APK ke folder APK_Installers...
copy /Y "%~dp0app-mobile\build\outputs\apk\debug\app-mobile-debug.apk" "%~dp0APK_Installers\Mobile_HP\IDLIX-Mobile-Modified (Auto-Detect).apk" >nul
copy /Y "%~dp0app-tv\build\outputs\apk\debug\app-tv-debug.apk" "%~dp0APK_Installers\Android_TV\IDLIX-TV-Modified (Auto-Detect).apk" >nul

echo.
echo ======================================================================
echo [BERHASIL] Kompilasi selesai 100%% di laptop ini!
echo.
echo APK Mobile: APK_Installers\Mobile_HP\IDLIX-Mobile-Modified (Auto-Detect).apk
echo APK TV    : APK_Installers\Android_TV\IDLIX-TV-Modified (Auto-Detect).apk
echo ======================================================================
echo.
pause
