import { Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { MediaInfo } from '../../../../models/media.info.model';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TimeRemaining } from '../../../../models/time-remaining.model';
import { MediaTimeService } from '../../../../services/media-time.service';
import { MatIcon } from '@angular/material/icon';
@Component({
  selector: 'app-hero-carousel',
  imports: [NgOptimizedImage, CommonModule, MatIcon],
  templateUrl: './hero-carousel.component.html',
  styleUrl: './hero-carousel.component.css',
})
export class HeroCarouselComponent implements OnInit {
  mediaList = input.required<MediaInfo[]>();
  currentIndex = signal<number>(0);
  currentMedia = computed(() => this.mediaList()[this.currentIndex()]);
  isAnimating = signal<boolean>(false);
  destroyRef = inject(DestroyRef);
  mediaTimeService = inject(MediaTimeService);
  
  timeRemaining = computed(() => {
    const media = this.currentMedia();
    return media ? this.mediaTimeService.getTimeRemaining(media.id) || {} as TimeRemaining : {} as TimeRemaining;
  });
  
  ngOnInit(): void {
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
    if (newIndex < 0){
      newIndex = this.mediaList().length - 1;
    }
    this.goToSlide(newIndex);
  }
  
  trimWithDots(text: string, limit: number): string {
    if (text.length <= limit) return text;
    return text.slice(0, limit) + '...';
  }
}