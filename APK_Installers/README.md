# 📦 Koleksi Installer APK IDLIX Player

Folder ini menyediakan paket installer APK siap pasang untuk **Android TV / Google TV** dan **HP Android Smartphone / Tablet**.

---

### 📂 Struktur Folder Installer

```
APK_Installers/
├── Android_TV/                                    # Untuk Smart TV, Android TV Box & Google TV
│   ├── IDLIX-TV-Modified (Auto-Detect).apk        # ⭐ Rekomendasi: Dilengkapi Auto-Detect Server (22.1 MB)
│   ├── IDLIX-TV-Original.apk                      # Versi build standar (4.1 MB)
│   └── README.md                                  # Panduan khusus instalasi Android TV
│
├── Mobile_HP/                                     # Untuk Smartphone & Tablet Android
│   ├── IDLIX-Mobile-Modified (Auto-Detect).apk    # ⭐ Rekomendasi: Dilengkapi Auto-Detect Server (23.1 MB)
│   ├── IDLIX-Mobile-Original.apk                  # Versi build standar (4.4 MB)
│   └── README.md                                  # Panduan khusus instalasi Mobile
│
├── PANDUAN_INSTALL.txt                            # Catatan teks petunjuk instalasi cepat
└── README.md                                      # Dokumentasi katalog installer
```

---

### ⚖️ Perbandingan Versi APK

| Fitur | Versi Modified ⭐ (Rekomendasi) | Versi Original |
| :--- | :---: | :---: |
| **Auto-Detect Server** | ✅ **Ada** (1-Klik Deteksi Wi-Fi) | ❌ Manual Ketik URL |
| **Tampilan UI Leanback TV** | ✅ Ya | ✅ Ya |
| **Material 3 UI Mobile** | ✅ Ya | ✅ Ya |
| **ExoPlayer HLS Streaming** | ✅ Ya | ✅ Ya |
| **SubDL Subtitle Integration**| ✅ Ya | ✅ Ya |
| **Picture-in-Picture (PiP)** | ✅ Ya | ✅ Ya |
| **Ukuran File** | ~22 MB - 23 MB | ~4.1 MB - 4.4 MB |

> 💡 **Saran Pemilihan**: Gunakan file **`Modified (Auto-Detect)`** agar Anda tidak perlu repot mengetik alamat IP laptop secara manual menggunakan remote TV atau keyboard ponsel.

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
