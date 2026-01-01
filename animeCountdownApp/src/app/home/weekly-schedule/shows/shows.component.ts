import {
  Component,
  DestroyRef,
  inject,
  input,
  OnInit,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { ShowComponent } from './show/show.component';
import { CommonModule } from '@angular/common';
import { SkeletonModule } from 'primeng/skeleton';
import { Media } from '../../../models/schedule.model';
import { ScheduleService } from '../../../services/schedule.service';
import { MediaSkeletonComponent } from "../../shared-home/media-skeleton/media-skeleton.component";
import { MediaStoreService } from '../../../services/media-store.service';
import { timer, interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { switchMap } from 'rxjs/operators';

type RenderPriority = 'high' | 'medium' | 'low';

@Component({
  selector: 'app-shows',
  imports: [ShowComponent, CommonModule, SkeletonModule, MediaSkeletonComponent],
  templateUrl: './shows.component.html',
  styleUrl: './shows.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShowsComponent implements OnInit {
  weekDay = input<string>('');
  renderPriority = input<RenderPriority>('low');

  media = signal<Media[]>([]);
  isLoading = signal(false);
  error = signal<string>('');

  private scheduleService = inject(ScheduleService);
  private destroyRef = inject(DestroyRef);
  private mediaStoreService = inject(MediaStoreService);

  // Check if this is the "Airing Soon" tab
  isAiringSoon = computed(() => this.weekDay() === 'soon');

  // Get active media use store for "Airing Soon", direct filtering for weekdays
  activeMedia = computed(() => {
    if (this.isAiringSoon()) {
      // Use store for "Airing Soon" - automatically pulls from queue
      return this.mediaStoreService.getDisplayMedia(this.weekDay());
    } else {
      // For weekdays: just keep all media (cards will handle their own visibility)
      return this.media();
    }
  });

  ngOnInit(): void {
    if (this.isAiringSoon()) {
      // "Airing Soon" tab: use store with polling
      this.loadAndStoreMedia();

      // Poll for new data every 10 minutes to refresh the store
      interval(10 * 60 * 1000) // 10 minutes
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => {
          this.loadAndStoreMedia();
        });
    } else {
      // Weekday tabs: simple load, no store, no polling
      this.loadMedia();
    }
  }

  // Used by "Airing Soon" - loads into store
  private loadAndStoreMedia(): void {
    this.isLoading.set(true);

    const subscription = this.scheduleService
      .renderMedia(this.weekDay(), this.error())
      .subscribe({
        next: (media) => {
          if (media != undefined) {
            // Add media to the store (will maintain priority queue)
            this.mediaStoreService.addMediaToStore(this.weekDay(), media);
          }
        },
        error: (error) => {
          console.log(error);
          this.error.set(error.message);
        },
        complete: () => {
          this.isLoading.set(false);
        },
      });

    this.destroyRef.onDestroy(() => {
      subscription.unsubscribe();
    });
  }

  // Used by weekday tabs - poll every 60 seconds for fresh data
  private loadMedia(): void {
    this.isLoading.set(true);

    // Start immediately (0ms), then poll every 60 seconds (60000ms)
    const subscription = timer(0, 60000)
      .pipe(
        switchMap(() => this.scheduleService.renderMedia(this.weekDay().toUpperCase(), this.error()))
      )
      .subscribe({
        next: (media) => {
          if (media != undefined) {
            this.media.set(media);
          }
          this.isLoading.set(false);
        },
        error: (error) => {
          console.log(error);
          this.error.set(error.message);
          this.isLoading.set(false);
        },
      });

    this.destroyRef.onDestroy(() => {
      subscription.unsubscribe();
    });
  }

  getShowPriority(index: number): RenderPriority {
    if (this.renderPriority() === 'high') {
      return index < 8 ? 'high' : 'medium';
    } else if (this.renderPriority() === 'medium') {
      return index < 4 ? 'medium' : 'low';
    } else {
      return 'low';
    }
  }
}
