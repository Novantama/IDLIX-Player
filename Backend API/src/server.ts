import 'dotenv/config';
import os from 'node:os';
import { exec } from 'node:child_process';
import Fastify from 'fastify';
import cors from '@fastify/cors';
import { apiRoutes } from './routes/api.js';
import { IdlixScraperEngine } from './engine/IdlixScraperEngine.js';
import { renderDashboardHtml } from './services/DashboardView.js';

const startTime = new Date();

function getLocalIpAddresses(): string[] {
  const nets = os.networkInterfaces();
  const ips: string[] = [];
  for (const name of Object.keys(nets)) {
    for (const net of nets[name] || []) {
      if (net.family === 'IPv4' && !net.internal && !net.address.startsWith('169.254')) {
        ips.push(net.address);
      }
    }
  }
  return ips;
}

const fastify = Fastify({
  logger: {
    level: process.env.LOG_LEVEL || 'info',
  },
});

async function main() {
  await fastify.register(cors, {
    origin: true,
  });

  const port = parseInt(process.env.PORT || '5376', 10);
  const host = process.env.HOST || '0.0.0.0';

  // Live Web Dashboard
  fastify.get('/', async (_req, reply) => {
    return reply.type('text/html').send(renderDashboardHtml(port, startTime));
  });

  await fastify.register(apiRoutes);

  try {
    await fastify.listen({ port, host });
    const localIps = getLocalIpAddresses();
    console.log('\n======================================================');
    console.log('       IDLIX BACKEND API BERHASIL AKTIF');
    console.log('======================================================');
    console.log(` > Akses Laptop (Local) : http://localhost:${port}`);
    if (localIps.length > 0) {
      localIps.forEach((ip) => {
        console.log(` > Akses Android TV/HP  : http://${ip}:${port}`);
      });
      console.log(' (Masukkan alamat Akses Android TV/HP ke aplikasi Android TV)');
    } else {
      console.log(` > Akses Network        : http://${host}:${port}`);
    }
    console.log('======================================================\n');

    // Automatically open live web dashboard in browser like KRX18
    if (process.env.OPEN_BROWSER !== 'false') {
      const dashboardUrl = `http://localhost:${port}/`;
      try {
        if (process.platform === 'win32') {
          exec(`start "" "${dashboardUrl}"`);
        } else if (process.platform === 'darwin') {
          exec(`open "${dashboardUrl}"`);
        } else if (process.platform === 'linux') {
          exec(`xdg-open "${dashboardUrl}"`);
        }
      } catch (_err) {}
    }

    // Pre-warm scraper browser session in the background
    IdlixScraperEngine.getInstance().init().catch((err) => {
      console.warn(`[Server] Initial scraper pre-warm notice: ${err?.message}`);
    });
  } catch (err) {
    fastify.log.error(err);
    process.exit(1);
  }
}

// Unhandled errors protection for 24/7 server stability
process.on('unhandledRejection', (reason, _promise) => {
  console.warn('[Server] Unhandled Rejection prevented crash:', reason);
});

process.on('uncaughtException', (err) => {
  console.error('[Server] Uncaught Exception prevented crash:', err);
});

// Graceful shutdown
const shutdown = async () => {
  console.log('\n[Server] Shutting down gracefully...');
  await IdlixScraperEngine.getInstance().close();
  await fastify.close();
  process.exit(0);
};

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

main();
