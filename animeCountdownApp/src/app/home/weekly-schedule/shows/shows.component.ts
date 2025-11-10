import {
  Component,
  DestroyRef,
  inject,
  input,
  OnInit,
  signal,
  computed,
} from '@angular/core';
import { ShowComponent } from './show/show.component';
import { CommonModule } from '@angular/common';
import { SkeletonModule } from 'primeng/skeleton';
import { Media } from '../../../models/schedule.model';
import { ScheduleService } from '../../../services/schedule.service';
import { MediaSkeletonComponent } from "../../shared-home/media-skeleton/media-skeleton.component";
import { MediaStoreService } from '../../../services/media-store.service';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

type RenderPriority = 'high' | 'medium' | 'low';

@Component({
  selector: 'app-shows',
  imports: [ShowComponent, CommonModule, SkeletonModule, MediaSkeletonComponent],
  templateUrl: './shows.component.html',
  styleUrl: './shows.component.css',
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

  // Get active media - use store for "Airing Soon", direct filtering for weekdays
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

            // Preload high priority images from display media
            const displayMedia = this.mediaStoreService.getDisplayMedia(this.weekDay());
            if (this.renderPriority() === 'high') {
              this.preloadImages(displayMedia.slice(0, 6));
            } else if (this.renderPriority() === 'medium') {
              this.preloadImages(displayMedia.slice(0, 3));
            }
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

  // Used by weekday tabs - simple direct load
  private loadMedia(): void {
    this.isLoading.set(true);

    const subscription = this.scheduleService
      .renderMedia(this.weekDay(), this.error())
      .subscribe({
        next: (media) => {
          if (media != undefined) {
            this.media.set(media);

            // Preload high priority images
            if (this.renderPriority() === 'high') {
              this.preloadImages(media.slice(0, 6));
            } else if (this.renderPriority() === 'medium') {
              this.preloadImages(media.slice(0, 3));
            }
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

  getShowPriority(index: number): RenderPriority {
    if (this.renderPriority() === 'high') {
      return index < 8 ? 'high' : 'medium';
    } else if (this.renderPriority() === 'medium') {
      return index < 4 ? 'medium' : 'low';
    } else {
      return 'low';
    }
  }

  // Preload important images
  private preloadImages(media: Media[]): void {
    for (const show of media) {
      if (show.cover_Image_Url) {
        const img = new Image();
        img.src = show.cover_Image_Url;
      }
    }
  }
}
