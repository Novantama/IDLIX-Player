@echo off
chcp 65001 >nul
cls
title IDLIX Player - Auto Push Update & Release
color 0B

echo ======================================================================
echo          IDLIX PLAYER - AUTO UPDATE PUBLISHER & NOTIFICATION
echo               Repository: claudiaaulerie/IDLIX-Player
echo ======================================================================
echo.

set "GH_TOKEN="
powershell -ExecutionPolicy Bypass -File "%~dp0scripts\auto_release_update.ps1"

pause
