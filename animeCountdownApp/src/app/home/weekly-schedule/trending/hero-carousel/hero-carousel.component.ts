import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
  computed,
  input,
} from '@angular/core';
import { MediaInfo } from '../../../../models/media.info.model';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TimeRemaining } from '../../../../models/time-remaining.model';
import { MediaTimeService } from '../../../../services/media-time.service';
import { MediaInfoService } from '../../../../services/media.info.service';
import { MatIcon } from '@angular/material/icon';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
  selector: 'app-hero-carousel',
  imports: [NgOptimizedImage, CommonModule, MatIcon, SkeletonModule],
  templateUrl: './hero-carousel.component.html',
  styleUrl: './hero-carousel.component.css',
})
export class HeroCarouselComponent implements OnInit {
  // Internal state
  mediaList = signal<MediaInfo[]>([]);
  error = signal<string>('');
  isLoading = signal(true);
  currentIndex = signal<number>(0);
  isAnimating = signal<boolean>(false);

  // Computed values
  currentMedia = computed(() =>
    this.mediaList().length > 0 ? this.mediaList()[this.currentIndex()] : null
  );
  timeRemaining = computed(() => {
    const media = this.currentMedia();
    return media
      ? this.mediaTimeService.getTimeRemaining(media.id) ||
          ({} as TimeRemaining)
      : ({} as TimeRemaining);
  });

  // Services
  private mediaInfoService = inject(MediaInfoService);
  mediaTimeService = inject(MediaTimeService);
  destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.loadTrendingMedia();
  }

  private loadTrendingMedia(): void {
    const subscription = this.mediaInfoService
      .getTrendingMedia(this.error())
      .subscribe({
        next: (media) => {
          if (media != undefined) {
            this.mediaList.set(media);
            if (media.length > 0 && media[0].banner) {
              const img = new Image();
              img.src = media[0].banner;
              img.fetchPriority = 'high';
            }
            this.initializeCarousel();
          }
          this.isLoading.set(false);
        },
        error: (error) => {
          this.error.set(error);
          console.log(error);
          this.isLoading.set(false);
        },
      });

    this.destroyRef.onDestroy(() => {
      subscription.unsubscribe();
    });
  }

  private initializeCarousel(): void {
    setTimeout(() => {
      const mediaItems = this.mediaList();
      if (mediaItems && mediaItems.length > 0) {
        const initialMedia = mediaItems[0];
        if (initialMedia) {
          this.mediaTimeService.initializeTimerFromMediaInfo(initialMedia);
        }
      }

      interval(10000)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => this.nextSlide());
    }, 100);
  }

  goToSlide(index: number) {
    if (this.isAnimating()) return;
    this.isAnimating.set(true);
    this.currentIndex.set(index);
    const newMedia = this.mediaList()[index];
    if (newMedia) {
      this.mediaTimeService.initializeTimerFromMediaInfo(newMedia);
    }
    setTimeout(() => {
      this.isAnimating.set(false);
    }, 600);
  }

  nextSlide() {
    const newIndex = (this.currentIndex() + 1) % this.mediaList().length;
    this.goToSlide(newIndex);
  }

  previousSlide() {
    let newIndex = (this.currentIndex() - 1) % this.mediaList().length;
    if (newIndex < 0) {
      newIndex = this.mediaList().length - 1;
    }
    this.goToSlide(newIndex);
  }

  trimWithDots(text: string, limit: number): string {
    if (text.length <= limit) return text;
    return text.slice(0, limit) + '...';
  }
}
