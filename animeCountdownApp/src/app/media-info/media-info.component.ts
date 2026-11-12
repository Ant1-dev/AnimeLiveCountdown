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
import { ActivatedRoute, ParamMap } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { Skeleton } from 'primeng/skeleton';
import { WhereToWatchComponent } from './where-to-watch/where-to-watch.component';
import { ScheduleNote } from '../models/schedule-note.model';
import { ScheduleNoteService } from '../services/schedule-note.service';
import { ScheduleNoteBadgeComponent } from '../shared-components/schedule-note-badge/schedule-note-badge.component';
import { ShareButtonComponent } from '../shared-components/share-button/share-button.component';
import { EpisodeReactionsComponent } from './episode-reactions/episode-reactions.component';

@Component({
  selector: 'app-media-info',
  imports: [CommonModule, Skeleton, NgOptimizedImage, WhereToWatchComponent, ScheduleNoteBadgeComponent, ShareButtonComponent, EpisodeReactionsComponent],
  templateUrl: './media-info.component.html',
  styleUrl: './media-info.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MediaInfoComponent implements OnInit {
  private mediaInfoService = inject(MediaInfoService);
  private mediaTimeService = inject(MediaTimeService);
  private scheduleNoteService = inject(ScheduleNoteService);
  protected scheduleNote = signal<ScheduleNote | null>(null);
  private destroyRef = inject(DestroyRef);
  public mediaDetails = signal<MediaInfo>({} as MediaInfo);
  private error = signal<string>('');
  private route = inject(ActivatedRoute);
  protected isLoading = signal<boolean>(false);

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
          this.isLoading.set(true);
          return this.mediaInfoService.getMedia();
        })
      )
      .subscribe({
        next: (data) => {
          this.mediaDetails.set(data);
          this.isLoading.set(false);
          this.mediaTimeService.initializeTimerFromMediaInfo(data);
        },
        error: (err) =>
          this.error.set(err.message || 'An error occured info component'),
      });
  }
}
