import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { Follow } from '../models/follow.model';
import { Media } from '../models/schedule.model';

/**
 * The signed-in user's followed shows and episode progress, shared by every card on the page.
 * Loads when the user signs in and clears on sign-out; mutations update the store in place.
 */
@Injectable({ providedIn: 'root' })
export class FollowingService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);
  private readonly url = environment.apiUrl + 'auth/following';

  private byId = signal<ReadonlyMap<number, Follow>>(new Map());
  readonly loaded = signal(false);
  readonly loadError = signal(false);

  readonly follows = computed(() => [...this.byId().values()]);

  /** Followed shows with aired episodes left to watch: next to air first, then the rest. */
  readonly continueWatching = computed(() =>
    this.follows()
      .filter((f) => (f.behind ?? 0) > 0)
      .sort((a, b) => airingTime(a) - airingTime(b))
  );

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) {
        this.load().subscribe({ error: () => this.loadError.set(true) });
      } else {
        this.byId.set(new Map());
        this.loaded.set(false);
      }
    });
  }

  /** The user's follow for a show, or undefined. Reactive when read inside computed()/templates. */
  get(mediaId: number | undefined): Follow | undefined {
    return mediaId == null ? undefined : this.byId().get(mediaId);
  }

  isFollowing(mediaId: number | undefined): boolean {
    return this.get(mediaId) !== undefined;
  }

  load(): Observable<Follow[]> {
    return this.http.get<Follow[]>(this.url).pipe(
      tap((follows) => {
        this.byId.set(new Map(follows.map((f) => [f.media.id, f])));
        this.loaded.set(true);
        this.loadError.set(false);
      })
    );
  }

  follow(mediaId: number): Observable<Follow> {
    return this.http.post<Follow>(`${this.url}/${mediaId}`, {}).pipe(tap((f) => this.put(f)));
  }

  unfollow(mediaId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${mediaId}`).pipe(
      tap(() => {
        const next = new Map(this.byId());
        next.delete(mediaId);
        this.byId.set(next);
      })
    );
  }

  setProgress(mediaId: number, watchedEpisode: number): Observable<Follow> {
    return this.http
      .put<Follow>(`${this.url}/${mediaId}/progress`, { watchedEpisode })
      .pipe(tap((f) => this.put(f)));
  }

  /** Marks the next unwatched episode as watched. */
  markNextWatched(mediaId: number): Observable<Follow> {
    const current = this.get(mediaId)?.watchedEpisode ?? 0;
    return this.setProgress(mediaId, current + 1);
  }

  /** My Schedule: followed shows airing on a weekday in the viewer's timezone. */
  airingOn(weekDay: string): Observable<Media[]> {
    const tz = viewerTimeZone();
    const params = tz ? new HttpParams().set('tz', tz) : undefined;
    return this.http
      .get<Follow[]>(`${this.url}/airing/${weekDay.toUpperCase()}`, { params })
      .pipe(map((follows) => follows.map((f) => f.media)));
  }

  private put(follow: Follow): void {
    const next = new Map(this.byId());
    next.set(follow.media.id, follow);
    this.byId.set(next);
  }
}

function airingTime(f: Follow): number {
  return f.media.next_Airing_At ? new Date(f.media.next_Airing_At).getTime() : Number.MAX_SAFE_INTEGER;
}

function viewerTimeZone(): string | undefined {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || undefined;
  } catch {
    return undefined;
  }
}
