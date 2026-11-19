@echo off
chcp 65001 >nul
cls
title IDLIX Player - Tool Decompile & Recompile APK
color 0E

echo ======================================================================
echo          IDLIX PLAYER - APK DECOMPILER ^& RECOMPILER TOOL
echo ======================================================================
echo.
echo Pilih opsi yang ingin Anda lakukan:
echo.
echo [1] Decompile APK Mobile (dari folder APK_Installers/Mobile_HP)
echo [2] Decompile APK Android TV (dari folder APK_Installers/Android_TV)
echo [3] Decompile APK Kustom (Ketik atau Seret/Drag file APK ke sini)
echo [4] Recompile Folder Decompiled kembali menjadi APK
echo [5] Keluar
echo.
set /p opt="Masukkan pilihan [1-5]: "

if "%opt%"=="1" (
    set "APK_TARGET=%~dp0APK_Installers\Mobile_HP\IDLIX-Mobile-Modified (Auto-Detect).apk"
    set "OUT_DIR=%~dp0decompiled_mobile"
    goto do_decompile
)
if "%opt%"=="2" (
    set "APK_TARGET=%~dp0APK_Installers\Android_TV\IDLIX-TV-Modified (Auto-Detect).apk"
    set "OUT_DIR=%~dp0decompiled_tv"
    goto do_decompile
)
if "%opt%"=="3" (
    echo.
    set /p "APK_TARGET=Seret file APK atau ketik path file APK: "
    set "OUT_DIR=%~dp0decompiled_custom"
    goto do_decompile
)
if "%opt%"=="4" (
    goto do_recompile
)
if "%opt%"=="5" exit /b 0

:do_decompile
echo.
if not exist "%APK_TARGET%" (
    echo [ERROR] File APK tidak ditemukan di: %APK_TARGET%
    pause
    exit /b 1
)
echo Memproses decompile file: %APK_TARGET%
echo Folder Output: %OUT_DIR%
echo.
call "%~dp0tools\apktool\apktool.bat" d -f "%APK_TARGET%" -o "%OUT_DIR%"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ======================================================================
    echo [BERHASIL] File APK berhasil di-decompile ke folder:
    echo %OUT_DIR%
    echo ======================================================================
    explorer "%OUT_DIR%"
) else (
    echo.
    echo [ERROR] Proses decompile gagal!
)
pause
exit /b

:do_recompile
echo.
set /p "REC_DIR=Masukkan path folder yang telah didecompile (contoh: decompiled_mobile): "
if not exist "%REC_DIR%" (
    echo [ERROR] Folder tidak ditemukan: %REC_DIR%
    pause
    exit /b 1
)
echo.
echo Sedang me-recompile folder menjadi file APK baru...
call "%~dp0tools\apktool\apktool.bat" b "%REC_DIR%" -o "%REC_DIR%_recompiled.apk"
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ======================================================================
    echo [BERHASIL] Recompile selesai! File APK tersimpan di:
    echo %REC_DIR%_recompiled.apk
    echo ======================================================================
) else (
    echo.
    echo [ERROR] Recompile gagal!
)
pause
exit /b
