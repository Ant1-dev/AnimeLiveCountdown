import { ChangeDetectionStrategy, Component, DestroyRef, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { Title } from '@angular/platform-browser';
import { catchError, distinctUntilChanged, interval, map, of, switchMap, tap } from 'rxjs';
import { MatIcon } from '@angular/material/icon';
import { SeasonCardComponent } from './season-card.component';
import {
  SEASON_NAMES, SeasonKey, SeasonName, SeasonOverview, SeasonResponse, SeasonSort,
} from '../models/season.model';
import {
  SeasonService, sameSeason, seasonLabel, seasonOf, shiftSeason, timeUntil,
} from '../services/season.service';

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
  private title = inject(Title);
  private destroyRef = inject(DestroyRef);
  private isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

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
    if (!this.isBrowser) return; // prerender: render the shell only

    interval(30_000).pipe(takeUntilDestroyed()).subscribe(() => this.now.set(Date.now()));

    this.seasonService.overview().pipe(
      catchError(() => of(null)),
      takeUntilDestroyed(),
    ).subscribe((o) => this.overview.set(o));

    this.route.queryParamMap.pipe(
      map((params) => this.parseQuery(params.get('season'), params.get('year'), params.get('genre'), params.get('sort'))),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      tap((q) => {
        this.query.set(q);
        this.loading.set(true);
        this.error.set(null);
        this.title.setTitle(`${seasonLabel(q.key.season)} ${q.key.year} Anime | Anime Live Countdown`);
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
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        season: merged.season,
        year: merged.year,
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
