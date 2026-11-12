// media-info.component.ts
import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { MediaInfoService } from '../services/media.info.service';
import { MediaTimeService } from '../services/media-time.service';
import { MediaInfo } from '../models/media.info.model';
import { TimeRemaining } from '../models/time-remaining.model';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { filter, switchMap } from 'rxjs';
import { CommonModule, Location, NgOptimizedImage } from '@angular/common';
import { Skeleton } from 'primeng/skeleton';
import { FollowControlsComponent } from './follow-controls/follow-controls.component';
import { WhereToWatchComponent } from './where-to-watch/where-to-watch.component';
import { ScheduleNote } from '../models/schedule-note.model';
import { ScheduleNoteService } from '../services/schedule-note.service';
import { ScheduleNoteBadgeComponent } from '../shared-components/schedule-note-badge/schedule-note-badge.component';
import { ShareButtonComponent } from '../shared-components/share-button/share-button.component';
import { EpisodeReactionsComponent } from './episode-reactions/episode-reactions.component';
import { SeoService, breadcrumbs, plainText, truncate } from '../seo/seo.service';
import { animePath, preferredTitle } from '../seo/slug';
import { SITE_URL } from '../seo/site';
import { injectResponseStatus } from '../seo/http-status';
import { injectIsBrowser } from '../shared/platform';
import { seasonLabel } from '../services/season.service';
import { SeasonName } from '../models/season.model';
import { OptimizedImagePipe } from '../shared/optimized-image.pipe';

/** What the page shows when the show can't be loaded. */
type LoadState = 'loading' | 'ready' | 'not-found' | 'unavailable';

@Component({
  selector: 'app-media-info',
  imports: [OptimizedImagePipe, CommonModule, Skeleton, NgOptimizedImage, RouterLink, FollowControlsComponent, WhereToWatchComponent, ScheduleNoteBadgeComponent, ShareButtonComponent, EpisodeReactionsComponent],
  templateUrl: './media-info.component.html',
  styleUrl: './media-info.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MediaInfoComponent implements OnInit {
  private mediaInfoService = inject(MediaInfoService);
  private mediaTimeService = inject(MediaTimeService);
  private scheduleNoteService = inject(ScheduleNoteService);
  private seo = inject(SeoService);
  private router = inject(Router);
  private location = inject(Location);
  private setStatus = injectResponseStatus();
  private isBrowser = injectIsBrowser();
  protected scheduleNote = signal<ScheduleNote | null>(null);
  private destroyRef = inject(DestroyRef);
  public mediaDetails = signal<MediaInfo>({} as MediaInfo);
  private route = inject(ActivatedRoute);
  protected state = signal<LoadState>('loading');
  protected isLoading = computed(() => this.state() === 'loading');

  /** English title if there is one, otherwise romaji. */
  protected displayTitle = computed(() => preferredTitle(this.mediaDetails().engtitle, this.mediaDetails().romtitle));
  /** Second line: the romaji title, only when it differs from the heading. */
  protected altTitle = computed(() => {
    const romaji = this.mediaDetails().romtitle;
    return romaji && romaji !== this.displayTitle() ? romaji : '';
  });
  protected seasonLink = computed(() => {
    const m = this.mediaDetails();
    return m.season && m.seasonYear
      ? { path: `/seasons/${m.seasonYear}/${m.season.toLowerCase()}`, label: `${seasonLabel(m.season as SeasonName)} ${m.seasonYear} anime` }
      : null;
  });

  timeRemaining = computed<TimeRemaining | null>(() => {
    const media = this.mediaDetails();
    if (!media || !media.id) return null;
    return this.mediaTimeService.getTimeRemaining(media.id);
  });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        filter((params: ParamMap) => params.has('id')),
        switchMap((params: ParamMap) => {
          const id = Number(params.get('id'));
          this.scheduleNote.set(null);
          this.scheduleNoteService
            .getNote(id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((note) => this.scheduleNote.set(note));
          this.mediaInfoService.getMediaId(id);
          this.state.set('loading');
          return this.mediaInfoService.getMedia();
        })
      )
      .subscribe({
        next: (data) => {
          this.mediaDetails.set(data);
          this.state.set('ready');
          this.mediaTimeService.initializeTimerFromMediaInfo(data);
          this.applySeo(data);
        },
        error: (err: HttpErrorResponse) => this.handleError(err),
      });
  }

  private applySeo(media: MediaInfo): void {
    const title = preferredTitle(media.engtitle, media.romtitle) || `Anime #${media.id}`;
    const path = animePath(media.id, title);
    this.redirectToCanonical(path);

    const airing = media.airingat && media.nextepisode && new Date(media.airingat).getTime() > Date.now()
      ? { episode: media.nextepisode, at: new Date(media.airingat) }
      : null;
    const sites = (media.streamingLinks ?? []).map((l) => l.site).filter((s, i, all) => all.indexOf(s) === i).slice(0, 3);
    const watchOn = sites.length ? ` Watch on ${sites.join(', ')}.` : '';

    const pageTitle = airing
      ? `${title} Episode ${airing.episode} Release Date & Countdown`
      : `${title}: Episodes, Air Dates & Where to Watch`;
    const description = airing
      ? `${title} episode ${airing.episode} airs ${formatUtc(airing.at)}. Live countdown in your time zone and the full airing schedule.${watchOn}`
      : `${truncate(plainText(media.description), 110) || `Episodes and air dates for ${title}.`}${watchOn}`;

    const season = this.seasonLink();
    const crumbs: [string, string][] = [['Home', '/']];
    if (season) crumbs.push([capitalize(season.label), season.path]);
    crumbs.push([title, path]);

    this.seo.setPage({
      title: pageTitle,
      description,
      path,
      image: media.banner || media.coverimage,
      imageAlt: `${title} key art`,
      type: 'video.tv_show',
      jsonLd: [tvSeriesSchema(media, title, path, airing), breadcrumbs(crumbs)],
    });
  }

  /** Old /media/:id links and wrong slugs move to /anime/:id/:slug (301 on the server). */
  private redirectToCanonical(path: string): void {
    const current = this.router.url.split(/[?#]/)[0];
    if (current === path) return;
    if (this.isBrowser) {
      this.location.replaceState(path);
    } else {
      this.setStatus(301, { Location: path });
    }
  }

  private handleError(err: HttpErrorResponse): void {
    // Only our API's explicit "not_found" means the show doesn't exist. Any other 404 (e.g. the API host
    // answering for a deployment that isn't there) means the API is unavailable: answer 503, not 404,
    // so search engines retry instead of dropping the page.
    if (err?.status === 404 && (err.error as { error?: string } | null)?.error === 'not_found') {
      this.state.set('not-found');
      this.setStatus(404);
      this.seo.setPage({
        title: 'Anime not found',
        description: 'This show isn’t in our schedule. Browse this season’s anime or search for another title.',
        path: this.router.url.split(/[?#]/)[0],
        noindex: true,
      });
      return;
    }
    // API down or slow: ask crawlers to come back later instead of indexing an empty page
    this.state.set('unavailable');
    this.setStatus(503, { 'Retry-After': '300' });
    this.seo.setPage({
      title: 'Temporarily unavailable',
      description: 'Show details couldn’t be loaded right now. Please try again in a few minutes.',
      path: this.router.url.split(/[?#]/)[0],
      noindex: true,
    });
  }
}

function formatUtc(date: Date): string {
  const day = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' });
  const time = date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', timeZone: 'UTC' });
  return `${day} at ${time} UTC`;
}

function capitalize(s: string): string {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

function tvSeriesSchema(media: MediaInfo, title: string, path: string, airing: { episode: number; at: Date } | null): object {
  const schema: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'TVSeries',
    name: title,
    url: SITE_URL + path,
    image: media.coverimage || undefined,
    description: truncate(plainText(media.description), 500) || undefined,
    genre: media.genres?.length ? media.genres : undefined,
    numberOfEpisodes: media.totalepisodes || undefined,
    inLanguage: 'ja',
  };
  if (media.romtitle && media.romtitle !== title) schema['alternateName'] = media.romtitle;
  if (media.streamingLinks?.length) {
    schema['potentialAction'] = media.streamingLinks.slice(0, 5).map((l) => ({
      '@type': 'WatchAction',
      target: l.url,
      name: `Watch on ${l.site}`,
    }));
  }
  if (airing) {
    schema['episode'] = {
      '@type': 'TVEpisode',
      episodeNumber: airing.episode,
      name: `${title} Episode ${airing.episode}`,
      publication: { '@type': 'BroadcastEvent', startDate: airing.at.toISOString(), isLiveBroadcast: false },
    };
  }
  return JSON.parse(JSON.stringify(schema)); // drop undefined fields
}
