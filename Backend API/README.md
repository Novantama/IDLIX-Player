# IDLIX Player - Backend API Gateway & Media Proxy Service

High-performance media gateway and streaming proxy for **IDLIX Player** (Android TV & Android Mobile).
By running this backend service, the Android apps run 100% native Kotlin with minimal memory footprint and hardware-accelerated ExoPlayer streaming.

## Fitur Utama
1. **Media Gateway Service**: Mengelola katalog konten, metadata film, serial TV, dan sesi pemutaran HLS berkecepatan tinggi.
2. **In-Memory Caching (LRU)**: Menyimpan cache data katalog di RAM (15-30 menit) sehingga respons ke Android TV & Mobile instan (<15ms) dan hemat bandwidth.
3. **All-in-One Playback Resolver**: Menyediakan endpoint `/api/playback/resolve` yang menangani inisiasi sesi video dan mengembalikan URL playlist master HLS siap putar beserta subtitle Bahasa Indonesia.
4. **Ringan & Hemat RAM**: Dirancang dengan konsumsi memori rendah (<1GB RAM) yang sangat efisien dijalankan di laptop lokal maupun server VPS.

---

## Cara Menjalankan di VPS (Rekomendasi Docker)

1. Clone repo atau upload folder `Backend API` ke VPS Anda:
   ```bash
   cd "Backend API"
   ```

2. Bangun dan jalankan kontainer:
   ```bash
   docker compose up -d --build
   ```

3. Cek log apakah service sudah aktif:
   ```bash
   docker compose logs -f
   ```

4. Uji endpoint:
   ```bash
   curl http://localhost:5376/health
   curl "http://localhost:5376/api/movies?page=1&limit=5"
   ```

---

## Cara Menjalankan Secara Lokal (Node.js)

1. Masuk ke direktori:
   ```bash
   cd "Backend API"
   ```
2. Install dependensi:
   ```bash
   npm install
   ```
3. Bangun dan jalankan:
   ```bash
   npm run build
   npm start
   ```

---

## REST Endpoints Tersedia
- `GET /health` : Status server dan sesi aktif
- `GET /api/movies?page=1&limit=20&sort=popularity&genre=slug` : Daftar film
- `GET /api/series?page=1&limit=20&sort=popularity&genre=slug` : Daftar serial TV
- `GET /api/movies/:slug` : Detail film
- `GET /api/series/:slug` : Detail serial beserta episode
- `GET /api/series/:slug/season/:season/episode/:episode` : Detail episode
- `GET /api/genres` : Daftar genre
- `GET /api/search?q=:query` : Pencarian film/serial
- `GET /api/watch/play-info/:contentType/:id` : Info pemutaran media
- `POST /api/watch/session/claim` : Inisiasi sesi pemutaran
- `POST /watch/session/redeem` : Sesi playlist HLS
- `GET /api/playback/resolve?contentType=:type&contentId=:id&contentTitle=:title` : Resolusi tuntas playlist video HLS & subtitle
