# ==============================================================================
# Stage 1: Build TypeScript Artifacts
# ==============================================================================
FROM node:22-slim AS builder

WORKDIR /app

COPY package*.json tsconfig.json ./
RUN npm ci

COPY src ./src
RUN npm run build

# ==============================================================================
# Stage 2: Production Runtime with Chromium & Xvfb (Multi-Linux Support)
# ==============================================================================
FROM node:22-slim

# Install system dependencies for Chromium and headless execution
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
      chromium \
      xvfb \
      fonts-liberation \
      fonts-noto-color-emoji \
      libasound2 \
      libatk-bridge2.0-0 \
      libatk1.0-0 \
      libc6 \
      libcairo2 \
      libcups2 \
      libdbus-1-3 \
      libexpat1 \
      libfontconfig1 \
      libgbm1 \
      libgcc1 \
      libglib2.0-0 \
      libgtk-3-0 \
      libnspr4 \
      libnss3 \
      libpango-1.0-0 \
      libpangocairo-1.0-0 \
      libstdc++6 \
      libx11-6 \
      libx11-xcb1 \
      libxcb1 \
      libxcomposite1 \
      libxcursor1 \
      libxdamage1 \
      libxext6 \
      libxfixes3 \
      libxi6 \
      libxrandr2 \
      libxrender1 \
      libxss1 \
      libxtst6 \
      ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Universal browser environment variables
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium \
    CHROME_PATH=/usr/bin/chromium \
    CHROME_BIN=/usr/bin/chromium \
    NODE_ENV=production \
    PORT=5376 \
    HOST=0.0.0.0

WORKDIR /app

# Install production dependencies only
COPY package*.json ./
RUN npm ci --omit=dev

# Copy compiled files from builder and static assets
COPY --from=builder /app/dist ./dist
COPY public ./public

# Setup non-root sandbox user
RUN groupadd -r pptruser && useradd -r -g pptruser -G audio,video pptruser \
    && mkdir -p /home/pptruser/Downloads \
    && chown -R pptruser:pptruser /home/pptruser \
    && chown -R pptruser:pptruser /app

USER pptruser

EXPOSE 5376

CMD ["node", "dist/server.js"]
