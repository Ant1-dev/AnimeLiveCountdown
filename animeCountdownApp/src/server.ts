import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import compression from 'compression';
import { environment } from './environments/environment';
import { buildSitemap, SitemapShow } from './app/seo/sitemap';

const serverDistFolder = dirname(fileURLToPath(import.meta.url));
const browserDistFolder = resolve(serverDistFolder, '../browser');
const app = express();
const angularApp = new AngularNodeAppEngine();

app.use(compression({
  level: 6,
  threshold: 0,
  filter: (req, res) => {
    // Images and video are already compressed
    if (req.headers['accept']?.includes('image/') || req.headers['accept']?.includes('video/')) {
      return false;
    }
    return compression.filter(req, res);
  },
}));

/**
 * sitemap.xml, generated here rather than by the API so it keeps working when the API is down:
 * static and season pages are always listed, show pages whenever the API answers.
 */
let sitemapCache: { xml: string; expires: number } | null = null;

app.get('/sitemap.xml', async (_req, res) => {
  const now = Date.now();
  if (!sitemapCache || sitemapCache.expires < now) {
    let shows: SitemapShow[] = [];
    let complete = false;
    try {
      const response = await fetch(`${environment.apiUrl}sitemap/media`, { signal: AbortSignal.timeout(8000) });
      if (response.ok) {
        shows = await response.json();
        complete = true;
      }
    } catch {
      // API unreachable: serve the static part and try again soon
    }
    // A full sitemap is cached for an hour; a partial one only for 5 minutes
    sitemapCache = { xml: buildSitemap(shows, new Date(now)), expires: now + (complete ? 3_600_000 : 300_000) };
  }
  res
    .type('application/xml')
    .set('Cache-Control', `public, max-age=${Math.max(60, Math.round((sitemapCache.expires - now) / 1000))}`)
    .send(sitemapCache.xml);
});

/**
 * Serve static files from /browser (only used when running this server directly; on Vercel the
 * static files are served from the CDN before requests reach this function).
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

/**
 * Render every other request with Angular. Pages set their own status codes (404 for unknown
 * pages, 301 to canonical URLs, 503 when the API is unavailable).
 */
app.use((req, res, next) => {
  angularApp
    .handle(req)
    .then((response) => (response ? writeResponseToNodeResponse(response, res) : next()))
    .catch(next);
});

if (isMainModule(import.meta.url)) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, () => {
    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/** Request handler used by the Angular CLI dev server and by the Vercel function (scripts/vercel-build.mjs). */
export const reqHandler = createNodeRequestHandler(app);
