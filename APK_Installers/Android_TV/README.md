# 📺 IDLIX Player untuk Android TV & Google TV

Folder ini berisi paket instalasi APK khusus perangkat layar lebar (**Android TV, Google TV, Mi TV Stick, Xiaomi Box, RealMe TV Stick, Chromecast with Google TV, dan Android TV Box lainnya**).

---

### 📂 File yang Tersedia

1. **`IDLIX-TV-Modified (Auto-Detect).apk`** ⭐ **(Sangat Direkomendasikan)**
   - **Ukuran**: ~22.1 MB
   - **Fitur Khusus**: Dilengkapi tombol **"Auto-Detect Server"** di menu Pengaturan TV. Cukup tekan 1 tombol pada remote TV, aplikasi akan secara otomatis memindai dan menghubungkan TV ke server backend laptop Anda di jaringan Wi-Fi lokal.
   
2. **`IDLIX-TV-Original.apk`**
   - **Ukuran**: ~4.1 MB
   - **Keterangan**: Versi build standar tanpa tombol Auto-Detect (memerlukan pengetikan URL backend secara manual via keyboard layar remote).

---

### 🛠️ Cara Instalasi ke Android TV

Pilih salah satu metode yang paling mudah bagi Anda:

#### Metode 1: Menggunakan Flashdisk / USB Drive
1. Salin file `IDLIX-TV-Modified (Auto-Detect).apk` ke dalam flashdisk.
2. Colokkan flashdisk ke port USB di Android TV / TV Box Anda.
3. Buka aplikasi File Manager di TV (seperti *File Commander* atau *AnExplorer* dari Play Store TV).
4. Klik file APK tersebut dan pilih **Install**.

#### Metode 2: Menggunakan Aplikasi "Send Files to TV" (Nirkabel)
1. Pasang aplikasi **Send Files to TV** di HP Android dan di TV Anda dari Google Play Store.
2. Kirim file APK dari HP ke TV melalui Wi-Fi.
3. Buka dan instal file tersebut di TV menggunakan file manager.

#### Metode 3: Menggunakan ADB (Untuk Pengembang / Pengguna Laptop)
```bash
adb connect <IP_TV>:5555
adb -s <IP_TV>:5555 install -r "IDLIX-TV-Modified (Auto-Detect).apk"
```

---

### ⚙️ Konfigurasi Setelah Terpasang
1. Pastikan server backend di laptop sudah menyala (jalankan `1-JALANKAN-SERVER.bat`).
2. Pastikan TV dan Laptop terhubung ke Wi-Fi yang sama.
3. Buka aplikasi IDLIX di TV -> Masuk ke menu **Pengaturan (Settings)** ⚙️.
4. Tekan tombol **"Auto-Detect Server"**.
5. Setelah muncul status koneksi sukses, simpan pengaturan dan nikmati tontonan!
