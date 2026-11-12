import { ImageLoaderConfig } from '@angular/common';
import { environment } from '../../environments/environment';

/**
 * Resized, re-encoded images through Vercel Image Optimization (/_vercel/image).
 *
 * AniList serves banners and covers at full size (a 1900px, ~200 KB JPEG banner for a 400px-wide
 * phone screen). In production builds, AniList image URLs are rewritten to Vercel's optimizer,
 * which returns WebP at the requested width. Widths must be one of IMAGE_WIDTHS: the same list is
 * written to .vercel/output/config.json by scripts/vercel-build.mjs. Few widths and one format keep
 * the number of distinct transformations (Vercel's billing unit) low.
 */
export const IMAGE_WIDTHS = [256, 640, 1080, 1920];
export const IMAGE_QUALITY = 70;
const OPTIMIZABLE = /^https:\/\/s4\.anilist\.co\//;

/** URL for `src` at (at least) `width` CSS pixels; the original URL when optimization is off. */
export function optimizedImageUrl(src: string | null | undefined, width: number): string {
  if (!src) return '';
  if (!environment.imageOptimization || !OPTIMIZABLE.test(src)) return src;
  const w = IMAGE_WIDTHS.find((allowed) => allowed >= width) ?? IMAGE_WIDTHS[IMAGE_WIDTHS.length - 1];
  return `/_vercel/image?url=${encodeURIComponent(src)}&w=${w}&q=${IMAGE_QUALITY}`;
}

/** Loader for NgOptimizedImage: builds every srcset entry through the optimizer. */
export function vercelImageLoader(config: ImageLoaderConfig): string {
  // Without a width (the plain src fallback), a mid-size image is a safe default
  return optimizedImageUrl(config.src, config.width ?? 1080);
}
