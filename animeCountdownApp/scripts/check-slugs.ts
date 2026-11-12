// Checks src/app/seo/slug.ts against the vectors the backend's SlugsTest also uses.
// Run: npm run test:slugs   (Node 22.6+ runs TypeScript directly)
import { readFileSync } from 'node:fs';
import { slugify } from '../src/app/seo/slug.ts';

const vectors: [string, string][] = JSON.parse(
  readFileSync(new URL('../../animeCountdownBackend/src/test/resources/slug-vectors.json', import.meta.url), 'utf8'),
);
const failures = vectors.filter(([input, want]) => slugify(input) !== want);
for (const [input, want] of failures) console.error(`FAIL ${JSON.stringify(input)} -> ${JSON.stringify(slugify(input))}, want ${JSON.stringify(want)}`);
console.log(`${vectors.length - failures.length}/${vectors.length} slug vectors pass`);
process.exit(failures.length ? 1 : 0);
