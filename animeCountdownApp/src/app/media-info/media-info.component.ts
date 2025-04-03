// media-info.component.ts
import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
  computed,
} from '@angular/core';
import { MediaInfoService } from '../services/media.info.service';
import { MediaTimeService } from '../services/media-time.service';
import { MediaInfo } from '../models/media.info.model';
import { TimeRemaining } from '../models/time-remaining.model';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-media-info',
  imports: [CommonModule],
  templateUrl: './media-info.component.html',
  styleUrl: './media-info.component.css',
})
export class MediaInfoComponent implements OnInit {
  private mediaInfoService = inject(MediaInfoService);
  private mediaTimeService = inject(MediaTimeService);
  private destroyRef = inject(DestroyRef);
  public mediaDetails = signal<MediaInfo | undefined>(undefined);
  private error = signal<string>('');
  private route = inject(ActivatedRoute);

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
          this.mediaInfoService.getMediaId(id);
          return this.mediaInfoService.getMedia();
        })
      )
      .subscribe({
        next: (data) => {
          this.mediaDetails.set(data);
          this.mediaTimeService.initializeTimerFromMediaInfo(data);
          console.log(data);
        },
        error: (err) =>
          this.error.set(err.message || 'An error occured info component'),
      });
  }
}
