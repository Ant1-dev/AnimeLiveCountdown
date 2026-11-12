/**
 * URL slugs for show pages: /anime/21/one-piece.
 *
 * Must stay identical to Slugs.java in the backend (sitemap feed and share pages build the same
 * URLs). Both are covered by the same test vectors.
 */
export function slugify(title: string | null | undefined): string {
  if (!title) return '';
  return title
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '') // strip accents: Pokémon -> Pokemon
    .toLowerCase()
    .replace(/['’]/g, '') // drop apostrophes: Frieren's -> frierens
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 80)
    .replace(/-+$/g, '');
}

/** The title a show is known by: English if it has one, otherwise romaji. */
export function preferredTitle(english?: string | null, romaji?: string | null): string {
  return english?.trim() || romaji?.trim() || '';
}

/** Canonical path of a show page. Falls back to /anime/:id when the title has no Latin characters. */
export function animePath(id: number, title?: string | null): string {
  const slug = slugify(title);
  return slug ? `/anime/${id}/${slug}` : `/anime/${id}`;
}
