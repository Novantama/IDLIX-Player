import fs from 'node:fs';
import path from 'node:path';
import { Readable } from 'node:stream';
import { fileURLToPath } from 'node:url';
import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { IdlixScraperEngine } from '../engine/IdlixScraperEngine.js';
import { globalCache } from '../services/CacheService.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export const apiRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  const engine = IdlixScraperEngine.getInstance();

  // Web Player Frontend (UI)
  const servePlayer = async (_req: any, reply: any) => {
    const candidatePaths = [
      path.resolve(process.cwd(), 'public/index.html'),
      path.resolve(process.cwd(), 'Backend API/public/index.html'),
      path.resolve(__dirname, '../../public/index.html'),
      path.resolve(__dirname, '../public/index.html'),
    ];

    for (const filePath of candidatePaths) {
      if (fs.existsSync(filePath)) {
        const html = fs.readFileSync(filePath, 'utf-8');
        return reply.type('text/html; charset=utf-8').send(html);
      }
    }

    return reply.status(404).send({ error: 'Web player index.html not found' });
  };

  fastify.get('/player', servePlayer);

  // Health and Status
  fastify.get('/health', async () => {
    return {
      status: 'ok',
      timestamp: new Date().toISOString(),
      session: engine.getSessionStatus(),
    };
  });

  // Movies list
  fastify.get<{
    Querystring: { page?: string; limit?: string; sort?: string; genre?: string };
  }>('/api/movies', async (req, reply) => {
    const { page = '1', limit = '20', sort, genre } = req.query;
    const cacheKey = `movies:${page}:${limit}:${sort || ''}:${genre || ''}`;

    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const queryParams = new URLSearchParams({ page, limit });
    if (sort) queryParams.set('sort', sort);
    if (genre) queryParams.set('genre', genre);

    const path = `/api/movies?${queryParams.toString()}`;
    const result = await engine.executeFetch(path);

    if (result.status === 200 && result.data) {
      globalCache.set(cacheKey, result.data, 15 * 60 * 1000); // 15 min cache
      return result.data;
    }

    return reply.status(result.status).send(result.data);
  });

  // Series list
  fastify.get<{
    Querystring: { page?: string; limit?: string; sort?: string; genre?: string };
  }>('/api/series', async (req, reply) => {
    const { page = '1', limit = '20', sort, genre } = req.query;
    const cacheKey = `series:${page}:${limit}:${sort || ''}:${genre || ''}`;

    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const queryParams = new URLSearchParams({ page, limit });
    if (sort) queryParams.set('sort', sort);
    if (genre) queryParams.set('genre', genre);

    const path = `/api/series?${queryParams.toString()}`;
    const result = await engine.executeFetch(path);

    if (result.status === 200 && result.data) {
      globalCache.set(cacheKey, result.data, 15 * 60 * 1000);
      return result.data;
    }

    return reply.status(result.status).send(result.data);
  });

  // Movie detail
  fastify.get<{ Params: { slug: string } }>('/api/movies/:slug', async (req, reply) => {
    const { slug } = req.params;
    const cacheKey = `movie_detail:${slug}`;

    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const result = await engine.executeFetch(`/api/movies/${slug}`);
    if (result.status === 200 && result.data) {
      globalCache.set(cacheKey, result.data, 30 * 60 * 1000); // 30 min cache
      return result.data;
    }

    return reply.status(result.status).send(result.data);
  });

  // Series detail
  fastify.get<{ Params: { slug: string } }>('/api/series/:slug', async (req, reply) => {
    const { slug } = req.params;
    const cacheKey = `series_detail:${slug}`;

    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const result = await engine.executeFetch(`/api/series/${slug}`);
    if (result.status === 200 && result.data) {
      globalCache.set(cacheKey, result.data, 30 * 60 * 1000);
      return result.data;
    }

    return reply.status(result.status).send(result.data);
  });

  // Episode detail
  fastify.get<{
    Params: { slug: string; season: string; episode: string };
  }>('/api/series/:slug/season/:season/episode/:episode', async (req, reply) => {
    const { slug, season, episode } = req.params;
    const cacheKey = `episode:${slug}:${season}:${episode}`;

    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const result = await engine.executeFetch(`/api/series/${slug}/season/${season}/episode/${episode}`);
    if (result.status === 200 && result.data) {
      globalCache.set(cacheKey, result.data, 30 * 60 * 1000);
      return result.data;
    }

    return reply.status(result.status).send(result.data);
  });

  // Genres
  fastify.get('/api/genres', async (_req, reply) => {
    const cacheKey = 'genres:all';
    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const result = await engine.executeFetch('/api/genres');
    if (result.status === 200 && result.data) {
      globalCache.set(cacheKey, result.data, 2 * 60 * 60 * 1000); // 2 hours
      return result.data;
    }

    return reply.status(result.status).send(result.data);
  });

  // Search
  fastify.get<{ Querystring: { q?: string } }>('/api/search', async (req, reply) => {
    const query = req.query.q?.trim();
    if (!query) {
      return { results: [], total: 0 };
    }

    const cacheKey = `search:${query.toLowerCase()}`;
    const cached = globalCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const result = await engine.executeFetch(`/api/search?q=${encodeURIComponent(query)}`);
    if (result.status === 200 && result.data) {
      globalCache.set(cacheKey, result.data, 10 * 60 * 1000);
      return result.data;
    }

    return reply.status(result.status).send(result.data);
  });

  // Play info (gateToken & countdown)
  const handlePlayInfo = async (req: any, reply: any) => {
    const { contentType, id } = req.params;
    await engine.ensureAuthenticated();
    let result = await engine.executeFetch(`/api/watch/play-info/${contentType}/${id}`);
    if (result.status === 401 || result.status === 403) {
      req.log.warn(`[API] Play-info returned ${result.status}, refreshing user session...`);
      await engine.ensureAuthenticated(true);
      result = await engine.executeFetch(`/api/watch/play-info/${contentType}/${id}`);
    }
    return reply.status(result.status).send(result.data);
  };
  fastify.get('/api/watch/play-info/:contentType/:id', handlePlayInfo);
  fastify.get('/watch/play-info/:contentType/:id', handlePlayInfo);

  // Claim session
  const handleClaim = async (req: any, reply: any) => {
    const { gateToken } = req.body || {};
    if (!gateToken) {
      return reply.status(400).send({ error: 'gateToken is required' });
    }

    await engine.ensureAuthenticated();

    let didCookie: string | undefined;
    try {
      const rawP0 = gateToken.split('.')[0];
      const decodedP0 = Buffer.from(rawP0, 'base64').toString('utf-8');
      const didMatch = /"owner":"d:([^"]+)"/.exec(decodedP0);
      if (didMatch?.[1]) {
        didCookie = `did=${didMatch[1]}`;
      }
    } catch (_) {}

    let result = await engine.executeFetch('/api/watch/session/claim', {
      method: 'POST',
      body: { gateToken },
      headers: didCookie ? { Cookie: didCookie } : undefined,
    });

    if (result.status === 403 || result.status === 401) {
      req.log.warn(`[API] Claim returned ${result.status}, re-authenticating...`);
      await engine.ensureAuthenticated(true);
      result = await engine.executeFetch('/api/watch/session/claim', {
        method: 'POST',
        body: { gateToken },
        headers: didCookie ? { Cookie: didCookie } : undefined,
      });
    }

    return reply.status(result.status).send(result.data);
  };
  fastify.post('/api/watch/session/claim', handleClaim);
  fastify.post('/watch/session/claim', handleClaim);

  // Redeem Play (Majorplay CDN / IDLIX Session Redeem)
  const handleRedeem = async (req: any, reply: any) => {
    let { url, claim, mode = 'browser' } = req.body || {};
    if (!claim) {
      return reply.status(400).send({ error: 'claim is required' });
    }

    let targetEndpoint = url || '/api/watch/session/redeem';
    if (
      req.url?.includes('/watch/session/redeem') ||
      req.url?.includes('/api/watch/session/redeem') ||
      targetEndpoint.includes('/watch/session/redeem')
    ) {
      targetEndpoint = '/api/watch/session/redeem';
    } else if (targetEndpoint.startsWith('/watch')) {
      targetEndpoint = '/api' + targetEndpoint;
    }

    let result = await engine.redeemPlaybackUrl(targetEndpoint, claim, mode);

    if (result.status === 401 || result.status === 403) {
      req.log.warn(`[API] Redeem returned ${result.status}, re-authenticating...`);
      await engine.ensureAuthenticated(true);
      result = await engine.redeemPlaybackUrl(targetEndpoint, claim, mode);
    }

    return reply.status(result.status).send(result.data);
  };

  fastify.post('/api/watch/redeem', handleRedeem);
  fastify.post('/api/watch/session/redeem', handleRedeem);
  fastify.post('/watch/session/redeem', handleRedeem);

  // High-performance All-in-One Playback Resolver
  fastify.get<{
    Querystring: { contentType?: string; contentId?: string; contentTitle?: string; slug?: string };
  }>('/api/playback/resolve', async (req, reply) => {
    const { contentType, contentId, contentTitle, slug } = req.query;
    if (!contentType || !contentId) {
      return reply.status(400).send({ error: 'contentType and contentId are required' });
    }

    try {
      const playback = await engine.resolvePlayback(contentType, contentId, contentTitle, slug);
      return playback;
    } catch (err: any) {
      req.log.error(err, 'Failed to resolve stream playback');
      return reply.status(500).send({
        error: err?.message || 'Failed to resolve stream playback',
      });
    }
  });

  // Web Player HLS Proxy (bypasses browser CORS/Referer blocks and preserves ?t= token across manifests & segments)
  fastify.get<{
    Querystring: { url?: string; ref?: string };
  }>('/api/proxy/hls', async (req, reply) => {
    const targetUrl = req.query.url?.trim();
    if (!targetUrl || (!targetUrl.startsWith('http://') && !targetUrl.startsWith('https://'))) {
      return reply.status(400).send({ error: 'Valid http(s) url parameter is required' });
    }

    const activeOrigin = engine.getSessionStatus().targetUrl || 'https://z2.idlixku.com';
    const rawRef = req.query.ref?.trim();
    const referer =
      rawRef && (rawRef.startsWith('http://') || rawRef.startsWith('https://'))
        ? rawRef
        : `${activeOrigin}/`;

    const buildHeaders = (refUrl: string, includeOrigin: boolean): Record<string, string> => {
      const headers: Record<string, string> = {
        'User-Agent':
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
        Accept: '*/*',
        Referer: refUrl,
      };
      if (includeOrigin) {
        try {
          headers.Origin = new URL(refUrl).origin;
        } catch (_) {}
      }
      if (typeof req.headers.range === 'string' && req.headers.range) {
        headers.Range = req.headers.range;
      }
      return headers;
    };

    try {
      let upstreamRes = await fetch(targetUrl, {
        headers: buildHeaders(referer, true),
        redirect: 'follow',
        signal: AbortSignal.timeout(25000),
      });

      // Some CDNs reject cross-origin Origin or prefer same-origin Referer
      if (upstreamRes.status === 403) {
        const selfReferer = `${new URL(targetUrl).origin}/`;
        upstreamRes = await fetch(targetUrl, {
          headers: buildHeaders(selfReferer, false),
          redirect: 'follow',
          signal: AbortSignal.timeout(25000),
        });
      }

      if (!upstreamRes.ok) {
        return reply.status(upstreamRes.status).send({ error: `Upstream HLS returned ${upstreamRes.status}` });
      }

      const finalUrl = upstreamRes.url || targetUrl;
      const contentType = (upstreamRes.headers.get('content-type') || '').toLowerCase();
      const pathname = new URL(finalUrl).pathname.toLowerCase();
      const isPlaylistByHeader =
        pathname.endsWith('.m3u8') ||
        contentType.includes('mpegurl') ||
        contentType.includes('vnd.apple.mpegurl');

      let bodyBuffer: Buffer | null = null;
      let looksLikeM3u8 = isPlaylistByHeader;

      if (!looksLikeM3u8 && (contentType.startsWith('text/') || pathname.endsWith('.txt') || !pathname.includes('.'))) {
        bodyBuffer = Buffer.from(await upstreamRes.arrayBuffer());
        const head = bodyBuffer.subarray(0, 16).toString('utf-8').trimStart();
        if (head.startsWith('#EXTM3U')) {
          looksLikeM3u8 = true;
        }
      }

      if (looksLikeM3u8) {
        const playlistText = bodyBuffer
          ? bodyBuffer.toString('utf-8')
          : await upstreamRes.text();

        const parentToken = new URL(finalUrl).searchParams.get('t');
        const rewriteUri = (rawUri: string): string => {
          try {
            const resolved = new URL(rawUri, finalUrl);
            if (parentToken && !resolved.searchParams.has('t')) {
              resolved.searchParams.set('t', parentToken);
            }
            return `/api/proxy/hls?url=${encodeURIComponent(resolved.toString())}&ref=${encodeURIComponent(referer)}`;
          } catch (_) {
            return rawUri;
          }
        };

        const rewritten = playlistText
          .split(/\r?\n/)
          .map((line) => {
            const trimmed = line.trim();
            if (!trimmed) return line;
            if (trimmed.startsWith('#')) {
              return line.replace(/URI="([^"]+)"/g, (_match, uri) => `URI="${rewriteUri(uri)}"`);
            }
            return rewriteUri(trimmed);
          })
          .join('\n');

        reply.header('Content-Type', 'application/vnd.apple.mpegurl; charset=utf-8');
        reply.header('Access-Control-Allow-Origin', '*');
        reply.header('Cache-Control', 'no-cache');
        return reply.send(rewritten);
      }

      reply.header('Content-Type', upstreamRes.headers.get('content-type') || 'application/octet-stream');
      reply.header('Access-Control-Allow-Origin', '*');
      reply.header('Cache-Control', 'public, max-age=300');

      const passthroughHeaders = ['content-length', 'content-range', 'accept-ranges'];
      for (const h of passthroughHeaders) {
        const val = upstreamRes.headers.get(h);
        if (val) reply.header(h, val);
      }

      if (bodyBuffer) {
        return reply.status(upstreamRes.status).send(bodyBuffer);
      }
      if (upstreamRes.body) {
        return reply.status(upstreamRes.status).send(Readable.fromWeb(upstreamRes.body as any));
      }
      const fallbackBuf = Buffer.from(await upstreamRes.arrayBuffer());
      return reply.status(upstreamRes.status).send(fallbackBuf);
    } catch (err: any) {
      req.log.error(err, 'HLS proxy failed');
      return reply.status(502).send({ error: err?.message || 'Failed to proxy HLS stream' });
    }
  });

  // Web Player Subtitle Proxy (adds CORS headers and converts .srt to WebVTT on the fly)
  fastify.get<{
    Querystring: { url?: string };
  }>('/api/proxy/subtitle', async (req, reply) => {
    let targetUrl = req.query.url?.trim() || '';
    if (!targetUrl) {
      return reply.status(400).send({ error: 'url parameter is required' });
    }

    const activeOrigin = engine.getSessionStatus().targetUrl || 'https://z2.idlixku.com';
    if (targetUrl.startsWith('/')) {
      targetUrl = `${activeOrigin}${targetUrl}`;
    }

    if (!targetUrl.startsWith('http://') && !targetUrl.startsWith('https://')) {
      return reply.status(400).send({ error: 'Valid http(s) url parameter is required' });
    }

    try {
      const subRes = await fetch(targetUrl, {
        headers: {
          'User-Agent':
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
          Accept: 'text/vtt, text/plain, */*',
          Referer: `${activeOrigin}/`,
        },
        redirect: 'follow',
        signal: AbortSignal.timeout(15000),
      });

      if (!subRes.ok) {
        return reply.status(subRes.status).send({ error: `Upstream subtitle returned ${subRes.status}` });
      }

      let text = (await subRes.text()).replace(/^\uFEFF/, '').replace(/\r\n/g, '\n');
      if (!text.trimStart().startsWith('WEBVTT')) {
        // Convert SRT timestamps (HH:MM:SS,mmm -> HH:MM:SS.mmm) for HTML5 <track>
        text = 'WEBVTT\n\n' + text.replace(/(\d{2}:\d{2}:\d{2}),(\d{3})/g, '$1.$2');
      }

      reply.header('Content-Type', 'text/vtt; charset=utf-8');
      reply.header('Access-Control-Allow-Origin', '*');
      reply.header('Cache-Control', 'public, max-age=3600');
      return reply.send(text);
    } catch (err: any) {
      req.log.error(err, 'Subtitle proxy failed');
      return reply.status(502).send({ error: err?.message || 'Failed to proxy subtitle' });
    }
  });

  // ==========================================
  // Auto-Update Engine (Connected to Novantama/IDLIX-Player)
  // ==========================================
  const resolveProjectRoot = () => {
    const candidates = [
      path.resolve(process.cwd()),
      path.resolve(process.cwd(), '..'),
      path.resolve(__dirname, '../../../'),
      path.resolve(__dirname, '../../'),
    ];
    for (const dir of candidates) {
      if (fs.existsSync(path.join(dir, 'releases.ini'))) {
        return dir;
      }
    }
    return path.resolve(process.cwd(), '..');
  };

  // Serve releases.ini to Android TV & Mobile
  fastify.get('/api/update/releases.ini', async (_req, reply) => {
    const rootDir = resolveProjectRoot();
    const iniPath = path.join(rootDir, 'releases.ini');
    if (fs.existsSync(iniPath)) {
      const content = fs.readFileSync(iniPath, 'utf-8');
      return reply.type('text/plain; charset=utf-8').send(content);
    }
    return reply.status(404).send({ error: 'releases.ini not found' });
  });

  // Stream latest APK directly from local server at full Wi-Fi speeds
  fastify.get<{ Params: { platform: string } }>('/api/update/apk/:platform', async (req, reply) => {
    const { platform } = req.params;
    const rootDir = resolveProjectRoot();
    const plat = (platform || '').toLowerCase();

    let targetApkPath = '';
    let fileName = '';

    if (plat === 'tv' || plat === 'android_tv') {
      targetApkPath = path.join(rootDir, 'APK_Installers/Android_TV/IDLIX-TV-Modified (Auto-Detect).apk');
      fileName = 'IDLIX-TV-Modified (Auto-Detect).apk';
    } else if (plat === 'mobile' || plat === 'mobile_hp') {
      targetApkPath = path.join(rootDir, 'APK_Installers/Mobile_HP/IDLIX-Mobile-Modified (Auto-Detect).apk');
      fileName = 'IDLIX-Mobile-Modified (Auto-Detect).apk';
    } else {
      return reply.status(400).send({ error: 'Invalid platform. Use "tv" or "mobile".' });
    }

    if (!fs.existsSync(targetApkPath)) {
      return reply.status(404).send({ error: `APK file not found for platform: ${platform}` });
    }

    const stat = fs.statSync(targetApkPath);
    reply.header('Content-Type', 'application/vnd.android.package-archive');
    reply.header('Content-Disposition', `attachment; filename="${fileName}"`);
    reply.header('Content-Length', stat.size);
    return reply.send(fs.createReadStream(targetApkPath));
  });

  // Status check for auto-update configuration
  fastify.get('/api/update/status', async (req, reply) => {
    const rootDir = resolveProjectRoot();
    const tvApkPath = path.join(rootDir, 'APK_Installers/Android_TV/IDLIX-TV-Modified (Auto-Detect).apk');
    const mobileApkPath = path.join(rootDir, 'APK_Installers/Mobile_HP/IDLIX-Mobile-Modified (Auto-Detect).apk');
    const iniPath = path.join(rootDir, 'releases.ini');

    const host = req.headers.host || 'localhost:5376';
    const protocol = req.headers['x-forwarded-proto'] || 'http';

    return {
      status: 'ok',
      repository: 'https://github.com/Novantama/IDLIX-Player',
      iniFound: fs.existsSync(iniPath),
      tv: {
        apkFound: fs.existsSync(tvApkPath),
        localDownloadUrl: `${protocol}://${host}/api/update/apk/tv`,
        cloudDownloadUrl: 'https://github.com/Novantama/IDLIX-Player/releases/download/v1.0.12/IDLIX-TV-Modified.Auto-Detect.apk'
      },
      mobile: {
        apkFound: fs.existsSync(mobileApkPath),
        localDownloadUrl: `${protocol}://${host}/api/update/apk/mobile`,
        cloudDownloadUrl: 'https://github.com/Novantama/IDLIX-Player/releases/download/v1.0.12/IDLIX-Mobile-Modified.Auto-Detect.apk'
      }
    };
  });

  // ==========================================
  // IDLIX User Account Authentication Endpoints
  // ==========================================
  const handleAuthLogin = async (req: any, reply: any) => {
    const { identifier, email, username, password } = req.body || {};
    const targetIdent = identifier || email || username;
    if (!targetIdent || !password) {
      return reply.status(400).send({
        success: false,
        message: 'Email / Username dan Password wajib diisi',
        status: 'Gagal terhubung'
      });
    }

    const authResult = await engine.loginWithCredentials(targetIdent, password);
    if (authResult.success) {
      return reply.send(authResult);
    } else {
      return reply.status(401).send(authResult);
    }
  };

  const handleAuthStatus = async (_req: any, reply: any) => {
    return reply.send(engine.getAuthStatus());
  };

  const handleAuthLogout = async (_req: any, reply: any) => {
    const res = await engine.logout();
    return reply.send(res);
  };

  fastify.post('/api/auth/login', handleAuthLogin);
  fastify.post('/auth/login', handleAuthLogin);
  fastify.get('/api/auth/status', handleAuthStatus);
  fastify.get('/auth/status', handleAuthStatus);
  fastify.post('/api/auth/logout', handleAuthLogout);
  fastify.post('/auth/logout', handleAuthLogout);
};
