import { ChangeDetectionStrategy, Component, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser, NgOptimizedImage } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { LiveService } from '../../services/live.service';
import { LiveEpisode } from '../../models/live.model';
import { MediaTimeService } from '../../services/media-time.service';
import { timeUntil } from '../../services/season.service';

interface NowAiring {
  episode: LiveEpisode;
  minutesIn: number;
  progress: number;
}

const HOUR = 3600_000;

/**
 * "3 episodes airing in the next hour" banner plus an "Airing now" strip on the home page.
 * The server sends raw airing times; we bucket them against the browser clock every 15 s.
 */
@Component({
  selector: 'app-live-banner',
  imports: [RouterLink, NgOptimizedImage],
  templateUrl: './live-banner.component.html',
  styleUrl: './live-banner.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LiveBannerComponent {
  private live = inject(LiveService);
  private mediaTime = inject(MediaTimeService);
  private episodes = signal<LiveEpisode[]>([]);
  protected now = signal(Date.now());

  protected nextHour = computed(() => {
    const now = this.now();
    return this.episodes().filter((e) => {
      const at = Date.parse(e.airingAt);
      return at > now && at <= now + HOUR;
    });
  });

  protected airingNow = computed<NowAiring[]>(() => {
    const now = this.now();
    return this.episodes()
      .map((episode) => {
        const at = Date.parse(episode.airingAt);
        const duration = episode.durationMinutes * 60_000;
        return { episode, at, duration };
      })
      .filter(({ at, duration }) => at <= now && now < at + duration)
      .map(({ episode, at, duration }) => ({
        episode,
        minutesIn: Math.max(0, Math.floor((now - at) / 60_000)),
        progress: Math.min(100, Math.round(((now - at) / duration) * 100)),
      }));
  });

  protected bannerText = computed(() => {
    const count = this.nextHour().length;
    return `${count} episode${count === 1 ? '' : 's'} airing in the next hour`;
  });

  protected firstIn = computed(() => {
    const first = this.nextHour()[0];
    return first ? timeUntil(Date.parse(first.airingAt), this.now()) : '';
  });

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    interval(15_000).pipe(takeUntilDestroyed()).subscribe(() => this.now.set(Date.now()));
    this.live.poll().pipe(takeUntilDestroyed()).subscribe((response) => {
      if (!response) return;
      this.episodes.set(response.episodes);
      this.now.set(Date.now());
      // Let the celebration fire for episodes in the next hour even if no card shows them.
      for (const e of response.episodes) {
        if (Date.parse(e.airingAt) > Date.now()) {
          this.mediaTime.watchAiring(e.mediaId, e.airingAt, this.title(e), e.episode);
        }
      }
    });
  }

  protected title(e: LiveEpisode): string {
    return e.engtitle || e.romtitle || 'Untitled';
  }
}
