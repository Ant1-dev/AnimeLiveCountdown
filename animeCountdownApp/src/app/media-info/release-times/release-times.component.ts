import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Release, ReleasesView } from '../../models/release.model';
import { timeUntil } from '../../services/season.service';
import { injectIsBrowser, tickEvery } from '../../shared/platform';

interface Row {
  label: string;
  hint: string;
  episode: number;
  at: Date;
  release?: Release;
}

/**
 * "When to watch": the Japanese broadcast plus when each episode is out subbed and dubbed, with links
 * to the streaming platforms. Server-rendered (it's what people search for); hidden until the
 * release-times feature is configured or when there's no sub/dub data for the show.
 */
@Component({
  selector: 'app-release-times',
  imports: [DatePipe],
  template: `
    @if (rows().length > 1) {
      <section class="release-times" aria-labelledby="release-times-heading">
        <h2 id="release-times-heading" class="info-heading">When to watch</h2>
        <ul>
          @for (row of rows(); track row.label) {
            <li>
              <div class="what">
                <span class="label">{{ row.label }}</span>
                <span class="hint">{{ row.hint }}</span>
              </div>
              <div class="when">
                <span class="episode">Episode {{ row.episode }}</span>
                <time [attr.datetime]="row.at.toISOString()">{{ row.at | date: 'EEE, MMM d, h:mm a' }}</time>
                <span class="relative" [class.out]="isOut(row)">{{ relative(row) }}</span>
                @if (row.release?.delayNote) {
                  <span class="delay" role="note">{{ row.release!.delayNote }}</span>
                }
              </div>
              @if (row.release?.streams?.length) {
                <div class="where">
                  @for (s of row.release!.streams; track s.platform) {
                    <a [href]="s.url" target="_blank" rel="noopener">{{ s.label }}</a>
                  }
                </div>
              }
            </li>
          }
        </ul>
        <p class="source">Sub and dub times from <a href="https://animeschedule.net" target="_blank" rel="noopener">AnimeSchedule</a>; shown in your time zone.</p>
      </section>
    }
  `,
  styles: `
    .release-times { margin: 1.5rem 0; }
    ul { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
    li { display: grid; grid-template-columns: minmax(140px, 1fr) 2fr; gap: 6px 16px; padding: 10px 12px; border: 1px solid var(--border-subtle); border-radius: 10px; background: var(--bg-lighter); }
    .what { display: flex; flex-direction: column; }
    .label { font-weight: 700; color: var(--text-strong); }
    .hint { font-size: 0.8rem; color: var(--text-muted); }
    .when { display: flex; flex-wrap: wrap; gap: 4px 10px; align-items: baseline; color: var(--text-primary); }
    .episode { color: var(--text-secondary); }
    .relative { color: var(--accent); font-weight: 600; }
    .relative.out { color: var(--text-secondary); font-weight: 400; }
    .delay { flex-basis: 100%; font-size: 0.85rem; color: var(--danger); }
    .where { grid-column: 1 / -1; display: flex; flex-wrap: wrap; gap: 6px; }
    .where a { padding: 3px 10px; border-radius: 999px; border: 1px solid var(--border-subtle); color: var(--text-primary); text-decoration: none; font-size: 0.85rem; }
    .where a:hover { border-color: var(--accent); color: var(--accent); }
    .source { margin: 8px 0 0; font-size: 0.8rem; color: var(--text-muted); }
    .source a { color: inherit; }
    @media (max-width: 600px) { li { grid-template-columns: 1fr; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReleaseTimesComponent {
  private http = inject(HttpClient);

  mediaId = input.required<number>();
  /** The Japanese broadcast (from AniList), shown as the first row. */
  broadcastAt = input<Date | string | undefined>();
  broadcastEpisode = input<number | undefined>();

  private view = signal<ReleasesView | null>(null);
  private now = signal(Date.now());

  protected rows = computed<Row[]>(() => {
    const view = this.view();
    if (!view?.enabled) return [];
    const rows: Row[] = [];
    const at = this.broadcastAt();
    const episode = this.broadcastEpisode();
    if (at && episode) {
      rows.push({ label: 'Japan broadcast', hint: 'Original TV airing', episode, at: new Date(at) });
    }
    const sub = view.releases.SUB;
    if (sub) rows.push({ label: 'Subtitled', hint: 'Streaming with English subtitles', episode: sub.episode, at: new Date(sub.releaseAt), release: sub });
    const dub = view.releases.DUB;
    if (dub) rows.push({ label: 'English dub', hint: 'Dubbed release', episode: dub.episode, at: new Date(dub.releaseAt), release: dub });
    return rows;
  });

  constructor() {
    tickEvery(30_000, injectIsBrowser()).pipe(takeUntilDestroyed()).subscribe(() => this.now.set(Date.now()));
    effect((onCleanup) => {
      const id = this.mediaId();
      if (!id) return;
      const sub = this.http
        .get<ReleasesView>(`${environment.apiUrl}releases/${id}`)
        .pipe(catchError(() => of(null)))
        .subscribe((view) => this.view.set(view));
      onCleanup(() => sub.unsubscribe());
    });
  }

  protected isOut(row: Row): boolean {
    return row.at.getTime() <= this.now();
  }

  protected relative(row: Row): string {
    const ms = row.at.getTime() - this.now();
    if (ms > 0) return timeUntil(row.at.getTime(), this.now());
    const hours = Math.floor(-ms / 3_600_000);
    if (hours < 1) return 'out now';
    if (hours < 48) return `out ${hours}h ago`;
    return `out ${Math.floor(hours / 24)} days ago`;
  }
}
