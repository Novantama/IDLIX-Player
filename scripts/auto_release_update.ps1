param(
    [string]$MobileVersion = "",
    [string]$TvVersion = "",
    [string]$Changelog = ""
)

$ErrorActionPreference = "Stop"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "       IDLIX PLAYER - AUTO PUSH UPDATE & CI/CD RELEASE SCRIPT         " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

# 1. Parse current versions
$releasesContent = Get-Content "releases.ini" -Raw
$currentMobile = [regex]::Match($releasesContent, "(?m)\[Mobile\][\s\S]*?Version=([^\r\n]+)").Groups[1].Value
$currentTv = [regex]::Match($releasesContent, "(?m)\[TV\][\s\S]*?Version=([^\r\n]+)").Groups[1].Value

Write-Host "Versi Saat Ini: Mobile = $currentMobile | TV = $currentTv" -ForegroundColor Yellow

if ([string]::IsNullOrWhiteSpace($MobileVersion)) {
    $MobileVersion = Read-Host "Masukkan Versi Baru Mobile HP (tekan Enter untuk tetap $currentMobile)"
    if ([string]::IsNullOrWhiteSpace($MobileVersion)) { $MobileVersion = $currentMobile }
}

if ([string]::IsNullOrWhiteSpace($TvVersion)) {
    $TvVersion = Read-Host "Masukkan Versi Baru Android TV (tekan Enter untuk tetap $currentTv)"
    if ([string]::IsNullOrWhiteSpace($TvVersion)) { $TvVersion = $currentTv }
}

if ([string]::IsNullOrWhiteSpace($Changelog)) {
    $Changelog = Read-Host "Catatan Perubahan / Changelog (opsional)"
    if ([string]::IsNullOrWhiteSpace($Changelog)) { $Changelog = "Pembaruan rutin server IDLIX dan peningkatan stabilitas pemutar video." }
}

# 2. Update releases.ini
$ini = Get-Content 'releases.ini' -Raw
if ($TvVersion -ne '') {
    $ini = [regex]::Replace($ini, "(?m)^Version=.*?(?=\r?\nLink=.*Android_TV)", "Version=$TvVersion")
}
if ($MobileVersion -ne '') {
    $ini = [regex]::Replace($ini, "(?m)^Version=.*?(?=\r?\nLink=.*Mobile_HP)", "Version=$MobileVersion")
}
if ($Changelog -ne '') {
    $ini = [regex]::Replace($ini, "(?m)^Changelog=.*", "Changelog=$Changelog")
}
Set-Content -Path 'releases.ini' -Value $ini -NoNewline
Write-Host "[✓] releases.ini diperbarui: Mobile v$MobileVersion, TV v$TvVersion" -ForegroundColor Green

# 3. Update build.gradle.kts
if ($MobileVersion -ne '') {
    $mobGradle = Get-Content 'app-mobile/build.gradle.kts' -Raw
    $curMobCode = [int][regex]::Match($mobGradle, "versionCode\s*=\s*(\d+)").Groups[1].Value
    $newMobCode = $curMobCode + 1
    $mobGradle = [regex]::Replace($mobGradle, "versionCode\s*=\s*\d+", "versionCode = $newMobCode")
    $mobGradle = [regex]::Replace($mobGradle, 'versionName\s*=\s*"[^"]+"', "versionName = `"$MobileVersion`"")
    Set-Content -Path 'app-mobile/build.gradle.kts' -Value $mobGradle -NoNewline
    Write-Host "[✓] app-mobile/build.gradle.kts: versionCode=$newMobCode, versionName=$MobileVersion" -ForegroundColor Green
}

if ($TvVersion -ne '') {
    $tvGradle = Get-Content 'app-tv/build.gradle.kts' -Raw
    $curTvCode = [int][regex]::Match($tvGradle, "versionCode\s*=\s*(\d+)").Groups[1].Value
    $newTvCode = $curTvCode + 1
    $tvGradle = [regex]::Replace($tvGradle, "versionCode\s*=\s*\d+", "versionCode = $newTvCode")
    $tvGradle = [regex]::Replace($tvGradle, 'versionName\s*=\s*"[^"]+"', "versionName = `"$TvVersion`"")
    Set-Content -Path 'app-tv/build.gradle.kts' -Value $tvGradle -NoNewline
    Write-Host "[✓] app-tv/build.gradle.kts: versionCode=$newTvCode, versionName=$TvVersion" -ForegroundColor Green
}

# 4. Compile APKs locally using Gradle and local Android SDK
Write-Host "Sedang mengompilasi APK secara LOKAL di laptop via Gradle..." -ForegroundColor Cyan
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.13.102-hotspot"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

& .\gradlew.bat assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host "[X] Gagal mengompilasi APK secara lokal!" -ForegroundColor Red
    exit 1
}

# 5. Move locally compiled APKs to installer folder
Write-Host "[✓] Kompilasi lokal berhasil! Menyalin file APK..." -ForegroundColor Green
Copy-Item "app-mobile\build\outputs\apk\debug\app-mobile-debug.apk" "APK_Installers\Mobile_HP\IDLIX-Mobile-Modified (Auto-Detect).apk" -Force
Copy-Item "app-tv\build\outputs\apk\debug\app-tv-debug.apk" "APK_Installers\Android_TV\IDLIX-TV-Modified (Auto-Detect).apk" -Force

# 6. Commit version bump and push source code
git add releases.ini app-mobile/build.gradle.kts app-tv/build.gradle.kts
git commit -m "chore(release): bump app version to Mobile v$MobileVersion, TV v$TvVersion"
git push origin main

# 7. Upload APK binaries to GitHub Releases
Write-Host "Sedang mengunggah APK ke GitHub Releases (v$MobileVersion)..." -ForegroundColor Cyan
gh release create "v$MobileVersion" `
    "APK_Installers\Mobile_HP\IDLIX-Mobile-Modified (Auto-Detect).apk" `
    "APK_Installers\Android_TV\IDLIX-TV-Modified (Auto-Detect).apk" `
    --title "IDLIX Player v$MobileVersion" `
    --notes "$Changelog" `
    --latest

Write-Host "======================================================================" -ForegroundColor Green
Write-Host "[BERHASIL] Seluruh proses Auto Push Update selesai!" -ForegroundColor Green
Write-Host "Semua perangkat HP & Android TV akan langsung menerima dialog update!" -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Green
