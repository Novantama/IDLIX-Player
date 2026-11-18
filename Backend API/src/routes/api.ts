import fs from 'node:fs';
import path from 'node:path';
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

    const activeOrigin = engine.getSessionStatus().targetUrl || 'https://z2.idlixku.com';
    let result = await engine.executeFetch(targetEndpoint, {
      method: 'POST',
      body: { claim, mode },
      headers: {
        Referer: `${activeOrigin}/`,
        Origin: activeOrigin,
      },
    });

    if (result.status === 401 || result.status === 403) {
      req.log.warn(`[API] Redeem returned ${result.status}, re-authenticating...`);
      await engine.ensureAuthenticated(true);
      result = await engine.executeFetch(targetEndpoint, {
        method: 'POST',
        body: { claim, mode },
        headers: {
          Referer: `${activeOrigin}/`,
          Origin: activeOrigin,
        },
      });
    }

    return reply.status(result.status).send(result.data);
  };

  fastify.post('/api/watch/redeem', handleRedeem);
  fastify.post('/api/watch/session/redeem', handleRedeem);
  fastify.post('/watch/session/redeem', handleRedeem);

  // High-performance All-in-One Playback Resolver
  fastify.get<{
    Querystring: { contentType?: string; contentId?: string; contentTitle?: string };
  }>('/api/playback/resolve', async (req, reply) => {
    const { contentType, contentId, contentTitle } = req.query;
    if (!contentType || !contentId) {
      return reply.status(400).send({ error: 'contentType and contentId are required' });
    }

    try {
      const playback = await engine.resolvePlayback(contentType, contentId, contentTitle);
      return playback;
    } catch (err: any) {
      req.log.error(err, 'Failed to resolve stream playback');
      return reply.status(500).send({
        error: err?.message || 'Failed to resolve stream playback',
      });
    }
  });

  // ==========================================
  // Auto-Update Engine (Connected to claudiaaulerie/IDLIX-Player)
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
      repository: 'https://github.com/claudiaaulerie/IDLIX-Player',
      iniFound: fs.existsSync(iniPath),
      tv: {
        apkFound: fs.existsSync(tvApkPath),
        localDownloadUrl: `${protocol}://${host}/api/update/apk/tv`,
        cloudDownloadUrl: 'https://raw.githubusercontent.com/claudiaaulerie/IDLIX-Player/main/APK_Installers/Android_TV/IDLIX-TV-Modified%20(Auto-Detect).apk'
      },
      mobile: {
        apkFound: fs.existsSync(mobileApkPath),
        localDownloadUrl: `${protocol}://${host}/api/update/apk/mobile`,
        cloudDownloadUrl: 'https://raw.githubusercontent.com/claudiaaulerie/IDLIX-Player/main/APK_Installers/Mobile_HP/IDLIX-Mobile-Modified%20(Auto-Detect).apk'
      }
    };
  });
};
