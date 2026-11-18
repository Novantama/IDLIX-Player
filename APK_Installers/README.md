# 📦 Koleksi Installer APK IDLIX Player

Folder ini menyediakan paket installer APK siap pasang untuk **Android TV / Google TV** dan **HP Android Smartphone / Tablet**.

---

### 📂 Struktur Folder Installer

```
APK_Installers/
├── Android_TV/
│   ├── IDLIX-TV-Modified (Auto-Detect).apk        # ⭐ Rilis TV v1.0.7: Auto-Detect Server & Leanback UI (~22.1 MB)
│   └── README.md                                  # Panduan khusus instalasi Android TV
│
├── Mobile_HP/
│   ├── IDLIX-Mobile-Modified (Auto-Detect).apk    # ⭐ Rilis Mobile v1.0.7: Subtitle Drawer Rapi & Auto-Update (~23.2 MB)
│   └── README.md                                  # Panduan khusus instalasi Mobile
│
├── PANDUAN_INSTALL.txt                            # Catatan teks petunjuk instalasi cepat
└── README.md                                      # Dokumentasi katalog installer
```

---

### 📋 Ringkasan Versi Rilis

| Platform | Versi | File APK | Ukuran | Fitur Utama |
| :--- | :---: | :--- | :---: | :--- |
| 📺 **Android TV / Google TV** | **v1.0.7** | [`IDLIX-TV-Modified (Auto-Detect).apk`](Android_TV/IDLIX-TV-Modified%20(Auto-Detect).apk) | ~22.1 MB | Leanback 10-foot UI, D-Pad Remote, Auto-Detect Server, Hardware Acceleration ExoPlayer |
| 📱 **Android Mobile (HP/Tablet)** | **v1.0.7** | [`IDLIX-Mobile-Modified (Auto-Detect).apk`](Mobile_HP/IDLIX-Mobile-Modified%20(Auto-Detect).apk) | ~23.2 MB | Material 3, Subtitle ID & EN Selector, Swipe Gestures, PiP |

---

### 🚀 Cara Penggunaan Cepat

1. **Jalankan Server Backend di Laptop**:
   - Klik dua kali file [`1-JALANKAN-SERVER.bat`](../1-JALANKAN-SERVER.bat) di folder utama project.
   - Server akan berjalan di port `5376`.
2. **Koneksi Wi-Fi**:
   - Pastikan Laptop dan HP / Android TV terhubung ke **jaringan Wi-Fi yang sama**.
3. **Buka Aplikasi IDLIX**:
   - Buka menu **Settings (Pengaturan)** ⚙️.
   - Klik tombol **"Auto-Detect Server"**.
   - Server otomatis terhubung dan siap memutar tayangan!

---

### 📖 Dokumentasi Lengkap
* Panduan Detail TV: [`docs/PANDUAN_INSTALASI_TV.md`](../docs/PANDUAN_INSTALASI_TV.md)
* Roadmap & Arsitektur: [`docs/PORTING_TO_MOBILE_PLAN.md`](../docs/PORTING_TO_MOBILE_PLAN.md)
* Rilis GitHub Resmi: [GitHub Releases](https://github.com/claudiaaulerie/IDLIX-Player/releases)
