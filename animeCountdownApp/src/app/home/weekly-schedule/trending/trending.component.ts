import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SkeletonModule } from 'primeng/skeleton';
import { HeroCarouselComponent } from './hero-carousel/hero-carousel.component';
import { MediaInfo } from '../../../models/media.info.model';
import { MediaInfoService } from '../../../services/media.info.service';

@Component({
  selector: 'app-trending',
  imports: [CommonModule, SkeletonModule, HeroCarouselComponent],
  templateUrl: './trending.component.html',
  styleUrl: './trending.component.css',
})
export class TrendingComponent implements OnInit {
  media = signal<MediaInfo[]>([]);
  error = signal<string>('');
  isLoading = signal(true);
  private mediaInfoService = inject(MediaInfoService);
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    const subscription = this.mediaInfoService
      .getTrendingMedia(this.error())
      .subscribe({
        next: (media) => {
          if (media != undefined) {
            this.media.set(media);

            if (media.length > 0 && media[0].banner) {
              const img = new Image();
              img.src = media[0].banner;
              img.fetchPriority = 'high';
            }
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
}
