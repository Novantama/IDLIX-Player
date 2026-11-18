import os from 'os';

export function getLocalIpAddresses(): string[] {
  const ips: string[] = [];
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const net of interfaces[name] || []) {
      if (net.family === 'IPv4' && !net.internal && !net.address.startsWith('169.254')) {
        ips.push(net.address);
      }
    }
  }
  return ips;
}

export function renderDashboardHtml(port: number, startTime: Date): string {
  const ips = getLocalIpAddresses();
  const primaryIp = ips[0] || 'localhost';

  const ipBadges = ips.length > 0
    ? ips.map(ip => `<div class="ip-item"><code>http://${ip}:${port}/</code><span class="badge">LAN Ready</span></div>`).join('')
    : `<div class="ip-item"><code>http://localhost:${port}/</code><span class="badge">Local Only</span></div>`;

  return `<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>IDLIX Backend Server • Live Dashboard</title>
  <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🎬</text></svg>">
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif; }
    body { background-color: #0c0d14; color: #f3f4f6; min-height: 100vh; padding: 30px 20px; display: flex; flex-direction: column; align-items: center; }
    .container { max-width: 860px; width: 100%; }
    .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid #1f2333; }
    .logo-group { display: flex; align-items: center; gap: 14px; }
    .logo-badge { background: #e50914; color: white; font-weight: 900; font-size: 20px; padding: 6px 14px; border-radius: 8px; letter-spacing: 1px; }
    .title-group h1 { font-size: 22px; font-weight: 700; color: #ffffff; }
    .title-group p { font-size: 13px; color: #9ca3af; margin-top: 2px; }
    .status-pill { display: inline-flex; align-items: center; gap: 8px; background: rgba(16, 185, 129, 0.15); border: 1px solid #10b981; color: #10b981; padding: 6px 14px; border-radius: 999px; font-weight: 600; font-size: 13px; }
    .status-dot { width: 8px; height: 8px; background-color: #10b981; border-radius: 50%; box-shadow: 0 0 10px #10b981; animation: pulse 2s infinite; }
    @keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.4; } 100% { opacity: 1; } }

    .card { background: #151824; border: 1px solid #232738; border-radius: 12px; padding: 22px; margin-bottom: 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.3); }
    .card-title { font-size: 16px; font-weight: 700; color: #f9fafb; margin-bottom: 14px; display: flex; align-items: center; gap: 8px; }
    
    .ip-box { background: #0c0d14; border: 1px solid #2d3348; border-radius: 8px; padding: 14px; margin-bottom: 12px; }
    .ip-item { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; }
    .ip-item code { font-family: 'JetBrains Mono', Consolas, Monaco, monospace; color: #60a5fa; font-size: 15px; font-weight: 600; }
    .badge { background: #1e3a8a; color: #93c5fd; font-size: 11px; font-weight: 600; padding: 3px 8px; border-radius: 4px; }
    .instruction { font-size: 13px; color: #9ca3af; line-height: 1.5; }
    .instruction strong { color: #f3f4f6; }

    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    @media (max-width: 650px) { .grid-2 { grid-template-columns: 1fr; } }
    
    .feature-item { display: flex; align-items: flex-start; gap: 12px; padding: 12px; background: #0f111a; border-radius: 8px; border: 1px solid #1f2333; }
    .feature-icon { font-size: 20px; line-height: 1; }
    .feature-text h4 { font-size: 14px; font-weight: 600; color: #ffffff; }
    .feature-text p { font-size: 12px; color: #9ca3af; margin-top: 3px; }

    .links-grid { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 8px; }
    .btn { display: inline-flex; align-items: center; gap: 6px; padding: 8px 14px; border-radius: 6px; font-size: 13px; font-weight: 600; text-decoration: none; transition: 0.2s all; cursor: pointer; border: none; }
    .btn-primary { background: #e50914; color: white; }
    .btn-primary:hover { background: #b80710; }
    .btn-secondary { background: #1f2937; color: #e5e7eb; border: 1px solid #374151; }
    .btn-secondary:hover { background: #374151; color: white; }
    .btn-blue { background: #1d4ed8; color: white; }
    .btn-blue:hover { background: #1e40af; }
    .btn-green { background: #059669; color: white; }
    .btn-green:hover { background: #047857; }

    .stats-row { display: flex; flex-wrap: wrap; gap: 20px; font-size: 12px; color: #6b7280; padding-top: 10px; border-top: 1px solid #1f2333; margin-top: 14px; }
    .footer { text-align: center; font-size: 12px; color: #4b5563; margin-top: 20px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <div class="logo-group">
        <div class="logo-badge">IDLIX</div>
        <div class="title-group">
          <h1>Backend Scraper API</h1>
          <p>Real-Time Media Scraper & Ad-Free Streaming Engine</p>
        </div>
      </div>
      <div class="status-pill">
        <div class="status-dot"></div>
        <span>SERVER ACTIVE</span>
      </div>
    </div>

    <!-- LAN IP Connection Card -->
    <div class="card">
      <div class="card-title">
        <span>🌐</span> Local Network Connection (Android TV & Mobile)
      </div>
      <p class="instruction" style="margin-bottom: 12px;">
        Hubungkan <strong>Android TV</strong> atau <strong>HP Android</strong> Anda ke server ini menggunakan URL berikut:
      </p>
      <div class="ip-box">
        ${ipBadges}
      </div>
      <p class="instruction">
        💡 <strong>Tips Pengguna:</strong> Di menu Pengaturan aplikasi IDLIX TV / Mobile, tekan tombol <strong>"Auto-Detect Server"</strong> untuk mendeteksi laptop secara otomatis tanpa perlu ketik manual!
      </p>
    </div>

    <!-- Smart Features & Capabilities Card -->
    <div class="card">
      <div class="card-title">
        <span>⚡</span> Active Service Capabilities
      </div>
      <div class="grid-2">
        <div class="feature-item">
          <div class="feature-icon">🔍</div>
          <div class="feature-text">
            <h4>LAN Auto-Discovery (Port ${port})</h4>
            <p>Pemindaian subnet paralel aktif. APK TV & Mobile otomatis terhubung meski IP laptop berubah.</p>
          </div>
        </div>
        <div class="feature-item">
          <div class="feature-icon">🛡️</div>
          <div class="feature-text">
            <h4>Cloudflare Stealth Engine</h4>
            <p>Bypass proteksi Cloudflare Turnstile & verifikasi anti-bot secara senyap di background tanpa pop-up.</p>
          </div>
        </div>
        <div class="feature-item">
          <div class="feature-icon">🔄</div>
          <div class="feature-text">
            <h4>Dynamic Domain Follower</h4>
            <p>Otomatis melacak dan mengikuti redirect domain upstream IDLIX (z2 ke z3) tanpa jeda.</p>
          </div>
        </div>
        <div class="feature-item">
          <div class="feature-icon">⚡</div>
          <div class="feature-text">
            <h4>In-Memory LRU Cache & HLS Resolver</h4>
            <p>Respons katalog instan (&lt;15ms) dan all-in-one playback token resolver siap tonton.</p>
          </div>
        </div>
      </div>

      <div class="stats-row">
        <span>Node: ${process.version}</span>
        <span>Platform: ${os.type()} (${os.arch()})</span>
        <span>Port: ${port} (HTTP)</span>
        <span id="uptime">Started: ${startTime.toLocaleTimeString()}</span>
      </div>
    </div>

    <!-- Quick API Endpoints & Client Downloads -->
    <div class="card">
      <div class="card-title">
        <span>🔗</span> Quick Test & Client Downloads
      </div>
      <div class="links-grid">
        <a href="/health" target="_blank" class="btn btn-secondary">🏥 Test Health API</a>
        <a href="/api/movies" target="_blank" class="btn btn-secondary">🎬 View Movies API</a>
        <a href="/api/series" target="_blank" class="btn btn-secondary">📺 View Series API</a>
        <a href="/api/update/status" target="_blank" class="btn btn-secondary">🔄 Auto-Update Status</a>
        <a href="/player" target="_blank" class="btn btn-green">🍿 Buka Web Player Laptop</a>
        <a href="/api/update/apk/tv" class="btn btn-primary">📺 Download TV APK (LAN)</a>
        <a href="/api/update/apk/mobile" class="btn btn-blue">📱 Download Mobile APK (LAN)</a>
      </div>
    </div>

    <div class="footer">
      IDLIX Player Streaming Platform • Developed by claudiaaulerie
    </div>
  </div>
</body>
</html>`;
}
