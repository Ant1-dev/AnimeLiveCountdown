// "My services" rules shared by the store, the settings page and scripts/check-my-services.ts.
// No imports, so Node can run the check script against this file directly.

export const MAX_CUSTOM_PLATFORMS = 10;
export const MAX_PLATFORM_NAME_LENGTH = 40;
export const MAX_PLATFORM_URL_LENGTH = 500;
/** Replaced by the show's URL-encoded title, turning a site link into a search link. */
export const TITLE_PLACEHOLDER = '{title}';

/** Same rule as the backend: lowercase letters and digits only, and a trailing "+" reads "plus". */
export function simplify(name: string): string {
  const base = name.toLowerCase().replace(/[^\p{L}\p{N}]/gu, '');
  return name.trim().endsWith('+') ? base + 'plus' : base;
}

/** An absolute http(s) URL with a host; {title} may appear only after the host. */
export function isValidPlatformUrl(url: string): boolean {
  const value = url.trim();
  if (!value || value.length > MAX_PLATFORM_URL_LENGTH) return false;
  const probe = value.split(TITLE_PLACEHOLDER).join('x');
  if (/[\s{}<>"]/.test(probe)) return false;
  try {
    const parsed = new URL(probe);
    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') return false;
    if (!parsed.hostname || parsed.username || parsed.password) return false;
    // Scheme and host as typed: the placeholder is only for paths and queries
    const match = /^https?:\/\/([^/?#]*)/i.exec(value);
    return match !== null && !match[1].includes('{');
  } catch {
    return false;
  }
}

/** The first rule a selection breaks, or null. Mirrors the backend's checks. */
export function validateSelection(selection: { customPlatforms?: { name?: string; url?: string }[] }): string | null {
  const customs = selection.customPlatforms ?? [];
  if (customs.length > MAX_CUSTOM_PLATFORMS) return `You can add up to ${MAX_CUSTOM_PLATFORMS} of your own platforms`;
  const names = new Set<string>();
  for (const c of customs) {
    const name = (c.name ?? '').trim();
    if (!name) return 'Give each of your platforms a name';
    if (name.length > MAX_PLATFORM_NAME_LENGTH) return `Platform names can be up to ${MAX_PLATFORM_NAME_LENGTH} characters`;
    if (names.has(name.toLowerCase())) return `You already added a platform called ${name}`;
    names.add(name.toLowerCase());
    if (!isValidPlatformUrl(c.url ?? '')) return `The link for ${name} must be a full http:// or https:// address`;
  }
  return null;
}

