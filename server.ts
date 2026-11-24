import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { GoogleGenAI, Type } from '@google/genai';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = 3000;
const HOST = '0.0.0.0';

const VALID_CATEGORIES = [
  'FOOD',
  'GROCERIES',
  'TRANSPORT_FUEL',
  'CONVENIENCE',
  'GADGETS',
  'UTILITIES',
  'SHOPPING',
  'OTHER',
] as const;

function parseSemver(version: string): number[] {
  return version
    .trim()
    .replace(/^[vV]/, '')
    .split('-')[0]
    .split('.')
    .map((part) => Number.parseInt(part, 10))
    .filter((n) => !Number.isNaN(n));
}

function isNewerVersion(remoteTag: string, localVersion: string): boolean {
  const remoteParts = parseSemver(remoteTag);
  const localParts = parseSemver(localVersion);
  if (remoteParts.length === 0 || localParts.length === 0) {
    return remoteTag.replace(/^v/i, '') !== localVersion.replace(/^v/i, '');
  }
  const maxLen = Math.max(remoteParts.length, localParts.length);
  for (let i = 0; i < maxLen; i += 1) {
    const r = remoteParts[i] ?? 0;
    const l = localParts[i] ?? 0;
    if (r > l) return true;
    if (r < l) return false;
  }
  return false;
}

async function createApp() {
  const app = express();
  app.use(express.json({ limit: '64kb' }));

  app.post('/api/categorize', async (req, res) => {
    const merchantRaw = typeof req.body?.merchant === 'string' ? req.body.merchant.trim().slice(0, 120) : '';
    const snippetRaw = typeof req.body?.rawSnippet === 'string' ? req.body.rawSnippet.trim().slice(0, 200) : '';

    if (!merchantRaw) {
      res.status(400).json({ error: 'Merchant name is required.' });
      return;
    }

    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      res.status(503).json({ error: 'GEMINI_API_KEY is not configured on the server.' });
      return;
    }

    try {
      const ai = new GoogleGenAI({
        apiKey,
        httpOptions: {
          headers: {
            'User-Agent': 'aistudio-build',
          },
        },
      });

      const promptLines = [
        'Classify this purchase merchant into exactly one spending category.',
        `Merchant: ${merchantRaw}`,
        ...(snippetRaw ? [`Alert context: ${snippetRaw}`] : []),
        'Categories: FOOD (restaurants, cafes, fast food, food delivery), GROCERIES (supermarkets, fresh markets, warehouse clubs), TRANSPORT_FUEL (gas stations, fuel, tolls, parking, ride-hailing, transit), CONVENIENCE (convenience stores, mini-marts), GADGETS (electronics, gadget stores, phone/computer shops, accessories), UTILITIES (electric, water, internet, mobile plans, streaming subscriptions), SHOPPING (apparel, department stores, online marketplaces, home goods), OTHER (anything else).',
      ];

      const response = await ai.models.generateContent({
        model: 'gemini-4-hydrogen',
        contents: promptLines.join('\n'),
        config: {
          responseMimeType: 'application/json',
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              category: {
                type: Type.STRING,
                description: 'One of FOOD, GROCERIES, TRANSPORT_FUEL, CONVENIENCE, GADGETS, UTILITIES, SHOPPING, OTHER',
              },
            },
          },
        },
      });

      const rawText = (response.text ?? '{}').trim();
      const parsed = JSON.parse(rawText) as { category?: string };
      const matched = VALID_CATEGORIES.includes((parsed.category ?? '') as (typeof VALID_CATEGORIES)[number])
        ? parsed.category
        : 'OTHER';
      res.json({ category: matched, usedAiFallback: true });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to classify merchant.';
      res.status(502).json({ error: message });
    }
  });

  app.get('/api/github-release', async (req, res) => {
    const repoParam = typeof req.query.repo === 'string' ? req.query.repo : '';
    const currentVersion = typeof req.query.currentVersion === 'string' ? req.query.currentVersion : '1.0.32';
    const currentCode = Number.parseInt(typeof req.query.currentCode === 'string' ? req.query.currentCode : '39', 10) || 39;

    const clean = repoParam.trim().replace(/^https:\/\/github\.com\//i, '').replace(/^\/+|\/+$/g, '');
    const parts = clean.split('/').filter(Boolean);
    if (parts.length !== 2 || !/^[a-zA-Z0-9_.-]+$/.test(parts[0]) || !/^[a-zA-Z0-9_.-]+$/.test(parts[1])) {
      res.json({ notice: null });
      return;
    }

    const [owner, repo] = parts;

    try {
      const releaseResp = await fetch(`https://api.github.com/repos/${owner}/${repo}/releases/latest`, {
        headers: {
          Accept: 'application/vnd.github+json',
          'User-Agent': 'SageLedger-Web',
        },
      });

      if (releaseResp.ok) {
        const dto = (await releaseResp.json()) as {
          tag_name?: string;
          name?: string;
          body?: string;
          html_url?: string;
          draft?: boolean;
          prerelease?: boolean;
          assets?: Array<{ name?: string; browser_download_url?: string }>;
        };
        if (!dto.draft && !dto.prerelease && dto.tag_name) {
          const apkAsset = dto.assets?.find((a) => a.name?.toLowerCase().endsWith('.apk'))?.browser_download_url;
          const targetUrl = apkAsset || dto.html_url || '';
          if (targetUrl && isNewerVersion(dto.tag_name, currentVersion)) {
            res.json({
              notice: {
                tagName: dto.tag_name,
                releaseName: dto.name || `Update ${dto.tag_name}`,
                notes: dto.body || `A new release (${dto.tag_name}) is available on GitHub.`,
                htmlUrl: targetUrl,
              },
            });
            return;
          }
        }
      }

      for (const branch of ['main', 'master']) {
        const manifestResp = await fetch(
          `https://raw.githubusercontent.com/${owner}/${repo}/${branch}/apk/version.json`,
          { headers: { 'User-Agent': 'SageLedger-Web' } }
        );
        if (manifestResp.status === 404) continue;
        if (!manifestResp.ok) break;
        const manifest = (await manifestResp.json()) as {
          versionName?: string;
          versionCode?: number;
          apkFile?: string;
          notes?: string;
        };
        const remoteName = (manifest.versionName ?? '').trim();
        const apkFile = (manifest.apkFile ?? '').trim() || 'SageLedger-latest.apk';
        const isNewer =
          (manifest.versionCode ?? 0) > currentCode ||
          (remoteName.length > 0 && isNewerVersion(remoteName, currentVersion));
        if (isNewer) {
          const tag = remoteName.toLowerCase().startsWith('v') ? remoteName : `v${remoteName}`;
          res.json({
            notice: {
              tagName: tag,
              releaseName: `New APK build (${tag})`,
              notes: manifest.notes || 'A new APK build is ready in the repository apk/ folder.',
              htmlUrl: `https://github.com/${owner}/${repo}/raw/${branch}/apk/${apkFile}`,
            },
          });
          return;
        }
        break;
      }

      res.json({ notice: null });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'GitHub release check failed';
      res.status(502).json({ error: message });
    }
  });

  if (process.env.NODE_ENV !== 'production') {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distDir = path.resolve(__dirname, 'dist');
    app.use(express.static(distDir));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distDir, 'index.html'));
    });
  }

  app.listen(PORT, HOST, () => {
    console.log(`SageLedger server running at http://${HOST}:${PORT}`);
  });
}

createApp().catch((err) => {
  console.error('Failed to start SageLedger server:', err);
  process.exit(1);
});
