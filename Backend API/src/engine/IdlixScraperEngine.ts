import { connect } from 'puppeteer-real-browser';

export interface ResolvedPlaybackResult {
  title: string;
  hlsMasterUrl: string;
  subtitles: Array<{ lang: string; label: string; path: string }>;
  durationSec: number;
  videoId: string;
}

export class IdlixScraperEngine {
  private static instance: IdlixScraperEngine | null = null;
  private browser: any = null;
  private page: any = null;
  private isReady = false;
  private initPromise: Promise<void> | null = null;
  private baseUrl = process.env.IDLIX_BASE_URL || 'https://z2.idlixku.com';
  private authIdentifier = process.env.IDLIX_IDENTIFIER || '';
  private authPassword = process.env.IDLIX_PASSWORD || '';
  private isLoggedIn = false;
  private loggedUser: any = null;

  private constructor() {}

  public static getInstance(): IdlixScraperEngine {
    if (!IdlixScraperEngine.instance) {
      IdlixScraperEngine.instance = new IdlixScraperEngine();
    }
    return IdlixScraperEngine.instance;
  }

  public async init(): Promise<void> {
    if (this.isReady && this.page && !this.page.isClosed()) {
      return;
    }
    if (this.initPromise) {
      return this.initPromise;
    }

    this.initPromise = (async () => {
      console.log(`[ScraperEngine] Launching real-browser stealth engine for Cloudflare bypass at ${this.baseUrl}...`);

      try {
        const browserArgs = [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-dev-shm-usage',
          '--disable-gpu',
          '--no-first-run',
          '--no-zygote',
          '--ignore-certificate-errors',
          '--ignore-certificate-errors-spki-list',
          '--window-position=-32000,-32000',
          '--window-size=1280,800',
          '--lang=id-ID,id,en-US,en',
        ];

        // Option A: puppeteer-real-browser with Xvfb virtual display on Linux VPS & SOCKS5 proxy
        const customConfig: any = {};
        const explicitChromePath = process.env.CHROME_PATH || process.env.PUPPETEER_EXECUTABLE_PATH;
        if (explicitChromePath) {
          customConfig.chromePath = explicitChromePath;
        }

        const realBrowser = await (connect as any)({
          headless: false,
          turnstile: true,
          disableXvfb: process.platform !== 'linux',
          customConfig,
          connectOption: {
            ignoreHTTPSErrors: true,
          },
          args: browserArgs,
        });

        this.browser = realBrowser.browser;
        this.page = realBrowser.page;

        // Auto-reconnect handlers when browser or page is closed
        if (this.browser && typeof this.browser.on === 'function') {
          this.browser.on('disconnected', () => {
            console.log('[ScraperEngine] Chromium disconnected. Resetting session state.');
            this.isReady = false;
            this.page = null;
            this.browser = null;
            this.isLoggedIn = false;
          });
        }

        if (this.page && typeof this.page.on === 'function') {
          this.page.on('close', () => {
            console.log('[ScraperEngine] Scraper tab closed. Resetting session state.');
            this.isReady = false;
            this.page = null;
            this.isLoggedIn = false;
          });
        }

        // Block heavy external ads only, never block challenge resources
        try {
          await this.page.setRequestInterception(true);
          this.page.on('request', (req: any) => {
            const url = req.url();
            const adBlock = ['xyzchat', 'doubleclick', 'google-analytics', 'analytics.google.com'];

            if (adBlock.some((d) => url.includes(d))) {
              req.abort();
            } else {
              req.continue();
            }
          });
        } catch (_) {}

        console.log(`[ScraperEngine] Navigating to ${this.baseUrl} to solve Cloudflare challenge...`);
        await this.page.goto(this.baseUrl, {
          waitUntil: 'domcontentloaded',
          timeout: 60000,
        });

        console.log('[ScraperEngine] Waiting for Cloudflare Turnstile verification...');
        await this.page.waitForFunction(
          () => {
            const title = document.title.toLowerCase();
            return !title.includes('just a moment') && !title.includes('attention required');
          },
          { timeout: 45000 }
        );

        console.log(`[ScraperEngine] Cloudflare challenge bypassed successfully! Page Title: "${await this.page.title()}"`);
        try {
          const finalUrl = this.page.url();
          const detectedOrigin = new URL(finalUrl).origin;
          if (detectedOrigin && detectedOrigin.startsWith('http')) {
            console.log(`[ScraperEngine] Auto-detected active upstream IDLIX origin: ${detectedOrigin} (configured: ${this.baseUrl})`);
            this.baseUrl = detectedOrigin;
          }
        } catch (_) {}
        this.isReady = true;

        try {
          await this.ensureAuthenticated();
        } catch (authErr: any) {
          console.warn(`[ScraperEngine] Auto-login warning: ${authErr?.message || authErr}`);
        }
      } catch (err: any) {
        console.error(`[ScraperEngine] Failed to initialize browser session: ${err?.message || err}`);
        this.isReady = false;
        if (this.browser) {
          try {
            await this.browser.close();
          } catch (_) {}
          this.browser = null;
          this.page = null;
        }
        throw err;
      } finally {
        this.initPromise = null;
      }
    })();

    return this.initPromise;
  }

  public getSessionStatus(): { isReady: boolean; targetUrl: string; isLoggedIn: boolean } {
    return {
      isReady: this.isReady && this.page !== null && !this.page.isClosed(),
      targetUrl: this.baseUrl,
      isLoggedIn: this.isLoggedIn,
    };
  }

  public async ensureAuthenticated(force = false): Promise<boolean> {
    if (this.isLoggedIn && !force) {
      return true;
    }
    const ident = process.env.IDLIX_IDENTIFIER || this.authIdentifier;
    const pass = process.env.IDLIX_PASSWORD || this.authPassword;
    if (!ident || !pass) {
      console.warn('[ScraperEngine] Missing IDLIX_IDENTIFIER or IDLIX_PASSWORD in environment config');
      return false;
    }

    try {
      console.log(`[ScraperEngine] Authenticating with IDLIX user account (${ident})...`);
      const result = await this.executeFetch('/api/auth/login', {
        method: 'POST',
        body: {
          identifier: ident,
          password: pass,
          replaceExistingSessions: true,
        },
      });

      if (result.status === 200 && result.data?.user) {
        console.log(`[ScraperEngine] Successfully logged in as: ${result.data.user.username || result.data.user.email} (tier: ${result.data.user.plan || 'free'})`);
        this.isLoggedIn = true;
        this.loggedUser = result.data.user;
        return true;
      } else {
        console.warn(`[ScraperEngine] Authentication response: status ${result.status}`, result.data);
        this.isLoggedIn = false;
        this.loggedUser = null;
        return false;
      }
    } catch (err: any) {
      console.error(`[ScraperEngine] Authentication error: ${err?.message || err}`);
      this.isLoggedIn = false;
      this.loggedUser = null;
      return false;
    }
  }

  public async loginWithCredentials(
    ident: string,
    pass: string
  ): Promise<{ success: boolean; message: string; user?: any; status: string }> {
    this.authIdentifier = ident.trim();
    this.authPassword = pass.trim();
    process.env.IDLIX_IDENTIFIER = this.authIdentifier;
    process.env.IDLIX_PASSWORD = this.authPassword;

    try {
      console.log(`[ScraperEngine] User login attempt with account: ${this.authIdentifier}`);
      const result = await this.executeFetch('/api/auth/login', {
        method: 'POST',
        body: {
          identifier: this.authIdentifier,
          password: this.authPassword,
          replaceExistingSessions: true,
        },
      });

      if (result.status === 200 && (result.data?.user || result.data?.token || result.data?.success !== false)) {
        this.isLoggedIn = true;
        this.loggedUser = result.data.user || {
          email: this.authIdentifier,
          username: this.authIdentifier.split('@')[0],
          plan: 'VIP / Active',
        };
        console.log(`[ScraperEngine] Login success for ${this.authIdentifier}`);
        return {
          success: true,
          message: 'Berhasil masuk',
          status: 'Berhasil terhubung',
          user: this.loggedUser,
        };
      } else {
        this.isLoggedIn = false;
        this.loggedUser = null;
        const errMsg =
          result.data?.message ||
          result.data?.error ||
          (result.status === 401 ? 'Email atau kata sandi tidak sesuai' : `Gagal masuk (${result.status})`);
        return {
          success: false,
          message: errMsg,
          status: 'Gagal terhubung',
        };
      }
    } catch (err: any) {
      this.isLoggedIn = false;
      this.loggedUser = null;
      return {
        success: false,
        message: err?.message || 'Gagal menghubungi server autentikasi IDLIX',
        status: 'Gagal terhubung',
      };
    }
  }

  public getAuthStatus() {
    return {
      success: true,
      isLoggedIn: this.isLoggedIn,
      status: this.isLoggedIn ? 'Berhasil terhubung' : 'Belum terhubung',
      identifier: this.authIdentifier,
      user: this.loggedUser || (this.isLoggedIn ? { email: this.authIdentifier, username: this.authIdentifier.split('@')[0] } : null),
      serverUrl: this.baseUrl,
    };
  }

  public async logout(): Promise<{ success: boolean; message: string }> {
    this.isLoggedIn = false;
    this.loggedUser = null;
    this.authIdentifier = '';
    this.authPassword = '';
    delete process.env.IDLIX_IDENTIFIER;
    delete process.env.IDLIX_PASSWORD;
    return {
      success: true,
      message: 'Berhasil keluar',
    };
  }

  public async executeFetch<T = any>(
    endpoint: string,
    options: {
      method?: 'GET' | 'POST';
      body?: any;
      headers?: Record<string, string>;
      timeoutMs?: number;
      retryOnCf?: boolean;
    } = {}
  ): Promise<{ status: number; data: T }> {
    await this.init();

    if (!this.page || this.page.isClosed()) {
      throw new Error('Browser page session is closed');
    }

    const { method = 'GET', body, headers = {}, retryOnCf = true } = options;
    let url = endpoint;
    if (endpoint.startsWith('http')) {
      try {
        const parsed = new URL(endpoint);
        if (parsed.hostname.includes('idlix')) {
          url = `${this.baseUrl}${parsed.pathname}${parsed.search}`;
        }
      } catch (_) {}
    } else {
      url = `${this.baseUrl}${endpoint.startsWith('/') ? '' : '/'}${endpoint}`;
    }

    try {
      const result = await this.page.evaluate(
        async (
          fetchUrl: string,
          fetchMethod: string,
          fetchBody: any,
          fetchHeaders: Record<string, string>
        ) => {
          try {
            const res = await fetch(fetchUrl, {
              method: fetchMethod,
              headers: {
                Accept: 'application/json, text/plain, */*',
                'Content-Type': 'application/json',
                ...fetchHeaders,
              },
              body: fetchBody ? JSON.stringify(fetchBody) : undefined,
            });

            const text = await res.text();
            let parsedData = null;
            try {
              parsedData = JSON.parse(text);
            } catch (_) {
              parsedData = text;
            }

            return {
              status: res.status,
              data: parsedData,
              error: null,
            };
          } catch (e: any) {
            return {
              status: 500,
              data: null,
              error: e?.message || 'In-browser fetch error',
            };
          }
        },
        url,
        method,
        body,
        headers
      );

      if (result.error) {
        throw new Error(result.error);
      }

      // If Cloudflare returns 403 interstitial, reset session and retry once
      if (
        result.status === 403 &&
        typeof result.data === 'string' &&
        (result.data.includes('Just a moment') ||
          result.data.includes('cf-mitigated') ||
          result.data.includes('challenges.cloudflare.com'))
      ) {
        console.warn(`[ScraperEngine] Received Cloudflare 403 challenge for ${url}. Invalidating scraper session.`);
        this.isReady = false;
        this.page = null;
        if (retryOnCf) {
          console.log(`[ScraperEngine] Re-initializing session and retrying ${url}...`);
          await this.init();
          return this.executeFetch<T>(endpoint, { ...options, retryOnCf: false });
        }
      }

      // If upstream session is expired or revoked (401 Unauthorized), reset logged-in state
      if (
        result.status === 401 ||
        (result.data && typeof result.data === 'object' && result.data.error?.includes('Access token required'))
      ) {
        console.warn(`[ScraperEngine] Upstream returned 401 Unauthorized for ${url}. Invalidating user session.`);
        this.isLoggedIn = false;
      }

      return { status: result.status, data: result.data };
    } catch (err: any) {
      console.warn(`[ScraperEngine] Request failed for ${url}: ${err?.message}`);
      // If session might be stale or blocked, trigger re-init in background
      if (err?.message?.includes('Target closed') || err?.message?.includes('Session closed')) {
        this.isReady = false;
        this.page = null;
      }
      throw err;
    }
  }

  /**
   * Redeems a playback claim against Majorplay CDN or IDLIX endpoint.
   * Uses Node-side fetch for external CDN hosts so Referer/Origin headers are sent reliably without browser CORS limits.
   */
  public async redeemPlaybackUrl(
    redeemUrl: string,
    claim: string,
    mode = 'browser'
  ): Promise<{ status: number; data: any }> {
    let isExternalHost = false;
    if (redeemUrl.startsWith('http://') || redeemUrl.startsWith('https://')) {
      try {
        isExternalHost = new URL(redeemUrl).origin !== new URL(this.baseUrl).origin;
      } catch (_) {}
    }

    if (isExternalHost) {
      try {
        const res = await fetch(redeemUrl, {
          method: 'POST',
          headers: {
            Accept: 'application/json, text/plain, */*',
            'Content-Type': 'application/json',
            Referer: `${this.baseUrl}/`,
            Origin: this.baseUrl,
            'User-Agent':
              'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
          },
          body: JSON.stringify({ claim, mode }),
          signal: AbortSignal.timeout(20000),
        });

        const text = await res.text();
        let data: any = null;
        try {
          data = JSON.parse(text);
        } catch (_) {
          data = text;
        }

        if (res.ok && data?.url) {
          return { status: res.status, data };
        }
        console.warn(
          `[ScraperEngine] Node-side redeem returned status ${res.status} for ${redeemUrl}, falling back to browser fetch`
        );
      } catch (err: any) {
        console.warn(`[ScraperEngine] Node-side redeem failed (${err?.message}), falling back to browser fetch`);
      }
    }

    return this.executeFetch(redeemUrl, {
      method: 'POST',
      body: { claim, mode },
      headers: {
        Referer: `${this.baseUrl}/`,
        Origin: this.baseUrl,
      },
    });
  }

  /**
   * Resolves complete HLS playback in a single call for high performance
   */
  public async resolvePlayback(
    contentType: string,
    contentId: string,
    contentTitle?: string,
    slug?: string
  ): Promise<ResolvedPlaybackResult> {
    await this.init();
    await this.ensureAuthenticated();

    let targetType = contentType;
    let targetId = contentId;

    // Handle series: if tv_series or series is requested, resolve first episode of season 1
    if (contentType === 'tv_series' || contentType === 'series') {
      const seriesIdentifier = slug || contentId;
      let seriesRes = await this.executeFetch(`/api/series/${seriesIdentifier}`);
      if (seriesRes.status !== 200 && slug && slug !== contentId) {
        seriesRes = await this.executeFetch(`/api/series/${contentId}`);
      }
      const series = seriesRes.data?.data || seriesRes.data;
      const firstEp =
        series?.defaultSeason?.episodes?.[0] ||
        series?.seasons?.[0]?.episodes?.[0] ||
        (series?.seasons?.[0] && series?.slug
          ? (await this.executeFetch(`/api/series/${series.slug}/season/${series.seasons[0].seasonNumber}/episode/1`)).data?.episode
          : null);

      if (firstEp?.id) {
        targetType = 'episode';
        targetId = firstEp.id;
      } else {
        throw new Error('No episodes available for this series');
      }
    }

    // Step 1: Get play-info (gateToken + countdown)
    let playInfoRes = await this.executeFetch(`/api/watch/play-info/${targetType}/${targetId}`);
    if (playInfoRes.status === 401 || playInfoRes.status === 403) {
      console.warn(`[ScraperEngine] play-info returned ${playInfoRes.status}, re-authenticating...`);
      await this.ensureAuthenticated(true);
      playInfoRes = await this.executeFetch(`/api/watch/play-info/${targetType}/${targetId}`);
    }
    const playInfo = playInfoRes.data;
    if (!playInfo?.gateToken) {
      const errDetail = playInfo?.error || playInfo?.message || `status ${playInfoRes.status}`;
      throw new Error(`Failed to obtain playback gate token from provider (${errDetail})`);
    }

    // Extract device ID (did) from gateToken payload
    let didCookie: string | undefined;
    try {
      const rawP0 = playInfo.gateToken.split('.')[0];
      const decodedP0 = Buffer.from(rawP0, 'base64').toString('utf-8');
      const didMatch = /"owner":"d:([^"]+)"/.exec(decodedP0);
      if (didMatch?.[1]) {
        didCookie = `did=${didMatch[1]}`;
      }
    } catch (_) {}

    // Step 2: Handle unlock countdown
    const serverNow = playInfo.serverNow || Date.now();
    const unlockAt = playInfo.unlockAt || serverNow + 15000;
    const waitMs = Math.max(0, unlockAt - serverNow);

    if (waitMs > 0) {
      console.log(`[ScraperEngine] Synchronizing with provider countdown (${Math.round(waitMs / 1000)}s)...`);
      await new Promise((resolve) => setTimeout(resolve, Math.min(waitMs + 500, 30000)));
    }

    // Step 3: Claim session
    let claimRes: any = null;
    for (let attempt = 1; attempt <= 15; attempt++) {
      try {
        const claimResp = await this.executeFetch('/api/watch/session/claim', {
          method: 'POST',
          body: { gateToken: playInfo.gateToken },
          headers: didCookie ? { Cookie: didCookie } : undefined,
        });

        if (claimResp.status === 403 || claimResp.status === 401) {
          console.warn(`[ScraperEngine] Claim received status ${claimResp.status}, refreshing user session...`);
          this.isLoggedIn = false;
          await this.ensureAuthenticated(true);
        }

        if (claimResp.data?.claim && (!claimResp.data?.kind || claimResp.data?.kind === 'pentos')) {
          claimRes = claimResp.data;
          break;
        }
      } catch (_) {}
      await new Promise((r) => setTimeout(r, 600));
    }

    if (!claimRes?.claim) {
      throw new Error('Failed to claim playback session from provider');
    }

    // Step 4: Redeem HLS master playlist from Majorplay CDN
    let redeemUrl = claimRes.redeemUrl || 'https://e2e.majorplay.net/api/play';
    if (redeemUrl.startsWith('/watch')) {
      redeemUrl = '/api' + redeemUrl;
    }

    const redeemResp = await this.redeemPlaybackUrl(redeemUrl, claimRes.claim, 'browser');
    const redeemData = redeemResp.data;
    if (!redeemData?.url) {
      const errDetail = redeemData?.error || redeemData?.message || `status ${redeemResp.status}`;
      throw new Error(`Failed to redeem HLS master playlist URL (${errDetail})`);
    }

    return {
      title: claimRes.title || contentTitle || 'Video',
      hlsMasterUrl: redeemData.url,
      subtitles: Array.isArray(redeemData.subtitles) ? redeemData.subtitles : [],
      durationSec: claimRes.durationSec || 0,
      videoId: claimRes.videoId || '',
    };
  }

  public async close(): Promise<void> {
    if (this.browser) {
      await this.browser.close();
      this.browser = null;
      this.page = null;
      this.isReady = false;
    }
  }
}
