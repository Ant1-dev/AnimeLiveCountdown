import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { WatchOrderEntry } from '../../models/media.info.model';
import { AnimePathPipe } from '../../seo/anime-path.pipe';
import { injectIsBrowser } from '../../shared/platform';

const FORMAT_LABELS: Record<string, string> = {
  TV: 'TV', TV_SHORT: 'TV short', MOVIE: 'Movie', SPECIAL: 'Special', OVA: 'OVA', ONA: 'ONA',
};

/**
 * The franchise in release order. Loads in the browser only: the first lookup for a franchise can
 * take a few seconds (AniList, rate limited), which must not hold up the server-rendered page.
 * Hidden when there's nothing to order (a standalone show) or the lookup fails.
 */
@Component({
  selector: 'app-watch-order',
  imports: [RouterLink, AnimePathPipe],
  template: `
    @if (entries().length > 1) {
      <section class="watch-order" aria-labelledby="watch-order-heading">
        <h2 id="watch-order-heading" class="info-heading">Watch order</h2>
        <ol>
          @for (e of entries(); track e.id) {
            <li [class.current]="e.current" [class.side]="e.kind === 'SIDE'">
              @if (e.current) {
                <span class="name" aria-current="page">{{ e.title }}</span>
              } @else if (e.onSite) {
                <a class="name" [routerLink]="e.id | animePath: e.title">{{ e.title }}</a>
              } @else {
                <a class="name" [href]="'https://anilist.co/anime/' + e.id" target="_blank" rel="noopener">{{ e.title }}</a>
              }
              <span class="meta">{{ details(e) }}</span>
            </li>
          }
        </ol>
        <p class="note">Release order, with movies, OVAs and specials marked as side stories.</p>
      </section>
    }
  `,
  styles: `
    .watch-order { margin: 1.5rem 0; }
    ol { margin: 0; padding-left: 1.6rem; display: flex; flex-direction: column; gap: 6px; }
    li { color: var(--text-primary); }
    li.side { color: var(--text-secondary); }
    li.current .name { font-weight: 700; color: var(--accent); }
    .name { color: inherit; text-decoration: none; }
    a.name:hover { text-decoration: underline; color: var(--accent); }
    .meta { margin-left: 8px; font-size: 0.85rem; color: var(--text-muted); }
    .note { margin: 8px 0 0; font-size: 0.8rem; color: var(--text-muted); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WatchOrderComponent {
  private http = inject(HttpClient);
  private isBrowser = injectIsBrowser();

  mediaId = input.required<number>();
  protected entries = signal<WatchOrderEntry[]>([]);

  constructor() {
    effect((onCleanup) => {
      const id = this.mediaId();
      if (!this.isBrowser || !id) return;
      const sub = this.http
        .get<WatchOrderEntry[]>(`${environment.apiUrl}info/${id}/watch-order`)
        .pipe(catchError(() => of([] as WatchOrderEntry[])))
        .subscribe((entries) => this.entries.set(entries));
      onCleanup(() => sub.unsubscribe());
    });
  }

  protected details(e: WatchOrderEntry): string {
    const parts = [
      e.kind === 'SIDE' ? `Side story · ${FORMAT_LABELS[e.format ?? ''] ?? e.format ?? ''}` : FORMAT_LABELS[e.format ?? ''] ?? e.format,
      e.year ? String(e.year) : e.status === 'NOT_YET_RELEASED' ? 'Upcoming' : '',
      e.episodes && e.format !== 'MOVIE' ? `${e.episodes} ep` : '',
    ];
    return parts.filter(Boolean).join(' · ');
  }
}
