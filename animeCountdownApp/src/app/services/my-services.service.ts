import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, map, Observable, of, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { injectIsBrowser } from '../shared/platform';
import {
  CustomPlatform,
  MyServicesSelection,
  ServiceLink,
  ServiceStatus,
  StreamingService,
} from '../models/streaming-service.model';
import {
  MAX_CUSTOM_PLATFORMS,
  TITLE_PLACEHOLDER,
  isValidPlatformUrl,
  simplify,
  validateSelection,
} from '../shared/my-services-rules';

export { isValidPlatformUrl } from '../shared/my-services-rules';

const SELECTION_KEY = 'alc.myServices';
const ONLY_MINE_KEY = 'alc.myServicesOnly';
const AVAILABILITY_BATCH = 200;
const EMPTY: MyServicesSelection = { services: [], customPlatforms: [] };

/**
 * The viewer's streaming services ("My services"): picked catalog services and their own platforms.
 * Signed in, they're saved on the account; signed out, in localStorage. Also caches which services
 * carry which shows, for the "My services only" filter and the Watch links on show cards.
 */
@Injectable({ providedIn: 'root' })
export class MyServicesService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private isBrowser = injectIsBrowser();

  readonly catalog = signal<StreamingService[]>([]);
  readonly selection = signal<MyServicesSelection>(EMPTY);
  /** True once the picks for the current session (account or device) are known. */
  readonly loaded = signal(false);
  /** The "My services only" toggle; remembered on this device. */
  readonly onlyMine = signal(false);

  private availability = signal<ReadonlyMap<number, ServiceLink[] | null>>(new Map());
  private requested = new Set<number>();

  readonly selected = computed(() => new Set(this.selection().services));
  readonly hasServices = computed(() => this.selection().services.length > 0);
  readonly hasAny = computed(() => this.hasServices() || this.selection().customPlatforms.length > 0);
  /** The filter hides shows only when it's on and there are services to filter by. */
  readonly filterActive = computed(() => this.onlyMine() && this.hasServices());

  private byKey = computed(() => new Map(this.catalog().map((s) => [s.key, s])));
  private keyByAlias = computed(() => {
    const out = new Map<string, string>();
    for (const s of this.catalog()) {
      out.set(simplify(s.key), s.key);
      for (const alias of s.aliases ?? []) out.set(simplify(alias), s.key);
    }
    return out;
  });

  constructor() {
    if (!this.isBrowser) return;
    this.onlyMine.set(readStorage(ONLY_MINE_KEY) === '1');
    this.http
      .get<StreamingService[]>(`${environment.apiUrl}streaming-services`)
      .pipe(catchError(() => of([] as StreamingService[])))
      .subscribe((catalog) => this.catalog.set(catalog));

    effect(() => {
      const user = this.auth.user();
      this.loaded.set(false);
      const local = readLocalSelection();
      if (!user) {
        this.selection.set(local);
        this.loaded.set(true);
        return;
      }
      this.http.get<MyServicesSelection>(`${environment.apiUrl}auth/my-services`).subscribe({
        next: (saved) => {
          const fromAccount = normalizeSelection(saved);
          // First sign-in after picking services signed out: keep those picks on the account
          if (isEmpty(fromAccount) && !isEmpty(local)) {
            this.selection.set(local);
            this.http.put<MyServicesSelection>(`${environment.apiUrl}auth/my-services`, local).subscribe({
              next: (clean) => this.selection.set(normalizeSelection(clean)),
              error: () => undefined,
            });
          } else {
            this.selection.set(fromAccount);
          }
          this.loaded.set(true);
        },
        error: () => {
          this.selection.set(local);
          this.loaded.set(true);
        },
      });
    });
  }

  /** Saves to the account (signed in) or this device. Errors carry a message fit to show. */
  save(selection: MyServicesSelection): Observable<MyServicesSelection> {
    const problem = validateSelection(selection);
    if (problem) return throwError(() => new Error(problem));
    const clean = normalizeSelection(selection);
    if (!this.auth.isAuthenticated()) {
      writeStorage(SELECTION_KEY, JSON.stringify(clean));
      this.selection.set(clean);
      return of(clean);
    }
    return this.http.put<MyServicesSelection>(`${environment.apiUrl}auth/my-services`, clean).pipe(
      map(normalizeSelection),
      tap((saved) => this.selection.set(saved)),
      catchError((err: HttpErrorResponse) =>
        throwError(() => new Error(err.error?.error ?? "Couldn't save your services. Try again."))
      )
    );
  }

  setOnlyMine(on: boolean): void {
    this.onlyMine.set(on);
    writeStorage(ONLY_MINE_KEY, on ? '1' : '0');
  }

  /** The catalog key for an AniList site name or AnimeSchedule platform key, if it's a known service. */
  keyFor(siteOrPlatform: string | null | undefined): string | undefined {
    return siteOrPlatform ? this.keyByAlias().get(simplify(siteOrPlatform)) : undefined;
  }

  service(key: string): StreamingService | undefined {
    return this.byKey().get(key);
  }

  /** Is this AniList site / AnimeSchedule platform one of the viewer's services? */
  isMine(siteOrPlatform: string | null | undefined): boolean {
    const key = this.keyFor(siteOrPlatform);
    return key !== undefined && this.selected().has(key);
  }

  /** A custom platform's link for a show: {title} becomes the URL-encoded title. */
  customUrl(platform: CustomPlatform, title: string | null | undefined): string {
    return platform.url.split(TITLE_PLACEHOLDER).join(encodeURIComponent(title ?? ''));
  }

  /** Fetches (once) which services carry these shows. Safe to call on every change. */
  ensureAvailability(mediaIds: readonly number[]): void {
    if (!this.isBrowser) return;
    const missing = [...new Set(mediaIds)].filter((id) => id != null && !this.requested.has(id));
    for (let i = 0; i < missing.length; i += AVAILABILITY_BATCH) {
      const batch = missing.slice(i, i + AVAILABILITY_BATCH);
      batch.forEach((id) => this.requested.add(id));
      this.http
        .get<Record<string, ServiceLink[]>>(`${environment.apiUrl}streaming-services/availability`, {
          params: { ids: batch.join(',') },
        })
        .subscribe({
          next: (result) => {
            const next = new Map(this.availability());
            for (const id of batch) next.set(id, result[id] ?? null);
            this.availability.set(next);
          },
          // Let a later call retry
          error: () => batch.forEach((id) => this.requested.delete(id)),
        });
    }
  }

  /** Built-in links for a show (catalog order), or null when it has no streaming data / isn't loaded. */
  links(mediaId: number | undefined): ServiceLink[] | null {
    if (mediaId == null) return null;
    return this.availability().get(mediaId) ?? null;
  }

  status(mediaId: number | undefined): ServiceStatus {
    if (mediaId == null) return 'unknown';
    const map = this.availability();
    if (!map.has(mediaId)) return 'loading';
    const links = map.get(mediaId);
    if (!links) return 'unknown';
    return links.some((l) => this.selected().has(l.key)) ? 'mine' : 'other';
  }

  /** False only when the filter is on and the show is known to stream elsewhere only. */
  passesFilter(mediaId: number | undefined): boolean {
    return !this.filterActive() || this.status(mediaId) !== 'other';
  }
}

function normalizeSelection(value: Partial<MyServicesSelection> | null | undefined): MyServicesSelection {
  const services = Array.isArray(value?.services) ? value!.services.filter((s) => typeof s === 'string') : [];
  const customPlatforms = Array.isArray(value?.customPlatforms)
    ? value!.customPlatforms
        .filter((c) => c && typeof c.name === 'string' && typeof c.url === 'string')
        .map((c) => ({ name: c.name.trim().replace(/\s+/g, ' '), url: c.url.trim() }))
    : [];
  return { services: [...new Set(services)], customPlatforms };
}

function isEmpty(selection: MyServicesSelection): boolean {
  return selection.services.length === 0 && selection.customPlatforms.length === 0;
}

function readLocalSelection(): MyServicesSelection {
  try {
    const raw = readStorage(SELECTION_KEY);
    if (!raw) return EMPTY;
    const parsed = normalizeSelection(JSON.parse(raw));
    // Drop anything that wouldn't pass (e.g. hand-edited storage)
    return {
      services: parsed.services,
      customPlatforms: parsed.customPlatforms.filter((c) => c.name && isValidPlatformUrl(c.url)).slice(0, MAX_CUSTOM_PLATFORMS),
    };
  } catch {
    return EMPTY;
  }
}

function readStorage(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeStorage(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    // Storage unavailable (private mode, blocked site data); the choice lasts for this visit
  }
}
