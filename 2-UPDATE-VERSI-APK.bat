@echo off
chcp 65001 >nul
cls
title IDLIX Player - Auto Push Update & Release
color 0B

echo ======================================================================
echo          IDLIX PLAYER - AUTO UPDATE PUBLISHER & NOTIFICATION
echo               Repository: Novantama/IDLIX-Player
echo ======================================================================
echo.

powershell -ExecutionPolicy Bypass -File "%~dp0scripts\auto_release_update.ps1"

pause
