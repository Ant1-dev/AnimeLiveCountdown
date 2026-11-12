import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { DEFAULT_IMAGE, SITE_NAME, SITE_URL } from './site';

export interface PageSeo {
  /** Page title without the site name; the name is appended when it fits in ~65 characters. */
  title: string;
  /** Aim for 120-160 characters; longer text is cut at a word boundary. */
  description: string;
  /** Canonical path, e.g. /anime/21/one-piece. Query strings are never part of the canonical. */
  path: string;
  image?: string;
  imageAlt?: string;
  type?: 'website' | 'video.tv_show';
  /** Keep the page out of search results (signed-in pages, not-found pages). */
  noindex?: boolean;
  /** Structured data (schema.org JSON-LD) for this page. */
  jsonLd?: object[];
}

const MAX_TITLE = 65;
const MAX_DESCRIPTION = 160;

/**
 * Sets every per-page tag search engines and link previews read: title, description, canonical,
 * robots, Open Graph, Twitter and JSON-LD. Works during server rendering, which is what crawlers
 * see. Every routed page calls setPage().
 */
@Injectable({ providedIn: 'root' })
export class SeoService {
  private title = inject(Title);
  private meta = inject(Meta);
  private document = inject(DOCUMENT);

  setPage(page: PageSeo): void {
    const fullTitle = withSiteName(page.title);
    const description = truncate(page.description, MAX_DESCRIPTION);
    const url = SITE_URL + (page.path === '/' ? '/' : page.path.replace(/\/+$/, ''));
    const image = page.image || DEFAULT_IMAGE;

    this.title.setTitle(fullTitle);
    this.meta.updateTag({ name: 'description', content: description });
    this.meta.updateTag({ name: 'robots', content: page.noindex ? 'noindex, follow' : 'index, follow, max-image-preview:large' });

    this.meta.updateTag({ property: 'og:site_name', content: SITE_NAME });
    this.meta.updateTag({ property: 'og:title', content: page.title });
    this.meta.updateTag({ property: 'og:description', content: description });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ property: 'og:type', content: page.type ?? 'website' });
    this.meta.updateTag({ property: 'og:image', content: image });
    this.meta.updateTag({ property: 'og:image:alt', content: page.imageAlt ?? page.title });

    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ name: 'twitter:title', content: page.title });
    this.meta.updateTag({ name: 'twitter:description', content: description });
    this.meta.updateTag({ name: 'twitter:image', content: image });

    this.setCanonical(url);
    this.setJsonLd(page.jsonLd ?? []);
  }

  private setCanonical(url: string): void {
    let link = this.document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!link) {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      this.document.head.appendChild(link);
    }
    link.setAttribute('href', url);
  }

  private setJsonLd(items: object[]): void {
    this.document.head.querySelectorAll('script[data-seo="page"]').forEach((el) => el.remove());
    for (const item of items) {
      const script = this.document.createElement('script');
      script.setAttribute('type', 'application/ld+json');
      script.setAttribute('data-seo', 'page');
      // Escape "<" so text from the API can never close the script tag
      script.textContent = JSON.stringify(item).replace(/</g, '\\u003c');
      this.document.head.appendChild(script);
    }
  }
}

function withSiteName(title: string): string {
  const full = `${title} | ${SITE_NAME}`;
  return full.length <= MAX_TITLE ? full : title;
}

/** Cut at a word boundary and add an ellipsis. */
export function truncate(text: string, max: number): string {
  const clean = text.replace(/\s+/g, ' ').trim();
  if (clean.length <= max) return clean;
  const cut = clean.slice(0, max - 1);
  const lastSpace = cut.lastIndexOf(' ');
  return (lastSpace > max * 0.6 ? cut.slice(0, lastSpace) : cut).replace(/[\s,.;:-]+$/, '') + '…';
}

/** AniList descriptions contain HTML (<br>, <i>) and a "(Source: …)" line; make them plain text. */
export function plainText(html: string | null | undefined): string {
  if (!html) return '';
  return html
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/<[^>]+>/g, '')
    .replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&lt;/g, '<').replace(/&gt;/g, '>')
    .replace(/\(Source:[^)]*\)/gi, '')
    .replace(/\s+/g, ' ')
    .trim();
}

/** schema.org BreadcrumbList from [name, path] pairs. */
export function breadcrumbs(items: [string, string][]): object {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: items.map(([name, path], i) => ({
      '@type': 'ListItem',
      position: i + 1,
      name,
      item: SITE_URL + path,
    })),
  };
}
