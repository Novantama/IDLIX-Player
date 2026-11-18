# IDLIX Player: Android TV & Android Mobile 🎬📺📱

[![Repository](https://img.shields.io/badge/GitHub-claudiaaulerie%2FIDLIX--Player-blue?logo=github)](https://github.com/claudiaaulerie/IDLIX-Player)
[![Platform](https://img.shields.io/badge/Platform-Android%20TV%20%7C%20Android%20Mobile-green?logo=android)](https://github.com/claudiaaulerie/IDLIX-Player)
[![Architecture](https://img.shields.io/badge/Architecture-Multi--Module%20Clean%20Kotlin-brightgreen)](https://github.com/claudiaaulerie/IDLIX-Player)
[![Backend](https://img.shields.io/badge/Backend-Fastify%20TypeScript%20%2B%20Stealth-orange)](https://github.com/claudiaaulerie/IDLIX-Player)

---

### 📑 Daftar Isi
* [Tentang Proyek](#tentang-proyek)
* [Fitur Utama (Key Features)](#fitur-utama)
  * [Fitur Unggulan Baru (New Features)](#fitur-unggulan-baru)
  * [Android Mobile Client (`app-mobile`)](#fitur-mobile)
  * [Android TV Client (`app-tv`)](#fitur-tv)
  * [Backend API & Web Player Service](#fitur-backend)
* [Struktur Direktori (Project Structure)](#struktur-direktori)
* [Download Installer APK](#download-apk)
* [Panduan Penggunaan Cepat](#panduan-cepat)
* [Panduan Kompilasi & Build APK](#panduan-build)
* [Frequently Asked Questions (FAQ)](#faq)
* [Keamanan, Lisensi & Pembuat](#keamanan-lisensi)

---

<a id="tentang-proyek"></a>
## 💡 Tentang Proyek

**IDLIX Player** adalah platform streaming native Android modern berkinerja tinggi untuk film dan serial TV dari IDLIX. Mendukung ekosistem layar ganda (**Android TV / Google TV** dan **Android Mobile Smartphone/Tablet**) yang dibangun menggunakan arsitektur **Clean Multi-Module Kotlin & Jetpack Compose**.

Proyek ini terbagi menjadi modul-modul independen yang terstruktur:
1. **Shared Core Data (`core/data/`)**: Modul pustaka Kotlin murni tanpa dependensi UI yang mengelola Retrofit client, model data, deserializer toleran, repository, penyimpanan riwayat tontonan lokal (`WatchHistoryStore`), serta utilitas *Auto-Detect Subnet Scanner*.
2. **Android TV Client (`app-tv/`)**: Aplikasi khusus Android TV/Google TV berbasis Jetpack Compose for TV (`androidx.tv.material3`) dan Media3 ExoPlayer dengan antarmuka Leanback (remote D-Pad navigation, 10-foot UI, fixed landscape).
3. **Android Mobile Client (`app-mobile/`)**: Aplikasi khusus smartphone & tablet berbasis Jetpack Compose **Material 3** modern (Bottom Navigation Bar, Swipeable Banner Pager, Inline-to-Fullscreen Player, Touch Gestures, dan **Picture-in-Picture**).
4. **Backend API Scraper & Web Player (`Backend API/`)**: Service Fastify TypeScript dengan headless stealth browser untuk bypass Cloudflare Turnstile, pelacakan redirect domain dinamis, scraping katalog, resolusi one-shot token HLS stream, dan antarmuka **Web Player bawaan**.

---

<a id="fitur-utama"></a>
## 🌟 Fitur Utama (Key Features)

<a id="fitur-unggulan-baru"></a>
### ✨ Fitur Unggulan Terbaru
* 🔍 **Auto-Detect Server (1-Click Discovery)**:
  * Tombol **"Auto-Detect Server"** di menu Pengaturan aplikasi Android TV dan Mobile.
  * Memindai subnet Wi-Fi lokal secara paralel (`1..254`) pada port `5376` (`/health`) dan langsung mengisi serta menyimpan alamat IP backend laptop/server secara otomatis.
* 🌐 **Built-in Web Player UI**:
  * Backend API kini dilengkapi web player modern yang dapat diakses langsung dari browser di laptop/PC (`http://localhost:5376` atau `http://localhost:5376/player`).
* 🔄 **Dynamic Domain Redirection Follower**:
  * Engine backend secara otomatis mendeteksi dan mengikuti perubahan domain upstream IDLIX (misal migrasi `z2.idlixku.com` ke `z3.idlixku.com`) tanpa memerlukan update manual pada kode.
* 🪟 **Silent Stealth Engine**:
  * Puppeteer browser berjalan di posisi off-screen sehingga tidak ada popup jendela browser yang mengganggu saat server aktif.
* 🚀 **1-Click Windows Launcher (`1-JALANKAN-SERVER.bat`)**:
  * Script praktis yang otomatis mendeteksi alamat IP lokal laptop/PC dan menampilkan petunjuk URL akses ke TV/HP.

<a id="fitur-mobile"></a>
### 📱 Android Mobile Client (`app-mobile`)
* **Material 3 & Edge-to-Edge Display**:
  * Tampilan immersive modern dengan handling `WindowInsets` bebas dari benturan notch / punch-hole kamera.
  * Navigasi intuitif **Bottom Navigation Bar** 5 tab: *Home*, *Movies*, *Series*, *Search*, dan *Library*.
* **Hero Carousel & Infinite Scrolling**:
  * Pager banner horizontal otomatis (`HorizontalPager`) dengan indikator titik dinamis dan tombol aksi cepat.
  * Barisan *Lanjutkan Menonton* (Continue Watching) dengan thumbnail 16:9 dan progress bar tontonan.
  * Grid vertikal 3 kolom (smartphone) dan 5 kolom (tablet) dengan infinite scroll pagination dan filter chips genre/sorting.
* **Dual-Mode Player (Inline ke Fullscreen)**:
  * **Mode Inline (Portrait 16:9)**: Video langsung berputar di atas layar sambil membaca sinopsis dan memilih episode serial di bawahnya.
  * **Mode Fullscreen (Landscape)**: Transisi mulus ke layar penuh immersive melalui tombol expand atau sensor rotasi.
  * **BackHandler Cerdas**: Menekan tombol Back saat fullscreen akan mengecilkan player kembali ke mode inline.
* **Touch Gestures & Media Controls**:
  * **Double-Tap**: Sisi kiri mundur 10 detik, sisi kanan maju 10 detik disertai animasi ripple.
  * **Vertical Drag**: Sisi kiri untuk kecerahan layar, sisi kanan untuk volume audio.
  * **Timeline Scrubber**: Scrub horizontal presisi untuk loncat ke menit tertentu.
* **Picture-in-Picture (PiP)**:
  * Otomatis masuk ke mode floating PiP saat pengguna menekan tombol Home atau beralih ke aplikasi lain (`onUserLeaveHint`).
* **Integrasi Subtitle SubDL**:
  * Pemilihan trek subtitle bawaan atau pencarian subtitle Bahasa Indonesia/Inggris via SubDL API.

<a id="fitur-tv"></a>
### 📺 Android TV Client (`app-tv`)
* **100% Native Architecture**: Bebas Chromium dan WebView di dalam APK. Menggunakan hardware decoder bawaan TV (`MediaCodec`) via AndroidX Media3 ExoPlayer.
* **Leanback 10-Foot UI**:
  * Floating navigation sidebar dengan smooth overlay (tanpa re-layout konten).
  * Billboard Hero dinamis dengan preview sinopsis dan tombol Putar Cepat.
  * Baris Swimlane horizontal untuk Tren, Populer, Serial TV, dan Kategori Genre.
  * Fitur *Lanjutkan Menonton* (Continue Watching) dengan bar progress visual.
* **Token Query Preserving HLS Player**:
  * Mengatasi proteksi CDN Majorplay (Cloudflare token authorization `?t=...`) secara otomatis pada sub-playlist dan chunk segmen video (`.webp`, `.jpg`, `.js`, dll.).
  * Buffer kontrol TV adaptif untuk pemutaran instan tanpa buffering.
* **Manajemen Subtitle & Integrasi SubDL**:
  * Subtitle internal IDLIX otomatis (Indonesian & English).
  * Pencarian dan pengunduhan subtitle alternatif langsung dari player menggunakan remote TV.
* **Menu Pengaturan Terintegrasi**:
  * Fitur **Auto-Detect Server** atau input manual URL Backend API dengan remote D-pad.
  * Pengaturan custom API Key SubDL & tombol Uji Koneksi real-time.

<a id="fitur-backend"></a>
### 🚀 Backend API & Web Player Service
* **Cloudflare Turnstile & Challenge Bypass**: Stealth engine otomatis menyelesaikan proteksi Cloudflare di sisi server.
* **In-Memory LRU Caching**: Respons data instan (<15ms) untuk katalog film, serial, dan kategori.
* **All-in-One Playback Resolver**: Endpoint `/api/playback/resolve` menyelesaikan siklus `gateToken`, countdown timer provider (15 detik), session claim, dan HLS URL redemption dalam satu panggilan.
* **Ringan & Hemat Resource**: Mendukung Docker dengan konsumsi memori rendah (<1GB RAM).

---

<a id="struktur-direktori"></a>
## 📁 Struktur Direktori (Project Structure)

Proyek ini telah diklasifikasikan dengan rapi ke dalam folder-folder terstruktur:

```
IDLIX Player/
├── 1-JALANKAN-SERVER.bat                # Script cepat 1-klik menjalankan Backend Server (Windows)
├── releases.ini                         # Konfigurasi update versi aplikasi
│
├── APK_Installers/                      # [Koleksi Installer APK Siap Pasang]
│   ├── Mobile_HP/
│   │   ├── IDLIX-Mobile-Modified (Auto-Detect).apk    # APK HP dengan fitur Auto-Detect Server (Rekomendasi)
│   │   └── IDLIX-Mobile-Original.apk                  # APK HP versi build standar
│   ├── Android_TV/
│   │   ├── IDLIX-TV-Modified (Auto-Detect).apk        # APK TV dengan fitur Auto-Detect Server (Rekomendasi)
│   │   └── IDLIX-TV-Original.apk                      # APK TV versi build standar
│   └── PANDUAN_INSTALL.txt              # Petunjuk lengkap pemilihan & instalasi file APK
│
├── Backend API/                         # Service Backend Scraper, Proxy & Web Player (Node.js/Fastify)
│   ├── src/
│   │   ├── engine/                      # Stealth Browser Engine & Dynamic Domain Redirection
│   │   ├── routes/                      # REST endpoints Fastify (/api/...)
│   │   └── services/                    # LRU Cache Service
│   ├── public/                          # Antarmuka Web Player bawaan (HTML5/HLS.js)
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── package.json
│
├── core/
│   ├── data/                            # [Shared Library] Model, Retrofit API, Subnet Scanner & DB
│   │   ├── src/main/java/com/idlix/data/
│   │   │   ├── api/                     # ApiClient (Universal & Auto-Detect Scanner), IdlixApiService
│   │   │   ├── local/                   # WatchHistoryStore (riwayat tontonan lokal)
│   │   │   ├── model/                   # MovieItem, SeriesItem, Subdl, Serializers
│   │   │   ├── update/                  # AppUpdateManager & ReleaseIniParser
│   │   │   └── repository/              # IdlixRepository & SubdlRepository
│   │   └── build.gradle.kts
│   └── player-ui/                       # [Shared UI Library] Subtitle Overlay, Cue Repositioner & Safe Bounds
│       └── build.gradle.kts
│
├── tools/                               # [Developer Tools] Script pendukung pengembang (dev mode)
│   └── dev-backend.bat
│
├── app-tv/                              # [Application] Modul Khusus Android TV / Google TV
│   ├── src/main/java/com/idlix/tv/
│   │   ├── ui/
│   │   │   ├── components/              # TvNavDrawer, TvMovieCard, TvHeroBillboard, dll.
│   │   │   ├── navigation/              # TV NavHost & routes
│   │   │   ├── screens/                 # Home, Movies, Series, Detail, Player, Settings (Auto-Detect)
│   │   │   └── theme/                   # Leanback Dark Theme
│   │   └── res/                         # Asset TV banner & strings localization
│   └── build.gradle.kts
│
├── app-mobile/                          # [Application] Modul Khusus Smartphone & Tablet
│   ├── src/main/java/com/idlix/mobile/
│   │   ├── ui/
│   │   │   ├── components/              # MobileMovieCard, HeroBannerPager, FilterChips, dll.
│   │   │   ├── navigation/              # Bottom Navigation Bar, NavHost, MainScaffold
│   │   │   ├── screens/                 # Home, Movies, Series, Search, Library, Detail, Settings
│   │   │   └── theme/                   # Material 3 Dark Theme & Edge-to-Edge styling
│   │   └── res/                         # Asset mobile & strings
│   └── build.gradle.kts
│
├── docs/                                # [Dokumentasi Lengkap]
│   ├── PANDUAN_INSTALASI_TV.md          # Panduan instalasi ke Android TV via Flashdisk / ADB
│   └── PORTING_TO_MOBILE_PLAN.md        # Rencana arsitektur multi-module & dokumentasi teknis
│
├── gradle/                              # Gradle wrapper & version catalogs (libs.versions.toml)
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

<a id="download-apk"></a>
## 📥 Download Installer APK

Installer APK yang telah dikompilasi tersedia langsung di:
1. **Folder Lokal Proyek**: Masuk ke folder [`APK_Installers/`](APK_Installers/)
   - Smartphone / Tablet: [`APK_Installers/Mobile_HP/`](APK_Installers/Mobile_HP/)
   - Smart TV / TV Box: [`APK_Installers/Android_TV/`](APK_Installers/Android_TV/)
2. **GitHub Releases**: Kunjungi halaman [Releases GitHub](https://github.com/claudiaaulerie/IDLIX-Player/releases).

---

<a id="panduan-cepat"></a>
## 🚀 Panduan Penggunaan Cepat

1. **Jalankan Backend API di Laptop/PC**:
   - Cukup klik dua kali file [`1-JALANKAN-SERVER.bat`](1-JALANKAN-SERVER.bat).
   - Server akan aktif dan menampilkan alamat IP lokal Anda (misalnya `http://192.168.1.163:5376`).
2. **Pastikan Terhubung ke Wi-Fi yang Sama**:
   - Hubungkan HP Android / Android TV ke jaringan Wi-Fi yang sama dengan Laptop.
3. **Buka Aplikasi IDLIX**:
   - Masuk ke menu **Settings (Pengaturan)** ⚙️.
   - Tekan tombol **"Auto-Detect Server"**.
   - Aplikasi otomatis menemukan laptop Anda dan status berubah menjadi **Connected**.
4. **Selesai**: Jelajahi katalog dan nikmati streaming film serta serial favorit!

---

<a id="panduan-build"></a>
## 🛠️ Panduan Kompilasi & Build APK (Build Guide)

### 1. Menjalankan Backend API Secara Manual

```bash
cd "Backend API"
npm install
npm run build
npm start
```
Atau menggunakan Docker:
```bash
cd "Backend API"
docker compose up -d --build
```
Uji koneksi server:
```bash
curl http://localhost:5376/health
```

### 2. Membangun APK Android TV (`app-tv`)

```bash
# Debug APK
./gradlew :app-tv:assembleDebug
# Output: app-tv/build/outputs/apk/debug/app-tv-debug.apk

# Release APK
./gradlew :app-tv:assembleRelease
# Output: app-tv/build/outputs/apk/release/app-tv-release.apk
```

### 3. Membangun APK Android Mobile (`app-mobile`)

```bash
# Debug APK
./gradlew :app-mobile:assembleDebug
# Output: app-mobile/build/outputs/apk/debug/app-mobile-debug.apk

# Release APK
./gradlew :app-mobile:assembleRelease
# Output: app-mobile/build/outputs/apk/release/app-mobile-release.apk
```

### 4. Build Otomatis via GitHub Actions
Repositori ini telah dilengkapi dengan workflow CI/CD GitHub Actions (`.github/workflows/build-apk.yml`) yang menggunakan JDK 21. Cukup buka tab **Actions** di repositori GitHub Anda dan jalankan **Build IDLIX Android APKs** untuk mengunduh APK hasil kompilasi cloud kapan saja.

---

<a id="faq"></a>
## ❓ Frequently Asked Questions (FAQ)

### ⚠️ Aplikasi menampilkan "Failed to Connect" atau data tidak muncul?
1. Pastikan server backend sedang menyala (jendela `1-JALANKAN-SERVER.bat` tetap terbuka).
2. Pastikan laptop dan HP/TV berada di jaringan Wi-Fi lokal yang sama.
3. Di menu Pengaturan aplikasi, klik tombol **"Auto-Detect Server"** atau masukkan URL backend secara manual (contoh: `http://192.168.1.163:5376/`).
4. Tekan **"Test Backend Connection"** untuk memastikan respon `HTTP 200`.

### 🌐 Apakah bisa menonton langsung dari laptop tanpa HP/TV?
Bisa! Buka browser di laptop Anda dan akses alamat **`http://localhost:5376`**. Web Player bawaan siap digunakan untuk mencari dan menonton film secara instan.

---

<a id="keamanan-lisensi"></a>
## 🔒 Keamanan, Lisensi & Pembuat

* **Pembuat & Pengembang**: [claudiaaulerie](https://github.com/claudiaaulerie)
* **Repositori Resmi**: [https://github.com/claudiaaulerie/IDLIX-Player](https://github.com/claudiaaulerie/IDLIX-Player)
* **Keamanan**: File sensitif seperti `local.properties`, sertifikat signing keystore (`*.jks`), dan credential environment dilindungi di `.gitignore`.
* **Lisensi**: Proyek ini dibuat untuk keperluan edukasi dan riset arsitektur native multi-module Android serta streaming HLS.
