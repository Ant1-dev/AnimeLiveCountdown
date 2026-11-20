import { ImageLoaderConfig } from '@angular/common';
import { environment } from '../../environments/environment';

/**
 * Resized, re-encoded AniList images from our own image server (imgproxy on the backend VM,
 * see animeCountdownBackend/deploy/imgproxy.sh).
 *
 * AniList serves banners and covers at full size (a 1900px, ~200 KB JPEG banner for a 400px-wide
 * phone screen). In production builds, AniList image URLs are rewritten to the image server, which
 * returns WebP at the requested width. The server only accepts the widths in IMAGE_WIDTHS (as
 * presets w256, w640, ...), so the two lists must match.
 *
 * If the image server fails, an inline script in index.html swaps in the original AniList URL.
 */
export const IMAGE_WIDTHS = [256, 640, 1080, 1920];
const OPTIMIZABLE = /^https:\/\/s4\.anilist\.co\//;
const PREFIX = environment.imageHost ? `${environment.imageHost}/_/` : '';

/** URL for `src` at (at least) `width` CSS pixels; the original URL when there's no image server. */
export function optimizedImageUrl(src: string | null | undefined, width: number): string {
  if (!src) return '';
  if (!PREFIX || !OPTIMIZABLE.test(src)) return src;
  const w = IMAGE_WIDTHS.find((allowed) => allowed >= width) ?? IMAGE_WIDTHS[IMAGE_WIDTHS.length - 1];
  return `${PREFIX}w${w}/plain/${encodeURIComponent(src)}`;
}

/** Loader for NgOptimizedImage: builds every srcset entry through the image server. */
export function imageLoader(config: ImageLoaderConfig): string {
  // Without a width (the plain src fallback), a mid-size image is a safe default
  return optimizedImageUrl(config.src, config.width ?? 1080);
}
