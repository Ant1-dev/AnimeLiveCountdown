import { RenderMode, ServerRoute } from '@angular/ssr';

/**
 * How each route is rendered.
 * - Public pages render on the server per request so search engines and link previews get real
 *   content and per-page meta tags. Their data (airing times) changes constantly, so they aren't
 *   prerendered at build time.
 * - Signed-in-only pages render in the browser; the server never knows who is signed in.
 */
export const serverRoutes: ServerRoute[] = [
  { path: 'following', renderMode: RenderMode.Client },
  { path: 'notifications', renderMode: RenderMode.Client },
  { path: '**', renderMode: RenderMode.Server },
];
