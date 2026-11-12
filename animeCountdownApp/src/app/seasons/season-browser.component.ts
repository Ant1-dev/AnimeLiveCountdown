import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, combineLatest, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';
import { MatIcon } from '@angular/material/icon';
import { SeasonCardComponent } from './season-card.component';
import {
  SEASON_NAMES, SeasonKey, SeasonName, SeasonOverview, SeasonResponse, SeasonSort,
} from '../models/season.model';
import {
  SeasonService, sameSeason, seasonLabel, seasonOf, shiftSeason, timeUntil,
} from '../services/season.service';
import { injectIsBrowser, tickEvery } from '../shared/platform';
import { SeoService, breadcrumbs } from '../seo/seo.service';
import { injectResponseStatus } from '../seo/http-status';
import { SITE_URL } from '../seo/site';
import { animePath, preferredTitle } from '../seo/slug';

interface Query {
  key: SeasonKey;
  genre: string | null;
  sort: SeasonSort;
}

const SORTS: { value: SeasonSort; label: string }[] = [
  { value: 'popularity', label: 'Popularity' },
  { value: 'score', label: 'Score' },
  { value: 'next_airing', label: 'Next episode' },
];

@Component({
  selector: 'app-season-browser',
  imports: [SeasonCardComponent, MatIcon],
  templateUrl: './season-browser.component.html',
  styleUrl: './season-browser.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeasonBrowserComponent {
  private seasonService = inject(SeasonService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private seo = inject(SeoService);
  private setStatus = injectResponseStatus();
  private destroyRef = inject(DestroyRef);
  private isBrowser = injectIsBrowser();

  protected readonly seasons = SEASON_NAMES;
  protected readonly sorts = SORTS;
  protected readonly seasonLabel = seasonLabel;

  protected overview = signal<SeasonOverview | null>(null);
  protected query = signal<Query | null>(null);
  protected data = signal<SeasonResponse | null>(null);
  protected loading = signal(true);
  protected error = signal<string | null>(null);
  protected now = signal(Date.now());

  /** Current season by the browser clock, until the overview from the server arrives. */
  protected current = computed(() => this.overview()?.current ?? seasonOf(new Date(this.now())));
  protected next = computed(() => this.overview()?.next ?? shiftSeason(this.current(), 1));

  protected years = computed(() => {
    const max = this.next().year;
    const min = this.overview()?.minYear ?? 1970;
    return Array.from({ length: max - min + 1 }, (_, i) => max - i);
  });

  protected isCurrent = computed(() => sameSeason(this.query()?.key, this.current()));
  protected isNext = computed(() => sameSeason(this.query()?.key, this.next()));
  protected canGoNext = computed(() => {
    const key = this.query()?.key;
    return !!key && !sameSeason(key, this.next());
  });

  protected heading = computed(() => {
    const key = this.query()?.key;
    return key ? `${seasonLabel(key.season)} ${key.year}` : 'Seasons';
  });

  protected subheading = computed(() => {
    const d = this.data();
    const key = this.query()?.key;
    if (!key) return '';
    const count = d ? `${d.media.length} show${d.media.length === 1 ? '' : 's'}` : '';
    if (this.isCurrent()) return ['Airing this season', count].filter(Boolean).join(' · ');
    if (d && new Date(d.startsAt).getTime() > this.now()) {
      return [`Premieres ${timeUntil(new Date(d.startsAt).getTime(), this.now())}`, count].filter(Boolean).join(' · ');
    }
    return ['Past season', count].filter(Boolean).join(' · ');
  });

  constructor() {
    // Renders on the server too, so crawlers get the season's shows and per-season meta tags
    tickEvery(30_000, this.isBrowser).pipe(takeUntilDestroyed()).subscribe(() => this.now.set(Date.now()));

    this.seasonService.overview().pipe(
      catchError(() => of(null)),
      takeUntilDestroyed(),
    ).subscribe((o) => this.overview.set(o));

    combineLatest([this.route.paramMap, this.route.queryParamMap]).pipe(
      tap(([path, query]) => this.redirectLegacyUrl(path.has('year'), query.get('season'), query.get('year'))),
      map(([path, query]) => this.parseQuery(
        path.get('season') ?? query.get('season'),
        path.get('year') ?? query.get('year'),
        query.get('genre'),
        query.get('sort'),
      )),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      tap((q) => {
        this.query.set(q);
        this.loading.set(true);
        this.error.set(null);
        this.applySeo(q, null);
      }),
      switchMap((q) => this.seasonService.season(q.key, q.genre, q.sort).pipe(
        catchError((err: HttpErrorResponse) => {
          this.error.set(err.status === 503
            ? 'AniList is busy right now, so this season couldn’t be loaded. Try again in a couple of minutes.'
            : 'This season couldn’t be loaded. Check your connection and try again.');
          return of(null);
        }),
      )),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((response) => {
      this.data.set(response);
      this.loading.set(false);
      const q = this.query();
      if (q) this.applySeo(q, response);
      if (!response && this.error()) this.setStatus(503, { 'Retry-After': '300' });
    });
  }

  /** Canonical season URL: /seasons/2026/fall. Genre and sort are filters, not separate pages. */
  private seasonPath(key: SeasonKey): string {
    return `/seasons/${key.year}/${key.season.toLowerCase()}`;
  }

  /** Old links used ?season=FALL&year=2026; move them to the path form (301 on the server). */
  private redirectLegacyUrl(hasPath: boolean, season: string | null, year: string | null): void {
    if (hasPath || !season || !year) return;
    const q = this.parseQuery(season, year, null, null);
    const target = this.seasonPath(q.key);
    if (this.isBrowser) {
      this.router.navigateByUrl(target, { replaceUrl: true });
    } else {
      this.setStatus(301, { Location: target });
    }
  }

  private applySeo(q: Query, response: SeasonResponse | null): void {
    const name = `${seasonLabel(q.key.season)} ${q.key.year}`;
    const path = this.seasonPath(q.key);
    const count = response?.media.length;
    const top = (response?.media ?? []).slice(0, 4)
      .map((m) => preferredTitle(m.engtitle, m.romtitle)).filter(Boolean);
    const intro = this.isCurrent() ? `Every anime airing this season (${name})`
      : this.isNext() ? `Every anime premiering in ${name}` : `Every anime from ${name}`;
    const description = `${intro}${count ? `: ${count} shows` : ''}${top.length ? ` including ${top.join(', ')}` : ''}. Air dates, live episode countdowns and where to watch.`;
    this.seo.setPage({
      title: `${name} Anime: Schedule & Release Dates`,
      description,
      path,
      jsonLd: [
        {
          '@context': 'https://schema.org',
          '@type': 'CollectionPage',
          name: `${name} anime`,
          url: SITE_URL + path,
          mainEntity: {
            '@type': 'ItemList',
            numberOfItems: count ?? 0,
            itemListElement: (response?.media ?? []).slice(0, 50).map((m, i) => ({
              '@type': 'ListItem',
              position: i + 1,
              url: SITE_URL + animePath(m.id, preferredTitle(m.engtitle, m.romtitle)),
              name: preferredTitle(m.engtitle, m.romtitle) || undefined,
            })),
          },
        },
        breadcrumbs([['Home', '/'], ['Seasons', '/seasons'], [name, path]]),
      ],
    });
  }

  private parseQuery(season: string | null, year: string | null, genre: string | null, sort: string | null): Query {
    const fallback = this.current();
    const s = (season ?? '').toUpperCase() as SeasonName;
    const y = Number(year);
    return {
      key: {
        season: SEASON_NAMES.includes(s) ? s : fallback.season,
        year: Number.isInteger(y) && y >= 1970 && y <= fallback.year + 1 ? y : fallback.year,
      },
      genre: genre || null,
      sort: SORTS.some((o) => o.value === sort) ? (sort as SeasonSort) : 'popularity',
    };
  }

  protected navigate(changes: Partial<{ season: SeasonName; year: number; genre: string | null; sort: SeasonSort }>): void {
    const q = this.query();
    if (!q) return;
    const merged = {
      season: changes.season ?? q.key.season,
      year: changes.year ?? q.key.year,
      genre: changes.genre !== undefined ? changes.genre : q.genre,
      sort: changes.sort ?? q.sort,
    };
    this.router.navigate([this.seasonPath({ season: merged.season, year: merged.year })], {
      queryParams: {
        genre: merged.genre || null,
        sort: merged.sort === 'popularity' ? null : merged.sort,
      },
    });
  }

  protected go(key: SeasonKey): void {
    this.navigate({ season: key.season, year: key.year });
  }

  protected step(by: number): void {
    const key = this.query()?.key;
    if (key) this.go(shiftSeason(key, by));
  }

  protected goCurrent(): void { this.go(this.current()); }
  protected goNext(): void { this.go(this.next()); }

  protected onSeason(value: string): void { this.navigate({ season: value as SeasonName }); }
  protected onYear(value: string): void { this.navigate({ year: Number(value) }); }
  protected onGenre(value: string): void { this.navigate({ genre: value || null }); }
  protected onSort(value: string): void { this.navigate({ sort: value as SeasonSort }); }

  protected retry(): void {
    const q = this.query();
    if (!q) return;
    this.loading.set(true);
    this.error.set(null);
    this.seasonService.season(q.key, q.genre, q.sort).pipe(
      catchError(() => {
        this.error.set('Still couldn’t load this season. Try again in a couple of minutes.');
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((response) => {
      this.data.set(response);
      this.loading.set(false);
    });
  }
}
