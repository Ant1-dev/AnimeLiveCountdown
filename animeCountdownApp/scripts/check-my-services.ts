// Checks src/app/shared/my-services-rules.ts against the vectors the backend's MyServicesVectorsTest
// also uses, so service names match and links validate the same way on both sides.
// Run: npm run test:my-services   (Node 22.6+ runs TypeScript directly)
import { readFileSync } from 'node:fs';
import { isValidPlatformUrl, simplify, validateSelection } from '../src/app/shared/my-services-rules.ts';

const vectors: { names: [string, string][]; urls: [string, boolean][] } = JSON.parse(
  readFileSync(new URL('../../animeCountdownBackend/src/test/resources/my-services-vectors.json', import.meta.url), 'utf8'),
);

const failures: string[] = [];
for (const [input, want] of vectors.names) {
  if (simplify(input) !== want) failures.push(`simplify(${JSON.stringify(input)}) = ${JSON.stringify(simplify(input))}, want ${JSON.stringify(want)}`);
}
for (const [input, want] of vectors.urls) {
  if (isValidPlatformUrl(input) !== want) failures.push(`isValidPlatformUrl(${JSON.stringify(input)}) = ${!want}, want ${want}`);
}

const selectionCases: [Parameters<typeof validateSelection>[0], boolean][] = [
  [{ customPlatforms: [] }, true],
  [{ customPlatforms: [{ name: 'Plex', url: 'https://app.plex.tv/search?q={title}' }] }, true],
  [{ customPlatforms: [{ name: '', url: 'https://a.example' }] }, false],
  [{ customPlatforms: [{ name: 'x'.repeat(41), url: 'https://a.example' }] }, false],
  [{ customPlatforms: [{ name: 'Plex', url: 'https://a.example' }, { name: 'plex', url: 'https://b.example' }] }, false],
  [{ customPlatforms: Array.from({ length: 11 }, (_, i) => ({ name: `Site ${i}`, url: `https://s${i}.example` })) }, false],
];
for (const [selection, ok] of selectionCases) {
  if ((validateSelection(selection) === null) !== ok) failures.push(`validateSelection(${JSON.stringify(selection).slice(0, 80)}) should ${ok ? 'pass' : 'fail'}`);
}

for (const f of failures) console.error(`FAIL ${f}`);
const total = vectors.names.length + vectors.urls.length + selectionCases.length;
console.log(`${total - failures.length}/${total} my-services checks pass`);
process.exit(failures.length ? 1 : 0);
