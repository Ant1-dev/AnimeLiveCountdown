// Builds the Angular app with server-side rendering and packages it for Vercel using the
// Build Output API (https://vercel.com/docs/build-output-api):
//   .vercel/output/static            browser files, served from Vercel's CDN
//   .vercel/output/functions/ssr.func Angular's server bundle as a Node.js function
//   .vercel/output/config.json        routing: static files first, everything else to the function
//
// Vercel has no built-in Angular SSR support, so this keeps the setup explicit and self-contained.
import { execSync } from 'node:child_process';
import { cpSync, existsSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();
const dist = join(root, 'dist/anime-countdown-app');
const out = join(root, '.vercel/output');
const apiOrigin = process.env.API_ORIGIN || 'https://api.animelivecountdown.com';

execSync('npx ng build', { stdio: 'inherit' });
if (!existsSync(join(dist, 'server/server.mjs'))) {
  throw new Error('Expected an SSR build at dist/anime-countdown-app/server/server.mjs');
}

rmSync(out, { recursive: true, force: true });

// Static assets (hashed bundles, icons, images, robots.txt, sw.js)
cpSync(join(dist, 'browser'), join(out, 'static'), { recursive: true });

// SSR function. The server bundle is self-contained (only Node built-ins are imported).
const fn = join(out, 'functions/ssr.func');
mkdirSync(fn, { recursive: true });
cpSync(join(dist, 'server'), join(fn, 'server'), { recursive: true });
writeFileSync(join(fn, 'index.mjs'), "import { reqHandler } from './server/server.mjs';\nexport default reqHandler;\n");
writeFileSync(join(fn, 'package.json'), JSON.stringify({ type: 'module' }) + '\n');
writeFileSync(
  join(fn, '.vc-config.json'),
  JSON.stringify(
    { runtime: 'nodejs22.x', handler: 'index.mjs', launcherType: 'Nodejs', shouldAddHelpers: false, maxDuration: 15 },
    null,
    2,
  ) + '\n',
);

// Link-preview bots (Discord, X, Slack, ...) get the API's share page, which has a live-countdown image.
const previewBots =
  'Discordbot|Twitterbot|Slackbot|facebookexternalhit|Facebot|WhatsApp|TelegramBot|LinkedInBot|SkypeUriPreview|redditbot|Embedly|Iframely|Mastodon|Bluesky|vkShare|Pinterest';

// Must match IMAGE_WIDTHS / IMAGE_QUALITY in src/app/shared/images.ts
const imageWidths = [256, 640, 1080, 1920];

const config = {
  version: 3,
  // Vercel Image Optimization at /_vercel/image: resizes AniList images and serves WebP
  images: {
    sizes: imageWidths,
    domains: [],
    remotePatterns: [{ protocol: 'https', hostname: '^s4\\.anilist\\.co$', pathname: '^/file/.*$' }],
    qualities: [70],
    formats: ['image/webp'],
    minimumCacheTTL: 2678400, // 31 days: AniList image URLs change when the image does
  },
  routes: [
    // www -> apex, permanently (308). Vercel's domain settings may also do this.
    {
      src: '^/(.*)$',
      has: [{ type: 'host', value: 'www.animelivecountdown.com' }],
      headers: { Location: 'https://animelivecountdown.com/$1' },
      status: 308,
    },
    // Hashed build files never change
    {
      src: '^/(?:[^/]+-[A-Z0-9]{8}\\.(?:js|mjs|css)|media/.+\\.(?:woff2?|ttf))$',
      headers: { 'Cache-Control': 'public, max-age=31536000, immutable' },
      continue: true,
    },
    {
      src: '^/(?:media|anime)/(\\d+)(?:/[^/]*)?$',
      has: [{ type: 'header', key: 'user-agent', value: `.*(?:${previewBots}).*` }],
      dest: `${apiOrigin}/api/share/media/$1`,
    },
    { handle: 'filesystem' },
    { src: '^/(.*)$', dest: '/ssr' },
  ],
};
writeFileSync(join(out, 'config.json'), JSON.stringify(config, null, 2) + '\n');

console.log('Vercel output written to .vercel/output');
