import { animePath, preferredTitle } from './slug';
import { SITE_URL } from './site';

/** One show from the backend's sitemap feed (GET /api/sitemap/media). */
export interface SitemapShow {
  id: number;
  engtitle?: string | null;
  romtitle?: string | null;
}

const WEEK_DAYS = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'];
const SEASONS = ['winter', 'spring', 'summer', 'fall'] as const;

/** Season pages worth listing: two years back through next season. */
export function seasonPaths(now: Date): string[] {
  let year = now.getUTCFullYear();
  let index = Math.floor(now.getUTCMonth() / 3); // Jan-Mar winter, Apr-Jun spring, ...
  // start at next season
  index += 1;
  if (index > 3) { index = 0; year += 1; }
  const paths: string[] = [];
  for (let i = 0; i < 10; i++) {
    paths.push(`/seasons/${year}/${SEASONS[index]}`);
    index -= 1;
    if (index < 0) { index = 3; year -= 1; }
  }
  return paths;
}

/** The sitemap XML. Static pages are always listed; shows only when the backend answered. */
export function buildSitemap(shows: SitemapShow[], now: Date): string {
  const paths = [
    '/',
    '/seasons',
    '/most-hyped',
    ...WEEK_DAYS.map((d) => `/schedule/${d}`),
    ...seasonPaths(now),
    ...shows.map((s) => animePath(s.id, preferredTitle(s.engtitle, s.romtitle))),
  ];
  const unique = [...new Set(paths)];
  const urls = unique.map((p) => `  <url><loc>${escapeXml(SITE_URL + (p === '/' ? '/' : p))}</loc></url>`).join('\n');
  return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`;
}

function escapeXml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;');
}
